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
package eu.kliegr.ac1.rule;

import eu.kliegr.ac1.rule.extend.ExtendRule;
import eu.kliegr.ac1.rule.extend.ExtendRuleAnnotation;
import eu.kliegr.ac1.rule.parsers.GUHASerializerWithAnnotationSupport;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

/**
 *
 * @author tomas
 */
public class Rule implements RuleInt {

    private final static Logger LOGGER = Logger.getLogger(Rule.class.getName());
    private static int lastERID = -1;

    /**
     *
     * @return
     */
    public static int getNextERID() {
        return ++lastERID;
    }

    /**
     *
     */
    public static void resetERIDcounter() {
        lastERID = -1;
    }

    /**
     *
     */
    protected Consequent consequent;

    /**
     *
     */
    protected Antecedent antecedent;
    //as observed on the for pruning dataset

    /**
     *
     */
    protected RuleQuality quality;

    /**
     *
     */
    protected int RID;
    //extension number

    /**
     *
     */
    protected int ERID = -1;
    //protected Node sourceXML;
    private ExtendRuleAnnotation annot;

    /**
     *
     */
    public Data data;

    /**
     *
     * @param antecedent
     * @param consequent
     * @param quality
     * @param annot
     * @param RID
     * @param data
     */
    public Rule(Antecedent antecedent, Consequent consequent, RuleQuality quality, ExtendRuleAnnotation annot, int RID, Data data) {
        this.data = data;
        this.antecedent = antecedent;
        this.consequent = consequent;
        this.quality = quality;
        //        this.sourceXML = sourceXML;
        this.annot = annot;
        this.RID = RID;
        LOGGER.log(Level.FINE, "Created rule RID:{0}", RID);
    }

    /**
     *
     * @return
     */
    public Data getData() {
        return data;
    }

    /**
     *
     * @return
     */
    public Node getXMLRepresentation() {
        GUHASerializerWithAnnotationSupport serializer = new GUHASerializerWithAnnotationSupport();
        Document newXmlDocument;
        try {
            newXmlDocument = DocumentBuilderFactory.newInstance()
                    .newDocumentBuilder().newDocument();
            Node thisRuleAsXML = serializer.getXMLforRule(this, newXmlDocument);
            return thisRuleAsXML;
        } catch (ParserConfigurationException ex) {
            Logger.getLogger(ExtendRule.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
    }

    /**
     *
     * @return
     */
    public Node generateXML() {
        return null;
    }

    /**
     *
     * @param quality
     */
    public void setQuality(RuleQuality quality) {
        this.quality = quality;
    }

    /**
     *
     * @return
     */
    public RuleQuality getQuality() {
        return quality;
    }

    public String toString() {
        return toString(false);
    }

    /**
     *
     * @return
     */
    public String getRuleAsArulesString() {
        StringBuilder sb = new StringBuilder();
        sb.append(antecedent.toString(true));
        sb.append(" => ");
        sb.append(consequent.toString(false, true));
        return sb.toString();
    }

    /**
     *
     * @return
     */
    public String getArulesRepresentation() {
        StringBuilder sb = new StringBuilder();
        sb.append("\"");
        sb.append(this.getRID());
        sb.append("\"");
        sb.append(",");
        sb.append("\"");
        sb.append(getRuleAsArulesString());
        sb.append("\"");
        sb.append(",");
        sb.append(quality.toString());
        return sb.toString();

    }

    /**
     *
     * @param printAnnotation
     * @return
     */
    public String toString(boolean printAnnotation) {
        StringBuilder sb = new StringBuilder();
        sb.append("RID=").append(RID).append(":");
        sb.append(antecedent.toString());
        sb.append(" => ");
        sb.append(consequent.toString());
        sb.append(",");
        sb.append(quality.toString());
        if (printAnnotation && getAnnotation() != null) {
            sb.append("\n").append(getAnnotation().toString());
        } else {
            //sb.append("\n Rule annotation not generated");
        }
        return sb.toString();
    }

    /**
     *
     * @param erid
     */
    public void setERID(int erid) {
        this.ERID = erid;
    }

    /**
     *
     * @param annot
     */
    public void setAnnotation(ExtendRuleAnnotation annot) {
        this.annot = annot;
    }

    /**
     *
     * @return
     */
    public ExtendRuleAnnotation getAnnotation() {
        return annot;
    }

    /**
     *
     * @return
     */
    @Override
    public Consequent getConsequent() {
        return consequent;
    }

    /**
     *
     * @return
     */
    @Override
    public int getRID() {
        return RID;
    }

    /**
     *
     * @return
     */
    public int getERID() {
        return RID;
    }

    /**
     *
     * @return
     */
    @Override
    public int getAntecedentLength() {
        return antecedent.getItems().size();
    }

    /**
     *
     * @return
     */
    @Override
    public float getConfidence() {
        return quality.getConfidence();
    }

    /**
     *
     * @return
     */
    @Override
    public int getSupport() {
        return quality.getSupport();
    }

    /**
     *
     * @return
     */
    @Override
    public Antecedent getAntecedent() {
        return antecedent;
    }

}
