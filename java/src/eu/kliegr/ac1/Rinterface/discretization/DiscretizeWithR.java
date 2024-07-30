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
package eu.kliegr.ac1.Rinterface.discretization;

import eu.kliegr.ac1.data.AttributeType;
import eu.kliegr.ac1.data.parsers.CSVparser;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

public class DiscretizeWithR {

    private final static Logger LOGGER = Logger.getLogger(DiscretizeWithR.class.getName());


    /**
     *
     * @param path
     * @param outputPath
     * @param mapping
     * @param sep
     * @param missingValueTreatment
     * @throws FileNotFoundException
     * @throws Exception
     */
    public static void convertCSVwithHeader(String path, String outputPath, AttributeDiscretization[] mapping, String sep, MissingValueTreatmentEnum missingValueTreatment) throws FileNotFoundException, Exception {
        final OutputStream os = new FileOutputStream(outputPath);
        final PrintStream printStream = new PrintStream(os);

        LOGGER.log(Level.INFO, "Parsing csv file:{0}", path);

        final Stream<String> firstLine = Files.lines(Paths.get(path));
        printStream.println(firstLine.findFirst().get());

        final Stream<String> allLines = Files.lines(Paths.get(path));
        final Stream<String[]> tokenized = allLines.map((line) -> line.replaceAll(CSVparser.weirdCharacter, "").split(sep));
        tokenized.skip(1).forEach((items) -> {
            boolean skipThisEntry = false;
            for (int i = 0; i < items.length; i++) {
                items[i] = CSVparser.removeEnclosingQuotes(items[i]);
                items[i] = mapping[i].convert(items[i]);
                if (items[i].isEmpty()) {
                    if (missingValueTreatment == MissingValueTreatmentEnum.remove) {
                        skipThisEntry = true;
                    } else if (missingValueTreatment == MissingValueTreatmentEnum.replaceWithNAN) {
                        items[i] = "NAN";
                    }
                }
            }
            if (!skipThisEntry) {
                printStream.println(String.join(sep, items));
            }
        }
        );

        printStream.close();
        LOGGER.log(Level.INFO, "Result saved to file:{0}", outputPath);
    }

    /**
     *
     * @param name
     * @param path
     * @param sep
     * @return
     * @throws IOException
     */
    public static int getColPos(String name, String path, String sep) throws IOException {
        final Stream<String> firstLine = Files.lines(Paths.get(path));
        String[] colNames = firstLine.findFirst().get().replaceAll(CSVparser.weirdCharacter, "").split(sep);
        for (int i = 0; i < colNames.length; i++) {
            String curName = CSVparser.removeEnclosingQuotes(colNames[i]);
            if (curName.equals(name)) {
                return i;
            }
        }
        return -1;
    }

    /**
     *
     * @param datapath
     * @param attributeTypes
     * @param IDcolumnName
     * @param targetColName
     * @param sep
     * @return
     * @throws FileNotFoundException
     * @throws IOException
     * @throws InterruptedException
     */
    public static AttributeDiscretization[] executeRdiscretization(String datapath, ArrayList<AttributeType> attributeTypes, String IDcolumnName, String targetColName, String sep) throws FileNotFoundException, IOException, InterruptedException {

        //change data type if id column to nominal to avoid this column being discretized
        int IDcolumnPos = getColPos(IDcolumnName, datapath, sep);
        // R index is shifted by one
        int targetColPos = getColPos(targetColName, datapath, sep);
        if (targetColPos == -1) {
            throw new UnsupportedOperationException("Target column '" + targetColName + "' not found in data, using dataset : " + datapath);
        }

        int i = 0;
        ArrayList<AttributeDiscretization> discs = new ArrayList();
        for (AttributeType at : attributeTypes) {

            if (at == AttributeType.numerical) {
                if (targetColPos == i | IDcolumnPos == i) {
                    //no discretization
                    discs.add(new AttributeDiscretization());

                } else {
                    try {
                        // R index is shifted by one
                        AttributeDiscretization disc = interfaceWithRScript(datapath, i + 1, sep, targetColPos + 1);
                        discs.add(disc);
                    } catch (Exception e) {
                        LOGGER.log(Level.INFO, "Discretization for attribute {0}  failed, setting discretization to all", i);
                        discs.add(new AttributeDiscretization());
                    }
                }
            } else {
                //empty constructor means no discretization
                discs.add(new AttributeDiscretization());
            }
            //if the target attribute is nominal, it was not added by the previous cycle
            i++;
        }
        return discs.toArray(new AttributeDiscretization[discs.size()]);

    }

    private static AttributeDiscretization interfaceWithRScript(String datapath, int colpos, String sep, int targetColPos) throws FileNotFoundException, IOException, InterruptedException {
        String scriptID = Integer.toString(ThreadLocalRandom.current().nextInt(1, 1000000));
        String scriptName = "temp/discr_" + scriptID + ".R";
        String outputFileName = "temp/discr_" + scriptID + ".out";

        File Rscript = new File(scriptName);
        final OutputStream os = new FileOutputStream(Rscript);
        final PrintStream printStream = new PrintStream(os);

        StringBuilder Rcode = new StringBuilder();
        Rcode.append("sink(\"").append(outputFileName).append("\", append=FALSE, split=FALSE) # send to file\n");
        Rcode.append("library(discretization) # load lib + \n");
        Rcode.append("train <- read.csv(\"").append(datapath).append("\",header=TRUE, sep = \"").append(sep).append("\") # load csv + \n");
        Rcode.append("numeric <- subset( train, select = c (").append(colpos).append(",").append(targetColPos).append(") ) # store numeric columns\n");
        Rcode.append("numeric <- na.omit(numeric) # remove rows with missing data\n");
        Rcode.append("mdlp(numeric)$cutp\n");
        printStream.print(Rcode);
        printStream.close();
        Process p = Runtime.getRuntime().exec("Rscript " + Rscript);
        p.waitFor();

        int exitValue = p.exitValue();

        if (exitValue != 0) {
            LOGGER.log(Level.INFO, "R exit value {0} (NOT OK)", exitValue);
            throw new IOException("Discretization program execution failed. Program is at " + Rscript.getAbsolutePath());
        } else {
            LOGGER.log(Level.INFO, "R exit value {0} (OK)", exitValue);
        }

        AttributeDiscretization disc = parseDiscFile(outputFileName);
        return disc;
    }

    private static AttributeDiscretization parseDiscFile(String path) throws IOException {
        AttributeDiscretization discr = null;
        BufferedReader br = new BufferedReader(new FileReader(path));
        String thisRow;
        ArrayList<String> cutPoints = new ArrayList();
        while ((thisRow = br.readLine()) != null) {
            if (thisRow.startsWith("[[")) {
                cutPoints = new ArrayList();
            } else if (thisRow.trim().startsWith("[")) {
                String[] linePoints = thisRow.replaceFirst("\\s*\\[\\d+\\]\\s+", "").split("\\s+");
                List asList = Arrays.asList(linePoints);
                cutPoints.addAll(asList);
            } else if (thisRow.trim().isEmpty()) {
                discr = new AttributeDiscretization(cutPoints);
            }
        }
        br.close();
        if (discr == null) {
            throw new IOException("Discretization failed. Is R and discretization package installed?");
        }
        return discr;
    }

    private DiscretizeWithR() {
    }
}
