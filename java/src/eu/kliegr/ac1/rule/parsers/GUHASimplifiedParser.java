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

import eu.kliegr.ac1.data.AttributeValue;
import eu.kliegr.ac1.data.AttributeValueType;
import eu.kliegr.ac1.rule.Antecedent;
import eu.kliegr.ac1.rule.AttributeNotFoundException;
import eu.kliegr.ac1.rule.Consequent;
import eu.kliegr.ac1.rule.Data;
import eu.kliegr.ac1.rule.Rule;
import eu.kliegr.ac1.rule.RuleInt;
import eu.kliegr.ac1.rule.RuleMultiItem;
import eu.kliegr.ac1.rule.RuleQuality;
import eu.kliegr.ac1.rule.extend.AttributeValueAnnotation;
import eu.kliegr.ac1.rule.extend.ExtendRule;
import eu.kliegr.ac1.rule.extend.RuleMultiItemAnnotation;
import eu.kliegr.ac1.rule.extend.TestRuleAnnotation;
import eu.kliegr.ac1.rule.extend.ValueOrigin;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPathExpressionException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 *
 * @author tomas
 */
public class GUHASimplifiedParser {

    private final static Logger LOGGER = Logger.getLogger(GUHASimplifiedParser.class.getName());

    /*
    this method serializes rules to a text file    
     */
    /**
     *
     * @param rules
     * @param path
     * @throws ParserConfigurationException
     * @throws TransformerException
     */
    public static void serializeRules(List<ExtendRule> rules, String path) throws ParserConfigurationException, TransformerException {
        //TODO: This method shoudl be moved to separate class
        File f = new File(path);
        File dir = new File(new File(f.getAbsolutePath()).getParent());
        dir.mkdir();

        FileOutputStream fop = null;
        File file;
        StringBuilder content = new StringBuilder();
        content.append("\"\",\"rules\",\"support\",\"confidence\"\n");
        rules.stream().forEach((rule) -> {
            content.append(rule.getArulesRepresentation()).append("\n");
        });

        try {

            file = new File(path);
            fop = new FileOutputStream(file);

            // if file doesnt exists, then create it
            if (!file.exists()) {
                file.createNewFile();
            }

            // get the content in bytes
            byte[] contentInBytes = content.toString().getBytes();

            fop.write(contentInBytes);
            fop.flush();
            fop.close();

            LOGGER.info("Serialization done");

        } catch (IOException e) {
        } finally {
            try {
                if (fop != null) {
                    fop.close();
                }
            } catch (IOException e) {
            }
        }

    }

    /* 
    this method saves rules to xml file (for pruning)
    assumes that the rules hold the original xml representation from deserialization
    
     */
    /**
     *
     * @param rules
     * @param path
     * @throws ParserConfigurationException
     * @throws TransformerException
     * @throws UnsupportedEncodingException
     * @throws FileNotFoundException
     * @throws IOException
     */
    public static void saveRules(List<? extends RuleInt> rules, String path) throws ParserConfigurationException, TransformerException, UnsupportedEncodingException, FileNotFoundException, IOException {
        File f = new File(path);

        String parent = f.getParent();
        if (parent != null) {
            File parentF = new File(parent);
            if (!parentF.exists()) {
                File dir = new File(parent);
                dir.mkdir();
            }

        }

        Writer writer = null;
        OutputStream os;
        if (path.endsWith(".gz")) {
            os = new GZIPOutputStream(new FileOutputStream(f));
        } else {
            os = new FileOutputStream(f);

        }
        OutputStreamWriter o = new OutputStreamWriter(os, "utf-8");
        writer = new BufferedWriter(o);

        writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n");
        writer.write("<AssociationRules xmlns=\"http://keg.vse.cz/lm/AssociationRules/v1.0\">\n");
        writer.write("<AssociationModel xmlns=\"http://keg.vse.cz/ns/GUHA0.1rev1\">\n");
        writer.flush();
        StreamResult result = new StreamResult(o);
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        //rule serialization needs to be done one by one to prevent excessive memory use on large documents
        for (RuleInt rule : rules) {
            Node node = rule.getXMLRepresentation();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            DOMSource source = new DOMSource(node);
            transformer.transform(source, result);
        }

        writer.write("</AssociationModel>");
        writer.write("</AssociationRules>");
        writer.flush();
        writer.close();

        // Output to console for testing
        // StreamResult result = new StreamResult(System.out);
        LOGGER.log(Level.INFO, "File saved to {0}", path);

    }


