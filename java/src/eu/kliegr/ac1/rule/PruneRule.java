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
import org.w3c.dom.Node;

/**
 *
 * @author tomas The prune rule is created from outside including quality
 * measures
 *
 */
public class PruneRule implements RuleInt {

    /**
     *
     */
    public int pruningCoverage;

    /**
     *
     */
    protected Rule rule;

    /**
     *
     * @param rule
     */
    public PruneRule(Rule rule) {
        this.rule = rule;
    }

    public String toString() {
        return rule.toString();
    }

    /**
     *
     * @return
     */
    public String getArulesRepresentation() {
        return rule.getArulesRepresentation();
    }


   
    /**
     *
     * @param hide
     * @return
     */
    public int removeSupportingTransactions(Boolean hide) {
        pruningCoverage = 0;
        try {
            Set<Transaction> supportingTransactions;

            Set<Transaction> antConOverlap = getAntecedent().getSupportingTransactions();
            Set<Transaction> conTran = getConsequent().getSupportingTransactions();
            if (antConOverlap == null) {
                //antecedent is empty (no items), the transactions supported by the rule are determined only by consequent
                antConOverlap = conTran;
            } else {
                antConOverlap.retainAll(conTran);
            }

            supportingTransactions = antConOverlap;
            pruningCoverage = supportingTransactions.size();
            if (pruningCoverage > 0) {
                //TODO: add support for parallel stream
                supportingTransactions.stream().forEach((t) -> rule.getData().getDataTable().removeTransaction(t, hide));
            }
            return pruningCoverage;
            //this should delete all references to the transaction
        } catch (NoSuchElementException e) {
            return 0;
        }
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
    private static final Logger LOG = Logger.getLogger(PruneRule.class.getName());

}
