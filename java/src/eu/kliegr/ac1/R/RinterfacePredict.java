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
package eu.kliegr.ac1.R;

import eu.kliegr.ac1.rule.CBARuleComparator;
import eu.kliegr.ac1.rule.TestRule;
import eu.kliegr.ac1.rule.TestRules;
import eu.kliegr.ac1.rule.TestingType;
import eu.kliegr.ac1.rule.parsers.GenericRuleParser;
import java.util.Comparator;
import java.util.logging.Level;
import java.util.logging.Logger;

public class RinterfacePredict extends Rinterface {

    private final static Logger LOGGER = Logger.getLogger(RinterfacePredict.class.getName());
    Comparator ruleComparator = new CBARuleComparator();

    /**
     *
     * @param att_types
     * @param targetColName
     * @param IDcolumnName
     * @param loglevel
     * @throws Exception
     */
    public RinterfacePredict(String[] att_types, String targetColName, String IDcolumnName, String loglevel) throws Exception {
        super(att_types, targetColName, IDcolumnName, loglevel);

    }

    /**
     *
     * @param path
     * @param testingType
     * @return
     * @throws Exception
     */
    public String[] predictWithRulesFromFile(String path, String testingType) throws Exception {
        TestingType ttype = TestingType.valueOf(testingType);
        if (data.getDataTable() == null) {
            throw new Exception("Load data first");
        }

        TestRules testRulesObj = new TestRules(GenericRuleParser.parseFileForRules(path, data), new CBARuleComparator(), data);
        testRulesObj.classifyData(ttype);
        String[] result = testRulesObj.getResult();
        LOGGER.log(Level.INFO, "Result dimensionality:{0}", result.length);
        return result;

    }

    /**
     *
     * @return @throws Exception
     */
    public String[] predict() throws Exception {
        boolean sort=false;
        TestingType ttype = TestingType.firstMatch;
        LOGGER.log(Level.INFO, "Predict invoked");
        LOGGER.log(Level.INFO, "Transaction count:{0}", data.getDataTable().getCurrentTransactionCount());
        if (data.getDataTable() == null) {
            throw new Exception("Load data first");
        }
        if (rules == null) {
            throw new Exception("Load rules first");
        }
        String[] result = null;
        try {
            TestRules testRulesObj = new TestRules(rules, ruleComparator, data);
            if (sort) testRulesObj.sortRules();

            //do some checks
            int lastRuleIndex = testRulesObj.getRules().size()-1;
            for (int i=0; i<= lastRuleIndex;i++)
            {

                int curAntLen = testRulesObj.getRules().get(i).getAntecedentLength();
                if (i==lastRuleIndex & curAntLen!=0)
                {
                        LOGGER.warning("Last rule not default rule");
                        throw new Exception ("Last rule not default rule");
                }
                else if (i!=lastRuleIndex & curAntLen ==0 )
                {
                        LOGGER.warning("Default rule on other position than last");
                        throw new Exception ("Default rule on other position than last");
                }                
            }
            
            testRulesObj.classifyData(ttype);
            result = testRulesObj.getResult();
        }
        catch(Exception e)
        {
            e.printStackTrace();
            throw e;
        }
        
        LOGGER.log(Level.INFO, "Result dimensionality:{0}", result.length);
        return result;
    }
}
