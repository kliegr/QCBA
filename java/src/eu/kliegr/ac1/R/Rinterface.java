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

import eu.kliegr.ac1.data.AttributeType;
import eu.kliegr.ac1.data.DataTable;
import eu.kliegr.ac1.rule.AttributeNotFoundException;
import eu.kliegr.ac1.rule.Data;
import eu.kliegr.ac1.rule.Rule;
import eu.kliegr.ac1.rule.parsers.ArulesParser;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 *
 * @author Adapted from the rCBA package
 */
public abstract class Rinterface {

    private final static Logger LOGGER = Logger.getLogger(Rinterface.class.getName());
    Data data = new Data();
    ArrayList<Rule> rules;
    ArrayList<AttributeType> att_types;
    String targetColName;
    String IDcolumnName;

    /**
     *
     * @param att_types
     * @param targetColName
     * @param IDcolumnName
     * @param loglevel
     * @throws Exception
     */
    public Rinterface(String[] att_types, String targetColName, String IDcolumnName, String loglevel) throws Exception {
        System.gc();
        ArrayList<AttributeType> attributeTypes = new ArrayList();
        setLoggerLevelGlobaly(Level.parse(loglevel));
        for (String atttype : att_types) {
            attributeTypes.add(AttributeType.valueOf(atttype));
        }
        if (IDcolumnName.isEmpty()) {
            IDcolumnName = null;
        }
        this.att_types = attributeTypes;
        this.targetColName = targetColName;
        this.IDcolumnName = IDcolumnName;
        LOGGER.info("Init finished");
    }

    /**
     *
     * @param dataFrame
     * @param cNames
     * @throws Exception
     */
    public void addDataFrame(Object dataFrame[], String[] cNames) throws Exception {
        if (data.getDataTable() != null) {
            throw new Exception("DataTable already set");
        }
        LOGGER.info("addDataFrame");
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.log(Level.FINE, "attTypes:{0}", att_types.stream().map(i -> i.toString()).collect(Collectors.joining(", ")));
            LOGGER.log(Level.FINE, "cnames:{0}", Arrays.toString(cNames));
        }
        DataTable dataTable = data.newDataTable(cNames, targetColName, att_types, IDcolumnName);
        int columns = dataFrame.length;
        LOGGER.log(Level.FINE, "columns:{0}", columns);

        if (columns > 0) {
            int rows = ((String[]) dataFrame[0]).length;
            LOGGER.log(Level.FINE, "rows:{0}", rows);
            for (int i = 0; i < rows; i++) {
                String[] row = new String[columns];
                for (int j = 0; j < columns; j++) {
                    row[j] = ((String[]) dataFrame[j])[i];
                    if (LOGGER.isLoggable(Level.FINE)) {
                        LOGGER.log(Level.FINE, "col:''{0}''value:''{1}'' atttype:''{2}''", new Object[]{cNames[j], row[j], att_types.get(j)});
                    }
                }
                LOGGER.log(Level.FINE, "Row length:{0}", row.length);

                try {
                    dataTable.addTransaction(row);
                } catch (Exception e) {
                    throw (e);
                }
            }
        }
        LOGGER.info("addDataFrame finished");
    }

    private void setLoggerLevelGlobaly(Level level) {
        Logger log = LogManager.getLogManager().getLogger("");
        for (Handler h : log.getHandlers()) {
            h.setLevel(level);
        }
    }

    /**
     *
     * @return
     */
    public String[] testFrame2() {
        String[] df = new String[3];
        df[0] = "rule 1";
        df[1] = "confidence 1";
        df[2] = "support 1";
        return df;
    }

    /**
     *
     */
    public void testFrame3() {

    }

    /**
     *
     * @return
     */
    public String[][] _getRules() {
        String[][] df = new String[3][2];
        df[0][0] = "rule 1";
        df[1][0] = "confidence 1";
        df[2][0] = "support 1";
        df[0][1] = "rule 1";
        df[1][1] = "confidence 1";
        df[2][1] = "support 1";
        return df;
    }

    /**
     *
     * @param dataFrame
     * @throws AttributeNotFoundException
     * @throws Exception
     */
    public void addRuleFrame(Object dataFrame[]) throws AttributeNotFoundException, Exception {
        if (rules != null) {
            throw new Exception("Rules already added");
        }
        try {
            rules = new ArrayList();
            LOGGER.fine("addRuleFrame");
            int columns = dataFrame.length;
            LOGGER.log(Level.FINE, "Columns{0}", dataFrame.length);

            ArulesParser parser = new ArulesParser(data);
            if (columns > 0) {
                int rows = ((String[]) dataFrame[0]).length;
                for (int i = 0; i < rows; i++) {
                    String rule = ((String[]) dataFrame[0])[i];
                    double confidence = (dataFrame[2] instanceof double[]) ? ((double[]) dataFrame[2])[i]
                            : ((int[]) dataFrame[2])[i];
                    double support = (dataFrame[1] instanceof double[]) ? ((double[]) dataFrame[1])[i]
                            : ((int[]) dataFrame[1])[i];
                    LOGGER.log(Level.INFO, "Rule{0} conf{1} support {2}", new Object[]{rule, confidence, support});
                    Rule r = parser.parseRule(rule, (float) confidence, (float) support, i);
                    LOGGER.info(r.toString());
                    rules.add(r);
                }
            }
            LOGGER.info("addRuleFrame finished");
        } catch (Exception e) {
        }

    }

}
