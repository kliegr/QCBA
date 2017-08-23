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
import eu.kliegr.ac1.rule.extend.DefaultRuleOverlapPruningType;
import eu.kliegr.ac1.rule.extend.ExtendRule;
import eu.kliegr.ac1.rule.extend.ExtendRuleConfig;
import eu.kliegr.ac1.rule.extend.ExtendRules;
import eu.kliegr.ac1.rule.extend.ExtendType;
import eu.kliegr.ac1.rule.extend.ExtensionStrategyEnum;
import eu.kliegr.ac1.rule.extend.PostPruningType;
import eu.kliegr.ac1.rule.parsers.GUHASimplifiedParser;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.logging.Logger;

public class RinterfaceExtend extends Rinterface {

    private final static Logger LOGGER = Logger.getLogger(RinterfaceExtend.class.getName());
    //default values
    boolean isContinuousPruningEnabled = false;
    boolean isFuzzificationEnabled = false;
    PostPruningType postpruningtype;
    boolean isAnnotationEnabled = true;

    Comparator ruleComparator = new CBARuleComparator();
    
    ExtendRuleConfig conf;
    ExtendRules extendRulesObj;

    /**
     *
     * @param att_types
     * @param targetColName
     * @param IDcolumnName
     * @param loglevel
     * @throws Exception
     */
    public RinterfaceExtend(String[] att_types, String targetColName, String IDcolumnName, String loglevel) throws Exception {
        super(att_types, targetColName, IDcolumnName, loglevel);

    }

    public void extend(String extendType,String  defaultRuleOverlapPruningType, boolean isAttPruningEnabled, boolean isTrimmingEnabled,boolean isContinuousPruningEnabled, String postpruningTypeStr, boolean isFuzzificationEnabled, boolean isAnnotationEnabled,double minImprovement,double minCondImprovement, double minConf, String extensionStrategy) throws Exception {

        this.isFuzzificationEnabled = isFuzzificationEnabled;
        this.postpruningtype = PostPruningType.valueOf(postpruningTypeStr);
        this.isContinuousPruningEnabled = isContinuousPruningEnabled;
        this.isAnnotationEnabled = isAnnotationEnabled;
        if (extendRulesObj != null) {
            throw new Exception("Rules already extended");
        }
        if (data.getDataTable() == null) {
            throw new Exception("Load data first");
        }
        if (rules == null) {
            throw new Exception("Load rules first");
        }
        conf  = new ExtendRuleConfig(minImprovement,minCondImprovement,minConf, ExtensionStrategyEnum.valueOf(extensionStrategy));
        extendRulesObj = new ExtendRules(rules, ruleComparator, ExtendType.valueOf(extendType), conf, data);
        extendRulesObj.sortRules();
        try{
            extendRulesObj.processRules(isAttPruningEnabled,isTrimmingEnabled,isContinuousPruningEnabled, isFuzzificationEnabled, postpruningtype, DefaultRuleOverlapPruningType.valueOf(defaultRuleOverlapPruningType) );
        }
        catch (Exception e)
        {
            e.printStackTrace();
            LOGGER.warning("There was exception, removing all results.");
            extendRulesObj = null;
            throw new Exception(e);
        }
        if (isAnnotationEnabled) {
            extendRulesObj.annotateRules();
        }
        LOGGER.info("Extend completed");
    }

    /**
     *
     * @throws Exception
     */
    public void annotate() throws Exception {
        if (extendRulesObj == null) {
            throw new Exception("Rules must be first extended");
        }
        extendRulesObj.annotateRules();

    }

    /**
     *
     * @param path
     * @throws Exception
     */
    public void saveToFile(String path) throws Exception {
        if (extendRulesObj == null) {
            throw new Exception("Rules must be first extended");
        }
        GUHASimplifiedParser.saveRules(extendRulesObj.getExtendedRules(), path);
    }

    /**
     *
     * @return @throws Exception
     */
    public int getRuleCount() throws Exception {
        int ruleCount = extendRulesObj.getExtendedRules().size();
        return ruleCount;

    }

    /**
     *
     * @return @throws Exception
     */
    public String[][] getRules() throws Exception {
        if (isAnnotationEnabled | isFuzzificationEnabled) {
            throw new Exception("Fuzzified or annotated rules cannot be exported to R, use saveToFile()!");
        }

        int ruleCount = extendRulesObj.getExtendedRules().size();

        String[][] df = new String[ruleCount][3];

        int i = 0;
        for (ExtendRule r : extendRulesObj.getExtendedRules()) {
            df[i][0] = r.getRule().getRuleAsArulesString();
            df[i][1] = Double.toString(r.getRuleQuality().getRelativeSupport());
            df[i++][2] = Double.toString(r.getRuleQuality().getConfidence());
        }

        return df;
    }
    
    public String[][] getRuleHistory() throws Exception {
        int ruleCount = extendRulesObj.getExtendedRules().size();
        ArrayList<String[]> histories  = new ArrayList();
        int totalHistorySize =0;
        int columns= extendRulesObj.getExtendedRules().get(0).copyHistory().historyTableHeader().length;
        for (int i=0; i<ruleCount;i++)
        {
            Collection<String[]> curHistory = extendRulesObj.getExtendedRules().get(i).copyHistory().toCollection();
            totalHistorySize = totalHistorySize + curHistory.size();
            histories.addAll(curHistory);
        }
        

        String[][] result =  new String[totalHistorySize][columns];
        int i=0;
        for (String[] ch :histories )
        {
                result[i++] = ch;        
        }
        return result;
    }    

    /**
     *
     * @return @throws Exception
     */
    public Object[][] getRules2() throws Exception {
        int ruleCount = extendRulesObj.getExtendedRules().size();

        Object[][] df = new Object[ruleCount][3];

        int i = 0;
        for (ExtendRule r : extendRulesObj.getExtendedRules()) {
            df[i][0] = r.getRule().getRuleAsArulesString();
            df[i][1] = (double) r.getRuleQuality().getRelativeSupport();
            df[i++][2] = (double) r.getRuleQuality().getConfidence();
        }

        return df;
    }

}
