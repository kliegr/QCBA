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
import eu.kliegr.ac1.rule.Consequent;
import eu.kliegr.ac1.rule.Prediction;
import eu.kliegr.ac1.rule.RuleQuality;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author tomas
 */
public class DistributionFactory {

    private final static Logger LOGGER = Logger.getLogger(DistributionFactory.class.getName());
    private ArrayList<Consequent> consequents;
    private Float[] equalWeights;

    private void init(Set<Consequent> cons) {
        //consequent order is used in convert()
        consequents = new ArrayList(cons);
        //init equal weight array used in convert()
        equalWeights = new Float[cons.size()];
        for (int i = 0; i < cons.size(); i++) {
            equalWeights[i] = new Float(1.0 / cons.size());
        }

    }

    /**
     *
     * @param finalDistr
     * @return
     */
    public Prediction getMax(Distribution finalDistr) {
        float max = Float.MIN_VALUE;
        int maxIndex = -1;
        float[] probs = finalDistr.getProbs();
        for (int i = 0; i < finalDistr.getProbs().length; i++) {
            if (probs[i] > max) {
                max = probs[i];
                maxIndex = i;
            }
        }

        if (max == Float.MIN_VALUE) {
            //TODO: This state is illegal and is likely a result of an earlier bug in code computing the distribution
            LOGGER.severe("Distribution has all probabilities set to zero. Picking class at random to avoid failure");
            maxIndex = ThreadLocalRandom.current().nextInt(0, consequents.size());
            //return null;
        }
        Prediction p = new Prediction(consequents.get(maxIndex), max);

        return p;
    }

    /**
     *
     * @param finalDistr
     * @param topn
     * @return
     */
    public Prediction[] getMax(Distribution finalDistr, int topn) {

        Prediction[] result;
        if (topn == 1) {
            result = new Prediction[1];
            result[0] = getMax(finalDistr);
        } else {
            float[] probs = finalDistr.getProbs();
            ArrayIndexComparator comparator = new ArrayIndexComparator(probs);
            Integer[] indexes = comparator.createIndexArray();
            Arrays.sort(indexes, comparator);

            int length;
            if (topn >= indexes.length) {
                length = indexes.length;
            } else {
                length = topn;
            }
            result = new Prediction[length];
            //the array is sorted in ascending order
            for (int i = 0; i < length; i++) {
                int index = indexes[probs.length - i - 1];
                Consequent cons = consequents.get(index);
                float trust = probs[index];
                result[i] = new Prediction(cons, trust);
            }

        }

        return result;
    }

    /**
     *
     * @param distributionsByAttribute
     * @return
     */
    public Distribution aggregateDistributions(HashMap<Attribute, ArrayList<Distribution>> distributionsByAttribute) {
        //aggregate histograms within attribute
        ArrayList<Distribution> ah_agg = new ArrayList();
        // cycle through attributes, aggregating all distributions
        distributionsByAttribute.entrySet().stream().map((distForAtt) -> aggregate(distForAtt.getValue())).forEach((aggDistForAtt) -> {
            ah_agg.add(aggDistForAtt);
        });
        //aggregate histograms across attributes
        Distribution finalDistr = aggregate(ah_agg);
        return finalDistr;
    }

    /**
     *
     * @param distribs
     * @return
     */
    public Distribution aggregate(ArrayList<Distribution> distribs) {
        if (distribs.isEmpty()) {
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.fine("Nothing to aggregate");
            }
            return null;
        }
        Float[] weightsNormalized;
        float weightSum = distribs.stream().map((d) -> d.getWeight()).reduce((val1, val2) -> val1 + val2).get();
        weightsNormalized = distribs.stream().map((d) -> d.getWeight()).map((weight) -> weight / weightSum).toArray(size -> new Float[size]);

        float[] distrib = new float[consequents.size()];
        for (int i = 0; i < consequents.size(); i++) {
            Consequent con = consequents.get(i);
            float totalconf = 0;
            for (int j = 0; j < distribs.size(); j++) {
                totalconf += distribs.get(j).getProbs()[i] * weightsNormalized[j];
            }
            distrib[i] = totalconf;
        }
        Distribution result = new Distribution(distrib);
        return result;

    }

    /*
    converts attribute value annotation to distribution
    edistributionnsuring that the distribution sums to 1
     */

    /**
     *
     * @param annot
     * @return
     */
    public Distribution convert(AttributeValueAnnotation annot) {
        if (consequents == null) {
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.fine("Distribution class init: caching consequents order and count");
            }

            init(annot.getDistribution().keySet());
        }

        float[] distrib = new float[consequents.size()];
        for (int i = 0; i < consequents.size(); i++) {
            Consequent con = consequents.get(i);
            RuleQuality rq = annot.getDistribution().get(con);
            if (rq == null) {
                LOGGER.log(Level.SEVERE, "No distribution associated with value:{0} and consequent{1}", new Object[]{annot.getValue(), con});
            }
            distrib[i] = rq.getConfidence();
        }
        Distribution result = new Distribution(distrib);
        return result;
    }
}
