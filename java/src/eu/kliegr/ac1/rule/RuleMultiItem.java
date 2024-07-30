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

import eu.kliegr.ac1.data.Attribute;
import eu.kliegr.ac1.data.AttributeType;
import eu.kliegr.ac1.data.AttributeValue;
import eu.kliegr.ac1.data.Transaction;
import eu.kliegr.ac1.rule.extend.ValueOrigin;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author tomas
 */
public class RuleMultiItem {

    private final static Logger LOGGER = Logger.getLogger(RuleMultiItem.class.getName());

    /**
     *
     */
    public static final String INTERVAL_SEPARATOR = ";";
    private ArrayList<AttributeValue> attributeValues;
    private ArrayList<ValueOrigin> valueOrigin;

    /**
     *
     */
    public ValueOrigin lastModificationType;
    private Attribute attribute;

    /**
     *
     * @param attributeValues
     * @param attribute
     */
    protected RuleMultiItem(ArrayList<AttributeValue> attributeValues, Attribute attribute) {
        this.lastModificationType = null;
        this.attribute = attribute;
        this.attributeValues = attributeValues;
        valueOrigin = new ArrayList();
        attributeValues.stream().forEach((value) -> {
            valueOrigin.add(ValueOrigin.core);
        });
    }

    /**
     *
     * @param attributeValues
     * @param valueOrigin
     * @param attribute
     * @param lastModificationType
     */
    protected RuleMultiItem(ArrayList<AttributeValue> attributeValues, ArrayList<ValueOrigin> valueOrigin, Attribute attribute, ValueOrigin lastModificationType) {
        this.lastModificationType = lastModificationType;
        this.attribute = attribute;
        this.attributeValues = attributeValues;
        this.valueOrigin = valueOrigin;
    }

    /**
     *
     * @return
     */
    public Attribute getAttribute() {
        return attribute;
    }

    /**
     *
     * @param val
     * @return
     */
    public ValueOrigin getValueOrigin(AttributeValue val) {
        int index = attributeValues.indexOf(val);
        if (index < 0) {
            return null;
        }
        return valueOrigin.get(index);
    }

    /**
     *
     * @return
     */
    public ArrayList<AttributeValue> getAttributeValues() {
        return attributeValues;
    }

    /**
     *
     * @return
     */
    public ArrayList<ValueOrigin> getValueOrigin() {
        return valueOrigin;
    }

    /**
     *
     * @return
     */
    public ArrayList<RuleMultiItem> getNeighbourhood() {
        ArrayList<RuleMultiItem> neighbourhood;
        if (attribute.getType() == AttributeType.numerical) {
            neighbourhood = new ArrayList();
            RuleMultiItem leftExtension = getExtended(ValueOrigin.extend_higher);
            RuleMultiItem rightExtension = getExtended(ValueOrigin.extend_lower);
            if (leftExtension != null) {
                neighbourhood.add(leftExtension);
            }
            if (rightExtension != null) {
                neighbourhood.add(rightExtension);
            }
        } else {
            neighbourhood = getExtended_Nominal_Greedy();
        }
        return neighbourhood;
    }

    /**
     *
     * @return
     */
    public ArrayList<RuleMultiItem> getExtended_Nominal_Greedy() {
        ArrayList<RuleMultiItem> rmiCandidates = new ArrayList();
        Collection<AttributeValue> candidates = attribute.getAllValues();
        candidates.removeAll(attributeValues);
        if (candidates.isEmpty()) {
            LOGGER.log(Level.FINE, "Cannot perform greedy nominal extension, no values remaining on item:{0}", toString(true, true, false));
            return rmiCandidates;
        }

        ArrayList<ValueOrigin> copyValOrigin = new ArrayList(valueOrigin);
        copyValOrigin.add(ValueOrigin.extend_greedy);

        candidates.forEach((candidate) -> {
            ArrayList<AttributeValue> copy = new ArrayList(attributeValues);
            copy.add(candidate);
            rmiCandidates.add(new RuleMultiItem(copy, copyValOrigin, attribute, ValueOrigin.extend_greedy));
        });

        return rmiCandidates;

    }

