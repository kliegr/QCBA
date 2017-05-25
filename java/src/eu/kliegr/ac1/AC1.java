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
package eu.kliegr.ac1;

import eu.kliegr.ac1.Rinterface.arules.RuleLearning;
import eu.kliegr.ac1.Rinterface.discretization.AttributeDiscretization;
import eu.kliegr.ac1.Rinterface.discretization.DiscretizeWithR;
import eu.kliegr.ac1.data.parsers.CSVparser;
import eu.kliegr.ac1.performance.StopWatches;
import eu.kliegr.ac1.pipeline.Experimentator;
import eu.kliegr.ac1.preprocess.generatefolds.GenerateFolds;
import eu.kliegr.ac1.rule.Data;
import eu.kliegr.ac1.rule.PruneRules;
import eu.kliegr.ac1.rule.Rule;
import eu.kliegr.ac1.rule.TestRules;
import eu.kliegr.ac1.rule.extend.ExtendRuleConfig;
import eu.kliegr.ac1.rule.extend.ExtendRules;
import eu.kliegr.ac1.rule.parsers.GUHASimplifiedParser;
import eu.kliegr.ac1.rule.parsers.GenericRuleParser;
import eu.kliegr.ac1.utils.EvaluateCrossvalidation;
import eu.kliegr.ac1.utils.GenerateCSVHeader;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;


public class AC1 {
private final static Logger LOGGER = Logger.getLogger(AC1.class.getName()); 
    private static final String usage = "*** Instructions *** \n 1] Run with one argument - path to config";
   
    /**
     *
     */
    public static double JAVA_VERSION = getVersion();

    static double getVersion() {
        String version = System.getProperty("java.version");
        int pos = 0, count = 0;
        for (; pos < version.length() && count < 2; pos++) {
            if (version.charAt(pos) == '.') {
                count++;
            }
        }
        LOGGER.log(Level.INFO, "Running Java {0}", version);
        return Double.parseDouble(version.substring(0, pos - 1));
    }

    /**
     *
     * @throws Exception
     */
    public static void cleanup() throws Exception {
        Rule.resetERIDcounter();
        
        
    }

