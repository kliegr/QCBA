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

import eu.kliegr.ac1.data.AttributeType;
import eu.kliegr.ac1.data.AttributeValue;
import eu.kliegr.ac1.rule.Consequent;
import eu.kliegr.ac1.rule.RuleMultiItem;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TestRuleAnnotation extends ExtendRuleAnnotation {

    private static final Logger LOGGER = Logger.getLogger(TestRuleAnnotation.class.getName());
    private final HashMap<AttributeValue, AttributeValueAnnotation> attributeValAnns = new HashMap();

    /**
     *
     * @param rmi
     * @param annot
     */
    public void addAnnotation(RuleMultiItem rmi, RuleMultiItemAnnotation annot) {
        if (annot == null) {
            return;
        }
        annot.getAnnotations().forEach((ann) -> attributeValAnns.put(ann.getValue(), ann));
    }

    /**
     *
     * @param r
     * @param consequents
     */
    @Override
    public void generate(ExtendRule r, ArrayList<Consequent> consequents) {
        throw new UnsupportedOperationException("Not supported");
    }

    /**
     *
     * @param rmi
     * @return
     */
    @Override
    public RuleMultiItemAnnotation getAnnotation(RuleMultiItem rmi) {
        RuleMultiItemAnnotation annot = new RuleMultiItemAnnotation();
        rmi.getAttributeValues().forEach((val) -> annot.add(attributeValAnns.get(val)));
        return annot;
    }

    @Override
    public String toString() {
        return attributeValAnns.values().stream().map((val) -> val.toString()).reduce((val1, val2) -> val1 + "\n" + val2).orElse("");
    }

    /**
     *
     * @param tVal
     * @return
     */
    public ValueOrigin getValueOrigin(AttributeValue tVal) {
        AttributeValueAnnotation val = attributeValAnns.get(tVal);
        if (val == null) {
            return null;
        }
        return val.getOrigin();
    }

    /**
     *
     * @param tVal
     * @param distrFactory
     * @return
     */
    public Distribution getDistributionForValue(AttributeValue tVal, DistributionFactory distrFactory) {

        Distribution dist;
        AttributeValueAnnotation val;
        val = attributeValAnns.get(tVal);
        if (val != null) {
            if (LOGGER.isLoggable(Level.FINEST)) {
                LOGGER.log(Level.FINEST, "Annotated value ''{0}'' found, exact match", tVal.toString(true, true));
            }
            dist = distrFactory.convert(val);
        } else {
            if (tVal.getAttribute().getType() != AttributeType.numerical) {
                if (LOGGER.isLoggable(Level.FINEST)) {
                    LOGGER.log(Level.FINEST, "Annotated value not found for ''{0}'', interpolation not possible for nominal attribute", tVal.toString(true, true));
                }
                return null;
            }
            //try finding closest values and interpolate the annotations
            AttributeValue higher = tVal.getAttribute().getAdjacentHigher(tVal, true);
            AttributeValue lower = tVal.getAttribute().getAdjacentLower(tVal, true);

            if (higher == null && lower == null) {
                if (LOGGER.isLoggable(Level.FINEST)) {
                    LOGGER.log(Level.FINEST, "No annotated adjacent values found for ''{0}''", tVal.toString(true, true));
                }
                return null;
            }

            if (higher == null) {
                if (LOGGER.isLoggable(Level.FINEST)) {
                    LOGGER.log(Level.FINEST, "Only lower value found for ''{0}'', using this.", tVal.toString(true, true));
                }
                val = attributeValAnns.get(lower);
                dist = distrFactory.convert(val);
            } else if (lower == null) {
                if (LOGGER.isLoggable(Level.FINEST)) {
                    LOGGER.log(Level.FINEST, "Only higher value found for ''{0}'', using this.", tVal.toString(true, true));
                }
                val = attributeValAnns.get(higher);
                dist = distrFactory.convert(val);
            } else //only numerical attributes reach here
            {
                //need to interpolate
                float[] weights = new float[2];
                weights[1] = (tVal.getNumericalValue() - lower.getNumericalValue()) / (higher.getNumericalValue() - lower.getNumericalValue());
                weights[0] = 1 - weights[1];
                ArrayList<Distribution> toAgg = new ArrayList();
                if (LOGGER.isLoggable(Level.FINEST)) {
                    LOGGER.log(Level.FINEST, "Performing interpolation for ''{0}'' between {1} with weight {2}(lower) and {3} with weight {4} (higher)", new Object[]{tVal.toString(true, true), lower.toString(true, true), weights[0], higher.toString(true, true), weights[1]});
                }
                Distribution lowerD = distrFactory.convert(attributeValAnns.get(lower));
                lowerD.setWeight(weights[0]);
                toAgg.add(lowerD);
                Distribution higherD = distrFactory.convert(attributeValAnns.get(higher));
                higherD.setWeight(weights[1]);
                toAgg.add(higherD);
                dist = distrFactory.aggregate(toAgg);
            }

        }
        return dist;
    }

}
