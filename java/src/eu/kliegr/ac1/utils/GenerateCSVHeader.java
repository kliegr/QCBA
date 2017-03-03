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
package eu.kliegr.ac1.utils;

import eu.kliegr.ac1.GenerateCSVHeaderConfig;
import eu.kliegr.ac1.rule.parsers.GUHASimplifiedParser;
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
public class GenerateCSVHeader {

    private final static Logger LOGGER = Logger.getLogger(GUHASimplifiedParser.class.getName());

    /**
     *
     * @param config
     * @throws FileNotFoundException
     * @throws IOException
     */
    public static void generateCSVHeader(GenerateCSVHeaderConfig config) throws FileNotFoundException, IOException {
        PrintStream outputFile = new PrintStream(new FileOutputStream(config.getOutputPath()));
        LOGGER.log(Level.INFO, "Reading input from {0}", config.getInputPath());
        BufferedReader in = new BufferedReader(new FileReader(new File(config.getInputPath())));
        int lineNumber = 0;
        StringBuilder firstLine = new StringBuilder();
        for (String line = in.readLine(); line != null; line = in.readLine()) {
            String outLine;
            if (lineNumber == 0) {
                String[] cols = line.split(config.getSeparator());
                for (int i = 0; i < cols.length; i++) {
                    if (i == 0 && config.IsGenerateID()) {
                        firstLine.append("ID,");
                    }
                    if (i == cols.length - 1) {
                        firstLine.append("XClass");
                    } else {
                        firstLine.append("c").append(i).append(",");
                    }
                }
                outLine = firstLine.toString();
            } else if (config.IsGenerateID()) {
                outLine = lineNumber + config.getSeparator() + line;
            } else {
                outLine = line;
            }
            lineNumber++;
            outputFile.println(outLine);
        }
        LOGGER.log(Level.INFO, "Written output to {0}", config.getOutputPath());
        outputFile.close();
    }

    private GenerateCSVHeader() {
    }
}