    /**
     *
     * Convert a string to a Document Object
     *
     * @param xml The xml to convert
     * @return A Node Object
     * @throws IOException
     * @throws SAXException
     * @throws ParserConfigurationException
     */
    public static Node string2Node(String xml) throws IOException, SAXException, ParserConfigurationException {

        if (xml == null) {
            return null;
        }

        return inputStream2Document(new ByteArrayInputStream(xml.getBytes())).getChildNodes().item(0);

    }

    /**
     * Convert an inputStream to a Document Object
     *
     * @param inputStream The inputstream to convert
     * @return a Document Object
     * @throws IOException
     * @throws SAXException
     * @throws ParserConfigurationException
     */
    public static Document inputStream2Document(InputStream inputStream) throws IOException, SAXException, ParserConfigurationException {
        DocumentBuilderFactory newInstance = DocumentBuilderFactory.newInstance();
        newInstance.setNamespaceAware(true);
        Document parse = newInstance.newDocumentBuilder().parse(inputStream);
        return parse;
    }
    Data data;

    /**
     *
     * @param rmi
     * @throws Exception
     */
    public GUHASimplifiedParser(Data rmi) throws Exception {
        this.data = rmi;
    }

    /**
     *
     * @param path
     * @return
     * @throws FileNotFoundException
     * @throws XMLStreamException
     * @throws IOException
     * @throws SAXException
     * @throws XPathExpressionException
     * @throws ParserConfigurationException
     * @throws Exception
     */
    public ArrayList<Rule> parseFileForRules(String path) throws FileNotFoundException, XMLStreamException, IOException, SAXException, XPathExpressionException, ParserConfigurationException, Exception {
        LOGGER.log(Level.INFO, "Reading rules from {0}\n", path);
        ArrayList<Rule> rules = new ArrayList();

        File fXmlFile = new File(path);
        String ls = System.getProperty("line.separator");
        BufferedReader reader;
        if (path.endsWith(".gz")) {
            InputStream fileStream = new FileInputStream(fXmlFile);
            InputStream gzipStream = new GZIPInputStream(fileStream);
            Reader decoder = new InputStreamReader(gzipStream, "utf-8");
            reader = new BufferedReader(decoder);
        } else {
            reader = new BufferedReader(new FileReader(fXmlFile));
        }

        String line = null;
        StringBuilder currentRuleTextSB = new StringBuilder();
        int lineCounter = 0;

        //to handle large documents, the xml parser is invoked for individual rules, not for the entire document
        long start2 = 0;

        while ((line = reader.readLine()) != null) {
            lineCounter++;
            if (line.trim().startsWith("<AssociationRule")) {
                currentRuleTextSB.delete(0, currentRuleTextSB.length());
                LOGGER.log(Level.FINEST, "Starting to read next rule from input line:{0}", lineCounter);
                currentRuleTextSB.append(line);
                start2 = System.nanoTime();

            } else if (line.trim().startsWith("</AssociationRule>")) {
                currentRuleTextSB.append("\n");
                currentRuleTextSB.append(line);
                Node ruleNode = string2Node(currentRuleTextSB.toString());
                long time2 = System.nanoTime() - start2;
                LOGGER.finest(String.format("Took %.3f seconds to read and parse ending on line:" + lineCounter, time2 / 1e9));
                Rule rule = parseAssociationRule(ruleNode);
                rules.add(rule);

            } else {
                currentRuleTextSB.append("\n");
                currentRuleTextSB.append(line);
            }
        }
        reader.close();
        return rules;
    }

