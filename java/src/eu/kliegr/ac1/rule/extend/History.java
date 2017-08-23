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
package eu.kliegr.ac1.rule.extend;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.util.logging.Logger;

public final class History {
    private final static Logger LOGGER = Logger.getLogger(ExtendRules.class.getName());
    private int RID;
    //ArrayList<Integer> historyRID = new ArrayList();
    LinkedHashMap<Integer,String[]> history = new LinkedHashMap();
    //ArrayList<Integer> historyERID = new ArrayList();
    //ArrayList<String> historyText = new ArrayList();

    /**
     *
     */
    public History(int RID) {
        this.RID = RID;
    }
    
    public String[][] toArray() {
        String[][] array  = new String[history.size()+1][];
        //array[0] = historyTableHeader();
        int i=0;
        for (Entry<Integer,String[]> e : history.entrySet()) {
            array[i] = e.getValue();
            i++;
        }
        return array;

    }
        public Collection<String[]> toCollection() {
            return history.values();        

    }

    /**
     *
     * @param RID
     * @param ERID
     * @param text
     */
    public History(int RID, int ERID,String[] text) {
        this.RID = RID;
        addRuleIdentifiers(ERID,text);
    }

    /**
     *
     * @return
     */
    public History copy() {
        History copy = new History(RID);
        for (Entry<Integer,String[]> e : history.entrySet()) {
            copy.addRuleIdentifiers(e.getKey(), e.getValue());
        }
        return copy;
    }

    /**
     *
     * @param RID
     * @param ERID
     * @param text
     */
    public void addRuleIdentifiers(int ERID,String[] text) {
        //historyRID.add(RID);
        history.put(ERID, text);
        //historyERID.add(ERID);
        //historyText.add(text);
    }

    public String[] historyTableHeader() {
        String[] header =  {"RID","ERID","rule","supp","conf"};
        return header;
    }
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("\nExtend history for rule: " + RID);
        for (Entry<Integer,String[]> e : history.entrySet()) {
            sb.append("(ERID=").append(e.getKey()).append(") ->");
        }
        sb.append("current\n");
        sb.append(String.join(",",historyTableHeader()));
        sb.append("\n");
        for (Entry<Integer,String[]> e : history.entrySet()) {
            sb.append(String.join(",",e.getValue())).append("\n");
        }
        return sb.toString();
    }
}
