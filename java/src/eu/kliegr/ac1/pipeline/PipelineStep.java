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
package eu.kliegr.ac1.pipeline;

import java.util.logging.Logger;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class PipelineStep {

    private final String filename;
    private String outputfilename;
    private ReplacementParam replaceparam;

    /**
     *
     * @param xml
     */
    public PipelineStep(Node xml) {
        Element step = (Element) xml;

        filename = step.getElementsByTagName("filename").item(0).getTextContent();

        NodeList outputFN = step.getElementsByTagName("outputfilename");
        if (outputFN.getLength() == 1) {
            outputfilename = step.getElementsByTagName("outputfilename").item(0).getTextContent();
        }

        NodeList params = step.getElementsByTagName("replaceparam");
        if (params.getLength() == 1) {
            replaceparam = new ReplacementParam(params.item(0));
        }

    }

    /**
     * @return the filename
     */
    public String getFilename() {
        return filename;
    }

    /**
     * @param replacementValue
     * @return the outputfilename
     */
    public String getOutputfilename(String replacementName, String replacementValue) {
        return outputfilename.replaceAll(replacementName, replacementValue);
    }

    /**
     * @return the replaceparams
     */
    public ReplacementParam getReplacementParam() {
        return replaceparam;
    }
    private static final Logger LOG = Logger.getLogger(PipelineStep.class.getName());
}