    private Rule parseAssociationRule(Node rule) throws Exception {
        int ruleid = Integer.parseInt(rule.getAttributes().getNamedItem("id").getNodeValue());
        TestRuleAnnotation ruleAnnot = new TestRuleAnnotation();
        Antecedent ant = null;
        Consequent con = null;
        RuleQuality qm = null;
        NodeList ruleParts = rule.getChildNodes();
        for (int i = 0; i < ruleParts.getLength(); i++) {
            Node ruleNode = ruleParts.item(i);
            String nodeName = ruleNode.getNodeName();
            if (nodeName.equals("Antecedent")) {
                ArrayList<AnnotatedRuleMultiItem> antItems = parseAntecedent(ruleNode);
                //create array from the rule multi items wrapped in AnnotatedRuleMultiItem
                ant = new Antecedent(antItems.stream().map((annRMI) -> annRMI.rmi).collect(Collectors.toCollection(ArrayList::new)));
                antItems.stream().forEach((annRMI) -> ruleAnnot.addAnnotation(annRMI.rmi, annRMI.annotation));
            } else if (nodeName.equals("Consequent")) {
                //for consequent, AnnotatedRuleMultiItem does not contain any annotation
                AnnotatedRuleMultiItem conItem = parseConsequent(ruleNode);

                con = ConsequentCache.getConsequent(conItem.rmi);
            } else if (nodeName.equals("FourFtTable")) {
                qm = parseFourFtTable(ruleNode);
            }
        }
        if (ant == null) {
            throw new Exception("Antecedent parsing failed");
        }
        if (con == null) {
            throw new Exception("Antecedent parsing failed");
        }
        if (qm == null) {
            throw new Exception("Rule quality parsing failed");
        }

        return new Rule(ant, con, qm, ruleAnnot, ruleid, null,data);
    }

    private ArrayList<AnnotatedRuleMultiItem> parseAntecedent(Node node) throws Exception {

        ArrayList<AnnotatedRuleMultiItem> items = new ArrayList();
        NodeList ant = node.getChildNodes();
        int cedentCounter = 0;
        for (int i = 0; i < ant.getLength(); i++) {
            Node antNode = ant.item(i);
            String nodeName = antNode.getNodeName();

            if (nodeName.equals("Cedent")) {
                cedentCounter++;
                if (!antNode.getAttributes().getNamedItem("connective").getTextContent().equals("Conjunction")) {
                    throw new UnsupportedOperationException("Disjunctions not supported.");
                } else {
                    NodeList attributeNodes = antNode.getChildNodes();
                    for (int j = 0; j < attributeNodes.getLength(); j++) {
                        if (attributeNodes.item(j).getNodeName().equals("Attribute")) {
                            AnnotatedRuleMultiItem at = parseAttribute(attributeNodes.item(j), false);
                            items.add(at);
                        } else if (attributeNodes.item(j).getNodeName().equals("Cedent")) {
                            if (attributeNodes.item(j).getAttributes().getNamedItem("connective").getTextContent().equals("Negation")) {
                                AnnotatedRuleMultiItem at = parseNegatedAttribute(attributeNodes.item(j));
                                items.add(at);
                            } else {
                                throw new UnsupportedOperationException("Unexpected or unsupported Cedent.");
                            }
                        }
                    }
                }
            }
        }

        if (cedentCounter == 0) {
            throw new UnsupportedOperationException("No cedents not supported.");
        } else if (cedentCounter > 1) {
            throw new UnsupportedOperationException("Multiple cedents not supported.");
        }

        return items;
    }

