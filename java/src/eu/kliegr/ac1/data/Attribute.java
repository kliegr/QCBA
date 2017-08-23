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
package eu.kliegr.ac1.data;

import static eu.kliegr.ac1.rule.parsers.ArulesParser.normInfinity;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.logging.Logger;

/**
 *
 * @author tomas
 */
public class Attribute {

    //submap of the previous map used for fast access to breakpoint values
    private final static Logger LOGGER = Logger.getLogger(Attribute.class.getName());

    /**
     *
     * @param name
     * @param target
     * @param type
     * @param IDcolumn
     * @param AID
     * @return
     */
    public static Attribute makeAttribute(String name, boolean target, AttributeType type, boolean IDcolumn, int AID) {
        // target attribute has AID equal to 0
        // predictors have AID greater than 0

        Attribute a = new Attribute(name, AID, target, type, IDcolumn);
        return a;
    }
    private final String name;
    private final ConcurrentSkipListMap attributeValues = new ConcurrentSkipListMap();
    private final ConcurrentSkipListMap breakpointAttributeValues = new ConcurrentSkipListMap();

    /**
     *
     */
    public final int AID;
    // target attribute has AID equal to 0
    // predictors have AID greater than 0

    final boolean isTargetAttribute;
    private final AttributeType type;
    final boolean isIDAttribute;
    
    //this indicates that the extreme values of this attribute are infinity
    //boolean nonFinite = false;
    /**
     *
     * @param name
     * @param AID
     * @param target
     * @param type
     * @param IDcolumnFlag
     */
    protected Attribute(String name, int AID, boolean target, AttributeType type, boolean IDcolumnFlag) {
        this.type = type;
        if (name == null) {
            name = "#A" + AID;
        }
        this.name = name;
        this.AID = AID;
        this.isTargetAttribute = target;
        this.isIDAttribute = IDcolumnFlag;
        //this.type = type;
    }
    
/*    public void setAsNonFinite()
    {
        //check if this attribute has at least two distinct values
        
        nonFinite=true;
        LOGGER.info("Setting attribute" + name + " as non finite");
    } */
    
    /**
     *
     * @return
     */
    public String getName() {
        return name;
    }

    /*   public Attribute getAnnotated()
    {
        if (annotated == null)
        {
            annotated = new Attribute(name,AID,isTargetAttribute,type,isIDAttribute);            
        }
        return annotated;
    }*/

    /**
     *
     * @return
     */
    public AttributeType getType() {
        return type;
    }

    /**
     *
     * @param minSupp
     * @return
     */
    public int getNumberOfValuesWithMinSupport(int minSupp) {
        int total = 0;
        total = getAllValues().stream().filter((val) -> (val.getTransactions().size() >= minSupp)).map((_item) -> 1).reduce(total, Integer::sum);
        return total;

    }

    /**
     *
     * @return
     */
    public ArrayList<Integer> getNumberOfValuesWithSupport() {
        ArrayList<Integer> list = new ArrayList();

        getAllValues().stream().forEach((val) -> {
            //arraylist supports duplicates
            list.add(val.getTransactions().size());
        });

        return list;

    }

    /**
     *
     * @param value
     * @return
     */
    public AttributeValue getValueByString(String value) {
        if (type == AttributeType.numerical) {
            if (value.isEmpty() | value == null) {
                //TODO CHECK IF THIS TREATMENT OF MISSING VALUES DOES NOT CAUSE PROBLEMS
                return (AttributeValue) attributeValues.get(Float.NaN);
            } else {
                return (AttributeValue) attributeValues.get(Float.parseFloat(normInfinity(value)));
            }

        } else {
            if (value == null) {
                LOGGER.warning("Passed null value converted to empty string");
                value = "";
            }
            return (AttributeValue) attributeValues.get(value);
        }

    }

    /**
     *
     * @param value
     * @return
     */
    public AttributeValue getAdjacentHigher(AttributeValue value) {
        return getAdjacentHigher(value, false);
    }

    /**
     *
     * @param value
     * @param requireBreakpoint
     * @return
     */
    public AttributeValue getAdjacentHigher(AttributeValue value, boolean requireBreakpoint) {
        ConcurrentSkipListMap map = requireBreakpoint ? breakpointAttributeValues : attributeValues;
        if (type == AttributeType.numerical) {
            Entry<Float, AttributeValue> e;
            e = map.higherEntry(Float.parseFloat(value.value));

            if (e == null) {
                return null;
            }
            if (e.getKey().equals(Float.NaN)) {
                return null;
            }
            return e.getValue();
        } else {
            throw new UnsupportedOperationException("nominal attributes not yet suppported");
        }
    }

    /**
     *
     * @param value
     * @return
     */
    public AttributeValue getAdjacentLower(AttributeValue value) {
        return getAdjacentLower(value, false);
    }

    /**
     *
     * @param value
     * @param requireBreakpoint
     * @return
     */
    public AttributeValue getAdjacentLower(AttributeValue value, boolean requireBreakpoint) {
        ConcurrentSkipListMap map = requireBreakpoint ? breakpointAttributeValues : attributeValues;
        if (type == AttributeType.numerical) {

            Entry<Float, AttributeValue> e = map.lowerEntry(Float.parseFloat(value.value));
            if (e == null) {
                return null;
            }
            return e.getValue();
        } else {
            throw new UnsupportedOperationException("nominal attributes not yet suppported");
        }
    }

    /**
     *
     * @param fromKey
     * @param fromInclusive
     * @param toKey
     * @param toInclusive
     * @return
     */
    public Collection<AttributeValue> getValuesInRange(Float fromKey, boolean fromInclusive, Float toKey, boolean toInclusive) {
        return attributeValues.subMap(fromKey, fromInclusive, toKey, toInclusive).values();
    }

    /**
     *
     * @return
     */
    public Collection<AttributeValue> getAllValues() {
        ArrayList copy = new ArrayList(attributeValues.values());
        return copy;
    }

    /**
     *
     * @param val
     */
    public void setAttributeValueAsBreakpoint(AttributeValue val) {
        if (type == AttributeType.numerical) {
            breakpointAttributeValues.put(val.getNumericalValue(), val);
        } else {
            breakpointAttributeValues.put(val.value, val);
        }
    }

    /**
     *
     * @param value
     * @param TID
     * @param attValtype
     * @return
     */
    public AttributeValue addNewValue(String value, Transaction TID, AttributeValueType attValtype) {

        if (TID == null && attValtype == AttributeValueType.dataBacked) {

            throw new UnsupportedOperationException("TID can be null only for breakpoint values");
        }

        AttributeValue val = new AttributeValue(value, this, attValtype);
        if (TID != null) {
            val.addTransaction(TID, true);
        }

        if (type == AttributeType.numerical) {
            attributeValues.put(val.getNumericalValue(), val);
        } else {
            attributeValues.put(val.value, val);
        }
        if (attValtype == AttributeValueType.breakpoint || attValtype == AttributeValueType.dataBackedbreakpoint) {
            setAttributeValueAsBreakpoint(val);
        }
        return val;

    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("ATTRIBUTE:");
        sb.append(name);
        Collection<AttributeValue> values = attributeValues.values();
        values.stream().map((at) -> {
            sb.append("\n#VALUE:");
            sb.append(at.value);
            return at;
        }).forEach((at) -> {
            sb.append("\n##transactions:");
            at.transactions.stream().map((t) -> String.valueOf(t.internalTID)).reduce((t, t2) -> t + "," + t2).ifPresent((transAsString) -> sb.append(transAsString));
        });
        return sb.toString();
    }
}
