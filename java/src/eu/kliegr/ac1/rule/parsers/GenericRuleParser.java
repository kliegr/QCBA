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

import eu.kliegr.ac1.rule.Data;
import eu.kliegr.ac1.rule.Rule;
import eu.kliegr.ac1.rule.extend.ExtendRule;
import eu.kliegr.ac1.rule.extend.ExtendRules;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.zip.GZIPOutputStream;

public class GenericRuleParser {

    /**
     *
     * @param path
     * @param data
     * @return
     * @throws Exception
     */
    public static ArrayList<Rule> parseFileForRules(String path, Data data) throws Exception {
        if (path.endsWith(".xml") | path.endsWith(".gz")) {
            GUHASimplifiedParser parser = new GUHASimplifiedParser(data);
            return parser.parseFileForRules(path);
        } else if (path.endsWith(".arules")) {
            ArulesParser parser = new ArulesParser(data);

            return parser.parseFileForRules(path);
        } else {
            throw new UnsupportedOperationException("Unknown suffix of the rules file");
        }
    }

    private GenericRuleParser() {
    }
}
