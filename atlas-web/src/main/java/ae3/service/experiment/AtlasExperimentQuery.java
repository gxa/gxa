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

package ae3.service.experiment;


import com.google.common.base.Supplier;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

/**
 * Atlas Experiment API query container class. Can be populated StringBuilder-style and converted to SOLR query string
 *
 * @author Pavel Kurnosov
 */
public class AtlasExperimentQuery {

    private int rows;
    private int start;
    private boolean all;

    private final List<String> experimentKeywords = new ArrayList<String>();
    private final List<String> factors = new ArrayList<String>();
    private final List<String> anyFactorValues = new ArrayList<String>();
    private final Multimap<String, String> factorValues = Multimaps.newListMultimap(
            new HashMap<String, Collection<String>>(),
            new Supplier<List<String>>() {
                @Override
                public List<String> get() {
                    return new ArrayList<String>();
                }
            }
    );

    private final List<String> geneIdentifiers = new ArrayList<String>();

    /**
     * @return <code>true</code> if the query should return list of all experiments
     */
    public boolean isListAll() {
        return all;
    }

    /**
     * @return collection of keywords to search experiments with
     */
    public Collection<String> getExperimentKeywords() {
        return Collections.unmodifiableCollection(experimentKeywords);
    }

    /**
     * @return collection of factors to search experiments with
     */
    public Collection<String> getFactors() {
        return Collections.unmodifiableCollection(factors);
    }

    /**
     * @return collection of gene identifiers to return with experiments
     */
    public Collection<String> getGeneIdentifiers() {
        return Collections.unmodifiableCollection(geneIdentifiers);
    }

    /**
     * @return factor -> values multimap to search experiments with
     */
    public Multimap<String, String> getFactorValues() {
        return Multimaps.unmodifiableMultimap(factorValues);
    }

    /**
     * @return collection of a factor values to search experiments with
     */
    public List<String> getAnyFactorValues() {
        return anyFactorValues;
    }

    /**
     * @return number of experiments to fetch
     */
    public int getRows() {
        return rows;
    }

    /**
     * @return start position for results
     */
    public int getStart() {
        return start;
    }

    public boolean hasExperimentKeywords() {
        return !experimentKeywords.isEmpty();
    }

    public boolean hasFactors() {
        return !factors.isEmpty();
    }

    public boolean hasAnyFactorValues() {
        return !anyFactorValues.isEmpty();
    }

    public boolean hasFactorValues() {
        return !factorValues.isEmpty();
    }

    /**
     * @return <code>true</code> if the query is valid
     */
    public boolean isValid() {
        return all ||
                hasExperimentKeywords() ||
                hasFactors() ||
                hasAnyFactorValues() ||
                hasFactorValues();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder()
                .append("AtlasExperimentQuery{")
                .append("rows=").append(rows)
                .append(", start=").append(start)
                .append(", all=").append(all)
                .append(", experimentKeywords=").append(Arrays.toString(experimentKeywords.toArray(new String[experimentKeywords.size()])))
                .append(", factors=").append(Arrays.toString(factors.toArray(new String[factors.size()])))
                .append(", anyFactorValues=").append(Arrays.toString(anyFactorValues.toArray(new String[anyFactorValues.size()])))
                .append(", geneIdentifiers=").append(Arrays.toString(geneIdentifiers.toArray(new String[geneIdentifiers.size()])))
                .append(", factorValues={");

        for (String key : factorValues.keySet()) {
            Collection<String> values = factorValues.get(key);
            sb.append(key).append(":").append(Arrays.toString(values.toArray(new String[values.size()]))).append(";");
        }

        return sb.append("}}").toString();
    }

    public static class Builder {
        private final AtlasExperimentQuery query = new AtlasExperimentQuery();

        public void setListAll() {
            query.all = true;
            query.rows = Integer.MAX_VALUE;
        }

        public void withExperimentKeywords(Collection<String> keywords) {
            query.experimentKeywords.addAll(keywords);
        }

        public void withFactors(Collection<String> factors) {
            query.factors.addAll(factors);
        }

        public void withGeneIdentifiers(Collection<String> genes) {
            query.geneIdentifiers.addAll(genes);
        }

        public void withFactorValues(String factor, Collection<String> values) {
            query.factorValues.putAll(factor, values);
        }

        public void withAnyFactorValues(Collection<String> values) {
            query.anyFactorValues.addAll(values);
        }

        public void setRows(int rows) {
            query.rows = rows;
        }

        public void setStart(int start) {
            query.start = start;
        }

        public AtlasExperimentQuery toQuery() {
            return query;
        }
    }
}
