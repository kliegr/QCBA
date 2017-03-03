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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.logging.Logger;

/**
 *
 * @author tomas
 */
public class RuleMultiItemCache {

    private final HashMap<String, RuleMultiItem> cache = new HashMap();

    /**
     *
     * @param rmi
     */
    public void put(RuleMultiItem rmi) {
        ArrayList<AttributeValue> values = rmi.getAttributeValues();
        String hash = getHashCode(values);

        cache.put(hash, rmi);
    }

    private String getHashCode(ArrayList<AttributeValue> values) {
        String hash = "";
        int[] hashcodes = new int[values.size()];
        for (int i = 0; i < hashcodes.length; i++) {
            hashcodes[i] = values.get(i).hashCode();
        }
        Arrays.sort(hashcodes, 0, hashcodes.length);
        String sep = "";
        for (int j = 0; j < hashcodes.length; j++) {
            hash = hash + sep + hashcodes[j];
        }
        return hash;
    }

    /*
    checks only values, not other properties of rule multi item
    not to be used for caching  multiitems created during rule extension
     */

    /**
     *
     * @param av
     * @return
     */
    public RuleMultiItem get(ArrayList<AttributeValue> av) {
        String hash = getHashCode(av);
        return cache.get(hash);
    }
    private static final Logger LOG = Logger.getLogger(RuleMultiItemCache.class.getName());

}
