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

import eu.kliegr.ac1.data.Attribute;
import eu.kliegr.ac1.data.AttributeValue;
import eu.kliegr.ac1.data.Transaction;
import eu.kliegr.ac1.performance.StopWatches;
import eu.kliegr.ac1.rule.extend.Distribution;
import eu.kliegr.ac1.rule.extend.DistributionFactory;
import eu.kliegr.ac1.rule.extend.TestRuleAnnotation;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 *
 * @author tomas
 */
public class TestRules {

    private static final Logger LOGGER = Logger.getLogger(TestRules.class.getName());
    private ArrayList<TestRule> rules = new ArrayList();
    private ConcurrentSkipListMap<Transaction, Prediction[]> allCovered;
    private int totalCorrect;
    private int totalIncorrect;
    private int oneRuleClassifications;
    private int mixtureClassifications;
    private int uncovered;
    private final Comparator ruleComparator;
    private final Data rmi;

    /**
     *
     * @param rules
     * @param ruleComparator
     * @param rmi
     */
    public TestRules(ArrayList<Rule> rules, Comparator ruleComparator, Data rmi) {
        //this.type = type;
        this.ruleComparator = ruleComparator;
        this.rules = rules.stream().map((rule) -> new TestRule(rule)).collect(Collectors.toCollection(ArrayList::new));
        this.rmi = rmi;

    }

    /**
     *
     * @return
     */
    public ArrayList<TestRule> getRules() {
        return rules;
    }

    /**
     *
     */
    public void sortRules() {
        //TODO: possible optimalization skip sort id comparator is PreserveRuleOrderComparator
        rules.sort(ruleComparator);
    }

    /**
     *
     * @param type
     */
    public void classifyData(TestingType type) {
        LOGGER.log(Level.INFO, "Number of rules {0}", this.rules.size());
        switch (type) {
            case mixture:
                classifyDataMixture(1);
                break;
            case firstMatch:
                classifyDataFirstMatch();
                break;
            default:
                throw new UnsupportedOperationException("This testing type is not supported");
        }
        if (LOGGER.isLoggable(Level.FINEST)) {
            LOGGER.info("\n\n#internal TID, TID, prediction, trust (!!missing are skipped !!)\n");

            allCovered.entrySet().stream().forEach((t) -> {
                for (Prediction p : t.getValue()) {
                    LOGGER.log(Level.INFO, "{0},{1},{2},{3},{4}", new Object[]{t.getKey().getInternalTID(), t.getKey().getExternalTID(), t.toString(), p.consequent, p.trust});
                }
            });

        }
        LOGGER.log(Level.INFO, "\n\nTotal correct {0}", totalCorrect);
        LOGGER.log(Level.INFO, "Total incorrect {0}", totalIncorrect);

    }

    /**
     *
     */
    public void classifyDataFirstMatch() {

        allCovered = new ConcurrentSkipListMap();
        LOGGER.info("#Begin classify\n");
        totalCorrect = 0;
        totalIncorrect = 0;
        rules.stream().map((rule) -> {
            LOGGER.log(Level.INFO, "#{0}\n", rule);
            return rule;
        }).forEach((rule) -> {
            Set<Transaction> covered = rule.fireRuleAgainstData(true, rmi);
            LOGGER.log(Level.INFO, "test conf={0}\n", rule.testQuality.getConfidence());
            LOGGER.log(Level.INFO, "true positives: {0}, false positives: {1}\n", new Object[]{rule.testQuality.a, rule.testQuality.b});
            totalCorrect += rule.testQuality.a;
            totalIncorrect += rule.testQuality.b;
            /* store classified transaction */
            covered.stream().map((t) -> {
                t.setCoveringRule(rule);
                return t;
            }).forEach((t) -> {
                allCovered.put(t, new Prediction[]{new Prediction(rule.getConsequent(), rule.getConfidence())});
            });
        });

    }

