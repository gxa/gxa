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

import uk.ac.ebi.gxa.netcdf.reader.NetCDFProxy;
import uk.ac.ebi.gxa.utils.EfvTree;
import uk.ac.ebi.microarray.atlas.model.UpDownExpression;
import uk.ac.ebi.gxa.exceptions.LogUtil;

import java.util.*;
import java.io.IOException;

/**
 * Lazy expression statistics class
 *
 * @author pashky
 */
public class ExpressionStats {
    private final NetCDFProxy proxy;
    private final EfvTree<Integer> efvTree = new EfvTree<Integer>();
    
    private EfvTree<Stat> lastData;
    long lastDesignElement = -1;
        
    ExpressionStats(NetCDFProxy proxy) throws IOException {
        this.proxy = proxy;

        final String[] factorsAndCharacteristics;
        {
            final String[] tmp = proxy.getFactorsAndCharacteristics();
            // Ensure backwards compatibility
            factorsAndCharacteristics = tmp.length != 0 ? tmp : proxy.getFactors();
        }
        final List<String> uvals = proxy.getUniqueValues();
        final int[] uvalIndexes = proxy.getUniqueValueIndexes();
        

        int index = 0;
        int k = 0;
        for (int propIndex = 0; propIndex < factorsAndCharacteristics.length && index < uvalIndexes.length; ++propIndex) {
            final String prop = ExperimentalData.normalized(factorsAndCharacteristics[propIndex], "ba_");
            int valNum = uvalIndexes[index];
            for (; valNum > 0 && k < uvals.size(); --valNum) {
                final String efv = uvals.get(k).replaceAll("^.*" + NetCDFProxy.NCDF_PROP_VAL_SEP_REGEX, "");
                efvTree.put(prop, efv, k++);
            }
        }
    }
        
    /**
     * Gets {@link uk.ac.ebi.gxa.utils.EfvTree} of expression statistics structures
     *
     * @param designElementId design element id
     * @return efv tree of stats
     */
    EfvTree<Stat> getExpressionStats(int designElementId) {
        if (lastData != null && designElementId == lastDesignElement)
            return lastData;
    
        try {
            final float[] pvals = proxy.getPValuesForDesignElement(designElementId);
            final float[] tstats = proxy.getTStatisticsForDesignElement(designElementId);
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
        } catch (IOException e) {
            throw LogUtil.createUnexpected("Exception during pvalue/tstat load", e);
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
