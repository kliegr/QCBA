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

import eu.kliegr.ac1.data.Transaction;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.logging.Logger;

/**
 *
 * @author tomas
 */
public class Consequent {

    private final RuleMultiItem item;

    /**
     *
     * @param items
     */
    public Consequent(RuleMultiItem items) {
        this.item = items;
    }

    /**
     *
     * @return
     */
    public RuleMultiItem getItems() {
        return item;
    }

    /**
     *
     * @return @throws NoSuchElementException
     */
    public Set<Transaction> getSupportingTransactions() throws NoSuchElementException {
        return getItems().getSupportingTransactions();

    }

    public String toString() {
        return toString(false, false);

    }

    /**
     *
     * @param onlyValue
     * @param succint
     * @return
     */
    public String toString(boolean onlyValue, boolean succint) {
        if (onlyValue) {
            return item.toString(false, false, succint);
        } else {
            return "{" + item.toString(true, false, succint) + "}";
        }

    }

    private static final Logger LOG = Logger.getLogger(Consequent.class.getName());

}
