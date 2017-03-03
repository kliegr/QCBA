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

import java.util.Comparator;
import java.util.logging.Logger;

/**
 *
 * @author tomas
 */
public class ArrayIndexComparator implements Comparator<Integer> {

    private final Float[] array;

    /**
     *
     * @param array
     */
    public ArrayIndexComparator(float[] array) {
        this.array = new Float[array.length];
        int i = 0;
        for (float f : array) {
            this.array[i++] = f;
        }

    }

    /**
     *
     * @return
     */
    public Integer[] createIndexArray() {
        Integer[] indexes = new Integer[array.length];
        for (int i = 0; i < array.length; i++) {
            indexes[i] = i; // Autoboxing
        }
        return indexes;
    }

    @Override
    public int compare(Integer index1, Integer index2) {
        // Autounbox from Integer to int to use as array indexes
        return array[index1].compareTo(array[index2]);
    }
    private static final Logger LOG = Logger.getLogger(ArrayIndexComparator.class.getName());
}
