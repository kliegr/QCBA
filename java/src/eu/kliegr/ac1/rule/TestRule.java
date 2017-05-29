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
import eu.kliegr.ac1.data.Transaction;
import eu.kliegr.ac1.rule.extend.ExtendRuleAnnotation;
import eu.kliegr.ac1.rule.extend.TestRuleAnnotation;
import eu.kliegr.ac1.rule.extend.ValueOrigin;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.w3c.dom.Node;

/**
 *
 * @author tomas
 */
public class TestRule implements RuleInt {

    private final static Logger LOGGER = Logger.getLogger(TestRule.class.getName());

    /**
     *
     */
    public RuleQuality testQuality;
    private final Rule rule;

    /**
     *
     * @param rule
     */
    public TestRule(Rule rule) {
        this.rule = rule;
    }

    /**
     *
     * @return
     */
    public ExtendRuleAnnotation getAnnotation() {
        return rule.getAnnotation();
    }

 /*
    setting removeCoveredTransaction will increase speed of rule execution for subsequent rules
     */
    /**
     *
     * @param removeCoveredTransaction
     * @param rmi
     * @return
     */
    public Set<Transaction> fireRuleAgainstData(boolean removeCoveredTransaction, Data rmi) {
        int a_plus_b;

        Set<Transaction> antecedentMatch = Collections.newSetFromMap(new HashMap<Transaction, Boolean>());

        Set<Transaction> antConOverlap = null;
        try {
            antConOverlap = this.getAntecedent().getSupportingTransactions();
            if (antConOverlap == null) {
                //antecedent is empty - it has no items, and is supported by all transactions
                a_plus_b = rmi.getDataTable().getCurrentTransactionCount();
                LOGGER.log(Level.FINE, "Default rule coverage:{0}", a_plus_b);
            } else {
                a_plus_b = antConOverlap.size();
            }

        } catch (NoSuchElementException e) {
            a_plus_b = 0;
        }
        if (a_plus_b == 0) {
            testQuality = new RuleQuality(0, 0);
            return antecedentMatch;

        }

        try {
            if (antConOverlap != null) {
                antecedentMatch.addAll(antConOverlap);
                antConOverlap.retainAll(getConsequent().getSupportingTransactions());
            } else {
                //if antConOverlap is null, antecedent is empty and matches all transactions
                antConOverlap = getConsequent().getSupportingTransactions();
                //TODO the following line may be very uneffective
                antecedentMatch = new HashSet(rmi.getDataTable().getAllCurrentTransactions());
            }
            if (removeCoveredTransaction) {
                //parallel stream on the following line results in inconsistency in number of false negatives
                antecedentMatch.stream().forEach((t) -> rmi.getDataTable().removeTransaction(t, false));
                if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.log(Level.FINE, "Deregistered transactions: {0}", antecedentMatch.size());
                }

            }
            int a = antConOverlap.size();
            int b = a_plus_b - a;
            testQuality = new RuleQuality(a, b);

        } catch (NoSuchElementException e) {
            testQuality = new RuleQuality(0, a_plus_b);
        }
        return antecedentMatch;
    }

    public String toString() {
        return rule.toString();
    }

    /**
     *
     * @return
     */
    @Override
    public int getAntecedentLength() {
        return rule.getAntecedentLength();
    }

    /**
     *
     * @return
     */
    @Override
    public float getConfidence() {
        return rule.getConfidence();
    }

    /**
     *
     * @return
     */
    @Override
    public Consequent getConsequent() {
        return rule.getConsequent();
    }

    /**
     *
     * @return
     */
    @Override
    public Antecedent getAntecedent() {
        return rule.getAntecedent();
    }

    /**
     *
     * @return
     */
    @Override
    public int getRID() {
        return rule.getRID();
    }

    /**
     *
     * @return
     */
    @Override
    public int getSupport() {
        return rule.getSupport();
    }

    /**
     *
     * @return
     */
    @Override
    public Node getXMLRepresentation() {
        return rule.getXMLRepresentation();
    }

    /**
     *
     * @param t
     * @return
     */
    public boolean isMatchesInFuzzyBorder(Transaction t) {
        for (RuleMultiItem rmi : rule.getAntecedent().getItems()) {
            AttributeValue atVal = t.getValue(rmi.getAttribute());

            TestRuleAnnotation annot = (TestRuleAnnotation) rule.getAnnotation();

            //TODO find closest values with annotation and check if one of them has fuzzy border
            ValueOrigin valOrig = annot.getValueOrigin(atVal);

            if (valOrig == null) {
                //for this value there is not annotation (did not appear in training data), it lies between two values
                //that appeared in the tranining data, this will have annotation
                AttributeValue lower = atVal.getAttribute().getAdjacentLower(atVal, true);
                AttributeValue higher = atVal.getAttribute().getAdjacentHigher(atVal, true);
                ValueOrigin valOrigHigher = annot.getValueOrigin(higher);
                ValueOrigin valOrigLower = annot.getValueOrigin(lower);
                if (valOrigHigher == null || valOrigLower == null) {
                    LOGGER.log(Level.SEVERE, "Transaction {0} is out of bounds of rule {1} for rule item {2}", new Object[]{t, rule, rmi.toString(true, true, false)});
                } else if (valOrigHigher == ValueOrigin.fuzzy_border || valOrigLower == ValueOrigin.fuzzy_border) {
                    if (LOGGER.isLoggable(Level.FINE)) {
                        LOGGER.log(Level.FINE, "Rule {0} matches in the fuzzy border region for {1}", new Object[]{this, rmi});
                    }
                    return true;
                }
            } else if (valOrig == ValueOrigin.fuzzy_border) {
                if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.log(Level.FINE, "Rule {0} matches in the fuzzy border region for {1}", new Object[]{this, rmi});
                }
                return true;
            } else if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(Level.FINE, "Rule {0} matches in {1} region", new Object[]{this, valOrig});
            }
        }
        return false;
    }

}
