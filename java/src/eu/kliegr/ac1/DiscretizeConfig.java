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

import eu.kliegr.ac1.Rinterface.discretization.DiscretizeType;
import eu.kliegr.ac1.Rinterface.discretization.MissingValueTreatmentEnum;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Logger;

public class DiscretizeConfig extends BaseConfig {

    private final static Logger LOGGER = Logger.getLogger(BaseConfig.class.getName());
    private final String dataPath;
    private final String outputDataPath;
    private final String testDataPath;
    private final String testOutputDataPath;
    private final DiscretizeType discretizeType;
    private final MissingValueTreatmentEnum missingValueTreatment;

    /**
     *
     * @param path
     * @throws FileNotFoundException
     * @throws IOException
     * @throws ClassNotFoundException
     * @throws InstantiationException
     * @throws IllegalAccessException
     */
    public DiscretizeConfig(String path) throws FileNotFoundException, IOException, ClassNotFoundException, InstantiationException, IllegalAccessException {
        InputStream input = new BufferedInputStream(
                new FileInputStream(path));
        Properties prop = new Properties();
        prop.loadFromXML(input);
        dataPath = prop.getProperty("DataPath");
        outputDataPath = prop.getProperty("OutputPath");

        if (prop.containsKey("TestDataPath")) {
            testDataPath = prop.getProperty("TestDataPath");
            testOutputDataPath = prop.getProperty("TestOutputPath");
        } else {
            testDataPath = null;
            testOutputDataPath = null;
        }
        discretizeType = DiscretizeType.valueOf(prop.getProperty("DiscretizeType"));
        missingValueTreatment = MissingValueTreatmentEnum.valueOf(prop.getProperty("MissingValueTreatment"));

        if (prop.getProperty("DataTypes").contains(";")) {
            csvSeparator = ";";
        } else {
            csvSeparator = ",";
        }
        attType = parseAttributeTypes(prop.getProperty("DataTypes").split(csvSeparator));
        IDcolumnName = prop.getProperty("IDcolumnName");
        targetAttribute = prop.getProperty("TargetAttribute");
    }

    /**
     *
     * @return
     */
    public MissingValueTreatmentEnum getMissingValueTreatment() {
        return missingValueTreatment;
    }

    /**
     *
     * @return
     */
    public String getDataPath() {
        return dataPath;
    }

    /**
     *
     * @return
     */
    public String getOutputDataPath() {
        return outputDataPath;
    }

    /**
     *
     * @return
     */
    public String getTestDataPath() {
        return testDataPath;
    }

    /**
     *
     * @return
     */
    public String getTestOutputDataPath() {
        return testOutputDataPath;
    }

    /**
     *
     * @return
     */
    public DiscretizeType getDiscretizeType() {
        return discretizeType;
    }

}