    /**
     *
     * @param args
     * @throws IOException
     * @throws Exception
     */
    public static void main(String[] args) throws IOException, Exception {
        if (JAVA_VERSION < 1.8) {
            LOGGER.info("Only Java 1.8+ supported");
            return;
        }

        String path;
        Usage mode;
        if (args.length == 1) {
            try {
                path = args[0];
                mode = detectUsageType(path);                
                LOGGER.log(Level.INFO, "Running in {0} mode", mode);
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }
        } else {
            return;
        }
        //check successful, now read configuration    
        StopWatches performance = new StopWatches();
        performance.startStopWatch("Total");
        if (null != mode) switch (mode) {
            case prune:{
                performance.startStopWatch("Config load");
                PruneConfig conf = new PruneConfig(path);
                performance.stopStopWatch("Config load");
                performance.startStopWatch("Load data");
                Data data = CSVparser.parseFromCSVwithHeader(conf.getDataPath(), conf.getTargetAttribute(), conf.getAttributeType(), conf.getIDcolumnName(), conf.getCSVSeparator());
                performance.stopStopWatch("Load data");
                performance.startStopWatch("Load rules");
                PruneRules pruneRulesObj = new PruneRules(GenericRuleParser.parseFileForRules(conf.getRulesPath(),data), conf.getRuleComparator(), conf.getPruningType());
                performance.stopStopWatch("Load rules");
                performance.startStopWatch("Sort rules");
                pruneRulesObj.sortRules();
                performance.stopStopWatch("Sort rules");
                performance.startStopWatch("Count rules");
                int origRuleCount = pruneRulesObj.getRules().size();
                performance.stopStopWatch("Count rules");
                performance.startStopWatch("Prune rules");
                pruneRulesObj.pruneRules(false);
                performance.stopStopWatch("Prune rules");
                performance.startStopWatch("Save rules");
                GUHASimplifiedParser.saveRules(pruneRulesObj.getRules(), conf.getOutputPath());
                performance.stopStopWatch("Save rules");
                performance.stopStopWatch("Total");
                pruneRulesObj.saveSummary(origRuleCount, conf.getOutputSummaryPath(), performance);
                    break;
                }
            case extend:{
                performance.startStopWatch("Config load");
                ExtendConfig conf = new ExtendConfig(path);
                performance.stopStopWatch("Config load");
                performance.startStopWatch("Load data");
                Data data = CSVparser.parseFromCSVwithHeader(conf.getDataPath(), conf.getTargetAttribute(), conf.getAttributeType(), conf.getIDcolumnName(), conf.getCSVSeparator());
                performance.stopStopWatch("Load data");
                performance.startStopWatch("Load rules");                
                ExtendRules extendRulesObj = new ExtendRules(GenericRuleParser.parseFileForRules(conf.getRulesPath(),data), conf.getRuleComparator(), conf.getExtendType(),conf.getExtendRuleConfig(), data);
                performance.stopStopWatch("Load rules");
                performance.startStopWatch("Sort rules");
                extendRulesObj.sortRules();
                performance.stopStopWatch("Sort rules");
                performance.startStopWatch("Extend rules");
                extendRulesObj.extendRules(conf.isContinuousPruningEnabled(),conf.isFuzzificationEnabled(),conf.isPostPruningEnabled());
                performance.stopStopWatch("Extend rules");
                if (conf.isAnnotationEnabled())
                {
                    performance.startStopWatch("Annotate rules");
                    extendRulesObj.annotateRules();
                    performance.stopStopWatch("Annotate rules");
                }       performance.startStopWatch("Save rules");
                GUHASimplifiedParser.serializeRules(extendRulesObj.getSeedRules(), conf.getOutputSeedRulesPath());
                GUHASimplifiedParser.serializeRules(extendRulesObj.getExtendedRules(), conf.getOutputExtendedRulesPath()    );
                GUHASimplifiedParser.saveRules(extendRulesObj.getExtendedRules(), conf.getOutputPath());
                performance.stopStopWatch("Save rules");
                performance.stopStopWatch("Total");
                extendRulesObj.saveSummary(conf.getOutputSummaryPath(), performance);
                    break;
                }
            case test:{
                performance.startStopWatch("Config load");
                TestConfig conf = new TestConfig(path);
                performance.stopStopWatch("Config load");
                performance.startStopWatch("Load data");
                Data data = CSVparser.parseFromCSVwithHeader(conf.getDataPath(), conf.getTargetAttribute(), conf.getAttributeType(), conf.getIDcolumnName(), conf.getCSVSeparator());
                performance.stopStopWatch("Load data");
                performance.startStopWatch("Count rows");
                int totalTransactions = data.getDataTable().getLoadedTransactionCount();
                performance.stopStopWatch("Count rows");
                performance.startStopWatch("Load rules");
                TestRules testRulesObj = new TestRules(GenericRuleParser.parseFileForRules(conf.getRulesPath(),data), conf.getRuleComparator(),data);
                performance.stopStopWatch("Load rules");
                performance.startStopWatch("Sort rules");
                testRulesObj.sortRules();
                performance.stopStopWatch("Sort rules");
                performance.startStopWatch("Classify data");
                testRulesObj.classifyData(conf.getTestingType());
                performance.stopStopWatch("Classify data");
                performance.startStopWatch("Save result");
                testRulesObj.saveDebugResult(conf.getOutputPath());
                performance.stopStopWatch("Save result");
                performance.stopStopWatch("Total");
                testRulesObj.saveSummary(conf.getOutputSummaryPath(), totalTransactions, performance);
                    break;
                }
            case discretize:{
                performance.startStopWatch("Config load");
                DiscretizeConfig conf = new DiscretizeConfig(path);
                performance.stopStopWatch("Config load");
                performance.startStopWatch("Run discretization in R");
                //TODO: pass the config object instead
                AttributeDiscretization[] mapping = DiscretizeWithR.executeRdiscretization(conf.getDataPath(), conf.getAttributeType(), conf.getIDcolumnName(), conf.getTargetAttribute(), conf.getCSVSeparator());
                performance.stopStopWatch("Run discretization in R");
                performance.startStopWatch("Convert data table");
                //TODO: pass the config object instead
                DiscretizeWithR.convertCSVwithHeader(conf.getDataPath(), conf.getOutputDataPath(), mapping, conf.getCSVSeparator(), conf.getMissingValueTreatment());
                if (conf.getTestDataPath()!=null)
                {
                    DiscretizeWithR.convertCSVwithHeader(conf.getTestDataPath(), conf.getTestOutputDataPath(), mapping, conf.getCSVSeparator(), conf.getMissingValueTreatment());
                }       performance.stopStopWatch("Convert data table");
                performance.stopStopWatch("Total");
                    break;
                }
            case generatefolds:{
                performance.startStopWatch("Config load");
                GenerateFoldsConfig conf = new GenerateFoldsConfig(path);
                performance.stopStopWatch("Config load");
                performance.startStopWatch("Run fold generation");
                GenerateFolds.generateFolds(conf);
                performance.stopStopWatch("Run fold generation");
                performance.stopStopWatch("Total");
                    break;
                }
            case learnrules:{
                performance.startStopWatch("Config load");
                RuleLearningConfig conf = new RuleLearningConfig(path);
                performance.stopStopWatch("Config load");
                performance.startStopWatch("Run fold generation");
                RuleLearning.learnRules(conf);
                performance.stopStopWatch("Run fold generation");
                performance.stopStopWatch("Total");
                    break;
                }
            case generateheader:{                        
                performance.startStopWatch("Config load");
                GenerateCSVHeaderConfig conf = new GenerateCSVHeaderConfig(path);
                performance.stopStopWatch("Config load");
                performance.startStopWatch("Run generate header");
                GenerateCSVHeader.generateCSVHeader(conf);
                performance.stopStopWatch("Run generate header");
                performance.stopStopWatch("Total");
                    break;
                }
            case evaluatecrossvalidation:{
                performance.startStopWatch("Config load");
                EvaluateCrossvalidationConfig conf = new EvaluateCrossvalidationConfig(path);
                performance.stopStopWatch("Config load");
                performance.startStopWatch("Run generate header");
                EvaluateCrossvalidation.evaluateCrossvalidation(conf);
                performance.stopStopWatch("Run generate header");
                performance.stopStopWatch("Total");
                    break;
                }
            case pipeline:{
                performance.startStopWatch("Config load");
                PipelineConfig conf = new PipelineConfig(path);
                performance.stopStopWatch("Config load");
                performance.startStopWatch("Run pipeline");
                Experimentator.runPipeline(conf);
                performance.stopStopWatch("Run pipeline");
                performance.stopStopWatch("Total");
                    break;
                }
            default:
                break;
        }
    }

    private static Usage detectUsageType(String path) throws FileNotFoundException, IOException {
        LOGGER.log(Level.INFO, "Detecting usage type from {0}", path);

        InputStream input = new BufferedInputStream(
                new FileInputStream(path));
        Properties prop = new Properties();
        prop.loadFromXML(input);
        Usage mode = Usage.valueOf(prop.getProperty("Method"));
        input.close();
        return mode;
    }

}
