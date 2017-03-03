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
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Logger;

public class GenerateFoldsConfig {

    private final static Logger LOGGER = Logger.getLogger(GenerateFoldsConfig.class.getName());
    String basePath;
    String dataPath;
    String datasetNameWithoutSuffix;
    int foldCount;
    String emptyValue;

    /**
     *
     * @param path
     * @throws FileNotFoundException
     * @throws IOException
     * @throws ClassNotFoundException
     * @throws InstantiationException
     * @throws IllegalAccessException
     */
    public GenerateFoldsConfig(String path) throws FileNotFoundException, IOException, ClassNotFoundException, InstantiationException, IllegalAccessException {
        //File f = new File(path);
        InputStream input = new BufferedInputStream(
                new FileInputStream(path));
        Properties prop = new Properties();
        prop.loadFromXML(input);
        dataPath = prop.getProperty("DataPath");
        emptyValue = prop.getProperty("EmptyValue");
        if ("".equals(emptyValue)) {
            emptyValue = null;
        }
        foldCount = Integer.parseInt(prop.getProperty("FoldCount"));
        File f = new File(dataPath);
        basePath = new File(f.getParent()).getParent();
        datasetNameWithoutSuffix = f.getName().replace(".csv", "");

    }

    /**
     *
     * @return
     */
    public String getEmptyValue() {
        return emptyValue;
    }

    /**
     *
     * @return
     */
    public int getFoldCount() {
        return foldCount;
    }

    /**
     *
     * @param i
     * @return
     */
    public String getTestOutputPath(int i) {
        return basePath + File.separator + "test" + File.separator + datasetNameWithoutSuffix + (i + 1) + ".csv";
    }

    /**
     *
     * @param i
     * @return
     */
    public String getTrainOutputPath(int i) {
        return basePath + File.separator + "train" + File.separator + datasetNameWithoutSuffix + (i + 1) + ".csv";
    }

    /**
     *
     * @return
     */
    public String getInputPath() {
        return dataPath;
    }
}
