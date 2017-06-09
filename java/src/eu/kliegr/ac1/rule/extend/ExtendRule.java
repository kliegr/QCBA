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
package eu.kliegr.ac1.rule.extend;

import eu.kliegr.ac1.data.Attribute;
import eu.kliegr.ac1.data.AttributeType;
import eu.kliegr.ac1.data.AttributeValue;
import eu.kliegr.ac1.data.Transaction;
import eu.kliegr.ac1.rule.*;
import eu.kliegr.ac1.rule.parsers.GUHASerializerWithAnnotationSupport;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

/**
 *
 * @author tomas The prune rule is created from outside including quality
 * measures
 *
 */
public final class ExtendRule extends PruneRule implements RuleInt {

    private final static Logger LOGGER = Logger.getLogger(ExtendRule.class.getName());
    private ExtendRuleConfig extensionConfig;
    private Float confidenceOfSeedRule;
    public float getConfidenceOfSeedRule()
    {
            return confidenceOfSeedRule;
    }

    public ExtendRuleConfig getExtendRuleConfig()
    {
        return extensionConfig;
    }
    
    private static Antecedent constructNewAntecedent(Antecedent antecedent, RuleMultiItem extension) {
        HashMap<Attribute,RuleMultiItem> extensionMap = new HashMap();
        extensionMap.put(extension.getAttribute(), extension);
        return constructNewAntecedent(antecedent,extensionMap);
    }
    private static Antecedent constructNewAntecedent(Antecedent antecedent, HashMap<Attribute,RuleMultiItem> extension) {
        ArrayList<RuleMultiItem> newAntecedentItems = new ArrayList();
        antecedent.getItems().stream().forEach((multiitem) -> {
            if (extension.containsKey(multiitem.getAttribute())) {
                newAntecedentItems.add(extension.get(multiitem.getAttribute()));
            } else {
                newAntecedentItems.add(multiitem);
            }
        });
        return new Antecedent(newAntecedentItems);
    }

    private ExtendType extendType;

    /**
     *
     */
    public RuleMultiItem lastExtension;
    //private ExtendRuleAnnotation annot;
    //contains IDs of rules from which this rule is derived
    private History history;


    /**
     * constructor for seed rule
     * @param rule
     * @param history
     * @param type
     * @param conf
     */
    public ExtendRule(Rule rule, History history, ExtendType type,ExtendRuleConfig conf) {
        super(rule);
        this.extendType = type;
        this.extensionConfig = conf;

        updateQuality();
        
        //we are in constructor for seed rule
        this.confidenceOfSeedRule = rule.getConfidence();
        if (history == null) {
            this.history = new History(this.rule.getRID());
        } else {
            this.history = history;
        }
        this.history.addRuleIdentifiers(this.rule.getERID(), this.rule.toArray());
        LOGGER.fine(rule.toString());
    }

    /** 
     * constructor for rule derived from seed rule
     * @param rule
     * @param extension
     * @param history
     * @param type
     * @param extensionConfig
     */
    public ExtendRule(Rule rule, HashMap<Attribute, RuleMultiItem> extension, History history, ExtendType type,ExtendRuleConfig extensionConfig, float seedRuleConfidence) {
        super(new Rule(constructNewAntecedent(rule.getAntecedent(), extension), rule.getConsequent(), null, null, rule.getRID(), Rule.getNextERID(), rule.getData()));
        this.extendType = type;
        this.extensionConfig = extensionConfig;
        this.confidenceOfSeedRule = seedRuleConfidence;

        lastExtension=null;

        LOGGER.fine(rule.toString());
        this.rule.setQuality(computeQuality());

        if (history == null) {
            this.history = new History(this.getRID());
        } else {
            this.history = history;
        }
        this.history.addRuleIdentifiers(rule.getERID(), rule.toArray());
    }