    /**
     *
     * @param extensionType
     * @return
     */
    public RuleMultiItem getExtended(ValueOrigin extensionType) {
        if (attribute.getType() == AttributeType.nominal) {
            LOGGER.log(Level.FINE, "Extension {0} not possible for nominal attributes", extensionType);

            return null;

        }
        AttributeValue toBeAddedLower = null;
        AttributeValue toBeAddedHigher = null;
        if (extensionType == ValueOrigin.extend_lower || extensionType == ValueOrigin.fuzzy_border) {
            if (attributeValues.isEmpty()) {
                LOGGER.severe("Rule with zero values");
            } else {

                toBeAddedLower = attribute.getAdjacentLower(attributeValues.get(0));
            }

        }
        if (extensionType == ValueOrigin.extend_higher || extensionType == ValueOrigin.fuzzy_border) {
            if (attributeValues.isEmpty()) {
                LOGGER.severe("Rule with zero values");
            } else {
                toBeAddedHigher = attribute.getAdjacentHigher(attributeValues.get(attributeValues.size() - 1));
            }

        }
        if (!(extensionType == ValueOrigin.extend_lower || extensionType == ValueOrigin.extend_higher || extensionType == ValueOrigin.fuzzy_border)) {
            throw new UnsupportedOperationException("Unsupported extension operation");
        }
        if (toBeAddedLower == null && toBeAddedHigher == null) {
            LOGGER.log(Level.FINE, "Cannot perform ''{0}'', no values remaining on item:{1}", new Object[]{extensionType, toString(true, true, false)});
            return null;
        } else {
            LOGGER.log(Level.FINE, "Performing ''{0}'' on multiitem:{1}", new Object[]{extensionType, toString(true, true, false)});
        }
        /*else if(extensionState.get(toBeAdded)==ExtensionState.rejected)
        {
            System.out.println(toString()  + ": cannot extend, rejected");
        }*/
        ArrayList<AttributeValue> copy = new ArrayList(attributeValues);
        ArrayList<ValueOrigin> copyValOrigin = new ArrayList(valueOrigin);
        if (toBeAddedLower != null) {
            copy.add(0, toBeAddedLower);
            copyValOrigin.add(0, extensionType);

        }
        if (toBeAddedHigher != null) {
            //order of these two commands matters
            copyValOrigin.add(copy.size(), extensionType);
            copy.add(copy.size(), toBeAddedHigher);

        }
        return new RuleMultiItem(copy, copyValOrigin, attribute, extensionType);

    }

    /**
     *
     * @param withAttributeName
     * @param valueOrigin
     * @param succint
     * @return
     */
    public String toString(boolean withAttributeName, boolean valueOrigin, boolean succint) {
        StringBuilder sb = new StringBuilder();
        if (withAttributeName) {
            sb.append(attribute.getName()).append("=");
        }
        String del = "";

        if (succint & attribute.getType() == AttributeType.numerical & attributeValues.size() > 1) {
            sb.append("[");
            sb.append(attributeValues.get(0));
            sb.append(INTERVAL_SEPARATOR);
            sb.append(attributeValues.get(attributeValues.size() - 1));
            sb.append("]");
        } else {
            for (AttributeValue value : attributeValues) {
                sb.append(del);
                sb.append(value.toString(false, valueOrigin));

                del = ",";
            }
        }
        return sb.toString();

    }

    @Override
    public String toString() {
        return toString(true, false, false);
    }

    /**
     *
     * @return
     */
    public Set<Transaction> getSupportingTransactions() {
        //merges transactions supporting all values into one set

        return attributeValues.parallelStream().map((value)
                -> value.getTransactions()).
                reduce(Collections.newSetFromMap(new ConcurrentHashMap<Transaction, Boolean>()), (set, set2) -> {
                    set.addAll(set2);
                    return set;
                });

    }

}
