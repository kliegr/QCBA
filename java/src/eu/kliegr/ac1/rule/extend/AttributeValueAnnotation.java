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
import eu.kliegr.ac1.rule.Consequent;
import eu.kliegr.ac1.rule.RuleQuality;
import java.util.HashMap;
import java.util.logging.Logger;

/**
 *
 * @author tomas
 */
public class AttributeValueAnnotation {

    private final AttributeValue value;
    private final ValueOrigin origin;
    HashMap<Consequent, RuleQuality> qualityByConsequent = new HashMap();

    /**
     *
     * @param value
     * @param origin
     */
    public AttributeValueAnnotation(AttributeValue value, ValueOrigin origin) {
        this.value = value;
        this.origin = origin;
    }

    /**
     *
     * @param cons
     * @param quality
     */
    public void add(Consequent cons, RuleQuality quality) {
        qualityByConsequent.put(cons, quality);
    }

    /**
     *
     * @return
     */
    public AttributeValue getValue() {
        return value;
    }

    /**
     *
     * @return
     */
    public ValueOrigin getOrigin() {
        return origin;
    }

    /**
     *
     * @return
     */
    public HashMap<Consequent, RuleQuality> getDistribution() {
        return qualityByConsequent;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Value:'").append(value).append("' origin:").append(origin).append("\n");
        qualityByConsequent.forEach((consequent, quality) -> {
            sb.append("=> ").append(consequent).append(" | ").append(quality).append("\n");
        });
        return sb.toString();
    }
    private static final Logger LOG = Logger.getLogger(AttributeValueAnnotation.class.getName());

}
