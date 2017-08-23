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
package eu.kliegr.ac1.rule;

import java.util.Comparator;
import java.util.logging.Logger;

/**
 *
 * @author tomas
 */
public class CBARuleComparator implements Comparator {

    @Override
    public int compare(Object o1, Object o2) {
        RuleInt r1 = (RuleInt) o1;
        RuleInt r2 = (RuleInt) o2;
        //put default rule last
        if (r1.getAntecedentLength()==0 & r2.getAntecedentLength()>0)
        {
            return 1;            
        }
        //put default rule last
        if (r2.getAntecedentLength()==0 & r1.getAntecedentLength()>0)
        {
            return -1;            
        }
        else if (r1.getConfidence() > r2.getConfidence()) {
            return -1;
        } else if (r1.getConfidence() < r2.getConfidence()) {
            return 1;
        } else // (r1.getConfidence() == r2.getConfidence())
         if (r1.getSupport() > r2.getSupport()) {
                return -1;
            } else if (r1.getSupport() < r2.getSupport()) {
                return 1;
            } else //r1.getConfidence() == r2.getConfidence() and r1.getSupport() == r2.getSupport()
             if (r1.getAntecedentLength() < r2.getAntecedentLength()) {
                    return -1;
                } else if (r1.getAntecedentLength() > r2.getAntecedentLength()) {
                    return 1;
                } else //make arbitrary decision roughly corresponding to "r1 was generated before r2"
                {
                    return r1.getRID() - r2.getRID();
                }

    }
    private static final Logger LOG = Logger.getLogger(CBARuleComparator.class.getName());

}
