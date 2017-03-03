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

import eu.kliegr.ac1.data.AttributeType;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;
import java.util.logging.Logger;

public class Config extends BaseConfig {

    private final static Logger LOGGER = Logger.getLogger(Config.class.getName());
    String trainDataPath = "/home/tomas/NetBeansProjects/AC1/resources/iris_train.csv";
    String testDataPath = "/home/tomas/NetBeansProjects/AC1/resources/iris_test.csv";
    String rulesPath = "/home/tomas/NetBeansProjects/AC1/resources/Iris1_iris_1_0.5.xml";
    String prunedRulesPath = "/home/tomas/NetBeansProjects/AC1/resources/Iris1_iris_1_0.5_pruned.xml";
    String testRulesPath = "/home/tomas/NetBeansProjects/AC1/resources/Iris1_iris_1_0.5_pruned.xml";
    String testOutputPath = "/home/tomas/NetBeansProjects/AC1/resources/Iris1_iris_1_0.5_pruned_predicted.csv";
    String testOutputSummaryPath = "/home/tomas/NetBeansProjects/AC1/resources/Iris1_iris_1_0.5_pruned_predicted.summary";
    String testRuleSortComparator = "PreserveRuleOrderComparator";
    String pruneRuleSortComparator = "MMACRuleComparator";
    Boolean checkConsequentPruning = true;

    private void writeSampleFullConfig() throws FileNotFoundException, IOException {
        OutputStream output = new BufferedOutputStream(
                new FileOutputStream("config.xml"));
        Properties prop = new Properties();
        //pruning            
        prop.setProperty("TrainDataPath", trainDataPath);
        prop.setProperty("RulesPath", rulesPath);
        prop.setProperty("PruneRuleSortComparator", pruneRuleSortComparator);
        prop.setProperty("CheckConsequentWhenPruning", String.valueOf(checkConsequentPruning));
        prop.setProperty("TargetAttribute", targetAttribute);
        prop.setProperty("DataTypes", AttributeType.numerical.toString() + ";" + AttributeType.numerical.toString() + ";" + AttributeType.numerical.toString() + ";" + AttributeType.numerical.toString() + ";" + AttributeType.nominal.toString());
        prop.setProperty("PrunedRulesOutputPath", prunedRulesPath);
        //test            
        prop.setProperty("TestRulesPath", testRulesPath);
        prop.setProperty("TestRuleSortComparator", testRuleSortComparator);
        prop.setProperty("TestOutputPath", testOutputPath);
        prop.setProperty("TestOutputSummaryPath", testOutputSummaryPath);
        prop.setProperty("TestDataPath", testDataPath);
        prop.storeToXML(output, null);

    }

    private void readFullConfig(String path) throws FileNotFoundException, IOException {
        InputStream input = new BufferedInputStream(
                new FileInputStream(path));
        Properties prop = new Properties();
        prop.loadFromXML(input);
        trainDataPath = prop.getProperty("TrainDataPath");
        testDataPath = prop.getProperty("TestDataPath");
        prunedRulesPath = prop.getProperty("PrunedRulesOutputPath");
        testRulesPath = prop.getProperty("TestRulesPath");
        testRuleSortComparator = prop.getProperty("TestRuleSortComparator");
        pruneRuleSortComparator = prop.getProperty("PruneRuleSortComparator");

        rulesPath = prop.getProperty("RulesPath");
        targetAttribute = prop.getProperty("TargetAttribute");
        testOutputPath = prop.getProperty("TestOutputPath");
        testOutputSummaryPath = prop.getProperty("TestOutputSummaryPath");
        checkConsequentPruning = Boolean.parseBoolean(prop.getProperty("CheckConsequentWhenPruning"));
        attType = parseAttributeTypes(prop.getProperty("DataTypes").split(";"));

    }

}
