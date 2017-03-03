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
package eu.kliegr.ac1.pipeline;

import eu.kliegr.ac1.AC1;
import eu.kliegr.ac1.PipelineConfig;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.logging.Logger;


public class Experimentator {

    private final static Logger LOGGER = Logger.getLogger(Experimentator.class.getName());

    /**
     *
     * @param config
     * @throws IOException
     * @throws InterruptedException
     * @throws Exception
     */
    public static void runPipeline(PipelineConfig config) throws IOException, InterruptedException, Exception {
        for (PipelineStep step : config.pipelineStep) {

            if (step.getReplacementParam() != null) {
                String content = new String(Files.readAllBytes(Paths.get(step.getFilename())), StandardCharsets.UTF_8);
                /* This step has replacements, which means 
                    that the step file is saved for each replacement to designated output file name and then executed.       
                 */
                int i = 0;
                for (String replacement : step.getReplacementParam().getReplacements()) {
                    if (step.getReplacementParam().getSkips().get(i) == true) {
                        i++;
                        continue;
                    }
                    i++;
                    String replacedContent = content.replaceAll(step.getReplacementParam().getReplaceregex(), replacement);
                    String outputfilename = step.getOutputfilename(step.getReplacementParam().getName(), replacement);
                    Files.write(Paths.get(outputfilename), replacedContent.getBytes(StandardCharsets.UTF_8));

                    executeStep(outputfilename);
                }

            } else {
                //this step is directly executed and no config file is created.
                executeStep(step.getFilename());
            }

        }
    }

    private static void executeStep(String outputfilename) throws Exception {
        AC1.main(new String[]{outputfilename});
        AC1.cleanup();
    }

    private Experimentator() {
    }
}