        public ExtendRule(Rule rule, RuleMultiItem extension, History history, ExtendType type,ExtendRuleConfig extensionConfig, float seedRuleConfidence) {
        super(new Rule(constructNewAntecedent(rule.getAntecedent(), extension), rule.getConsequent(), null, null, rule.getRID(), Rule.getNextERID(), rule.getData()));
        this.extendType = type;
        this.extensionConfig = extensionConfig;
        this.confidenceOfSeedRule = seedRuleConfidence;

        lastExtension=extension;

        LOGGER.fine(rule.toString());
        this.rule.setQuality(computeQuality());

        if (history == null) {
            this.history = new History(this.getRID());
        } else {
            this.history = history;
        }
        this.history.addRuleIdentifiers(rule.getERID(), rule.toArray());
    }
        
    /**
     * constructor for rule with fuzzy borders
     * @param antecedent
     * @param cons
     * @param history
     * @param type
     * @param data
     */
    public ExtendRule(Antecedent antecedent, Consequent cons, History history, ExtendType type, Data data,ExtendRuleConfig conf, int seedRuleRID) {
        super(new Rule(antecedent, cons, null, null, seedRuleRID, Rule.getNextERID(), data));
        this.extendType = type;
        this.extensionConfig = conf;
        lastExtension = null;
        
        this.rule.setQuality(computeQuality());
        if (history == null) {
            this.history = new History(this.getRID());
        } else {
            this.history = history;
        }
        this.history.addRuleIdentifiers(this.rule.getERID(), this.rule.toArray());
        LOGGER.fine(rule.toString());
    }

    /**
     *
     * @return
     */
    public Rule getRule() {
        return rule;

    }

    /**
     *
     * @return
     */
    public ExtendRule enlargeLastExtension() {
        RuleMultiItem nextExtension = lastExtension.getExtended(lastExtension.lastModificationType);
        if (nextExtension == null) {
            LOGGER.fine("No more values");
            //try to extend attribute
            return null;
        }
        return new ExtendRule(rule, nextExtension, this.getHistory(), extendType,extensionConfig, this.getConfidenceOfSeedRule());
    }

    /**
     *
     * @param consequents
     */
    public void generateAnnotation(ArrayList<Consequent> consequents) {
        LOGGER.log(Level.INFO, "Generate annotation started for rule {0}", this);
        ExtendRuleAnnotation annot = new ExtendRuleAnnotation();
        annot.generate(this, consequents);
        rule.setAnnotation(annot);

        LOGGER.info("Generate annotation finished");
        LOGGER.fine(this.toString());
    }

    /**
     *
     * @return
     */
    public ExtendRuleAnnotation getAnnotation() {
        return rule.getAnnotation();
    }

