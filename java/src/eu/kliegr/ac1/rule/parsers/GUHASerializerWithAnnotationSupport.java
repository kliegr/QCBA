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

import eu.kliegr.ac1.data.AttributeType;
import eu.kliegr.ac1.data.AttributeValue;
import eu.kliegr.ac1.rule.Antecedent;
import eu.kliegr.ac1.rule.Consequent;
import eu.kliegr.ac1.rule.Rule;
import eu.kliegr.ac1.rule.RuleMultiItem;
import eu.kliegr.ac1.rule.RuleQuality;
import eu.kliegr.ac1.rule.extend.AttributeValueAnnotation;
import eu.kliegr.ac1.rule.extend.ExtendRuleAnnotation;
import eu.kliegr.ac1.rule.extend.RuleMultiItemAnnotation;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Logger;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 *
 * @author tomas
 */
public class GUHASerializerWithAnnotationSupport {

    /**
     *
     * @param rule
     * @param doc
     * @return
     */
    public Node getXMLforRule(Rule rule, Document doc) {
        Element AssociationRuleEl = doc.createElement("AssociationRule");
        Attr attr = doc.createAttribute("id");
        attr.setValue(String.valueOf(rule.getRID()));
        AssociationRuleEl.setAttributeNode(attr);
        Element TextEl = doc.createElement("Text");
        TextEl.appendChild(doc.createTextNode(rule.toString()));
        AssociationRuleEl.appendChild(TextEl);
        Element AntecedentEl = getXMLforAntecedent(rule.getAntecedent(), rule.getAnnotation(), doc);
        AssociationRuleEl.appendChild(AntecedentEl);
        Element ConsequentEl = getXMLforConsequent(rule.getConsequent(), doc);
        AssociationRuleEl.appendChild(ConsequentEl);

        Element IMValuesEl = getXMLforIMValues(rule.getQuality(), doc);
        AssociationRuleEl.appendChild(IMValuesEl);
        Element FourFtTableEl = getXMLforFourFtTable(rule.getQuality(), doc);
        AssociationRuleEl.appendChild(FourFtTableEl);
        return AssociationRuleEl;
    }

    private Element getXMLforAntecedent(Antecedent antecedent, ExtendRuleAnnotation annot, Document doc) {
        Element AntecedentEl = doc.createElement("Antecedent");
        Element CedentEl = doc.createElement("Cedent");
        CedentEl.setAttribute("connective", "Conjunction");
        AntecedentEl.appendChild(CedentEl);
        for (RuleMultiItem rmi : antecedent.getItems()) {
            RuleMultiItemAnnotation rmiAnnot = null;
            if (annot != null) {
                rmiAnnot = annot.getAnnotation(rmi);
            }
            Element AttributeEl = getXMLforMultiItem(rmi, rmiAnnot, doc);
            CedentEl.appendChild(AttributeEl);
        }

        return AntecedentEl;
    }

    private Element getXMLforConsequent(Consequent consequent, Document doc) {
        Element ConsequentEl = doc.createElement("Consequent");
        Element CedentEl = doc.createElement("Cedent");
        CedentEl.setAttribute("connective", "Conjunction");
        ConsequentEl.appendChild(CedentEl);
        Element AttributeEl = getXMLforMultiItem(consequent.getItems(), null, doc);
        CedentEl.appendChild(AttributeEl);

        return ConsequentEl;
    }

    private Element getXMLforMultiItem(RuleMultiItem rmi, RuleMultiItemAnnotation rmiAnnot, Document doc) {
        Element AttributeEl = doc.createElement("Attribute");
        //Attribute Name  element (user defined name) is ignored.
        Element ColumnEl = doc.createElement("Column");
        ColumnEl.appendChild(doc.createTextNode(rmi.getAttribute().getName()));
        AttributeEl.appendChild(ColumnEl);
        Element CategoryEl = null;
        if (null != rmi.getAttribute().getType()) {
            switch (rmi.getAttribute().getType()) {
                case nominal:
                    CategoryEl = getXMLforNominalAttributeValueRange(rmi, doc);
                    break;
                case numerical:
                    CategoryEl = getXMLforNumericalAttributeValueRange(rmi, doc);
                    break;
                default:
                    throw new UnsupportedOperationException("Unknown attribute type");
            }
        }
        AttributeEl.appendChild(CategoryEl);
        if (rmiAnnot != null) {
            AttributeEl.appendChild(getXMLforValueAnnotation(rmiAnnot, doc));

        }

        return AttributeEl;
    }

    private Element getXMLforNominalAttributeValueRange(RuleMultiItem rmi, Document doc) {
        Element CategoryEl = doc.createElement("Category");
        Element NameEl = doc.createElement("Name");
        AttributeValue first = rmi.getAttributeValues().get(0);
        AttributeValue last = rmi.getAttributeValues().get(rmi.getAttributeValues().size() - 1);
        switch (rmi.getAttributeValues().size()) {
            case 1:
                NameEl.appendChild(doc.createTextNode(first.toString()));
                break;
            case 2:
                NameEl.appendChild(doc.createTextNode(first + "," + last));
                break;
            default:
                NameEl.appendChild(doc.createTextNode(first + "..." + last));
                break;
        }
        CategoryEl.appendChild(NameEl);
        Element DataEl = doc.createElement("Data");

        rmi.getAttributeValues().stream().forEach((value) -> {
            Element ValueEl = doc.createElement("Value");
            ValueEl.appendChild(doc.createTextNode(value.toString()));
            DataEl.appendChild(ValueEl);
        });
        CategoryEl.appendChild(DataEl);
        return CategoryEl;
    }

