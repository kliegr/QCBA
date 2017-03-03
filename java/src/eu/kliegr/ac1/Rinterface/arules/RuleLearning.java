/*
 * Monotonicity Exploiting Association Rule Classification (MARC)
 *
 *     Copyright (C)2014-2017 Tomas Kliegr
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Affero General Public License as
 *     published by the Free Software Foundation, either version 3 of the
 *     License, or (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Affero General Public License for more details.
 *
 *     You should have received a copy of the GNU Affero General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package eu.kliegr.ac1.Rinterface.arules;

import eu.kliegr.ac1.PruningEnum;
import eu.kliegr.ac1.RuleLearningAlgEnum;
import eu.kliegr.ac1.RuleLearningConfig;
import eu.kliegr.ac1.data.AttributeValue;
import eu.kliegr.ac1.data.DataTable;
import eu.kliegr.ac1.data.parsers.CSVparser;
import eu.kliegr.ac1.rule.Data;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Collection;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;
import java.util.logging.Logger;

public class RuleLearning {

    private final static Logger LOGGER = Logger.getLogger(RuleLearning.class.getName());

    /**
     *
     * @param conf
     * @return
     * @throws Exception
     */
    public static String formatrCBArscript(RuleLearningConfig conf) throws Exception {
        StringBuilder Rcode = new StringBuilder();
        Rcode.append("library(methods) # this has to be explicitly loaded when executing via Rscript \n");
        Rcode.append("library(arules) # load lib + \n");
        Rcode.append("library(rCBA) # load lib + \n");

        Rcode.append("train <- read.csv(\"").append(conf.getDataPath()).append("\",header=TRUE, check.names=FALSE, sep = \"").append(conf.getCSVSeparator()).append("\") # load csv + \n");
        //this sets empty strings to NA, which causes the respective items not to be created
        //if this line is missing, there will be items created for emtpy value
        //this may create problems if the corresponding attribute is numeric
        Rcode.append("train[train==\"\"] <-NA \n");
        if (conf.getIDcolumnName() != null) {
            Rcode.append("train <- subset( train, select = -c (").append(conf.getIDcolumnName()).append(") ) # remove id column\n");
        }
        Rcode.append("output <- rCBA::build(train)\n");
        Rcode.append("rules <- output$model\n");
        Rcode.append("write.csv(rules, \"").append(conf.getOutputPath()).append("\", row.names=TRUE,quote = TRUE)\n");
        Rcode.append("cat(paste(\"confidence,support,maxlen\n\",output$confidence,\",\",output$support,\",\",output$maxlen),file=\"").append(conf.getOutputPath("opt")).append("\")");
        return Rcode.toString();
    }

    /**
     *
     * @param conf
     * @return
     * @throws Exception
     */
    public static Data learnRules(RuleLearningConfig conf) throws Exception {
        LOGGER.log(Level.INFO, "Input CSV {0}", conf.getDataPath());
        Data rmi = CSVparser.parseFromCSVwithHeader(conf.getDataPath(), conf.getTargetAttribute(), conf.getAttributeType(), conf.getIDcolumnName(), conf.getCSVSeparator());
        DataTable train = rmi.getDataTable();


        //sets support threshold so that there are maximum x one itemsets for the given threshold
        float minSup = train.getMinSupportThreshold(1000);
        Collection<AttributeValue> targetAttValues = train.getTargetAttribute().getAllValues();
        String targetAttName = train.getTargetAttribute().getName();

        File Rscript;
        String Rcode;

        //to make sure that the script has unique name on disk to allow parallel execution
        String scriptID = Integer.toString(ThreadLocalRandom.current().nextInt(1, 1000000));
        String scriptName;
        if (conf.getAlgorithm() == RuleLearningAlgEnum.arules | conf.getAlgorithm() == RuleLearningAlgEnum.arulesrCBApruning) {
            scriptName = "temp/arules_" + scriptID + ".R";
            Rcode = formatArulesRscript(conf, targetAttValues, targetAttName, minSup, conf.getAlgorithm());
        } else if (conf.getAlgorithm() == RuleLearningAlgEnum.rCBAauto) {
            scriptName = "temp/rcba_" + scriptID + ".R";
            Rcode = formatrCBArscript(conf);
        } else if (conf.getAlgorithm() == RuleLearningAlgEnum.arcCBA) {
            scriptName = "temp/arccba_" + scriptID + ".R";
            Rcode = formatARCrscript(conf);
        } else {
            throw new Exception("Unsupported algorithm");
        }

        Rscript = new File(scriptName);
        final OutputStream os = new FileOutputStream(Rscript);
        final PrintStream printStream = new PrintStream(os);
        printStream.print(Rcode);
        printStream.close();
        execRscript(Rscript.getAbsolutePath(), conf.getOutputPath());
        return rmi;
    }

    private static String formatArulesRscript(RuleLearningConfig conf, Collection<AttributeValue> targetAttValues, String targetAttName, float minSup, RuleLearningAlgEnum alg) {

        StringBuilder Rcode = new StringBuilder();

        Rcode.append("library(methods) # this has to be explicitly loaded when executing via Rscript \n");
        Rcode.append("library(R.utils) # load lib + \n");
        Rcode.append("library(arules) # load lib + \n");
        if (alg == RuleLearningAlgEnum.arulesrCBApruning) {
            Rcode.append("library(rCBA)\n");
        }

        Rcode.append("train <- read.csv(\"").append(conf.getDataPath()).append("\",header=TRUE, check.names=FALSE, sep = \"").append(conf.getCSVSeparator()).append("\") # load csv + \n");

        //this sets empty strings to NA, which causes the respective items not to be created
        //if this line is missing, there will be items created for emtpy value
        //this may create problems if the corresponding attribute is numeric
        Rcode.append("train[train==\"\"] <-NA \n");
        if (conf.getIDcolumnName() != null) {
            Rcode.append("train <- subset( train, select = -c (").append(conf.getIDcolumnName()).append(") ) # remove id column\n");
        }

        //if id target columns are nominal, the following will raise an error
        //        Rcode.append("train  <- subset(train, select = -c ("+IDcolumnName+")) #remove id and target cols \n" );
        //        Rcode.append("train  <- subset(train, select = -c ("+targetColName+")) #remove id and target cols \n" );
        //Rcode.append("train <- sapply(train,as.factor) # convert\n");
        //Rcode.append("train <- data.frame(train,check.names=FALSE)\n");
        Rcode.append("txns <- as(train,\"transactions\")\n");

        Rcode.append("conf=").append(conf.getMinConfidence()).append("\n");
        LOGGER.log(Level.INFO, "Ignoring set minSup, using autodetected value:{0}", minSup);
        Rcode.append("confEpsilon=").append(conf.getConfEpsilon()).append("\n");
        Rcode.append("maxExecTimeIter=").append(conf.getMaxExecTimeIter()).append("\n");
        Rcode.append("maxExecTimeTotal=").append(conf.getMaxExecTimeTotal()).append("\n");
        Rcode.append("support=").append(minSup).append("\n");
        Rcode.append("minlen=").append(conf.getMinLen()).append("\n");
        Rcode.append("maxlen=").append(conf.getMaxLen()).append("\n");
        Rcode.append("targetRuleCount=").append(conf.getTargetRuleCount()).append("\n");

        StringBuilder arulesLine = new StringBuilder();
        arulesLine.append("rules <- apriori(txns, parameter = list(confidence = conf");
        arulesLine.append(", support= support");
        arulesLine.append(", minlen= minlen");
        arulesLine.append(", maxlen= maxlen");
        arulesLine.append("),appearance = list(rhs = c(");

        int counter = 0;
        for (AttributeValue val : targetAttValues) {
            arulesLine.append("\"");
            arulesLine.append(targetAttName);
            arulesLine.append("=");
            arulesLine.append(val.getValue());
            arulesLine.append("\"");
            if (++counter < targetAttValues.size()) {
                arulesLine.append(", ");
            }
        }
        arulesLine.append("),default=\"lhs\"));");

        Rcode.append("iteration_time_limit_exceeded=0\n"
                + "flag=TRUE\n "
                + "start.time <- Sys.time()\n"
                + "lastrulecount <- -1\n"
                + "while(flag)\n"
                + "{\n"
                + "    tryCatch(\n"
                + "    {\n"
                + "        rules <- evalWithTimeout({##arulesline##},timeout=maxExecTimeIter, onTimeout=\"error\");\n"
                + "        rulecount=length(rules)\n"
                + "        print(paste(\"Rule count:  \",rulecount))\n"
                + "        if (rulecount >= targetRuleCount)\n"
                + "        {\n"
                + "            flag<<-FALSE\n"
                + "            print(paste(\"Target rule count satisfied:  \",targetRuleCount))\n"
                + "        }\n"
                + "        else{\n"
                + "           exectime = Sys.time() -start.time \n"
                + "if (exectime > maxExecTimeTotal)\n"
                + "             {\n"
                + "               print(paste(\"Max execution time exceeded:  \",maxlen))\n"
                + "               flag<<-FALSE\n"
                + "            }\n"
                + "            else if (maxlen < dim(train)[2]-1 & lastrulecount!=rulecount)\n"
                + "            {                \n"
                + "                maxlen<<-maxlen+1\n"
                + "lastrulecount  <- rulecount\n"
                + "                print(paste(\"Increasing maxlen to:  \",maxlen))\n"
                + "            }\n"
                + "            else if (conf > confEpsilon){                \n"
                + "                conf<<-conf-confEpsilon\n"
                + "                print(paste(\"Decreasing confidence to:  \",conf))\n"
                + "lastrulecount  <-  -1\n"
                + "            }\n"
                + "            else{\n"
                + "                print(\"All options exhausted\")\n"
                + "                flag<<-FALSE\n"
                + "            }\n"
                + "        }\n"
                + "\n"
                + "    }, error= function(err)\n"
                + "    {\n"
                + "        print(paste(\"Individual exec time exceeded:  \",err))\n"
                + " iteration_time_limit_exceeded=iteration_time_limit_exceeded+1\n"
                + "exectime = Sys.time() -start.time \n"
                + "             if (exectime > maxExecTimeTotal)\n"
                + "        {\n"
                + "          print(paste(\"Max execution time exceeded:  \",maxlen))\n"
                + "          flag=FALSE\n"
                + "        } \n else if (maxlen > 1)\n"
                + "        {\n"
                + "            maxlen<<-maxlen-1\n"
                + "            print(paste(\"Decreasing maxlen to:  \",maxlen))\n"
                + "        }\n"
                + "        else{\n"
                + "            print(\"All options exhausted - returning no rules\")\n"
                + "            flag<<-FALSE\n"
                + "        }        \n"
                + "    })\n"
                + "}\n");
        Rcode.append("exectime = Sys.time() -start.time \n");
        if (alg == RuleLearningAlgEnum.arulesrCBApruning) {
            Rcode.append("rules <- rCBA::pruning(train, rules, method=\"m2cba\") \n");
        }
        Rcode.append("write.csv(as(rules,\"data.frame\"), \"").append(conf.getOutputPath()).append("\", row.names=TRUE,quote = TRUE)\n");
        Rcode.append("cat(paste(\"confidence,support,maxlen,totalexectime,number of times local exec time exceeded\n\",conf,\",\",support,\",\",maxlen,\",\",exectime,\",\",iteration_time_limit_exceeded),file=\"").append(conf.getOutputPath("opt")).append("\")");
        return Rcode.toString().replace("##arulesline##", arulesLine);
    }

    private static void execRscript(String scriptPath, String outputPath) throws IOException, Exception, InterruptedException {
        LOGGER.log(Level.INFO, "Started executing R script:{0}", scriptPath);

        Process p = Runtime.getRuntime().exec("Rscript " + scriptPath);
        BufferedReader reader
                = new BufferedReader(new InputStreamReader(p.getInputStream()));
        while ((reader.readLine()) != null) {
        }
        p.waitFor();

        int exitValue = p.exitValue();

        if (exitValue != 0) {
            LOGGER.log(Level.INFO, "R exit value {0} (NOT OK)", exitValue);
            throw new Exception("R program execution failed. Program is at " + scriptPath);
        } else {
            LOGGER.log(Level.INFO, "R exit value {0} (OK)", exitValue);
        }

        LOGGER.log(Level.INFO, "Rules written to {0}", outputPath);
    }

    private static void _execRscript(String scriptPath, String outputPath) throws IOException, Exception, InterruptedException {

        Process p = Runtime.getRuntime().exec("Rscript " + scriptPath);
        p.waitFor();
        int exitValue = p.exitValue();

        if (exitValue != 0) {
            LOGGER.log(Level.INFO, "R exit value {0} (NOT OK)", exitValue);
            throw new Exception("R program execution failed. Program is at " + scriptPath);
        } else {
            LOGGER.log(Level.INFO, "R exit value {0} (OK)", exitValue);
        }

        LOGGER.log(Level.INFO, "Rules written to {0}", outputPath);
    }

    private static String formatARCrscript(RuleLearningConfig conf) {
        StringBuilder Rcode = new StringBuilder();
        Rcode.append("library(arc) # load lib + \n");
        String idColumn;
        if (conf.getIDcolumnName() == null) {
            idColumn = "";
        } else {
            idColumn = "\"" + conf.getIDcolumnName() + "\"";
        }
        String rulelearning_options = "list(target_rule_count=" + conf.getTargetRuleCount() + ")";
        String pruning_options;
        if (conf.getPruning() == PruningEnum.CBA) {
            pruning_options = "TRUE";
        } else {
            pruning_options = "FALSE";
        }
        pruning_options = "list(default_rule_pruning=" + pruning_options + ")";
        Rcode.append("cbaCSV(path=\"").append(conf.getDataPath()).append("\",classatt=\"").append(conf.getTargetAttribute()).append("\",idcolumn=").append(idColumn).append(",rulelearning_options=").append(rulelearning_options).append(",pruning_options=").append(pruning_options).append(",outpath=\"").append(conf.getOutputPath()).append("\")");
        return Rcode.toString();

    }

    private RuleLearning() {
    }

}
