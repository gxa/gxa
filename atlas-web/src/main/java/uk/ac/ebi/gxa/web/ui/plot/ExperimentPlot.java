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

package uk.ac.ebi.gxa.web.ui.plot;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.gxa.data.*;
import uk.ac.ebi.gxa.utils.FactorValueOrdering;
import uk.ac.ebi.gxa.utils.Pair;
import uk.ac.ebi.microarray.atlas.model.ArrayDesign;

import javax.annotation.Nullable;
import java.util.*;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newHashMap;
import static com.google.common.collect.Maps.newTreeMap;
import static com.google.common.primitives.Ints.asList;
import static java.util.Collections.unmodifiableList;

/**
 * Loads experiment chart specific data in order to convert it into JSON. Used by charts on the experiment page.
 * Not appropriate for using in the backend services.
 *
 * @author Olga Melnichuk
 */
public class ExperimentPlot {
    private static final Logger log = LoggerFactory.getLogger(ExperimentPlot.class);

    private static final Comparator<String> FACTOR_VALUE_COMPARATOR = new FactorValueOrdering();

    private FloatMatrixProxy expressions;
    private List<List<BoxAndWhisker>> boxAndWhisker = newArrayList();

    private List<EfName> efNames;
    private List<Set<String>> efvNames = newArrayList();
    private Map<Integer, Map<Integer, List<Integer>>> efEfvAssays = newHashMap();

    private int[] deIndices;

    public List<EfName> getEfNames() {
        return unmodifiableList(efNames);
    }

    public Collection<? extends Collection<String>> getEfvNames() {
        return efvNames;
    }

    public float[][] getExpressions() {
        return expressions.asMatrix();
    }

    public List<Integer> getDeIndices() {
        return unmodifiableList(asList(deIndices));
    }

    public Collection<? extends Collection<BoxAndWhisker>> getBoxAndWhisker() {
        return boxAndWhisker;
    }

    public Map<Integer, Map<Integer, List<Integer>>> getEfEfvAssays() {
        return efEfvAssays;
    }

    public static ExperimentPlot create(int[] deIndices, ExperimentWithData ewd, ArrayDesign ad, Function<String, String> stringConverter) throws AtlasDataException {
        ExperimentPlot plot = new ExperimentPlot();
        plot.load(deIndices, ewd, ad, stringConverter);
        return plot;
    }

    private void load(int[] deIndices, ExperimentWithData ewd, ArrayDesign ad, Function<String, String> stringConverter) throws AtlasDataException {
        this.deIndices = Arrays.copyOf(deIndices, deIndices.length);
        expressions = ewd.getExpressionValues(ad, deIndices);

        final String[] efs = ewd.getFactors(ad);
        efNames = createEfNames(efs, stringConverter);

        String[][] factorValues = ewd.getFactorValues(ad);

        // for all EFs
        for (int i = 0; i < efNames.size(); i++) {
            String[] efvs = factorValues[i];

            /* Note: Re-writing this with Guava multimaps introduces complications only.
             * E.g The usage of List interface is required here. The Collection forces us to
             * change efvAssays type to Collection also (in this case the idea of using
             * efEfvAssays.get(i).get(j) approach is fail) or to use an explicit cast or
             * copy Collection into a new ArrayList.
             */
            Map<String, List<Integer>> efvMap = newTreeMap();

            // for all non-empty EFVs
            for (int j = 0; j < efvs.length; j++) {
                String efv = efvs[j];
                if (isNullOrEmpty(efv) || "(empty)".equals(efv)) {
                    continue;
                }
                List<Integer> assays = efvMap.get(efv);
                if (assays == null) {
                    efvMap.put(efv, assays = newArrayList());
                }
                assays.add(j);
            }

            efvNames.add(efvMap.keySet());

            Map<Integer, List<Integer>> efvAssays = efEfvAssays.get(i);
            if (efvAssays == null) {
                efEfvAssays.put(i, efvAssays = newHashMap());
            }

            int efvIndex = 0;
            for (List<Integer> assayIndices : efvMap.values()) {
                efvAssays.put(efvIndex++, assayIndices);
            }
        }

        try {
            final Map<Integer, Map<Pair<String, String>, BoxAndWhisker>> baw = retrieveBoxAndWhiskersData(ewd, ad, deIndices);

            for (int de : deIndices) {
                List<BoxAndWhisker> bawForEF = newArrayList();
                for (int i = 0; i < efvNames.size(); i++) {
                    for (String efv : efvNames.get(i)) {
                        bawForEF.add(baw.get(de).get(Pair.create(efs[i], efv)));
                    }
                }
                boxAndWhisker.add(bawForEF);
            }
        } catch (StatisticsNotFoundException e) {
            log.warn("No statistics found for {}", ewd);
        }
    }

    private static Map<Integer, Map<Pair<String, String>, BoxAndWhisker>> retrieveBoxAndWhiskersData(
            ExperimentWithData ewd, ArrayDesign ad, int[] deIndices)
            throws AtlasDataException, StatisticsNotFoundException {
        Map<Integer, Map<Pair<String, String>, BoxAndWhisker>> baw = newHashMap();
        StatisticsCursor statistics = ewd.getStatistics(ad, deIndices);
        while (statistics.nextBioEntity()) {
            final Map<Pair<String, String>, BoxAndWhisker> deCharts = newHashMap();
            while (statistics.nextEFV()) {
                deCharts.put(statistics.getEfv(), new BoxAndWhisker(statistics));
            }
            baw.put(statistics.getDeIndex(), deCharts);
        }
        return baw;
    }

    private List<EfName> createEfNames(String[] factors, final Function<String, String> stringConverter) {
        return Lists.transform(Arrays.asList(factors), new Function<String, EfName>() {
            @Override
            public EfName apply(@Nullable String input) {
                return new EfName(input, stringConverter.apply(input));
            }
        });
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
