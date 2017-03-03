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
import java.util.ArrayList;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author tomas
 */
public class Antecedent {

    private final static Logger LOGGER = Logger.getLogger(Antecedent.class.getName());
    private final ArrayList<RuleMultiItem> items;

    /**
     *
     * @param items
     */
    public Antecedent(ArrayList<RuleMultiItem> items) {
        this.items = items;
    }

    public String toString() {
        return toString(false);
    }

    /**
     *
     * @param succint
     * @return
     */
    public String toString(boolean succint) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        String sep = "";
        for (RuleMultiItem item : items) {
            sb.append(sep);
            sep = ",";
            sb.append(item.toString(true, false, succint));
        }
        sb.append("}");
        return sb.toString();

    }

    /*
    if the antecedent has no items, it is supported by all transactions, however, the method returns null
    if the antecedent has items, but no supporting transactions, the method returns empty list
     */

    /**
     *
     * @return @throws NoSuchElementException
     */
    public Set<Transaction> getSupportingTransactions() throws NoSuchElementException {

        LOGGER.log(Level.FINE, "Computing supporting transaction for rule with {0} items and {1} values", new Object[]{getItems().size(), getItems().stream().mapToInt((x) -> x.getAttributeValues().size()).sum()});

        if (getItems().isEmpty()) {
            return null;
        }

        Optional<Set<Transaction>> result = getItems().parallelStream().map((item) -> item.getSupportingTransactions()).reduce((s, s1)
                -> {
            s.retainAll(s1);
            return s;
        });

        return result.get();

    }

    /**
     *
     * @return
     */
    public ArrayList<RuleMultiItem> getItems() {
        return items;
    }
}
