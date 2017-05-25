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

import eu.kliegr.ac1.data.AttributeValue;
import eu.kliegr.ac1.rule.Antecedent;
import eu.kliegr.ac1.rule.Consequent;
import eu.kliegr.ac1.rule.Rule;
import eu.kliegr.ac1.rule.RuleMultiItem;
import eu.kliegr.ac1.rule.RuleQuality;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author tomas
 */
public class ExtendRuleAnnotation {

    private final static Logger LOGGER = Logger.getLogger(Antecedent.class.getName());
    //annotation of all multiitems in rule antecedent
    //consequents - set of consequents to consider on RHS
    private final HashMap<RuleMultiItem, RuleMultiItemAnnotation> annotations = new HashMap();

    /**
     *
     * @param rmi
     * @return
     */
    public RuleMultiItemAnnotation getAnnotation(RuleMultiItem rmi) {
        return annotations.get(rmi);
    }

    /**
     *
     * @param r
     * @param consequents
     */
    public void generate(ExtendRule r, ArrayList<Consequent> consequents) {

        //get number of narrow rule computations
        int total_combinations = r.getAntecedent().getItems().stream().mapToInt((x) -> x.getAttributeValues().size()).sum() * consequents.size();
        LOGGER.log(Level.INFO, "There are {0} narrow rules to be generated to annotate the current rule", total_combinations);
        AtomicInteger processedCombinations = new AtomicInteger(0);

        r.getAntecedent().getItems().stream().forEach((ruleConstituent) -> {
            RuleMultiItemAnnotation annot = generateRuleMultiItemAnnotation(r, ruleConstituent, consequents);
            processedCombinations.addAndGet(ruleConstituent.getAttributeValues().size() * consequents.size());
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(Level.FINE, "Processed {0} combinations out of {1}", new Object[]{processedCombinations, total_combinations});
            }
            annotations.put(ruleConstituent, annot);
        });
    }

    @Override
    public String toString() {

        if (annotations == null) {
            return "No items - not annotations";
        } else {
            StringBuilder sb = new StringBuilder();
            annotations.forEach((item, annotation) -> {
                sb.append("Annotations of multiitem'").append(item).append("'\n");
                sb.append(annotation);
            });
            return sb.toString();
        }

    }

    private ExtendRule deriveNarrowRule(ExtendRule seedRule, RuleMultiItem toReplace, AttributeValue replacementAntConstituent, Consequent consequent) {
        //prepare antecedent
        ArrayList<AttributeValue> replacementItemWrapped = new ArrayList();
        replacementItemWrapped.add(replacementAntConstituent);
        ValueOrigin val = toReplace.getValueOrigin(replacementAntConstituent);
        if (val == null) {
            LOGGER.severe("Something is wrong, the value origin should be known");
        }
        ArrayList<ValueOrigin> valOriginAsArray = new ArrayList();
        valOriginAsArray.add(val);

        RuleMultiItem replacement = seedRule.getRule().getData().makeRuleItem(replacementItemWrapped, valOriginAsArray, toReplace.getAttribute(), ValueOrigin.narrow);

        ArrayList<RuleMultiItem> itemsForAntecedent = new ArrayList();
        //copy rule antecedent
        seedRule.getAntecedent().getItems().stream().forEach((rmi) -> {
            if (rmi == toReplace) {
                if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.log(Level.FINE, "Replacing {0} with {1}", new Object[]{toReplace, replacement});
                }
                //perform replacement
                itemsForAntecedent.add(replacement);
            } else {
                itemsForAntecedent.add(rmi);
            }
        });
        //antecedent is now ready
        Antecedent newAnt = new Antecedent(itemsForAntecedent);

        //prepare rule
        Rule r = new Rule(newAnt, consequent, null, null, seedRule.getRID(), seedRule.getRule().getData());
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine("Creating new Extend rule within narrow rule procedure");
        }

        ExtendRule extRule = new ExtendRule(r, seedRule.getHistory(), seedRule.getExtendType(),seedRule.getExtendRuleConfig());
        return extRule;
        //copy rule antecedent

    }

    private RuleMultiItemAnnotation generateRuleMultiItemAnnotation(ExtendRule r, RuleMultiItem rmi, ArrayList<Consequent> consequents) {
        RuleMultiItemAnnotation rmiAnnot = new RuleMultiItemAnnotation();

        rmi.getAttributeValues().stream().map((val) -> {
            AttributeValueAnnotation annot = new AttributeValueAnnotation(val, rmi.getValueOrigin(val));
            consequents.stream().forEach((cons) -> {
                ExtendRule narrowRule = deriveNarrowRule(r, rmi, val, cons);
                RuleQuality quality = narrowRule.getRuleQuality();
                annot.add(cons, quality);
            });
            return annot;
        }).forEach((annot) -> {
            rmiAnnot.add(annot);
        });
        return rmiAnnot;


    }

}