    private AnnotatedRuleMultiItem parseConsequent(Node node) throws Exception {
        ArrayList<AnnotatedRuleMultiItem> items = new ArrayList();
        NodeList ant = node.getChildNodes();
        int cedentCounter = 0;
        for (int i = 0; i < ant.getLength(); i++) {
            Node conNode = ant.item(i);
            String nodeName = conNode.getNodeName();

            if (nodeName.equals("Cedent")) {
                cedentCounter++;
                if (!conNode.getAttributes().getNamedItem("connective").getTextContent().equals("Conjunction")) {
                    throw new UnsupportedOperationException("Disjunctions not supported.");
                } else {
                    NodeList attributeNodes = conNode.getChildNodes();
                    for (int j = 0; j < attributeNodes.getLength(); j++) {
                        if (attributeNodes.item(j).getNodeName().equals("Attribute")) {
                            AnnotatedRuleMultiItem at = parseAttribute(attributeNodes.item(j), false);
                            items.add(at);
                        } else if (attributeNodes.item(j).getNodeName().equals("Cedent")) {
                            if (attributeNodes.item(j).getAttributes().getNamedItem("connective").getTextContent().equals("Negation")) {
                                AnnotatedRuleMultiItem at = parseNegatedAttribute(attributeNodes.item(j));
                                items.add(at);
                            } else {
                                throw new UnsupportedOperationException("Unexpected or unsupported Cedent.");
                            }
                        }
                    }
                }
            }
        }

        if (cedentCounter == 0) {
            throw new UnsupportedOperationException("No cedents not supported.");
        } else if (cedentCounter > 1) {
            throw new UnsupportedOperationException("Multiple cedents not supported.");
        }
        if (items.size() != 1) {
            throw new UnsupportedOperationException("Consequent must have exactly one attribute and has " + items.size());
        }

        return items.get(0);
    }

    private AnnotatedRuleMultiItem parseAttribute(Node attribute, boolean negation) throws Exception {
        NodeList attributeNodes = attribute.getChildNodes();
        // name as appears in the data
        String attributeName = null;

        ArrayList<AttributeValue> allValues = new ArrayList();
        for (int i = 0; i < attributeNodes.getLength(); i++) {
            Node node = attributeNodes.item(i);
            String nodeName = node.getNodeName();
            if (nodeName.equals("Column")) {
                attributeName = node.getTextContent();
                break;
            }
        }

        if (attributeName == null) {
            throw new UnsupportedOperationException("Name for attribute not found");
        }
        for (int i = 0; i < attributeNodes.getLength(); i++) {
            Node node = attributeNodes.item(i);
            String nodeName = node.getNodeName();
            if (nodeName.equals("Category")) {
                ArrayList<AttributeValue> values = parseCategory(node, attributeName, negation);
                allValues.addAll(values);
            }
        }
        RuleMultiItem rmi = data.makeRuleItem(allValues, attributeName);
        RuleMultiItemAnnotation annotation = null;
        for (int i = 0; i < attributeNodes.getLength(); i++) {
            Node node = attributeNodes.item(i);
            String nodeName = node.getNodeName();
            if (nodeName.equals("Annotations")) {
                annotation = parseAnnotations(node, attributeName);
                break;
            }
        }
        AnnotatedRuleMultiItem rmiannot = new AnnotatedRuleMultiItem(rmi, annotation);

        return rmiannot;

    }

    private ArrayList<AttributeValue> parseCategory(Node category, String attributeName, boolean negated) throws AttributeNotFoundException {
        NodeList categoryNodes = category.getChildNodes();
        ArrayList<AttributeValue> allValues = new ArrayList();
        for (int i = 0; i < categoryNodes.getLength(); i++) {
            Node node = categoryNodes.item(i);
            String nodeName = node.getNodeName();
            if (nodeName.equals("Data")) {
                allValues.addAll(parseData(node, attributeName, negated));
            }
        }
        return allValues;

    }

