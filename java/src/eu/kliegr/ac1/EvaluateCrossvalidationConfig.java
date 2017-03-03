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

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Logger;

public class EvaluateCrossvalidationConfig {

    private final static Logger LOGGER = Logger.getLogger(EvaluateCrossvalidationConfig.class.getName());
    String evaluationDir;
    String pattern;

    /**
     *
     * @param path
     * @throws FileNotFoundException
     * @throws IOException
     * @throws ClassNotFoundException
     * @throws InstantiationException
     * @throws IllegalAccessException
     */
    public EvaluateCrossvalidationConfig(String path) throws FileNotFoundException, IOException, ClassNotFoundException, InstantiationException, IllegalAccessException {

        InputStream input = new BufferedInputStream(
                new FileInputStream(path));
        Properties prop = new Properties();
        prop.loadFromXML(input);
        evaluationDir = prop.getProperty("EvaluationDir");
        pattern = prop.getProperty("FilePattern");
    }

    /**
     *
     * @return
     */
    public String getEvaluationDir() {
        return evaluationDir;
    }

    /**
     *
     * @return
     */
    public String getFilePattern() {
        return pattern;
    }

    /**
     *
     * @return
     */
    public String getOutputPath() {
        return evaluationDir + "accuracy.total";
    }
}
