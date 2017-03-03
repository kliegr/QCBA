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
package eu.kliegr.ac1.rule.qualityMeasure;

/**
 *
 * @author tomas
 */
public abstract class QualityMeasure {

    /**
     *
     * @param name
     * @param value
     * @return
     */
    public static QualityMeasure makeQualityMeasure(String name, String value) {
        QualityMeasure qm;
        for (String alias : Confidence.aliases) {
            if (alias.equals(name)) {
                qm = new Confidence(Double.parseDouble(value));
                return qm;
            }
        }
        for (String alias : Support.aliases) {
            if (alias.equals(name)) {
                qm = new Support(Double.parseDouble(value));
                return qm;
            }
        }
        return null;
    }

    /**
     *
     */
    protected double value;

    /**
     *
     * @param value
     */
    public QualityMeasure(double value) {
        this.value = value;
    }

    /**
     *
     * @return
     */
    public double getValue() {
        return value;
    }

    /**
     *
     * @return
     */
    public int getPriority() {
        return 1;
    }

}