    private ArrayList<AttributeValue> parseDataOfNegatedAttribute(Node data, String attributeName) throws AttributeNotFoundException {
        NodeList dataNodes = data.getChildNodes();
        ArrayList<AttributeValue> allValues = null;
        for (int i = 0; i < dataNodes.getLength(); i++) {
            Node node = dataNodes.item(i);
            if (node.getNodeName().equals("Interval")) {
                Collection<AttributeValue> valuesMatchingCategory = parseInterval(node, attributeName, true);
                if (allValues == null) {
                    allValues = new ArrayList(valuesMatchingCategory);
                } else {
                    // the code in this branch is executed if the category comprises of multiple negated intervals
                    // for second and consecutive interval, we need to keep only the intersection

                    allValues.retainAll(valuesMatchingCategory);

                }

            } else if (node.getNodeName().equals("Value")) {
                Collection<AttributeValue> valuesMatchingCategory = parseValue(node, attributeName, true);
                if (allValues == null) {
                    allValues = new ArrayList(valuesMatchingCategory);
                } else {
                    // the code in this branch is executed if the category comprises of multiple negated intervals
                    // for second and consecutive interval, we need to keep only the intersection

                    allValues.retainAll(valuesMatchingCategory);

                }

            }
        }
        return allValues;

    }

    private ArrayList<AttributeValue> parseData(Node data, String attributeName, boolean negated) throws AttributeNotFoundException {
        if (negated) {
            return parseDataOfNegatedAttribute(data, attributeName);
        }
        NodeList dataNodes = data.getChildNodes();
        ArrayList<AttributeValue> allValues = new ArrayList();
        for (int i = 0; i < dataNodes.getLength(); i++) {
            Node node = dataNodes.item(i);
            if (node.getNodeName().equals("Interval")) {
                allValues.addAll(parseInterval(node, attributeName, negated));
            } else if (node.getNodeName().equals("Value")) {
                allValues.addAll(parseValue(node, attributeName, negated));
            }
        }
        return allValues;

    }

    private String[] concat(String[] A, String[] B) {
        int aLen = A.length;
        int bLen = B.length;
        String[] C = new String[aLen + bLen];
        System.arraycopy(A, 0, C, 0, aLen);
        System.arraycopy(B, 0, C, aLen, bLen);
        return C;
    }

    private Collection<AttributeValue> parseInterval(Node node, String attributeName, boolean negated) {
        float leftMargin = Float.parseFloat(node.getAttributes().getNamedItem("leftMargin").getTextContent());
        float rightMargin = Float.parseFloat(node.getAttributes().getNamedItem("rightMargin").getTextContent());
        String closure = node.getAttributes().getNamedItem("closure").getTextContent();
        boolean fromInclusive;
        boolean toInclusive;
        if (closure.equals("closedOpen")) {
            fromInclusive = true;
            toInclusive = false;
        } else if (closure.equals("closedClosed")) {
            fromInclusive = true;
            toInclusive = true;
        } else if (closure.equals("openOpen")) {
            fromInclusive = false;
            toInclusive = false;
        } else //if (closure.equals("openClosed"))
        {
            fromInclusive = false;
            toInclusive = true;
        }
        Collection values = data.getValuesInRange(attributeName, leftMargin, fromInclusive, rightMargin, toInclusive, negated);

        return values;
    }

    private RuleQuality parseFourFtTable(Node tableNode) {

        Integer a = Integer.parseInt(tableNode.getAttributes().getNamedItem("a").getTextContent());
        Integer b = Integer.parseInt(tableNode.getAttributes().getNamedItem("b").getTextContent());
        RuleQuality rq;
        try {
            Integer c = Integer.parseInt(tableNode.getAttributes().getNamedItem("c").getTextContent());
            Integer d = Integer.parseInt(tableNode.getAttributes().getNamedItem("d").getTextContent());
            rq = new RuleQuality(a, b, c, d);
        } catch (NumberFormatException e) {
            rq = new RuleQuality(a, b);
        }
        return rq;
    }

