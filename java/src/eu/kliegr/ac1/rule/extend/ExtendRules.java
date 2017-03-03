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

import eu.kliegr.ac1.data.AttributeValue;
import eu.kliegr.ac1.performance.StopWatches;
import eu.kliegr.ac1.rule.Consequent;
import eu.kliegr.ac1.rule.Data;
import eu.kliegr.ac1.rule.PruneRules;
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
import java.util.List;
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
     * @param data
     */
    public ExtendRules(ArrayList<Rule> rules, Comparator ruleComparator, ExtendType type, Data data) {
        //Create a Stream from the personList
        this.type = type;
        this.ruleComparator = ruleComparator;
        this.data = data;
        this.seedRules = rules.stream().map((rule) -> new ExtendRule(rule, null, type)).collect(Collectors.toCollection(() -> Collections.synchronizedList(new ArrayList<ExtendRule>())));
        LOGGER.log(Level.INFO, "Rules loaded: {0}", seedRules.size());
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
     * @param isPostPruningEnabled
     */
    public void extendRules(boolean isContinuousPruningEnabled, boolean isFuzzificationEnabled, boolean isPostPruningEnabled) {
        LOGGER.info("STARTED Extension phase\n");
        //TODO change to parallel stream
        //extendedRules = seedRules.stream().map((r)->r.extend()).collect(Collectors.toCollection(ArrayList::new));
        AtomicInteger processedRules = new AtomicInteger(0);

        int lastRuleRID = seedRules.get(seedRules.size() - 1).getRID();
        //cannot be parallel stream if pruning is performed in the same iteration
        extendedRules = seedRules.stream().map(rule -> {
            LOGGER.log(Level.INFO, "Rules already extended:{0} ; out of {1}", new Object[]{processedRules.addAndGet(1), seedRules.size()});
            int antLength = rule.getAntecedent().getItems().size();

            if (antLength == 0) {
                if (rule.getRID() == lastRuleRID) {
                    LOGGER.info("Leaving default rule unchanged");
                    return rule;
                } else {
                    LOGGER.severe("Unexpected rule with empty antecedent on other than last position, leaving out this rule");
                    return null;
                }
            } else if (rule.getRID() == lastRuleRID) {
                LOGGER.severe("Last rule is expected to be a default rule with empty antecedent");
            }
            if (data.getDataTable().getCurrentTransactionCount() == 0) {
                return null;
            }
            if (isContinuousPruningEnabled) {
                //we want the quality to be computed only on the subset of transactions not covered by rules so far
                rule.updateQuality();
            }
            ExtendRule ex = rule.extend();

            if (isContinuousPruningEnabled) {
                //and now remove the transactions covered by the extended rule
                int transRemoved = ex.removeSupportingTransactions(true);
                LOGGER.log(Level.INFO, "Removed {0} supporting transactions", transRemoved);
                if (transRemoved == 0) {
                    return null;
                }
            }
            if (isFuzzificationEnabled) {
                ex = ex.addFuzzyBorders();
                if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.fine("Finished creating version with fuzzy borders");
                    LOGGER.log(Level.FINE, "*Result:{0}", ex);
                }
            }

            return ex;
        }).filter(out -> out != null).collect(Collectors.toList());

        if (isContinuousPruningEnabled) {
            //rule metrics computed during continous pruning are computed on subset of data
            // and thus are not valid metrics for ordering the rule set in a test setting`
            data.getDataTable().unhideAllTransactions();
            extendedRules.stream().forEach((r) -> {
                r.updateQuality();
            });

        }

        LOGGER.info("FINISHED Extension phase\n");
        //TODO: this is unnecessary
        if (isPostPruningEnabled) {
            LOGGER.info("STARTED PRUNING phase\n");
            LOGGER.log(Level.INFO, "Rules before pruning:{0}", extendedRules.size());
            PruneRules pruneRulesObj = new PruneRules(extendedRules);

            //hideTransaction set to true, because we want to be able to unhide the transactions for possible follow-up annotation
            pruneRulesObj.pruneRules(true);
            data.getDataTable().unhideAllTransactions();

            LOGGER.log(Level.INFO, "Rules after pruning:{0}", extendedRules.size());
            LOGGER.info("FINISHED PRUNING phase\n");
            //}
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
}