    private Element getXMLforValueAnnotation(RuleMultiItemAnnotation rmiAnnot, Document doc) {
        Element AnnotationsEl = doc.createElement("Annotations");

        ArrayList<AttributeValueAnnotation> annots = rmiAnnot.getAnnotations();
        annots.stream().forEach((annot) -> {
            Element AnnotationEl = doc.createElement("Annotation");
            AnnotationsEl.appendChild(AnnotationEl);
            Element ValueEl = doc.createElement("Value");
            AnnotationEl.appendChild(ValueEl);
            ValueEl.appendChild(doc.createTextNode(annot.getValue().toString()));

            Element OriginEl = doc.createElement("Origin");
            OriginEl.appendChild(doc.createTextNode(annot.getOrigin().toString()));
            AnnotationEl.appendChild(OriginEl);

            HashMap<Consequent, RuleQuality> distribution = annot.getDistribution();
            Element DistributionEl = getXMLforDistribution(distribution, doc);
            AnnotationEl.appendChild(DistributionEl);
        });

        return AnnotationsEl;
        /*                          <Annotations>                        
                        <Annotation>
                            <Value>0.5</Value>
                            <Origin>core</Origin>
                            <Distribution>
                                <TargetValue>
                                    <Name>Iris-virginica</Name>
                                    <IMValues>
                                        <IMValue name="Confidence">0.88235295</IMValue>
                                        <IMValue name="Support">45</IMValue>
                                    </IMValues>
                                    <FourFtTable a="45" b="6" c="0" d="0"/>
                                </TargetValue>
                                <TargetValue>
                                    <Name>Iris-setosa</Name>
                                    <IMValues>
                                        <IMValue name="Confidence">0.88235295</IMValue>
                                        <IMValue name="Support">45</IMValue>
                                    </IMValues>
                                    <FourFtTable a="45" b="6" c="0" d="0"/>
                                </TargetValue>
                            </Distribution>                            
                        </Annotation>
         */

    }

    private Element getXMLforDistribution(HashMap<Consequent, RuleQuality> distribution, Document doc) {
        Element DistributionEl = doc.createElement("Distribution");
        distribution.forEach((consequent, quality)
                -> {
            Element TargetValueEl = doc.createElement("Consequent");
            DistributionEl.appendChild(TargetValueEl);
            for (AttributeValue consValue : consequent.getItems().getAttributeValues()) {
                Element ValueEl = doc.createElement("Value");
                ValueEl.appendChild(doc.createTextNode(consValue.toString()));
                TargetValueEl.appendChild(ValueEl);
            }
            TargetValueEl.appendChild(getXMLforIMValues(quality, doc));
            TargetValueEl.appendChild(getXMLforFourFtTable(quality, doc));
        });
        return DistributionEl;
    }

    private Element getXMLforNumericalAttributeValueRange(RuleMultiItem rmi, Document doc) {
        Element CategoryEl = doc.createElement("Category");
        AttributeValue first = rmi.getAttributeValues().get(0);
        AttributeValue last = rmi.getAttributeValues().get(rmi.getAttributeValues().size() - 1);
        Element NameEl = doc.createElement("Name");
        NameEl.appendChild(doc.createTextNode("[" + first.toString() + RuleMultiItem.INTERVAL_SEPARATOR + last.toString() + "]"));
        CategoryEl.appendChild(NameEl);
        Element DataEl = doc.createElement("Data");
        Element IntervalEl = doc.createElement("Interval");
        IntervalEl.setAttribute("closure", "closedClosed");
        IntervalEl.setAttribute("leftMargin", first.toString());
        IntervalEl.setAttribute("rightMargin", last.toString());
        DataEl.appendChild(IntervalEl);
        CategoryEl.appendChild(DataEl);
        return CategoryEl;
    }

    private Element getXMLforIMValues(RuleQuality quality, Document doc) {
        Element IMValuesEl = doc.createElement("IMValues");

        Element ConfidenceEl = doc.createElement("IMValue");
        ConfidenceEl.setAttribute("name", "Confidence");
        ConfidenceEl.appendChild(doc.createTextNode(String.valueOf(quality.getConfidence())));
        IMValuesEl.appendChild(ConfidenceEl);
        Element SupportEl = doc.createElement("IMValue");
        SupportEl.setAttribute("name", "Support");
        SupportEl.appendChild(doc.createTextNode(String.valueOf(quality.getSupport())));
        IMValuesEl.appendChild(SupportEl);
        return IMValuesEl;

    }

    private Element getXMLforFourFtTable(RuleQuality quality, Document doc) {
        Element FourFtTableEl = doc.createElement("FourFtTable");
        FourFtTableEl.setAttribute("a", String.valueOf(quality.getA()));
        FourFtTableEl.setAttribute("b", String.valueOf(quality.getB()));
        FourFtTableEl.setAttribute("c", String.valueOf(quality.getC()));
        FourFtTableEl.setAttribute("d", String.valueOf(quality.getD()));
        return FourFtTableEl;
    }
    private static final Logger LOG = Logger.getLogger(GUHASerializerWithAnnotationSupport.class.getName());

}
