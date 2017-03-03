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
package eu.kliegr.ac1;

import eu.kliegr.ac1.pipeline.PipelineStep;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Properties;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class PipelineConfig {

    private final static Logger LOGGER = Logger.getLogger(PipelineConfig.class.getName());

    /**
     *
     */
    public ArrayList<PipelineStep> pipelineStep = new ArrayList();

    /**
     *
     * @param path
     * @throws SAXException
     * @throws ParserConfigurationException
     * @throws IOException
     */
    public PipelineConfig(String path) throws SAXException, ParserConfigurationException, IOException {
        System.out.println("Reading config from " + path);
        InputStream input = new BufferedInputStream(
                new FileInputStream(path));
        Properties prop = new Properties();
        prop.loadFromXML(input);
        String scriptPath = prop.getProperty("ScriptPath");
        input.close();
        File fXmlFile = new File(scriptPath);
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document doc = dBuilder.parse(fXmlFile);
        NodeList nList = doc.getElementsByTagName("step");
        for (int i = 0; i < nList.getLength(); i++) {
            pipelineStep.add(new PipelineStep(nList.item(i)));
        }

    }
}
