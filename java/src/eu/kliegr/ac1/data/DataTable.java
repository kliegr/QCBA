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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 *
 * @author tomas
 */
public class DataTable {

    private final static Logger LOGGER = Logger.getLogger(DataTable.class.getName());
    /* when reading data file, this arraylist is used to map the value at the i-th position
    in the csv file to the correct attribute
     */
    private final ArrayList<Attribute> attribute_byPosition = new ArrayList();
    /* when reading file with rules, this map is used to get access
    to attributes by name
     */
    private final ConcurrentHashMap<String, Attribute> attributes = new ConcurrentHashMap();

    private final ArrayList<Transaction> transactions = new ArrayList();
    private ArrayList<Transaction> hiddenTransactions = new ArrayList();
    private Attribute target;
    private Attribute id;
    int loadedTransactions = 0;

    private int lastAID = -1;

    /**
     *
     */
    public final int firstTID = 0;
    private int lastTID = firstTID - 1;

    /**
     *
     * @param colNames
     * @param targetColName
     * @param attributeTypes
     * @param IDcolumnName
     * @throws Exception
     */
    public DataTable(String[] colNames, String targetColName, ArrayList<AttributeType> attributeTypes, String IDcolumnName) throws Exception {
        int i = -1;
        for (String col : colNames) {
            if (col.contains("=")) {
                throw new Exception("Column name cannot contain =.");
            }
            i++;
            boolean targetFlag = col.equals(targetColName);
            boolean idColumnFlag = col.equals(IDcolumnName);
            Attribute att = Attribute.makeAttribute(col, targetFlag, attributeTypes.get(i), idColumnFlag, ++lastAID);
            attributes.put(att.getName(), att);
            attribute_byPosition.add(att);
            if (targetFlag) {
                target = att;
            }
            if (idColumnFlag) {
                id = att;
            }
        }
        checkDataTable(IDcolumnName, attributeTypes);
    }

    /**
     *
     * @return
     */
    public Attribute getTargetAttribute() {
        return target;
    }

    /**
     *
     * @param name
     * @return
     */
    public Attribute getAttribute(String name) {
        return attributes.get(name);
    }

    /**
     *
     * @return
     */
    public int getLoadedTransactionCount() {
        return loadedTransactions;
    }

    /**
     *
     * @return
     */
    public int getCurrentTransactionCount() {
        return transactions.size();
    }

    /**
     *
     * @return
     */
    public ArrayList<Transaction> getAllCurrentTransactions() {
        return transactions;
    }

    /**
     *
     */
    public void removeAllTransactions() {
        while (transactions.size() > 0) {
            removeTransaction(transactions.get(0), false);
        }
    }

    /**
     *
     */
    public void unhideAllTransactions() {
        hiddenTransactions.stream().map((t) -> {
            t.reregisterWithAllAttributeValues();
            return t;
        }).forEach((t) -> {
            transactions.add(t);
        });
        hiddenTransactions = new ArrayList();
    }

    /**
     *
     * @param t
     * @param hide
     */
    public void removeTransaction(Transaction t, Boolean hide) {
        t.deregisterFromAllAttributeValues();
        transactions.remove(t);
        if (hide) {
            hiddenTransactions.add(t);
        }
    }

    /**
     *
     * @param numberOfOneItemsets
     * @return
     */
    public float getMinSupportThreshold(int numberOfOneItemsets) {
        List<Integer> supps = attribute_byPosition.stream().filter(a -> !a.isTargetAttribute).map(attribute -> attribute.getNumberOfValuesWithSupport()).flatMap(l -> l.stream()).collect(Collectors.toList());
        //sorted ascending
        Collections.sort(supps);
        float relSupport;
        if (supps.size() <= numberOfOneItemsets) {
            //set minimum support to zero, even with this threshold there is less one itemsets than desired
            relSupport = 0;
        } else {
            long absoluteSupport = supps.get(supps.size() - numberOfOneItemsets);
            relSupport = absoluteSupport / (float) this.loadedTransactions;

        }
        LOGGER.log(Level.INFO, "Determined relative support to obtain maximum {0}  one itemsets:{1}", new Object[]{numberOfOneItemsets, relSupport});
        return relSupport;

    }

    /**
     *
     * @param targetSupport
     * @param maxlen
     * @return
     */
    public long getMaxCombinationCount(int targetSupport, long maxlen) {
        //for each attribute holds number of values  meeting targetSupport
        ArrayList<Integer> freqs = attribute_byPosition.stream().map(attribute -> attribute.getNumberOfValuesWithMinSupport(targetSupport)).collect(Collectors.toCollection(ArrayList::new));

        //for simplicity, assume that attribute values correspond to items
        //number of distinct items = number of distinct attribute values
        long itemcount = 0;
        for (int freq : freqs) {
            itemcount += freq;
        }

        return Combinations.choose(itemcount, maxlen);
    }

    /**
     *
     * @param position
     * @return
     */
    public Attribute getAttribute(int position) {
        return attribute_byPosition.get(position);
    }

    private Transaction makeTransaction(String[] vector) {
        Transaction t = new Transaction(++lastTID, firstTID);
        return t;
    }

    /**
     *
     * @param vector
     * @throws NumberFormatException
     */
    public void addTransaction(String[] vector) throws java.lang.NumberFormatException {
        Transaction t = makeTransaction(vector);
        transactions.add(t);
        loadedTransactions++;
        //transactions.add(t);
        for (int i = 0; i < vector.length; i++) {
            Attribute at = getAttribute(i);
            AttributeValue attributeValue = null;
            try {
                attributeValue = at.getValueByString(vector[i]);
            } catch (java.lang.NumberFormatException e) {
                LOGGER.log(Level.INFO, "NumberFormatException for value {0}", vector[i]);
                throw e;

            }
            if (attributeValue == null) {
                try {
                    at.addNewValue(vector[i], t, AttributeValueType.dataBacked);
                } catch (java.lang.NumberFormatException e) {
                    LOGGER.log(Level.INFO, "NumberFormatException for value {0}", vector[i]);
                    at.addNewValue(vector[i], t, AttributeValueType.dataBacked);
                    throw e;
                }

            } else {
                attributeValue.addTransaction(t, true);
            }
        }
    }

    /**
     *
     */
    public void printDataTable() {
        attributes.values().stream().forEach((at) -> {
            LOGGER.info(at.toString());
        });
    }

    private void checkDataTable(String IDcolumnName, ArrayList<AttributeType> attributeTypes) throws Exception {
        if (target == null) {
            throw new Exception("Target variable not specified or found");
        } else if (attributes.size() < 2) {
            throw new Exception("No predictor specified");
        } else if (IDcolumnName != null && id == null) {
            throw new Exception("ID column not found");
        } else if (attributeTypes.size() != attributes.size()) {
            throw new Exception("Mismatch in the number of specified attribute types (" + attributeTypes.size() + ") and detected attributes (" + attributes.size() + ")");
        }

    }

    /**
     *
     * @return
     */
    public Attribute getIDAttribute() {
        return id;

    }
}
