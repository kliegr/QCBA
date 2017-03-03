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

import eu.kliegr.ac1.EvaluateCrossvalidationConfig;
import eu.kliegr.ac1.rule.parsers.GUHASimplifiedParser;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

public class EvaluateCrossvalidation {

    private final static Logger LOGGER = Logger.getLogger(GUHASimplifiedParser.class.getName());

    /**
     *
     * @param config
     * @throws FileNotFoundException
     * @throws IOException
     */
    public static void evaluateCrossvalidation(EvaluateCrossvalidationConfig config) throws FileNotFoundException, IOException {
        File dir = new File(config.getEvaluationDir());
        double totalAccuracy = 0;
        int fileCount = 0;
        File[] files = dir.listFiles();
        for (File f : files) {
            if (f.getName().matches(config.getFilePattern())) {
                final Stream<String> lines = Files.lines(Paths.get(f.getAbsolutePath()));
                Optional<String> accuracyS = lines.filter((line) -> line.startsWith("Accuracy:")).findFirst().map(line -> line.substring("Accuracy:".length()));
                if (accuracyS.isPresent()) {
                    totalAccuracy += Double.parseDouble(accuracyS.get());
                    fileCount++;
                }
            }
        }
        PrintStream outputFile = new PrintStream(new FileOutputStream(config.getOutputPath()));
        LOGGER.log(Level.INFO, "Files matched {0}", fileCount);
        LOGGER.info(String.valueOf(totalAccuracy / fileCount));
        outputFile.append(config.getFilePattern() + "," + totalAccuracy / fileCount + "," + fileCount + "\n");
    }

    private EvaluateCrossvalidation() {
    }
}
