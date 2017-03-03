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

public class RuleLearningConfig extends BaseConfig {

    private final static Logger LOGGER = Logger.getLogger(RuleLearningConfig.class.getName());
    private final float minConfidence;
    private final int minLen;
    private final int maxLen;
    private final float minSupport;
    private final RuleLearningAlgEnum algorithm;
    private final PruningEnum pruning;
    private final int targetRuleCount;
    private final float confEpsilon;
    private final float maxExecTimeTotal;
    private final float maxExecTimeIter;
    //private String targetAttribute = "XClass";

    private final boolean useDiscretizedInput;

    /**
     *
     * @param path
     * @throws FileNotFoundException
     * @throws IOException
     * @throws ClassNotFoundException
     * @throws InstantiationException
     * @throws IllegalAccessException
     */
    public RuleLearningConfig(String path) throws FileNotFoundException, IOException, ClassNotFoundException, InstantiationException, IllegalAccessException {

        InputStream input = new BufferedInputStream(
                new FileInputStream(path));
        Properties prop = new Properties();
        prop.loadFromXML(input);
        this.setOutputPath(prop.getProperty("OutputPath"));
        dataPath = prop.getProperty("TrainDataPath");
        minConfidence = Float.parseFloat(prop.getProperty("minConfidence"));
        minLen = Integer.parseInt(prop.getProperty("minLen"));
        maxLen = Integer.parseInt(prop.getProperty("maxLen"));
        targetRuleCount = Integer.parseInt(prop.getProperty("targetRuleCount"));
        confEpsilon = Float.parseFloat(prop.getProperty("confEpsilon"));
        maxExecTimeIter = Float.parseFloat(prop.getProperty("maxExecTimeIter"));
        maxExecTimeTotal = Float.parseFloat(prop.getProperty("maxExecTimeTotal"));
        minSupport = Float.parseFloat(prop.getProperty("minSupport"));
        useDiscretizedInput = Boolean.parseBoolean(prop.getProperty("UseDiscretizedInput"));

        String _pruning = prop.getProperty("PruningAlgorithm");
        if (_pruning != null) {
            pruning = PruningEnum.valueOf(_pruning);
        } else {
            pruning = null;
        }
        if (prop.getProperty("DataTypes").contains(";")) {
            csvSeparator = ";";
        } else {
            csvSeparator = ",";
        }
        attType = parseAttributeTypes(prop.getProperty("DataTypes").split(csvSeparator));

        String _algorithm = prop.getProperty("Algorithm");
        if (_algorithm == null) {
            algorithm = RuleLearningAlgEnum.arules;
        } else {
            algorithm = RuleLearningAlgEnum.valueOf(_algorithm);
        }

        targetAttribute = prop.getProperty("TargetAttribute");
        IDcolumnName = prop.getProperty("IDcolumnName");
    }

    /**
     * @return the minConfidence
     */
    public float getMinConfidence() {
        return minConfidence;
    }

    /**
     * @return the minLen
     */
    public int getMinLen() {
        return minLen;
    }

    /**
     * @return the maxLen
     */
    public int getMaxLen() {
        return maxLen;
    }

    /**
     *
     * @return
     */
    public float getMaxExecTimeIter() {
        return this.maxExecTimeIter;
    }

    /**
     *
     * @return
     */
    public float getMaxExecTimeTotal() {
        return this.maxExecTimeTotal;
    }

    /**
     *
     * @return
     */
    public float getConfEpsilon() {
        return this.confEpsilon;
    }

    /**
     *
     * @return
     */
    public int getTargetRuleCount() {
        return targetRuleCount;
    }

    /**
     * @return the minSupport
     */
    public float getMinSupport() {
        return minSupport;
    }

    /**
     *
     * @return
     */
    public PruningEnum getPruning() {
        return pruning;
    }

    /**
     *
     * @return
     */
    public RuleLearningAlgEnum getAlgorithm() {
        return algorithm;
    }

    /**
     * @return the useDiscretizedInput
     */
    public boolean isUseDiscretizedInput() {
        return useDiscretizedInput;
    }

}
