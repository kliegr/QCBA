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
package eu.kliegr.ac1.rule.parsers;

import eu.kliegr.ac1.rule.RuleMultiItem;
import eu.kliegr.ac1.rule.extend.RuleMultiItemAnnotation;
import java.util.logging.Logger;

/**
 *
 * @author tomas
 */
public class AnnotatedRuleMultiItem {

    RuleMultiItem rmi;
    RuleMultiItemAnnotation annotation;

    /**
     *
     * @param rmi
     * @param annotation
     */
    public AnnotatedRuleMultiItem(RuleMultiItem rmi, RuleMultiItemAnnotation annotation) {
        this.rmi = rmi;
        this.annotation = annotation;
    }
    private static final Logger LOG = Logger.getLogger(AnnotatedRuleMultiItem.class.getName());

}