    /**
     *
     * @return
     */
    public ExtendRule extend() {
        if (!isExtendable()) {
            return this;
        }
        boolean extensionSuccessful = false;
        ExtendRule curAcceptedExtension = this;
        
        LOGGER.fine("*************************************");
        LOGGER.log(Level.FINE, "STARTED Extension on rule: {0}\n", curAcceptedExtension);

        do {
            LOGGER.finest("*************************************");
            //new extension candidates: neighbours of the currently best extension
            LOGGER.log(Level.FINEST, "Computing neigbourhood for seed rule:{0}\n", curAcceptedExtension);
            ArrayList<ExtendRule> curNeighbourhood = curAcceptedExtension.getNeighourhood();
            LOGGER.log(Level.FINEST, "Candidate rules:{0}", curNeighbourhood.size());
            LOGGER.finest("Finished computing neigbourhood");
            //the candidates will be processed from the best to worst
            curNeighbourhood.sort(new MMACRuleComparator());
            extensionSuccessful = false;
            for (ExtendRule candExt : curNeighbourhood) {
                LOGGER.finest("+++++++++++++++++++++++++++++++++++++");
                LOGGER.log(Level.FINEST, "Current extension candidate {0}", candExt);
                LOGGER.finest("+++++++++++++++++++++++++++++++++++++");

                if (extensionConfig.acceptRule(candExt.getConfidence(), curAcceptedExtension.getConfidence(), getConfidenceOfSeedRule(), candExt.getSupport(), curAcceptedExtension.getSupport())){
//                if (candExt.getConfidence() - curAcceptedExtension.getConfidence() >=  extensionConfig.minImprovement && candExt.getSupport() - curAcceptedExtension.getSupport() > 0) {
                    extensionSuccessful = true;
                    curAcceptedExtension = candExt;
                    // improvement over the current best was found, the other candidates will not be considered
                    // the system will now use the new best as the seed for improvement
                    break;
                } else if (extensionConfig.conditionalAcceptRule(candExt.getConfidence(), curAcceptedExtension.getConfidence())) {
                    LOGGER.finest("Trying additional extensions to reach crisp accept");

                    ExtendRule candExtEnlargement = candExt;
                    do {
                        candExtEnlargement = candExtEnlargement.enlargeLastExtension();
                        if (candExtEnlargement == null) {
                            LOGGER.finest("-->Exhausted all values on this attribute in this direction, without finding acceptable candidate<--");
                            break;
                        }
                        LOGGER.log(Level.FINEST, "Evaluating extension: {0}", candExtEnlargement);
                        // see what the improvement is over the current best                       
                        if (extensionConfig.acceptRule(candExtEnlargement.getConfidence(), curAcceptedExtension.getConfidence(), getConfidenceOfSeedRule(), candExtEnlargement.getSupport(), curAcceptedExtension.getSupport())){
                            if (LOGGER.isLoggable(Level.FINE))
                            {
                                LOGGER.log(Level.FINE, "Accepting:{0}", candExtEnlargement);
                            }                            
                            curAcceptedExtension = candExtEnlargement;
                            extensionSuccessful = true;
                            break;
                        } else if (extensionConfig.conditionalAcceptRule(candExtEnlargement.getConfidence(), curAcceptedExtension.getConfidence())) {
                        } else {
                            //newly added
                            break;
                        }
                    } while (true);

                    if (extensionSuccessful) {
                        break;
                    }
                } else {
                    LOGGER.finest("Improvement below conditional threshold, going to next candidate");
                    //go to the next candidate, if there is any
                }

            }
            LOGGER.finest("Candidates exhausted or candidate accepted");
        } while (extensionSuccessful == true);
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.log(Level.FINE, "-->Finished extension on rule {0}<--", rule);
            LOGGER.log(Level.FINE, "*Result: {0}", curAcceptedExtension);            
            if (LOGGER.isLoggable(Level.FINE)){
                LOGGER.log(Level.FINE, "*History: \n {0}", curAcceptedExtension.history.toString());            
                LOGGER.log(Level.FINE, "*End of history");            
            }
        }

