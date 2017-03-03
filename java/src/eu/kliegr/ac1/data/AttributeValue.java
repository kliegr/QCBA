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

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class AttributeValue {

    String value;
    Set<Transaction> transactions = Collections.newSetFromMap(new ConcurrentHashMap<Transaction, Boolean>());
    Attribute attribute;
    private AttributeValueType type;

    AttributeValue(String value, Attribute attribute, AttributeValueType type) {
        this.type = type;
        this.value = value;
        this.attribute = attribute;
    }

    /**
     *
     * @return
     */
    public String getValue() {
        return value;
    }

    /**
     *
     * @param withAttributeName
     * @param valueOrigin
     * @return
     */
    public String toString(boolean withAttributeName, boolean valueOrigin) {
        String attName = "";
        String origin = "";
        if (withAttributeName) {
            attName = attribute.getName() + "=";
        }
        if (valueOrigin) {
            if (type != AttributeValueType.dataBacked) {
                origin = " (" + type.toString() + ")";
            }

        }

        return attName + value + origin;

    }

    public String toString() {
        return toString(false, false);
    }

    /*
    throws an exception if this is not a numerical attribute value
     */

    /**
     *
     * @return
     */
    public Float getNumericalValue() {
        if ((value.isEmpty() | value == null)) {
            return Float.NaN;
        } else {
            return Float.parseFloat(value);
        }
    }

    /**
     *
     * @return
     */
    public AttributeValueType getType() {
        return type;
    }

    /**
     *
     * @param type
     */
    public void updateType(AttributeValueType type) {
        if ((type == AttributeValueType.breakpoint || type == AttributeValueType.dataBackedbreakpoint) && getType() == AttributeValueType.dataBacked) {
            attribute.setAttributeValueAsBreakpoint(this);
            this.type = AttributeValueType.dataBackedbreakpoint;
        } else if (type == AttributeValueType.breakpoint && this.type == AttributeValueType.dataBackedbreakpoint) {
            // do nothing
        } else {
            throw new UnsupportedOperationException("Unforeseen combination of states");
        }
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
     * @param t
     * @param registerAttributeValue
     */
    public void addTransaction(Transaction t, boolean registerAttributeValue) {
        if (registerAttributeValue) {
            t.registerAttributeValue(this);
        }
        transactions.add(t);

    }

    /**
     *
     * @param t
     */
    public void removeTransaction(Transaction t) {
        transactions.remove(t);
    }


    /**
     *
     * @return
     */
    public Set<Transaction> getTransactions() {
        return Collections.unmodifiableSet(transactions);
    }

}
