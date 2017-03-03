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
package eu.kliegr.ac1.preprocess.generatefolds;

import eu.kliegr.ac1.GenerateFoldsConfig;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author tomas
 */
public class GenerateFolds {

    private final static Logger LOGGER = Logger.getLogger(GenerateFolds.class.getName());

    /**
     *
     * @param config
     * @throws FileNotFoundException
     * @throws IOException
     */
    public static void generateFolds(GenerateFoldsConfig config) throws FileNotFoundException, IOException {
        PrintStream[] testPrintStream = new PrintStream[config.getFoldCount()];
        PrintStream[] trainPrintStream = new PrintStream[config.getFoldCount()];
        for (int i = 0; i < config.getFoldCount(); i++) {
            testPrintStream[i] = new PrintStream(new FileOutputStream(config.getTestOutputPath(i)));
            trainPrintStream[i] = new PrintStream(new FileOutputStream(config.getTrainOutputPath(i)));
        }

        LOGGER.log(Level.INFO, "Reading input from {0}", config.getInputPath());
        BufferedReader in = new BufferedReader(new FileReader(new File(config.getInputPath())));
        int lineNumber = 0;
        int lineWithinFold = 0;

        for (String line = in.readLine(); line != null; line = in.readLine()) {
            if (lineNumber == 0) {
                for (int i = 0; i < config.getFoldCount(); i++) {
                    testPrintStream[i].println(line);
                    trainPrintStream[i].println(line);
                }
            } else {
                if (config.getEmptyValue() != null && line.contains(config.getEmptyValue())) {
                    LOGGER.info("Skipping line because it contains empty value");
                    continue;
                }
                testPrintStream[lineWithinFold].println(line);
                for (int i = 0; i < config.getFoldCount(); i++) {
                    if (i != lineWithinFold) {
                        trainPrintStream[i].println(line);
                    }
                }

                if (lineWithinFold == (config.getFoldCount() - 1)) {
                    lineWithinFold = 0;
                } else {
                    lineWithinFold++;
                }
            }
            lineNumber++;
        }
        for (int i = 0; i < config.getFoldCount(); i++) {
            LOGGER.log(Level.INFO, "Written output to {0}", config.getTestOutputPath(i));
            LOGGER.log(Level.INFO, "Written output to {0}", config.getTrainOutputPath(i));
            trainPrintStream[i].close();
            testPrintStream[i].close();
        }

    }

    private GenerateFolds() {
    }

}
