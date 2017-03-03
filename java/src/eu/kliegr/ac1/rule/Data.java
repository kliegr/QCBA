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
import eu.kliegr.ac1.data.AttributeValueType;
import eu.kliegr.ac1.data.DataTable;
import eu.kliegr.ac1.rule.extend.ValueOrigin;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Data {

    private final static Logger LOGGER = Logger.getLogger(Data.class.getName());
    private DataTable dt;
    private RuleMultiItemCache cache = new RuleMultiItemCache();

    /**
     *
     */
    public void purge() {
        dt = null;
        cache = new RuleMultiItemCache();
    }

    /**
     *
     * @return
     */
    public boolean isInitialized() {
        return dt != null;
    }


    /**
     *
     * @param colNames
     * @param targetColName
     * @param attributeTypes
     * @param IDcolumnName
     * @return
     * @throws Exception
     */
    public DataTable newDataTable(String[] colNames, String targetColName, ArrayList<AttributeType> attributeTypes, String IDcolumnName) throws Exception {
        dt = new DataTable(colNames, targetColName, attributeTypes, IDcolumnName);
        return dt;
    }

    /**
     *
     * @return
     */
    public DataTable getDataTable() {
        return dt;
    }

    /**
     *
     * @return
     */
    public Attribute getTargetAttribute() {
        return dt.getTargetAttribute();
    }

    /*
    this method is intended for loading rmis from data in default state and employs caching
     */
    /**
     *
     * @param values
     * @param attributeName
     * @return
     */
    public RuleMultiItem makeRuleItem(ArrayList<AttributeValue> values, String attributeName) {
        RuleMultiItem rmi;
        rmi = cache.get(values);
        if (rmi != null) {
        } else {
            rmi = new RuleMultiItem(values, dt.getAttribute(attributeName));
            cache.put(rmi);
        }
        return rmi;
    }

    /*
    this method is intended for run time derivation of rule multi items and does not use caching
    since rmis with the same underlying attribute values can have e.g. different origin properties
     */

    /**
     *
     * @param attributeValues
     * @param valueOrigin
     * @param attribute
     * @param lastModificationType
     * @return
     */
    public RuleMultiItem makeRuleItem(ArrayList<AttributeValue> attributeValues, ArrayList<ValueOrigin> valueOrigin, Attribute attribute, ValueOrigin lastModificationType) {
        RuleMultiItem ri = new RuleMultiItem(attributeValues, valueOrigin, attribute, lastModificationType);
        return ri;
    }

    /**
     *
     * @return
     */
    public Collection<AttributeValue> getValuesOfTargetAttribute() {
        {
            Attribute at = dt.getTargetAttribute();
            return at.getAllValues();
        }

    }

    /**
     *
     * @param attributeName
     * @param leftMargin
     * @param fromInclusive
     * @param rightMargin
     * @param toInclusive
     * @param negated
     * @return
     */
    public Collection<AttributeValue> getValuesInRange(String attributeName, float leftMargin, boolean fromInclusive, float rightMargin, boolean toInclusive, boolean negated) {
        Attribute at = dt.getAttribute(attributeName);

        Collection<AttributeValue> list;
        if (!negated) {
            try {
                list = at.getValuesInRange(leftMargin, fromInclusive, rightMargin, toInclusive);

            } catch (Exception e) {
                list = at.getValuesInRange(leftMargin, fromInclusive, rightMargin, toInclusive);
                throw e;
            }
        } else {
            Collection<AttributeValue> negativeList = at.getValuesInRange(leftMargin, fromInclusive, rightMargin, toInclusive);
            list = at.getAllValues();
            list.removeAll(negativeList);
        }
        if (list.isEmpty()) {
            LOGGER.log(Level.INFO, "Warning: no matching attribute value found for {0}={1};{2}", new Object[]{attributeName, leftMargin, rightMargin});
        }
        return list;

    }

  

    /**
     *
     * @param attributeName
     * @param value
     * @param type
     * @return
     * @throws AttributeNotFoundException
     */
    public AttributeValue getValue(String attributeName, String value, AttributeValueType type) throws AttributeNotFoundException {
        Attribute at = dt.getAttribute(attributeName);
        if (at == null) {
            throw new AttributeNotFoundException("Attribute " + attributeName + " not found in the data");
        }
        AttributeValue val = at.getValueByString(value);
        if (val == null) {
            val = at.addNewValue(value, null, type);
        } else if (val.getType() != type) {
            val.updateType(type);
        }
        return val;
    }

    /**
     *
     * @param attributeName
     * @param values
     * @param negated
     * @param type
     * @return
     * @throws AttributeNotFoundException
     */
    public Collection<AttributeValue> getValuesByEnumeration(String attributeName, String[] values, boolean negated, AttributeValueType type) throws AttributeNotFoundException {
        Attribute at = dt.getAttribute(attributeName);
        Collection<AttributeValue> list;
        if (!negated) {
            list = new ArrayList();
            for (String value : values) {
                AttributeValue val = getValue(attributeName, value, type);
                list.add(val);
            }
        } else {
            list = at.getAllValues();
            list.removeAll(Arrays.asList(values));
        }
        if (list.isEmpty()) {
            LOGGER.log(Level.INFO, "Warning: no matching attribute value found for {0}={1}", new Object[]{attributeName, Arrays.toString(values)});
        }
        return list;
    }

}
