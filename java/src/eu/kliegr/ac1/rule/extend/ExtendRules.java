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
import eu.kliegr.ac1.data.AttributeValue;
import eu.kliegr.ac1.data.Transaction;
import eu.kliegr.ac1.performance.StopWatches;
import eu.kliegr.ac1.rule.Antecedent;
import eu.kliegr.ac1.rule.Consequent;
import eu.kliegr.ac1.rule.Data;
import eu.kliegr.ac1.rule.PruneRules;
import eu.kliegr.ac1.rule.PruneType;
import eu.kliegr.ac1.rule.Rule;
import eu.kliegr.ac1.rule.RuleMultiItem;
import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 *
 * @author tomas
 */
public class ExtendRules {

    private final static Logger LOGGER = Logger.getLogger(ExtendRules.class.getName());
    private final List<ExtendRule> seedRules;
    private List<ExtendRule> extendedRules;
    private final Comparator ruleComparator;
    private final ExtendType type;
    private final Data data;

    /**
     *
     * @param rules
     * @param ruleComparator
     * @param type
     * @param extConf
     * @param data
     */
    public ExtendRules(ArrayList<Rule> rules, Comparator ruleComparator, ExtendType type, ExtendRuleConfig extConf,Data data) {
        //Create a Stream from the personList
        this.type = type;
        this.ruleComparator = ruleComparator;
        this.data = data;
        this.seedRules = rules.stream().map((rule) -> new ExtendRule(rule, null, type,extConf)).collect(Collectors.toCollection(() -> Collections.synchronizedList(new ArrayList<ExtendRule>())));
        LOGGER.log(Level.INFO, "Rules loaded: {0}", this.seedRules.size());
    }

    /**
     *
     * @return
     */
    public List<ExtendRule> getSeedRules() {
        return Collections.unmodifiableList(seedRules);
    }

    /**
     *
     * @return
     */
    public List<ExtendRule> getExtendedRules() {
        return Collections.unmodifiableList(extendedRules);
    }

    /**
     *
     */
    public void sortRules() {
        seedRules.sort(ruleComparator);
    }

    /**
     *
     */
    public void annotateRules() {
        ArrayList<Consequent> consequents = generateConsequents();
        final AtomicInteger i = new AtomicInteger(0);
        extendedRules.parallelStream().forEach((rule) -> {
            if (LOGGER.isLoggable(Level.INFO)) {
                LOGGER.log(Level.INFO, "Starting annotation of rule:{0}", rule.getRID());
            }
            if (rule == null) {
                LOGGER.severe("Rule object is null, this should not happen, skipping");
            } else {
                rule.generateAnnotation(consequents);
            }

            i.getAndAdd(1);
            LOGGER.log(Level.INFO, "Rules already annotated:{0} ; out of {1}", new Object[]{i, extendedRules.size()});
        }
        );
    }

    private ArrayList<Consequent> generateConsequents() {
        ArrayList<AttributeValue> cons = new ArrayList(data.getValuesOfTargetAttribute());
        ArrayList<ValueOrigin> consOrigin = new ArrayList();
        //TODO set to core only for the value actually appearing in the rule
        cons.stream().forEach((value) -> consOrigin.add(ValueOrigin.consequent));
        ArrayList<Consequent> consequents = new ArrayList();

        cons.stream().forEach((value) -> {
            ArrayList<AttributeValue> valuesInConsequent = new ArrayList();
            valuesInConsequent.add(value);
            RuleMultiItem consNewCons = data.makeRuleItem(valuesInConsequent, consOrigin, value.getAttribute(), ValueOrigin.fakeConsequent);
            consequents.add(new Consequent(consNewCons));
        });
        return consequents;
    }


    /**
     *
     * @param isContinuousPruningEnabled
     * @param isFuzzificationEnabled
     * @param postpruningType
     */


