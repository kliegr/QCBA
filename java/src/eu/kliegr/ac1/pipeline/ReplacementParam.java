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

import java.util.ArrayList;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class ReplacementParam {

    private final String name;
    private final String replaceregex;
    private final ArrayList<String> replacements = new ArrayList();
    private final ArrayList<Boolean> skips = new ArrayList();

    /**
     *
     * @param xml
     */
    public ReplacementParam(Node xml) {
        Element step = (Element) xml;
        name = step.getElementsByTagName("name").item(0).getTextContent();
        replaceregex = step.getElementsByTagName("replaceregex").item(0).getTextContent();
        NodeList replacementsNL = step.getElementsByTagName("replacement");
        for (int i = 0; i < replacementsNL.getLength(); i++) {
            replacements.add(replacementsNL.item(i).getTextContent());
            Node skip = replacementsNL.item(i).getAttributes().getNamedItem("skip");
            if (skip != null) {
                skips.add(Boolean.parseBoolean(skip.getTextContent()));
            } else {
                skips.add(Boolean.FALSE);
            }
        }
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @return the replaceregex
     */
    public String getReplaceregex() {
        return replaceregex;
    }

    /**
     * @return the replacements
     */
    public ArrayList<String> getReplacements() {
        return replacements;
    }

    /**
     *
     * @return
     */
    public ArrayList<Boolean> getSkips() {
        return skips;
    }
}
