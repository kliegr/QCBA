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

import eu.kliegr.ac1.data.AttributeValue;
import eu.kliegr.ac1.data.DataTable;
import eu.kliegr.ac1.performance.StopWatches;
import eu.kliegr.ac1.rule.extend.ExtendRule;
import eu.kliegr.ac1.rule.extend.ExtendType;
import eu.kliegr.ac1.rule.extend.History;
import eu.kliegr.ac1.rule.extend.ValueOrigin;
import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 *
 * @author tomas
 */
public class PruneRules {

    private final static Logger LOGGER = Logger.getLogger(PruneRules.class.getName());
    private List<ExtendRule> rules;
    private Comparator ruleComparator;
    private PruneType type;
    

    /**
     *
     * @param rules
     * @param ruleComparator
     * @param type
     */
    public PruneRules(ArrayList<Rule> rules, Comparator ruleComparator, PruneType type) {
        this.type = type;
        this.ruleComparator = ruleComparator;

        this.rules = rules.stream().map((rule) -> new ExtendRule(rule)).collect(Collectors.toCollection(() -> Collections.synchronizedList(new ArrayList<ExtendRule>())));
    }
    /**
     *
     * @return
     */
    public List<ExtendRule> getRules() {
        return Collections.unmodifiableList(rules);
    }

    /**
     *
     */
    public void sortRules() {
        rules.sort(ruleComparator);
    }

    /**
     *
     * @param hideTransaction
     */
     public void pruneRules() {
        LOGGER.info("STARTED Pruning");
        for (Iterator<ExtendRule> it = rules.iterator(); it.hasNext();) {

            ExtendRule rule = it.next();

            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(Level.FINE, "#Rule {0}", rule.toString());
            }

            if (type == PruneType.antecedentOnly) {
                throw new UnsupportedOperationException("Antecedent only pruning type no longer supported");
            }

            if (rule.getAntecedentLength() == 0) {
                LOGGER.info("Skipping default rule");
                continue;
            }

            int supportingTransactions = rule.removeTransactionsCoveredByAntecedent(false);

            if (supportingTransactions == 0) {
                if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.fine("0 transactions, REMOVED");
                }
                it.remove();
            } else if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(Level.FINE, "{0} transactions, RULE {1} KEPT", new Object[]{supportingTransactions, rule.getRID()});
            }

        }
        
        
        LOGGER.info("FINISHED Removing rules with zero coverage");
    }
     
    /**
     *
     * @param originalRulecount
     * @param path
     * @param watches
     * @throws FileNotFoundException
     * @throws IOException
     */
    public void saveSummary(int originalRulecount, String path, StopWatches watches) throws FileNotFoundException, IOException {
        OutputStream output = new BufferedOutputStream(new FileOutputStream(path));
        output.write(("Number of rules before pruning:" + originalRulecount + "\n").getBytes());
        output.write(("Number of rules after pruning:" + rules.size() + "\n").getBytes());
        output.write(("Pruning type:" + type.name()).getBytes());
        output.write(watches.toString().getBytes());
        output.flush();
        output.close();
    }
}