        return curAcceptedExtension;
    }

    /**
     *
     * @return
     */
    public ExtendRule addFuzzyBorders() {

        ArrayList<RuleMultiItem> newConsitutents = new ArrayList();
        getAntecedent().getItems().stream().forEach((ruleConstituent) -> {
            RuleMultiItem extendedWithFuzzyBorder = ruleConstituent.getExtended(ValueOrigin.fuzzy_border);
            if (extendedWithFuzzyBorder != null) {
                newConsitutents.add(extendedWithFuzzyBorder);
            } else {
                LOGGER.log(Level.FINE, "Fuzzy border not added for item {0}", ruleConstituent);
                newConsitutents.add(ruleConstituent);
            }
        });

        ExtendRule withfuzzyBorders = new ExtendRule(new Antecedent(newConsitutents), getConsequent(), getHistory(), extendType, rule.getData(),extensionConfig, rule.getRID());

        return withfuzzyBorders;
    }

    /**
     *
     * @return
     */
    public History getHistory() {
        return history.copy();
    }

    /**
     *
     * @return
     */
    public boolean isExtendable() {
        LOGGER.log(Level.FINE, "Checking rule {0} if it meets extensibility criteria.", rule);
        if (rule.getAntecedent().getItems().isEmpty()) {
            LOGGER.fine("Rules with empty antecedent cannot be extended.");
            return false;
        }

        for (RuleMultiItem rmi : rule.getAntecedent().getItems()) {

            Attribute at = rmi.getAttribute();
            AttributeValue lastVal = null;
            for (AttributeValue val : rmi.getAttributeValues()) {
                if (lastVal != null) {
                    if (at.getAdjacentHigher(lastVal) != val) {

                        LOGGER.log(Level.SEVERE, "Problem found for attribute {0}", at);
                        LOGGER.log(Level.SEVERE, "Value {0} is not higher than {1}", new Object[]{val, lastVal});
                        throw new UnsupportedOperationException(
                                "Rule contains values, which are not adjacent. This is not supported."); //To change body of generated methods, choose Tools | Templates.                
                    }
                }
            }
        }
        LOGGER.log(Level.FINE, "Rule {0} meets extensibility criteria", rule);
        return true;
    }

    /**
     *
     */
    public void updateQuality() {
        rule.setQuality(computeQuality());
    }

    /**
     *
     * @return
     */
    @Override
    public Node getXMLRepresentation() {
        GUHASerializerWithAnnotationSupport serializer = new GUHASerializerWithAnnotationSupport();
        Document newXmlDocument;
        try {
            newXmlDocument = DocumentBuilderFactory.newInstance()
                    .newDocumentBuilder().newDocument();
            Node thisRuleAsXML = serializer.getXMLforRule(rule, newXmlDocument);
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
    public ArrayList<ExtendRule> getNeighourhood() {
        ArrayList<ExtendRule> neighborhood = new ArrayList();
        //streaming version
        //neighborhood.addAll(ruleConstituent.getNeighbourhood().stream().map((multiitem)->new ExtendRule(rule,multiitem)).collect(Collectors.toCollection(ArrayList::new)));
        this.getAntecedent().getItems().stream().filter((ruleConstituent) -> !(extendType == ExtendType.numericOnly && ruleConstituent.getAttribute().getType() == AttributeType.nominal)).map((ruleConstituent) -> ruleConstituent.getNeighbourhood()).forEach((neighbourhood) -> {
            neighbourhood.stream().forEach((multiitem) -> {
                neighborhood.add(new ExtendRule(rule, multiitem, this.getHistory(), extendType,extensionConfig, this.getConfidenceOfSeedRule()));
            });
        });
        return neighborhood;
    }

    /**
     *
     * @return
     */
    public RuleQuality getRuleQuality() {
        return rule.getQuality();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(rule.toString(true));
        sb.append(history.toString());
        return sb.toString();

    }


    public ExtendRule trim(){
        
        LOGGER.log(Level.INFO, "STARTED TRIMMING on rule: {0}\n", rule);
        //determine correctly covered transactions in the current rule
        Set<Transaction> correctlyCoveredTrans = getAntecedent().getSupportingTransactions();
        Set<Transaction> conTran = getConsequent().getSupportingTransactions();        
        HashMap<Attribute,RuleMultiItem> newLiterals = new HashMap();
        if (correctlyCoveredTrans == null| correctlyCoveredTrans.isEmpty()) {
            LOGGER.info("Rule does not cover any transactions, leaving as is");     
        } else {            
            correctlyCoveredTrans.retainAll(conTran);                
            if (correctlyCoveredTrans.isEmpty()) {
                LOGGER.info("Rule does not CORRECTLY cover any transactions, leaving as is");     
            }
            //determine minimum and maximum value appearing in individual attributes in the current rule
            else if (rule.getAntecedent().getItems().isEmpty()) {
                LOGGER.fine("Rules with empty antecedent cannot be trimmed.");                        
            }
            else{                       
                for (RuleMultiItem rmi : rule.getAntecedent().getItems()) {
                    
                    Attribute at = rmi.getAttribute();
                    LOGGER.log(Level.FINE, "Processing attribute: {0}", at.getName());
                    if (at.getType()==AttributeType.nominal)
                    {
                        LOGGER.fine("Skipping nominal attribute.");
                        newLiterals.put(rmi.getAttribute(),rmi);
                        continue;
                    }
                    ArrayList<AttributeValue> attVals = rmi.getAttributeValues();
                    if (attVals.size()==1)
                    {
                        LOGGER.fine("Literals with single value cannot be trimmed.");
                        newLiterals.put(rmi.getAttribute(),rmi);
                        continue;
                    }
                    else if (attVals.isEmpty())
                    {
                        LOGGER.warning("Literal with no value!");
                        newLiterals.put(rmi.getAttribute(),rmi);
                        continue;
                    }            

                    //get maximum value of transactions in this attribute            
                    ArrayList<AttributeValue> coveredValues = new ArrayList();

                    correctlyCoveredTrans.stream().forEach((t) -> {
                        coveredValues.add(t.getValue(at));
                    });

                    LOGGER.log(Level.FINE, "correctlyCoveredTrans: {0}", correctlyCoveredTrans.size());
                    Collections.sort(coveredValues, new Comparator<AttributeValue>() {
                        @Override
                        public int compare(AttributeValue o1, AttributeValue o2) {
                            return Float.compare(o1.getNumericalValue(),o2.getNumericalValue());
                        }
                    });            
                    AttributeValue lowestValue = coveredValues.get(0);
                    AttributeValue highestValue = coveredValues.get(coveredValues.size()-1);

                    //get all values between lowest and highest and create a new literal (RuleMultiItem)
                    ArrayList<AttributeValue> list = new ArrayList(at.getValuesInRange(lowestValue.getNumericalValue(), true, highestValue.getNumericalValue(), true));            
                    ArrayList<ValueOrigin> valOriginAsArray = new ArrayList();
                    for (AttributeValue av : list)
                    {
                        valOriginAsArray.add(ValueOrigin.core);
                    }            

                    RuleMultiItem replacement = rule.getData().makeRuleItem(list, valOriginAsArray, at, ValueOrigin.trim);
                    newLiterals.put(replacement.getAttribute(),replacement);

                }
            }
        }
                        
        
         ExtendRule newRule = new ExtendRule(rule, newLiterals, this.getHistory(), this.getExtendType(),extensionConfig, this.getConfidenceOfSeedRule());
         LOGGER.log(Level.INFO, "FINISHED TRIMMING, result: {0}\n", newRule);
         return newRule;
        
   
    }
    /*
    @TODO:  Check if the following return codes are o.k.
    returns number of supporting transaction  removed,
     */
    private RuleQuality computeQuality() {
        Set<Transaction> antConOverlap = getAntecedent().getSupportingTransactions();
        Set<Transaction> conTran = getConsequent().getSupportingTransactions();
        //frequency a+b from the 4ft contingency table
        int coverage;
        int support;
        if (antConOverlap != null) {
            coverage = antConOverlap.size();
            antConOverlap.retainAll(conTran);
            support = antConOverlap.size();
        } else {
            //antecedent is empty, has no items, and thus matches every transaction
            coverage = rule.data.getDataTable().getCurrentTransactionCount();
            support = conTran.size();
        }
        return new RuleQuality(support, coverage - support, rule.getData().getDataTable().getLoadedTransactionCount());

    }

    /**
     *
     * @return
     */
    @Override
    public int getAntecedentLength() {
        return rule.getAntecedentLength();
    }

    /**
     *
     * @return
     */
    @Override
    public float getConfidence() {
        return rule.getConfidence();
    }

    /**
     *
     * @return
     */
    @Override
    public Consequent getConsequent() {
        return rule.getConsequent();
    }

    /**
     *
     * @return
     */
    @Override
    public Antecedent getAntecedent() {
        return rule.getAntecedent();
    }

    /**
     *
     * @return
     */
    @Override
    public int getRID() {
        return rule.getRID();
    }

    /**
     *
     * @return
     */
    @Override
    public int getSupport() {
        return rule.getSupport();
    }

    /**
     *
     * @return
     */
    public ExtendType getExtendType() {
        return extendType;
    }

}
