/*
 * Copyright 2008-2011 Microarray Informatics Team, EMBL-European Bioinformatics Institute
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

package uk.ac.ebi.gxa.plot;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import ucar.ma2.InvalidRangeException;
import uk.ac.ebi.gxa.netcdf.reader.ExpressionStatistics;
import uk.ac.ebi.gxa.netcdf.reader.NetCDFDescriptor;
import uk.ac.ebi.gxa.netcdf.reader.NetCDFProxy;
import uk.ac.ebi.gxa.netcdf.reader.UpDownExpression;

import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;

import static com.google.common.io.Closeables.closeQuietly;

/**
 * Loads experiment chart specific data in order to convert into JSON. Used by charts on the experiment page.
 * Not appropriate for using in the backend services.
 *
 * @author Olga Melnichuk
 *         Date: 15/04/2011
 */
public class ExperimentPlot {

    private static final Comparator<String> FACTOR_VALUE_COMPARATOR = new Comparator<String>() {
        private final Pattern startsOrEndsWithDigits = java.util.regex.Pattern.compile("^\\d+|\\d+$");

        public int compare(String s1, String s2) {
            boolean isEmptyS1 = (s1.length() == 0);
            boolean isEmptyS2 = (s2.length() == 0);

            if (isEmptyS1 && isEmptyS2) {
                return 0;
            }

            if (isEmptyS1) {
                return 1;
            }

            if (isEmptyS2) {
                return -1;
            }

            java.util.regex.Matcher m1 = startsOrEndsWithDigits.matcher(s1);
            java.util.regex.Matcher m2 = startsOrEndsWithDigits.matcher(s2);

            if (m1.find() && m2.find()) {
                Long i1 = new Long(s1.substring(m1.start(), m1.end()));
                Long i2 = new Long(s2.substring(m2.start(), m2.end()));

                int compareRes = i1.compareTo(i2);
                return (compareRes == 0) ? s1.compareToIgnoreCase(s2) : compareRes;
            }

            return s1.compareToIgnoreCase(s2);
        }
    };

    private float[][] expressions;
    private List<List<BoxAndWhisker>> boxAndWhisker;

    private String[] efNames;
    private String[] efCuratedNames;
    private List<Set<String>> efvNames;
    private Map<Integer, Map<Integer, List<Integer>>> efEfvAssays;

    private int[] deIndices;

    ExperimentPlot() {
    }

    public String[] getEfNames() {
        return efNames;
    }

    public String[] getEfCuratedNames() {
        return efCuratedNames;
    }

    public Collection<? extends Collection<String>> getEfvNames() {
        return efvNames;
    }

    public float[][] getExpressions() {
        return expressions;
    }

    public int[] getDeIndices() {
        return deIndices;
    }

    public Collection<? extends Collection<BoxAndWhisker>> getBoxAndWhisker() {
        return boxAndWhisker;
    }

    public Map<Integer, Map<Integer, List<Integer>>> getEfEfvAssays() {
        return efEfvAssays;
    }

    public static ExperimentPlot create(int[] deIndices, NetCDFDescriptor proxyDescr, Function<String[], String[]> stringConverter) throws IOException, InvalidRangeException {
        NetCDFProxy proxy = null;

        try {
            proxy = proxyDescr.createProxy();
            ExperimentPlot plot = new ExperimentPlot();
            plot.load(deIndices, proxy, stringConverter);
            return plot;
        } finally {
            closeQuietly(proxy);
        }
    }

    private ExperimentPlot load(int[] deIndices, NetCDFProxy proxy, Function<String[], String[]> stringConverter) throws IOException, InvalidRangeException {

        this.deIndices = Arrays.copyOf(deIndices, deIndices.length);

        expressions = proxy.getExpressionValues(deIndices);

        efNames = proxy.getFactors();
        efCuratedNames = stringConverter.apply(proxy.getFactors());
        efvNames = Lists.newArrayList();
        efEfvAssays = Maps.newHashMap();

        String[][] factorValues = proxy.getFactorValues();

        for (int i = 0; i < efNames.length; i++) {
            String[] efvs = factorValues[i];

            Map<String, List<Integer>> efvMap = Maps.newTreeMap(FACTOR_VALUE_COMPARATOR);
            for (int j = 0; j < efvs.length; j++) {
                String efv = efvs[j];
                if ("(empty)".equals(efv)) {
                    continue;
                }
                List<Integer> assays = efvMap.get(efv);
                if (assays == null) {
                    assays = new ArrayList<Integer>();
                    efvMap.put(efv, assays);
                }
                assays.add(j);
            }

            efvNames.add(efvMap.keySet());

            Map<Integer, List<Integer>> efvAssays = efEfvAssays.get(i);
            if (efvAssays == null) {
                efvAssays = Maps.newHashMap();
                efEfvAssays.put(i, efvAssays);
            }

            int efvIndex = 0;
            for (List<Integer> assayIndices : efvMap.values()) {
                efvAssays.put(efvIndex++, assayIndices);
            }
        }

        prepareBoxAndWhiskerData(proxy.getExpressionStatistics(deIndices));
        return this;
    }

    private void prepareBoxAndWhiskerData(ExpressionStatistics statistics) {
        boxAndWhisker = Lists.newArrayList();
        for (int k = 0; k < deIndices.length; k++) {
            List<BoxAndWhisker> list = Lists.newArrayList();
            DoubleIndexIterator<String> efEfvIterator = new DoubleIndexIterator<String>(efvNames);
            while (efEfvIterator.hasNext()) {
                DoubleIndexIterator.Entry<String> efEfv = efEfvIterator.next();
                String ef = efNames[efEfv.getI()];
                String efv = efEfv.getEntry();

                UpDownExpression expr = statistics.getUpDownExpression(ef, efv, deIndices[k]);
                Collection<Integer> assayIndices = efEfvAssays.get(efEfv.getI()).get(efEfv.getJ());
                List<Float> data = Lists.newArrayList();
                for (Integer index : assayIndices) {
                    float v = expressions[k][index];
                    if (!Float.isNaN(v)) {
                        data.add(v);
                    }
                }
                list.add(new BoxAndWhisker(data, expr));
            }

            boxAndWhisker.add(list);
        }
    }

    public static class BoxAndWhisker {
        private final float median;
        private final float uq;
        private final float lq;
        private final float max;
        private final float min;
        private final boolean up;
        private final boolean down;

        public BoxAndWhisker(List<Float> data, UpDownExpression upDown) {
            Collections.sort(data);
            this.median = round(data.get(data.size() / 2));
            this.max = round(data.get(data.size() - 1));
            this.min = round(data.get(0));
            this.uq = round(data.get(data.size() * 3 / 4));
            this.lq = round(data.get(data.size() / 4));
            this.up = upDown.isUp();
            this.down = upDown.isDown();
        }

        private float round(float v) {
            return Math.round(v * 100) / 100.0f;
        }

        public float getMedian() {
            return median;
        }

        public float getUq() {
            return uq;
        }

        public float getLq() {
            return lq;
        }

        public float getMax() {
            return max;
        }

        public float getMin() {
            return min;
        }

        public boolean isUp() {
            return up;
        }

        public boolean isDown() {
            return down;
        }
    }
}