    public void processRules(boolean isAttRemovalEnabled, boolean isTrimmingEnabled,boolean isContinuousPruningEnabled, boolean isFuzzificationEnabled, PostPruningType postpruningType, DefaultRuleOverlapPruningType defaultRuleOverlapPruningType) {
        LOGGER.info("STARTED Extension phase\n");
        //TODO change to parallel stream
        //extendedRules = seedRules.stream().map((r)->r.extend()).collect(Collectors.toCollection(ArrayList::new));
        AtomicInteger processedRules = new AtomicInteger(0);
        int lastRuleRID = seedRules.get(seedRules.size() - 1).getRID();
        extendedRules = new ArrayList();
        //cannot be parallel stream if pruning is performed in the same iteration
        //PHASE 1
        
        //extendedRules = rules.stream().map(rule -> {
        for (Iterator<ExtendRule> it = seedRules.iterator(); it.hasNext();)
        {
            ExtendRule rule = it.next();
            LOGGER.log(Level.INFO, "Rules already processed:{0}  out of {1}", new Object[]{processedRules.addAndGet(1), this.seedRules.size()});
            int antLength = rule.getAntecedent().getItems().size();

            if (antLength == 0) {
                if (rule.getRID() == lastRuleRID) {
                    LOGGER.info("Removing default rule ");
                    continue;
                } else {
                    LOGGER.severe("Unexpected rule with empty antecedent on other than last position, leaving out this rule");
                    continue;
                }
            } else if (rule.getRID() == lastRuleRID) {
                LOGGER.severe("Last rule is expected to be a default rule with empty antecedent");
            }
            /*if (data.getDataTable().getCurrentTransactionCount() == 0) {
                
            }*/
            
            if (isAttRemovalEnabled)
            {
                LOGGER.info("STARTED removeRedundantAttributes ");
                rule = rule.removeRedundantAttributes();
                LOGGER.info("FINISHED removeRedundantAttributes ");
            }
            
            if (rule.getAntecedent().getItems().size() == 0)
            {
                LOGGER.info("REMOVING DEFAULT RULE CREATED BY removeRedundantAttributes ");
                continue;
            }
                      
            
            if (isTrimmingEnabled)
            {
                rule = rule.trim();
            }

            if (type!=ExtendType.noExtend)
            {
                rule = rule.extend();                
            }
            if (isFuzzificationEnabled) {
                rule = rule.addFuzzyBorders();
            }
            if (isContinuousPruningEnabled) {
                //and now remove the transactions covered by the extended rule
                int transRemoved = rule.removeTransactionsCoveredByAntecedent(true);
                LOGGER.log(Level.INFO, "Removed {0} supporting transactions", transRemoved);
                if (transRemoved == 0) {
                    continue;
                }                    
            }
            extendedRules.add(rule);
        };
        
        LOGGER.info("FINISHED PHASE 1 phase\n");
        if (isContinuousPruningEnabled)
        {
            // only cp hides (removes) transactions
            data.getDataTable().unhideAllTransactions();
        }
        LOGGER.info("STARTED updating rule quality\n");    
        extendedRules.stream().forEach((r) -> {
             r.updateQuality();
        });   
        LOGGER.info("FINISHED updating rule quality\n");    

        extendedRules.sort(ruleComparator);
        if (postpruningType != PostPruningType.none) {
            
            LOGGER.info("STARTED POST PRUNING phase\n");
            LOGGER.info("STARTED Resorting rules\n");    
            
            LOGGER.info("FINISHED Resorting rules\n");                    

            LOGGER.log(Level.INFO, "Rules before pruning (default rule, if any, was removed):{0}", extendedRules.size());
            if (postpruningType == PostPruningType.cba  )
            {
                pruneRules_cbaLike();    
            }
            else if (postpruningType == PostPruningType.greedy)
            {
                pruneRules_greedy();    
            }
            
            data.getDataTable().unhideAllTransactions();

            LOGGER.log(Level.INFO, "Rules after pruning (with default rule):{0}", extendedRules.size());
            LOGGER.info("FINISHED PRUNING phase\n");
            //}
        }
        else
        {
            //insert default rule because it will not be done during postpruning and it was removed during phase 1
            ExtendRule finalDefRule = createNewDefaultRule(getDefaultRuleClass());
            extendedRules.add(finalDefRule);            
        }
        /*if (isContinuousPruningEnabled) {
            //rule metrics computed during continous pruning are computed on subset of data
            // and thus are not valid metrics for ordering the rule set in a test setting`
            data.getDataTable().unhideAllTransactions();
            extendedRules.stream().forEach((r) -> {
                r.updateQuality();
            });
        }*/                               

        if (defaultRuleOverlapPruningType == DefaultRuleOverlapPruningType.transactionBased | defaultRuleOverlapPruningType == DefaultRuleOverlapPruningType.rangeBased)
        {               
            if (defaultRuleOverlapPruningType == DefaultRuleOverlapPruningType.transactionBased){
                extendedRules = removeRedundantExtendedRules_transactionBased(extendedRules);
                data.getDataTable().unhideAllTransactions();
            }
            else{
                extendedRules = removeRedundantExtendedRules_rangeBased(extendedRules);            
            }            
        }
        
    }
    

