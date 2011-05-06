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
import uk.ac.ebi.microarray.atlas.model.UpDownExpression;
import uk.ac.ebi.gxa.utils.DoubleIndexIterator;
import uk.ac.ebi.gxa.utils.FactorValueComparator;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.*;

import static com.google.common.io.Closeables.closeQuietly;

/**
 * Loads experiment chart specific data in order to convert it into JSON. Used by charts on the experiment page.
 * Not appropriate for using in the backend services.
 *
 * @author Olga Melnichuk
 */
public class ExperimentPlot {

    private static final Comparator<String> FACTOR_VALUE_COMPARATOR = new FactorValueComparator();

    private float[][] expressions;
    private List<List<BoxAndWhisker>> boxAndWhisker;

    private List<EfName> efNames;
    private List<Set<String>> efvNames;
    private Map<Integer, Map<Integer, List<Integer>>> efEfvAssays;

    private int[] deIndices;

    ExperimentPlot() {
    }

    public Collection<EfName> getEfNames() {
        return Collections.unmodifiableCollection(efNames);
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

    public static ExperimentPlot create(int[] deIndices, NetCDFDescriptor proxyDescr, Function<String, String> stringConverter) throws IOException, InvalidRangeException {
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

    private ExperimentPlot load(int[] deIndices, NetCDFProxy proxy, Function<String, String> stringConverter) throws IOException, InvalidRangeException {

        this.deIndices = Arrays.copyOf(deIndices, deIndices.length);

        expressions = proxy.getExpressionValues(deIndices);

        efNames = createEfNames(proxy.getFactors(), stringConverter);
        efvNames = Lists.newArrayList();
        efEfvAssays = Maps.newHashMap();

        String[][] factorValues = proxy.getFactorValues();

        for (int i = 0; i < efNames.size(); i++) {
            String[] efvs = factorValues[i];

            /* Note: Re-writing this with Guava multimaps introduces complications only.
             * E.g The usage of List interface is required here. The Collection forces us to
             * change efvAssays type to Collection also (in this case the idea of using
             * efEfvAssays.get(i).get(j) approach is fail) or to use an explicit cast or
             * copy Collection into a new ArrayList.
             */
            Map<String, List<Integer>> efvMap = Maps.newTreeMap(FACTOR_VALUE_COMPARATOR);
            for (int j = 0; j < efvs.length; j++) {
                String efv = efvs[j];
                if ("(empty)".equals(efv)) {
                    continue;
                }
                List<Integer> assays = efvMap.get(efv);
                if (assays == null) {
                    assays = Lists.newArrayList();
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

    private List<EfName> createEfNames(String[] factors, final Function<String, String> stringConverter) {
        return Lists.transform(Arrays.asList(factors), new Function<String, EfName>() {
            @Override
            public EfName apply(@Nullable String input) {
                return new EfName(input, stringConverter.apply(input));
            }
        });
    }

    private void prepareBoxAndWhiskerData(ExpressionStatistics statistics) {
        boxAndWhisker = Lists.newArrayList();
        for (int k = 0; k < deIndices.length; k++) {
            List<BoxAndWhisker> list = Lists.newArrayList();
            DoubleIndexIterator<String> efEfvIterator = new DoubleIndexIterator<String>(efvNames);
            while (efEfvIterator.hasNext()) {
                DoubleIndexIterator.Entry<String> efEfv = efEfvIterator.next();
                String ef = efNames.get(efEfv.getI()).getName();
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

                if (!data.isEmpty()) {
                    list.add(new BoxAndWhisker(data, expr));
                }
            }

            boxAndWhisker.add(list);
        }
    }

    private static class EfName {
        private final String name;
        private final String curatedName;

        private EfName(String name, String curatedName) {
            this.name = name;
            this.curatedName = curatedName;
        }

        public String getName() {
            return name;
        }

        public String getCuratedName() {
            return curatedName;
        }
    }
}
