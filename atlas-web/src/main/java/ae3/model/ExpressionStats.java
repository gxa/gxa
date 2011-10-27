/*
 * Copyright 2008-2010 Microarray Informatics Team, EMBL-European Bioinformatics Institute
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *
 * For further details of the Gene Expression Atlas project, including source code,
 * downloads and documentation, please see:
 *
 * http://gxa.github.com/gxa
 */

package ae3.model;

import uk.ac.ebi.gxa.exceptions.LogUtil;
import uk.ac.ebi.gxa.data.ExperimentWithData;
import uk.ac.ebi.gxa.data.KeyValuePair;
import uk.ac.ebi.gxa.data.AtlasDataException;
import uk.ac.ebi.gxa.utils.EfvTree;
import uk.ac.ebi.gxa.utils.EscapeUtil;
import uk.ac.ebi.microarray.atlas.model.UpDownExpression;
import uk.ac.ebi.microarray.atlas.model.ArrayDesign;

import java.util.List;

/**
 * Lazy expression statistics class
 *
 * @author pashky
 */
public class ExpressionStats {
    private final ExperimentWithData experiment;
    private final ArrayDesign arrayDesign;
    private final EfvTree<Integer> efvTree = new EfvTree<Integer>();

    private EfvTree<Stat> lastData;
    private long lastDesignElement = -1;

    ExpressionStats(ExperimentWithData experiment, ArrayDesign arrayDesign) throws AtlasDataException {
        this.experiment = experiment;
        this.arrayDesign = arrayDesign;

        int valueIndex = 0;
        for (KeyValuePair uefv : experiment.getUniqueEFVs(arrayDesign)) {
            efvTree.put(uefv.key, uefv.value, valueIndex);
            ++valueIndex;
        }
    }

    private static String normalized(String name, String prefix) {
        if (name.startsWith(prefix)) {
            name = name.substring(prefix.length());
        }
        return EscapeUtil.encode(name);
    }

    /**
     * Gets {@link uk.ac.ebi.gxa.utils.EfvTree} of expression statistics structures
     *
     * @param designElementId design element id
     * @return efv tree of stats
     */
    EfvTree<Stat> getExpressionStats(int designElementId) throws AtlasDataException {
        if (lastData != null && designElementId == lastDesignElement) {
            return lastData;
        }

        try {
            final float[] pvals = experiment.getPValuesForDesignElement(arrayDesign, designElementId);
            final float[] tstats = experiment.getTStatisticsForDesignElement(arrayDesign, designElementId);
            final EfvTree<Stat> result = new EfvTree<Stat>();
            for (EfvTree.EfEfv<Integer> efefv : efvTree.getNameSortedList()) {
                float pvalue = pvals[efefv.getPayload()];
                float tstat = tstats[efefv.getPayload()];
                if (tstat > 1e-8 || tstat < -1e-8) {
                    result.put(efefv.getEf(), efefv.getEfv(), new Stat(tstat, pvalue));
                }
            }
            lastDesignElement = designElementId;
            lastData = result;
            return result;
        } catch (ArrayIndexOutOfBoundsException e) {
            throw LogUtil.createUnexpected("Exception during pvalue/tstat load", e);
        }
    }

    /**
     * Expression statistics for ef/efv pair for one design element
     */
    public static class Stat implements Comparable<Stat> {
        private final float pvalue;
        private final float tstat;

        /**
         * Constructor
         *
         * @param tstat  t-statistics
         * @param pvalue p-value
         */
        public Stat(float tstat, float pvalue) {
            this.pvalue = pvalue;
            this.tstat = tstat;
        }

        /**
         * Gets p-value
         *
         * @return p-value
         */
        public float getPvalue() {
            return pvalue;
        }

        /**
         * Gets t-statistics
         *
         * @return t-statistics value
         */
        public float getTstat() {
            return tstat;
        }

        /**
         * Returns whether gene is over-expressed or under-expressed
         *
         * @return gene expression
         */
        public UpDownExpression getExpression() {
            return UpDownExpression.valueOf(pvalue, tstat);
        }

        /**
         * Useful, as {@link uk.ac.ebi.gxa.utils.EfvTree} can return elements sorted by value.
         * P-value of statistics, in this case.
         *
         * @param o other object
         * @return 1, 0 or -1
         */
        public int compareTo(Stat o) {
            return Float.valueOf(getPvalue()).compareTo(o.getPvalue());
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof Stat && compareTo((Stat) obj) == 0;
        }

        @Override
        public int hashCode() {
            return pvalue != +0.0f ? Float.floatToIntBits(pvalue) : 0;
        }
    }
}
