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
package eu.kliegr.ac1.Rinterface.discretization;

import java.util.ArrayList;
import java.util.logging.Logger;

public class AttributeDiscretization {

    String attributeName;
    float[] cutOffpoints;
    boolean entireRange = false;
    //HashMap<AttributeValue,String> mapping;

    // CALL FOR NO DISCRETIZATION
    /**
     *
     */
    public AttributeDiscretization() {

    }

    /**
     *
     * @param cutpoints
     */
    public AttributeDiscretization(ArrayList<String> cutpoints) {
        int i = 0;
        cutOffpoints = new float[cutpoints.size()];
        for (String cpoint : cutpoints) {
            if (cpoint.equals("\"All\"")) {
                entireRange = true;
                break;
            }
            this.cutOffpoints[i++] = Float.parseFloat(cpoint);
        }
    }

    /**
     *
     * @param originalValue
     * @return
     */
    public String convert(String originalValue) {
        //BEWARE: Float.MIN_VALUE is positive:http://stackoverflow.com/questions/9746850/min-value-of-float-in-java-is-positive-why
        if (entireRange) {
            return ("\"[" + -Float.MAX_VALUE + ";" + Float.MAX_VALUE + "]\"");
        } else if (originalValue.isEmpty()) {
            return originalValue;
        } else if (cutOffpoints == null) {
            return originalValue;
        } else {
            float originalValueF = Float.parseFloat(originalValue);
            for (int i = 0; i < cutOffpoints.length; i++) {
                if (originalValueF < cutOffpoints[i]) {
                    float lowerBound;
                    float upperBound;
                    if (i == 0) {
                        lowerBound = -Float.MAX_VALUE;
                    } else {
                        lowerBound = cutOffpoints[i - 1];
                    }
                    upperBound = cutOffpoints[i];
                    return "\"(" + lowerBound + ";" + upperBound + "]\"";
                }

            }
            return "\"(" + cutOffpoints[cutOffpoints.length - 1] + ";" + Float.MAX_VALUE + "]\"";

        }
    }
    private static final Logger LOG = Logger.getLogger(AttributeDiscretization.class.getName());
}
