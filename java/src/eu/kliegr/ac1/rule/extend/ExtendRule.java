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
import java.util.Iterator;
import java.util.NoSuchElementException;
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
public final class ExtendRule implements RuleInt {

    private final static Logger LOGGER = Logger.getLogger(ExtendRule.class.getName());
    private ExtendRuleConfig extensionConfig;
    private Float confidenceOfSeedRule;
    protected Rule rule;
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
    private static Antecedent constructNewAntecedent(Antecedent antecedent, HashMap<Attribute,RuleMultiItem> replacement) {
        ArrayList<RuleMultiItem> newAntecedentItems = new ArrayList();
        antecedent.getItems().stream().forEach((multiitem) -> {
            if (replacement.containsKey(multiitem.getAttribute())) {
                newAntecedentItems.add(replacement.get(multiitem.getAttribute()));
            } else {
                newAntecedentItems.add(multiitem);
            }
        });
        return new Antecedent(newAntecedentItems);
    }
    private static Antecedent constructNewAntecedent(Antecedent antecedent,  Attribute attributeToRemove) {
        ArrayList<RuleMultiItem> newAntecedentItems = new ArrayList();
        antecedent.getItems().stream().forEach((multiitem) -> {
            if (attributeToRemove  == multiitem.getAttribute()) {
                
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

    public ExtendRule(Rule rule)
    {
        this.rule = rule;
    }

    /**
     * constructor for seed rule
     * @param rule
     * @param history
     * @param type
     * @param conf
     */
    public ExtendRule(Rule rule, History history, ExtendType type,ExtendRuleConfig conf) {
        this.rule = rule;
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
        this.rule = new Rule(constructNewAntecedent(rule.getAntecedent(), extension), rule.getConsequent(), null, null, rule.getRID(), Rule.getNextERID(), rule.getData());
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

    public ExtendRule(Rule rule, Attribute atToRemove, History history, ExtendType type,ExtendRuleConfig extensionConfig, float seedRuleConfidence) {
        this.rule = new Rule(constructNewAntecedent(rule.getAntecedent(), atToRemove), rule.getConsequent(), null, null, rule.getRID(), Rule.getNextERID(), rule.getData());
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
        this.rule = new Rule(constructNewAntecedent(rule.getAntecedent(), extension), rule.getConsequent(), null, null, rule.getRID(), Rule.getNextERID(), rule.getData());
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
        this.rule = new Rule(antecedent, cons, null, null, seedRuleRID, Rule.getNextERID(), data);
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
        return new ExtendRule(rule, nextExtension, this.copyHistory(), extendType,extensionConfig, this.getConfidenceOfSeedRule());
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
            curNeighbourhood.sort(new CBARuleComparator());
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
            LOGGER.log(Level.FINE, "*History: \n {0}", curAcceptedExtension.history.toString());            
            LOGGER.log(Level.FINE, "*End of history");            
            
        }
        curAcceptedExtension.addItselfToHistory();
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

        ExtendRule withfuzzyBorders = new ExtendRule(new Antecedent(newConsitutents), getConsequent(), copyHistory(), extendType, rule.getData(),extensionConfig, rule.getRID());
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine("Finished creating version with fuzzy borders");
            LOGGER.log(Level.FINE, "*Result:{0}", withfuzzyBorders);
        }
        return withfuzzyBorders;
    }

    /**
     *
     * @return
     */
    public History copyHistory() {
        return history.copy();
    }
    public void addItselfToHistory() {
        history.addRuleIdentifiers(this.rule.getERID(), this.rule.toArray());
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
                neighborhood.add(new ExtendRule(rule, multiitem, this.copyHistory(), extendType,extensionConfig, this.getConfidenceOfSeedRule()));
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


   
    /**
     *
     * @param hide
     * @return
     */
        public int removeTransactionsCoveredByAntecedent(Boolean hide) {
         int pruningCoverage = 0;
        try {
  
            Set<Transaction> supportingTransactions = getAntecedent().getSupportingTransactions();
            if (getAntecedent() == null) {
                //antecedent is empty (no items), the antecedent matches all transactions
                pruningCoverage= rule.getData().getDataTable().getAllCurrentTransactions().size();
                rule.getData().getDataTable().removeAllTransactions(hide);                
            } 
            else {
                pruningCoverage = supportingTransactions.size();
                if (pruningCoverage > 0) {
                    //TODO: add support for parallel stream
                    supportingTransactions.stream().forEach((t) -> rule.getData().getDataTable().removeTransaction(t, hide));
                }

            }            
            //this should delete all references to the transaction
        } catch (NoSuchElementException e) {
            LOGGER.warning(e.toString());
            pruningCoverage = 0;
        }
        return pruningCoverage;
    }
    public int removeCorrectlyClassifiedTransactions(Boolean hide) {
         int pruningCoverage = 0;
        try {
            Set<Transaction> supportingTransactions;

            Set<Transaction> antConOverlap = getAntecedent().getSupportingTransactions();
            Set<Transaction> conTran = getConsequent().getSupportingTransactions();
            if (antConOverlap == null) {
                //antecedent is empty (no items), the transactions supported by the rule are determined only by consequent
                antConOverlap = conTran;
            } else {
                antConOverlap.retainAll(conTran);
            }

            supportingTransactions = antConOverlap;
            pruningCoverage = supportingTransactions.size();
            if (pruningCoverage > 0) {
                //TODO: add support for parallel stream
                supportingTransactions.stream().forEach((t) -> rule.getData().getDataTable().removeTransaction(t, hide));
            }
            return pruningCoverage;
            //this should delete all references to the transaction
        } catch (NoSuchElementException e) {
            return 0;
        }
    }

public ExtendRule removeRedundantAttributes(){
        boolean ruleChanged =false;
        LOGGER.log(Level.INFO, "STARTED REMOVING REDUNDANT ATTRIBUTES on rule: {0}\n", rule);
        //determine correctly covered transactions in the current rule
        Set<Transaction> correctlyCoveredTrans = getAntecedent().getSupportingTransactions();
        Set<Transaction> conTran = getConsequent().getSupportingTransactions();        
        ExtendRule newRule = this;
        // at this point correctlyCoveredTrans holds covered transactions (not only correctly covered)
        if (correctlyCoveredTrans == null| correctlyCoveredTrans.isEmpty()) {
            LOGGER.info("Rule does not cover any transactions, leaving as is");     
        } else {            
            correctlyCoveredTrans.retainAll(conTran);                
            if (correctlyCoveredTrans.isEmpty()) {
                LOGGER.info("Rule does not CORRECTLY cover any transactions, leaving as is");     
            }
            //determine minimum and maximum value appearing in individual attributes in the current rule
            else if (rule.getAntecedent().getItems().isEmpty()) {
                LOGGER.fine("Rule with empty antecedent.");                        
            }
            else{                    
                //HashMap<Attribute,RuleMultiItem> allConfirmedLiterals = new HashMap();
                
                boolean attrRemoved = false;
                do
                {
                    //TODO: processing attributes in some sensible order may improve results
                    
                    for (RuleMultiItem rmi : newRule.getAntecedent().getItems()) {
                        // consider remove current literal
                        LOGGER.fine("Considering removal of literal created from attribute " + rmi.getAttribute());
                        ExtendRule candNewRule = new ExtendRule(newRule.getRule(), rmi.getAttribute(), this.copyHistory(), this.getExtendType(),extensionConfig, -1);
                        candNewRule.updateQuality();
                        if (this.getConfidence() <= candNewRule.getConfidence())
                        {
                           LOGGER.fine("Confidence did not decrease after removing literal " + rmi + " from " + this.getConfidence() + " to " + candNewRule.getConfidence() );
                           //allConfirmedLiterals.remove(rmi.getAttribute());
                           attrRemoved = true;
                           ruleChanged = true;
                           newRule = candNewRule;
                           break;
                        }
                        else{
                            attrRemoved = false;
                        }                        
                    }                                       
                    // added on 23/08/19 "&& newRule.getAntecedent().getItems().size()>0"
                } while (attrRemoved==true && newRule.getAntecedent().getItems().size()>0);                                          
            }
        }
        if (!ruleChanged)
        {
             LOGGER.fine("Rule did not change during attribute pruning" );
             return this;
        }
        else
        {
        // changed base confidence for trimming from original seed rule to the trimmed rule                       
            
            return newRule;            
            
        }
        
}

    public ExtendRule trim(){
        boolean ruleChanged =false;
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

                    //get distinct values appearing in the attribute in CORRECTLY COVERED TRANSACTIONS
                    ArrayList<AttributeValue> coveredValues = new ArrayList();

                    correctlyCoveredTrans.stream().forEach((t) -> {
                        coveredValues.add(t.getValue(at));
                    });
                    // get lowest and highest value
                    LOGGER.log(Level.FINE, "correctlyCoveredTrans: {0}", correctlyCoveredTrans.size());
                    Collections.sort(coveredValues, new Comparator<AttributeValue>() {
                        @Override
                        public int compare(AttributeValue o1, AttributeValue o2) {
                            return Float.compare(o1.getNumericalValue(),o2.getNumericalValue());
                        }
                    });            
                    AttributeValue lowestValue = coveredValues.get(0);
                    AttributeValue highestValue = coveredValues.get(coveredValues.size()-1);
                    
                    // check if these values are different from the original literal
                    AttributeValue origMin = rmi.getAttributeValues().get(0);
                    AttributeValue origMax = rmi.getAttributeValues().get(rmi.getAttributeValues().size()-1);
                    if (origMin==lowestValue && highestValue== origMax)
                    {
                        LOGGER.fine("Trimming produced same literal as original.");
                        newLiterals.put(rmi.getAttribute(),rmi);
                        continue;
                    }
                    
                    //get all values between lowest and highest and create a new literal (RuleMultiItem)
                    ArrayList<AttributeValue> list = new ArrayList(at.getValuesInRange(lowestValue.getNumericalValue(), true, highestValue.getNumericalValue(), true));            
                    ArrayList<ValueOrigin> valOriginAsArray = new ArrayList();
                    for (AttributeValue av : list)
                    {
                        valOriginAsArray.add(ValueOrigin.core);
                    }            

                    RuleMultiItem replacement = rule.getData().makeRuleItem(list, valOriginAsArray, at, ValueOrigin.trim);
                    newLiterals.put(replacement.getAttribute(),replacement);
                    ruleChanged = true;
                }
            }
        }
        if (!ruleChanged)
        {
             LOGGER.fine("Rule did not change during trimming" );
             return this;
        }
        else
        {
        // changed base confidence for trimming from original seed rule to the trimmed rule
            ExtendRule newRule = new ExtendRule(rule, newLiterals, this.copyHistory(), this.getExtendType(),extensionConfig, -1);

            newRule.updateQuality();

            if (this.getConfidence() != newRule.getConfidence())
            {
               LOGGER.fine("Confidence changed after trimming from " + this.getConfidence() + " to " + newRule.getConfidence() );
            }

            newRule.confidenceOfSeedRule = newRule.getConfidence();
            LOGGER.log(Level.INFO, "FINISHED TRIMMING, result: {0}\n", newRule);
            return newRule;
            
        }
        
   
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
    public String getArulesRepresentation() {
        return rule.getArulesRepresentation();
    }

}
