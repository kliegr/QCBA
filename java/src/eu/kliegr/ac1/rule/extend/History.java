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

public final class History {

    ArrayList<Integer> historyRID = new ArrayList();
    ArrayList<Integer> historyERID = new ArrayList();

    /**
     *
     */
    public History() {

    }

    /**
     *
     * @param RID
     * @param ERID
     */
    public History(int RID, int ERID) {
        addRuleIdentifiers(RID, ERID);
    }

    /**
     *
     * @return
     */
    public History copy() {
        History copy = new History();
        for (int i = 0; i < historyRID.size(); i++) {
            copy.addRuleIdentifiers(historyRID.get(i), historyERID.get(i));
        }
        return copy;
    }

    /**
     *
     * @param RID
     * @param ERID
     */
    public void addRuleIdentifiers(int RID, int ERID) {
        historyRID.add(RID);
        historyERID.add(ERID);
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("\nHistory:");
        for (int i = 0; i < historyRID.size(); i++) {
            sb.append(historyRID.get(i)).append("(ERID=").append(historyERID.get(i)).append(") ->");
        }
        sb.append("current");
        return sb.toString();
    }
}