    private Collection<AttributeValue> parseValue(Node node, String attributeName, boolean negated) throws AttributeNotFoundException {

        return data.getValuesByEnumeration(attributeName, new String[]{node.getTextContent()}, negated, AttributeValueType.breakpoint);
    }

    private AnnotatedRuleMultiItem parseNegatedAttribute(Node cedent) throws Exception {

        NodeList cedentNodes = cedent.getChildNodes();
        AnnotatedRuleMultiItem multiItem = null;
        for (int i = 0; i < cedentNodes.getLength(); i++) {
            if (cedentNodes.item(i).getNodeName().equals("Attribute")) {
                if (multiItem != null) {
                    throw new UnsupportedOperationException("Too many cedents");
                }
                multiItem = parseAttribute(cedentNodes.item(i), true);
            }
        }
        return multiItem;
    }

    private RuleMultiItemAnnotation parseAnnotations(Node annotations, String attributeName) throws AttributeNotFoundException {
        RuleMultiItemAnnotation annotationList = new RuleMultiItemAnnotation();
        NodeList annotationsNodes = annotations.getChildNodes();
        for (int i = 0; i < annotationsNodes.getLength(); i++) {
            Node node = annotationsNodes.item(i);
            String nodeName = node.getNodeName();

            if (nodeName.equals("Annotation")) {
                AttributeValueAnnotation annot = parseAnnotation(node, attributeName);
                annotationList.add(annot);
            }
        }
        return annotationList;
    }

    private AttributeValueAnnotation parseAnnotation(Node annotation, String attributeName) throws AttributeNotFoundException {
        NodeList annotationNodes = annotation.getChildNodes();
        AttributeValueAnnotation annot = null;

        String value = null;
        ValueOrigin origin = null;
        for (int i = 0; i < annotationNodes.getLength(); i++) {
            Node node = annotationNodes.item(i);
            String nodeName = node.getNodeName();
            if (nodeName.equals("Value")) {
                value = node.getTextContent();
            } else if (nodeName.equals("Origin")) {
                origin = ValueOrigin.valueOf(node.getTextContent());
            } else if (nodeName.equals("Distribution")) {
                AttributeValue atVal = data.getValue(attributeName, value, AttributeValueType.breakpoint);
                annot = new AttributeValueAnnotation(atVal, origin);
                annot = parseDistribution(node, annot);
            }
        }
        return annot;

    }

    private AttributeValueAnnotation parseDistribution(Node distribution, AttributeValueAnnotation annot) throws AttributeNotFoundException {
        NodeList distributionNodes = distribution.getChildNodes();
        for (int i = 0; i < distributionNodes.getLength(); i++) {
            Node node = distributionNodes.item(i);
            String nodeName = node.getNodeName();
            if (nodeName.equals("Consequent")) {
                annot = parseDistributionConsequent(node, annot);
            }
        }
        return annot;
    }

    private AttributeValueAnnotation parseDistributionConsequent(Node distrCons, AttributeValueAnnotation annot) throws AttributeNotFoundException {
        NodeList distrConsNodes = distrCons.getChildNodes();
        ArrayList<AttributeValue> values = new ArrayList();

        RuleQuality quality = null;
        for (int i = 0; i < distrConsNodes.getLength(); i++) {
            Node node = distrConsNodes.item(i);
            String nodeName = node.getNodeName();
            if (nodeName.equals("Value")) {
                String value = node.getTextContent();

                values.add(data.getValue(data.getTargetAttribute().getName(), value, AttributeValueType.breakpoint));

            }
            if (nodeName.equals("FourFtTable")) {
                quality = parseFourFtTable(node);
            }
        }
        RuleMultiItem rmi = null;
        try {
            rmi = data.makeRuleItem(values, data.getTargetAttribute().getName());
        } catch (Exception ex) {

        }

        Consequent cons = ConsequentCache.getConsequent(rmi);
        annot.add(cons, quality);
        return annot;
    }
}
