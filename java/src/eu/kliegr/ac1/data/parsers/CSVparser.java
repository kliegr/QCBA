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
package eu.kliegr.ac1.data.parsers;

import eu.kliegr.ac1.data.AttributeType;
import eu.kliegr.ac1.data.DataTable;
import eu.kliegr.ac1.rule.Data;
import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

/**
 *
 * @author tomas
 */
public class CSVparser {

    private final static Logger LOGGER = Logger.getLogger(CSVparser.class.getName());

    /**
     *
     */
    final public static String weirdCharacter = new String(Character.toChars(65279));

    /*
    The last attribute is the class
     */
    /**
     *
     * @param path
     * @param targetColName
     * @param attributeTypes
     * @param IDcolumnName
     * @param sep
     * @return
     * @throws FileNotFoundException
     * @throws Exception
     */
    public static Data parseFromCSVwithHeader(String path, String targetColName, ArrayList<AttributeType> attributeTypes, String IDcolumnName, String sep) throws FileNotFoundException, Exception {

        DataTable dataTable;
        LOGGER.log(Level.INFO, "Parsing csv file:{0}", path);
        //read first line and init data structures

        final Stream<String> firstLine = Files.lines(Paths.get(path));

        final Stream<String[]> firstLineTokenized = firstLine.map((line) -> line.replaceAll(weirdCharacter, "").split(sep)).map(stringarr -> {
            for (int i = 0; i < stringarr.length; i++) {
                stringarr[i] = removeEnclosingQuotes(stringarr[i]);
            }
            return stringarr;
        });
        Data rmi = new Data();
        dataTable = rmi.newDataTable(firstLineTokenized.findFirst().get(), targetColName, attributeTypes, IDcolumnName);
        //read remainder of the file
        final Stream<String> allLines = Files.lines(Paths.get(path));
        final Stream<String[]> tokenized = allLines.map((line) -> line.replaceAll(weirdCharacter, "").split(sep));
        tokenized.skip(1).forEach((items) -> {
            for (int i = 0; i < items.length; i++) {
                items[i] = removeEnclosingQuotes(items[i]);

            }
            try {
                dataTable.addTransaction(items);
            } catch (java.lang.NumberFormatException e) {
                LOGGER.severe("Skipping transaction - possibly missing value");
            }

        }
        );
        LOGGER.log(Level.INFO, "Loaded transactions:{0}", dataTable.getLoadedTransactionCount());
        return rmi;
    }

    /**
     *
     * @param string
     * @return
     */
    public static String removeEnclosingQuotes(String string) {
        if (string.startsWith("\"") && string.endsWith("\"")) {
            string = string.substring(1, string.length() - 1);
        }
        return string;
    }

    private CSVparser() {
    }

}