    /**
     *
     * @param path
     * @param watches
     * @throws FileNotFoundException
     * @throws IOException
     */
    public void saveSummary(String path, StopWatches watches) throws FileNotFoundException, IOException {
        OutputStream output = new BufferedOutputStream(new FileOutputStream(path));
        output.write(("Number of rules to extend:" + seedRules.size() + "\n").getBytes());
        output.write(("Number of extended rules :" + extendedRules.size() + "\n").getBytes());

        output.write(watches.toString().getBytes());
        output.flush();
        output.close();
    }
          /*
      This prunes the rules, making the cut off at the first rule where replacement with the default rule decreases error
      */
      public void pruneRules_greedy() {
        LOGGER.info("STARTED Postpruning");
        AttributeValue defClass =  getDefaultRuleClass();
        int defError = getDefaultRuleError(defClass);
        boolean removeTail=false;
        for (Iterator<ExtendRule> it = extendedRules.iterator(); it.hasNext();) {

            ExtendRule rule = it.next();
            rule.updateQuality();
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(Level.FINE, "#Rule {0}", rule.toString());
            }
            
            if (removeTail)
            {
                it.remove();
            }
            else if (rule.getAntecedentLength() == 0) {
                it.remove();
            }     
            else if (rule.getRuleQuality().getA() == 0)
            {
                it.remove();
            }
            else
            {
                int supportingTransactions = rule.removeTransactionsCoveredByAntecedent(true);
                AttributeValue newDefClass =  getDefaultRuleClass();
                int newDefError = getDefaultRuleError(newDefClass);
                if (defError<=newDefError)
                {
                    //adding the current rule did not decrease the errors compared to a default rule
                    it.remove();
                    removeTail=true;
                }
                else{
                    LOGGER.log(Level.FINE, "{0} transactions, RULE {1} KEPT", new Object[]{supportingTransactions, rule.getRID()});
                    defClass  = newDefClass;
                    defError  = newDefError;
                }                
            }
        
            


        }
        LOGGER.fine("Creating new Extend rule within narrow rule procedure");
        extendedRules.add(createNewDefaultRule(defClass));
        LOGGER.info("FINISHED Postpruning");
    }

      /*
      This prunes the rules, making the cut off at the rule with globally lowest total error
      */
    public void pruneRules_cbaLike() {
        LOGGER.info("STARTED Postpruning");
        //HashMap<ExtendRule,Integer> ruleErrors = new HashMap();
        //HashMap<ExtendRule,AttributeValue> ruleDefClass = new HashMap();
        ArrayList<ExtendRule> rulesToRemove = new ArrayList();        
        int totalErrorsWithoutDefault = 0;        
        AttributeValue defClassForLowestTotalErrorsRule =  getDefaultRuleClass();
        int lowestTotalErrors = getDefaultRuleError(defClassForLowestTotalErrorsRule);;
        ExtendRule lowestTotalErrorsRule  = null;
        // DETERMINE TOTAL ERROR AND DEFAULT CLASS ASSOCIATED WITH EACH RULE 
        // REMOVE RULES MATCHING ZERO TRANSACTIONS AND OF ZERO LENGTH
        for (Iterator<ExtendRule> it = extendedRules.iterator(); it.hasNext();) {

            ExtendRule rule = it.next();
            rule.updateQuality();
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(Level.FINE, "Processing rule {0}", rule.toString());
            }

            if (rule.getAntecedentLength() == 0) {
                LOGGER.fine("Rule of length 0, MARKED FOR REMOVAL");
                rulesToRemove.add(rule); //covered transactions should not be removed
            }                 
            else if (rule.getRuleQuality().getA() == 0)
            {
                LOGGER.fine("Rule classifying 0 instances correctly, MARKED FOR REMOVAL");
                rulesToRemove.add(rule); //covered transactions should not be removed                
            }
            else
            {
                rule.removeTransactionsCoveredByAntecedent(true);                
                totalErrorsWithoutDefault = totalErrorsWithoutDefault + rule.getRuleQuality().getB();
                // since transactions matching the current rule have been removed, the default class and error can change
                AttributeValue newDefClass =  getDefaultRuleClass();
                int newDefError = getDefaultRuleError(newDefClass);
                int totalErrorWithDefault = newDefError + totalErrorsWithoutDefault;
                if (totalErrorWithDefault < lowestTotalErrors)
                {
                    lowestTotalErrors = totalErrorWithDefault;
                    lowestTotalErrorsRule = rule;
                    defClassForLowestTotalErrorsRule= newDefClass;
                }                    
                //ruleErrors.put(rule,totalErrorWithDefault );
                //ruleDefClass.put(rule, newDefClass);                    
            }
                    
        }
        boolean removeTail;
        // now we know the errors associated with each rule not marked for removal, we can perform pruning
        if (lowestTotalErrorsRule == null)
        {
            // no rule improves error over a classifier composed of only default rule
            // remove all rules
            removeTail = true;
        }
        else 
        {
            removeTail = false;
        }
        
        data.getDataTable().unhideAllTransactions();
        for (Iterator<ExtendRule> it = extendedRules.iterator(); it.hasNext();) {
            ExtendRule rule = it.next();
            if (rulesToRemove.contains(rule) || removeTail)
            {
                it.remove();
                continue;
            }
            if (rule.equals(lowestTotalErrorsRule))
            {
                removeTail = true;
            }
            rule.updateQuality();            
        }
        
        
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine("Creating new default rule within narrow rule procedure");
        }
        
        
        extendedRules.add(createNewDefaultRule(defClassForLowestTotalErrorsRule));
        
        
        LOGGER.info("FINISHED Postpruning");
    }
    
    // this version considers a rule redundant on the basis of transaction overlap in training data
    // this is less precise and removes more rules
    public static List<ExtendRule> removeRedundantExtendedRules_transactionBased(List<ExtendRule> rules) {
        LOGGER.info("STARTED removeRedundantExtendedRules - transaction based");
        LOGGER.info("Rules on start:" + rules.size());
        ExtendRule defRule = rules.get(rules.size()-1);
        if (defRule.getAntecedent().getItems().size()>0)
        {
            LOGGER.warning("Default rule is not last rule. Returning null. Last rule:" + defRule.toString() );
            
            return null;
        }
        Consequent defClass = defRule.getConsequent();        
        for (Iterator<ExtendRule> it = rules.iterator(); it.hasNext();) {             
            ExtendRule PRCandidate = it.next();
            // PRCandidate = go through all rules with default class in  the consequent
            if (!PRCandidate.getConsequent().toString().equals(defClass.toString()))
            {

            }
            // skip default rule
            else if (PRCandidate.equals(defRule))
            {
                
            }   
            else{
                Set<Transaction> suppTran = PRCandidate.getAntecedent().getSupportingTransactions();
                // get transactions only CORRECTLY classified by the candidate rule
                Set<Transaction> consTran = PRCandidate.getConsequent().getSupportingTransactions();
                suppTran.retainAll(consTran);

                // Check if transactions correctly classified by pruning candidate intersect with transactions covered by those rules below prunCand in the rule list that assign  to different than default class.  If there are no such transactions PRCandidate can be removed
                boolean nonEmptyIntersection=false;
                boolean positionBelowPRCand = false;
                for (Iterator<ExtendRule> innerIt = rules.iterator(); innerIt.hasNext();) {
                    ExtendRule candidateClash = innerIt.next();
                    // candidateClash is PRCandidate, which would always evaluate to overlap!
                    if (candidateClash.equals(PRCandidate))
                    {
                        positionBelowPRCand = true;
                        continue;
                    }
                    if (!positionBelowPRCand) continue;
                    // candidateClash = go through all rules  with OTHER than default class in consequent 
                    if (candidateClash.getConsequent().toString().equals(defClass.toString()))
                    {
                        continue;
                    }

                    // check if transactions covered by PRCandidate intersect with transactions covered by candidateClash                

                    for (Transaction t : candidateClash.getAntecedent().getSupportingTransactions())
                    {

                        if (suppTran.contains(t))
                        {
                            nonEmptyIntersection=true;
                        }
                            break;
                    }                
                    if (nonEmptyIntersection)
                    {
                        //go to next PRCandidate             
                        break;
                    }                    
                }
                if (nonEmptyIntersection == false)
                {
                    //no other rule with different consequent covering at least one shared transaction was found
                    //this rule can be removed
                    LOGGER.fine("Removing rule:" + PRCandidate.toString());
                    it.remove();
                }                           
            }
               
        }
        LOGGER.info("Rules on finish:" + rules.size());
        LOGGER.info("FINISHED removeRedundantExtendedRules - transaction based");
        return rules;
    }
    
    
    // this version considers a rule redundant on the basis of comparing regions matched by the rules
    // this is precise, but does not remove many rules
    public static List<ExtendRule> removeRedundantExtendedRules_rangeBased(List<ExtendRule> rules) {
        LOGGER.info("STARTED removeRedundantExtendedRules - range based");
        LOGGER.info("Rules on start:" + rules.size());
        ExtendRule defRule = rules.get(rules.size()-1);
        Consequent defClass = defRule.getConsequent();        
        for (Iterator<ExtendRule> it = rules.iterator(); it.hasNext();) {             
            ExtendRule PRCandidate = it.next();
            // PRCandidate = go through all rules with default class in  the consequent
            if (!PRCandidate.getConsequent().toString().equals(defClass.toString()))
            {
                continue;
            }
            // skip default rule
            if (PRCandidate.equals(defRule))
            {
                continue;
            }            
            ArrayList<RuleMultiItem> PR_items = PRCandidate.getAntecedent().getItems();
            //create hashmap for fast access
            HashMap<Attribute,RuleMultiItem> PR_itemsMap = new HashMap();
            for (RuleMultiItem rmi : PR_items)
            {
                PR_itemsMap.put(rmi.getAttribute(), rmi);
            }
            
            // check if there is no other rule classifying to different class that shares part of the region
            // matched by the antecedent of PRCandidate 
            boolean clashingRuleFound=false;
            boolean positionBelowPRCand = false;
            for (Iterator<ExtendRule> innerIt = rules.iterator(); innerIt.hasNext();) {
                ExtendRule candidateClash = innerIt.next();
                // candidateClash is PRCandidate, which would always evaluate to overlap!

                if (candidateClash.equals(PRCandidate))
                {
                    positionBelowPRCand=true;
                    continue;
                }
                // candidateClash = go through all rules  with OTHER than default class in consequent 
                if (candidateClash.getConsequent().toString().equals(defClass.toString()))
                {
                    continue;
                }
                if (!positionBelowPRCand) continue;
                
                // check if at leat one literal in PRCandidate has empty intersection with Clash on shared attribute
                
                //in case there is no shared attribute, the intersection is non empty
                ArrayList<RuleMultiItem> literalsInClashOnSharedAtt  = new ArrayList();
                for (RuleMultiItem clash_rmi : candidateClash.getAntecedent().getItems())
                {
                    if (PR_itemsMap.containsKey(clash_rmi.getAttribute()))
                    {
                        literalsInClashOnSharedAtt.add(clash_rmi);
                    }
                }             
                if (literalsInClashOnSharedAtt.isEmpty())
                {
                    clashingRuleFound =true;
                    break;
                }
                // if there is NO intersection on at leat one of the shared attributes => no CLASH
                // ELSE => CLASH
                boolean attLeastOneAttDisjunct=true;
                for (RuleMultiItem clash_rmi : literalsInClashOnSharedAtt)
                {
                    //PRCandidate has a literal created over the same attribute
                    RuleMultiItem machingPR_RMI = PR_itemsMap.get(clash_rmi.getAttribute());

                    //do values in the candidate clash intersect with values in PRCandidate?
                    
                    for (AttributeValue v : machingPR_RMI.getAttributeValues())
                    {
                        if (clash_rmi.getAttributeValues().contains(v))
                        {
                            attLeastOneAttDisjunct=false;
                        }
                    }
                    if (attLeastOneAttDisjunct) // if we have non-empty intersection on one att we do not have to test the other ones, rule matches different part of the data space
                    {
                        //go to next PRCandidate             
                        break;
                    }                    
                }                
                if (!attLeastOneAttDisjunct)
                {
                    //go to next PRCandidate             
                    clashingRuleFound=true;
                }                    
            }
            if (clashingRuleFound == false)
            {
                //no other rule with different consequent covering at least one shared value was found
                //this rule can be removed
                LOGGER.fine("Removing rule:" + PRCandidate.toString());
                it.remove();
            }                          
        }
        LOGGER.info("Rules on finish:" + rules.size());
        LOGGER.info("FINISHED removeRedundantExtendedRules - range based");
        return rules;
    }
    private ExtendRule createNewDefaultRule(AttributeValue conval)
    {
        ArrayList<AttributeValue> conValues= new ArrayList();
        ArrayList<ValueOrigin> origin= new ArrayList();
        conValues.add(conval);
        origin.add(ValueOrigin.consequent);
        
        Consequent consequent = new Consequent(data.makeRuleItem(conValues, origin, conval.getAttribute(), ValueOrigin.fakeConsequent));
        ArrayList<RuleMultiItem> emptyAntecedent = new ArrayList<>();
        Rule r = new Rule(new Antecedent(emptyAntecedent), consequent, null, null, -2, -2, data);
        ExtendRule finalDefRule = new ExtendRule(r, new History(r.getRID()),ExtendType.defaultRule,null);
        return finalDefRule;
        
    }
    
    private AttributeValue getDefaultRuleClass()
    {
        AttributeValue max = Collections.max(data.getDataTable().getTargetAttribute().getAllValues(), Comparator.comparing(c -> c.getTransactions().size()));
        return max;
    }
    private int getDefaultRuleError(AttributeValue def)
    {
        int correct = def.getTransactions().size();
        return data.getDataTable().getAllCurrentTransactions().size() - correct;
    }
        
}