    private HashMap<Transaction, ArrayList<TestRule>> createIndexTransRule() {
        HashMap<Transaction, ArrayList<TestRule>> transWmatchingRules = new HashMap();
        rules.stream().map((rule) -> {
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(Level.FINE, "#{0}\n", rule);
            }
            return rule;
        }).forEach((rule) -> {
            Set<Transaction> covered = rule.fireRuleAgainstData(false, rmi);
            /* build inverted index, where each transaction is associated with matching rules*/
            covered.stream().forEach((trans) -> {
                ArrayList<TestRule> exRules = transWmatchingRules.get(trans);
                if (exRules == null) {
                    exRules = new ArrayList();
                    transWmatchingRules.put(trans, exRules);
                }
                exRules.add(rule);
            });
        });
        return transWmatchingRules;
    }

    /* 
        distributions are associated with weight corresponding to rule confidence 
     */

    /**
     *
     * @param curRules
     * @param t
     * @param distrFactory
     * @return
     */
    public HashMap<Attribute, ArrayList<Distribution>> getDistributionsByAttribute(ArrayList<TestRule> curRules, Transaction t, DistributionFactory distrFactory) {
        HashMap<Attribute, ArrayList<Distribution>> distributionsByAttribute = new HashMap();
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.log(Level.FINE, "Rules to obtain distributions from: {0}", curRules.size());
        }
        for (TestRule r : curRules) {
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(Level.FINE, "Obtaining distributions from rule {0}", r);
                LOGGER.log(Level.FINE, "This rule has {0} multiitems. ", r.getAntecedent().getItems().size());
            }
            //cycle through attributes in the rule and collect distributions for each 
            for (RuleMultiItem rmi : r.getAntecedent().getItems()) {

                Attribute curAt = rmi.getAttribute();
                //for each attribute get value in the transaction
                AttributeValue tVal = t.getValue(curAt);
                //and histogam histogram
                //find values above and below the value and their distributions are averagedinterne se najdou hodnoty nad a pod danou hodnotou a jejich distribuce se zprumeruji
                Distribution distr = ((TestRuleAnnotation) r.getAnnotation()).getDistributionForValue(tVal, distrFactory);
                if (distr == null) {
                    if (LOGGER.isLoggable(Level.FINE)) {
                        LOGGER.log(Level.FINE, "No value with distribution found for {0}", tVal.toString(true, true));
                    }
                    continue;

                }
                //associate the weight
                //TODO: could be r.getSupport();
                //distr.setWeight(1.0f);
                distr.setWeight((float) r.getSupport());
                //get list of already existing Distributions for current attribute
                ArrayList<Distribution> allDistrForAtt = distributionsByAttribute.get(curAt);
                //get list of weights of existing annotations for current attribute

                //if this list is empty, set up a new list
                if (allDistrForAtt == null) {
                    //create empty lists
                    allDistrForAtt = new ArrayList();
                    //and associate them with the current attribute in the map
                    distributionsByAttribute.put(curAt, allDistrForAtt);
                }
                //now the list is sure to exist
                //associate the distribution with the attribute
                allDistrForAtt.add(distr);
            }
        }
        return distributionsByAttribute;
    }

    /**
     *
     * @param predictionsPerTransaction
     */
    public void classifyDataMixture(int predictionsPerTransaction) {

        oneRuleClassifications = 0;
        mixtureClassifications = 0;
        //build inverted index where each transaction is associated with matching rules
        HashMap<Transaction, ArrayList<TestRule>> transWmatchingRules = createIndexTransRule();

        allCovered = new ConcurrentSkipListMap();

        LOGGER.info("#Begin classify\n");

        totalCorrect = 0;
        totalIncorrect = 0;

        for (Entry<Transaction, ArrayList<TestRule>> entry : transWmatchingRules.entrySet()) {
            Transaction t = entry.getKey();
            ArrayList<TestRule> curRules = entry.getValue();
            Prediction[] prediction = null;
            Distribution finalDistr = null;
            DistributionFactory distrFactory = new DistributionFactory();

            if (curRules.size() > 1) {
                if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.log(Level.FINE, "There are {0} rules matching transaction {1}\n", new Object[]{curRules.size(), t});
                }
                //get a subset of the rules  for which none of the attributes in antecedent matches
                //the transaction in the fuzzy border region
                ArrayList<TestRule> nonFuzzyMatch = curRules.stream().filter((rule) -> !rule.isMatchesInFuzzyBorder(t)).collect(Collectors.toCollection(ArrayList::new));

                //if there are any, we will use them instead of the original rule set, which additionally contains fuzzy match rules
                if (nonFuzzyMatch.size() > 0 && nonFuzzyMatch.size() != curRules.size()) {
                    if (LOGGER.isLoggable(Level.FINE)) {
                        LOGGER.log(Level.FINE, "Not using rules with fuzzy matching regions: {0} rules remaining from {1}", new Object[]{nonFuzzyMatch.size(), curRules.size()});
                    }
                    curRules = nonFuzzyMatch;
                }

                //this repeated test is necessary, because due to the exclusion of fuzzy matching rules,
                //there may be only one rule left in curRules (if this is so, one rule classification should be used)
                if (curRules.size() > 1) {
                    HashMap<Attribute, ArrayList<Distribution>> distributionsByAttribute = getDistributionsByAttribute(curRules, t, distrFactory);
                    finalDistr = distrFactory.aggregateDistributions(distributionsByAttribute);
                    if (finalDistr == null) {
                        if (LOGGER.isLoggable(Level.FINE)) {
                            LOGGER.fine("No distribution found, defaulting to classification by most confident rule");
                        }
                    } else {
                        Prediction[] debug = distrFactory.getMax(finalDistr, 1);
                        prediction = distrFactory.getMax(finalDistr, predictionsPerTransaction);
                        if (prediction[0] == null) {
                            LOGGER.warning("NULL");
                            prediction = distrFactory.getMax(finalDistr, predictionsPerTransaction);
                        }

                        if (debug[0].consequent != prediction[0].consequent) {
                            LOGGER.warning("Unmatching best predictions");
                        }
                    }
                }

            }
            //if multi rule classification fails, default to one rule classification
            if (curRules.size() == 1 || prediction == null) {
                if (LOGGER.isLoggable(Level.FINE)) {
                    if (LOGGER.isLoggable(Level.FINE)) {
                        LOGGER.log(Level.FINE, "There is only one rule matching transaction {0}", t);
                    }
                }

                oneRuleClassifications++;
                TestRule r = curRules.get(0);

                if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.fine(r.toString());
                }
                t.setCoveringRule(r);

                /*TODO since transactions are not removed, testquality is incorrect !!!
                reason: it is computed from all transactions                
                 */
                if (predictionsPerTransaction > 1) {
                    HashMap<Attribute, ArrayList<Distribution>> distributionsByAttribute = getDistributionsByAttribute(curRules, t, distrFactory);
                    finalDistr = distrFactory.aggregateDistributions(distributionsByAttribute);
                    //finalDistr is null if rule has empty antecedent
                    if (finalDistr != null) {
                        prediction = distrFactory.getMax(finalDistr, predictionsPerTransaction);
                    } else {
                        throw new UnsupportedOperationException("prediction=distributionOfTargetAttributeInTrainingData");
                    }
                    if (prediction[0].consequent != r.getConsequent()) {
                        LOGGER.log(Level.WARNING, "WARNING, multiclass required: prediction outcome different from the mixture outcome. Using mixture outcome. Transaction:{0}", t);

                    }
                } else {
                    prediction = new Prediction[]{new Prediction(r.getConsequent(), r.getConfidence())};
                }

            }

            AttributeValue actual = t.getTarget();
            if (predictionsPerTransaction == 0) {

            }
            if (prediction[0].consequent.getItems().getAttributeValues().contains(actual)) {
                totalCorrect++;
            } else {
                totalIncorrect++;
            }
            allCovered.put(t, prediction);

        }
    }

    /**
     *
     * @return
     */
    public String[] getResult() {
        if (allCovered.isEmpty()) {
            return null;
        }
        int lastTransactionID = allCovered.firstEntry().getKey().getFirstTID();
        ArrayList<String> result = new ArrayList();
        for (Entry<Transaction, Prediction[]> t : allCovered.entrySet()) {
            Transaction curT = t.getKey();
            int curTID = curT.getInternalTID();
            int delta = curTID - lastTransactionID;
            //if the current transaction id is by more than 1 higher than the preceding one
            //the transactions in between were not classified.
            //this cycle outputs these transactions with empty classification
            for (int i = 1; i < delta; i++) {
                result.add(null);
            }

            TestRule coveringRule = t.getKey().getCoveringRule();

            for (Prediction p : t.getValue()) {
                result.add(p.consequent.toString(true, false));
            }

            lastTransactionID = curTID;
        }
        return result.toArray(new String[result.size()]);
    }

    /**
     *
     * @param path
     * @throws FileNotFoundException
     * @throws IOException
     */
    public void saveDebugResult(String path) throws FileNotFoundException, IOException {
        File dir = new File(new File(path).getParent());
        dir.mkdir();
        OutputStream output = new BufferedOutputStream(new FileOutputStream(path));

        Attribute IDattribute = rmi.getDataTable().getIDAttribute();
        String idname = "";
        if (IDattribute != null) {
            idname = IDattribute.getName();
        }

        output.write(("internal TID,prediction," + idname + ", covering rule, trust \n").getBytes());

        if (!allCovered.isEmpty()) {
            int lastTransactionID = allCovered.firstEntry().getKey().getFirstTID();
            for (Entry<Transaction, Prediction[]> t : allCovered.entrySet()) {
                Transaction curT = t.getKey();
                int curTID = curT.getInternalTID();
                int delta = curTID - lastTransactionID;
                //if the current transaction id is by more than 1 higher than the preceding one
                //the transactions in between were not classified.
                //this cycle outputs these transactions with empty classification
                for (int i = 1; i < delta; i++) {
                    output.write((lastTransactionID + i + ",,," + "\n").getBytes());
                }
                String ruleText;
                TestRule coveringRule = t.getKey().getCoveringRule();

                if (coveringRule == null) {
                    ruleText = "rule mixture";
                } else {
                    ruleText = String.join("", "\"", coveringRule.toString(), "\"");

                }
                for (Prediction p : t.getValue()) {
                    output.write((t.getKey().getInternalTID() + "," + p.consequent.toString(true, false) + "," + t.getKey().getExternalTID() + "," + ruleText + "," + p.trust + "\n").getBytes());

                }

                lastTransactionID = curTID;
            }
        }
        output.flush();
        output.close();
    }

    /**
     *
     * @param path
     * @param totalTransactions
     * @param watches
     * @throws FileNotFoundException
     * @throws IOException
     */
    public void saveSummary(String path, int totalTransactions, StopWatches watches) throws FileNotFoundException, IOException {
        File dir = new File(new File(path).getParent());
        dir.mkdir();

        OutputStream output = new BufferedOutputStream(new FileOutputStream(path));
        output.write(("Number of rules:" + rules.size()).getBytes());
        output.write("\n".getBytes());
        output.write(("Number of test instances:" + totalTransactions).getBytes());
        output.write("\n".getBytes());
        output.write(("True positives:" + totalCorrect).getBytes());
        output.write("\n".getBytes());
        output.write(("False positives:" + totalIncorrect).getBytes());
        output.write("\n".getBytes());
        output.write(("Uncovered:" + (totalTransactions - totalCorrect - totalIncorrect)).getBytes());
        output.write("\n".getBytes());
        output.write(("Accuracy (excl. unclassified):" + ((double) totalCorrect / (totalCorrect + totalIncorrect))).getBytes());
        output.write("\n".getBytes());
        output.write(("Accuracy:" + ((double) totalCorrect / totalTransactions)).getBytes());
        output.write("\n".getBytes());
        output.write(watches.toString().getBytes());
        output.flush();
        output.close();
    }

}
