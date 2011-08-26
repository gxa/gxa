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

package uk.ac.ebi.gxa.requesthandlers.api.v2;

import ae3.dao.ExperimentSolrDAO;
import ae3.service.experiment.AtlasExperimentQuery;
import uk.ac.ebi.microarray.atlas.model.Experiment;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

class ExperimentsQueryHandler implements QueryHandler {
    private final ExperimentSolrDAO experimentSolrDAO;

    ExperimentsQueryHandler(ExperimentSolrDAO experimentSolrDAO) {
        this.experimentSolrDAO = experimentSolrDAO;
    }

    private AtlasExperimentQuery toExperimentQuery(Map query, int start, int rows) {
        AtlasExperimentQuery.Builder qb = new AtlasExperimentQuery.Builder();
        qb.setStart(start);
        qb.setRows(rows);

        final Object o = query.get("hasFactor");
        if (o instanceof List) {
            final LinkedList<String> factors = new LinkedList<String>();
            for (Object element : (List)o) {
                if (element instanceof Map) {
                    final Object name = ((Map)element).get("name");
                    if (name instanceof String) {
                        factors.add((String)name);
                    }
                }
            }
            if (factors.size() > 0) {
                qb.withFactors(factors);
            }
        }
        return qb.toQuery();
    }

    private static class ExperimentDecorator {
        private final Experiment experiment;

        ExperimentDecorator(Experiment experiment) {
            this.experiment = experiment;
        }

        public String getAccession() {
            return experiment.getAccession();
        }
    }

    public Object getResponse(Map query) {
        final AtlasExperimentQuery expQuery = toExperimentQuery(query, 0, 200);
        if (!expQuery.isValid()) {
            return new Error("Invalid experiment query: " + expQuery);
        }
        final List<Experiment> experiments =
            experimentSolrDAO.getExperimentsByQuery(expQuery).getExperiments();
        final List<ExperimentDecorator> decorators = new ArrayList<ExperimentDecorator>(experiments.size());
        for (Experiment e : experiments) {
            decorators.add(new ExperimentDecorator(e));
        }
        return decorators;
    }
}
