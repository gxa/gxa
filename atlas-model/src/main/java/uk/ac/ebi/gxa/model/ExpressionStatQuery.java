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

package  uk.ac.ebi.gxa.model;


import uk.ac.ebi.gxa.utils.Pair;

import java.util.List;
import java.util.ArrayList;


/**
 * Created by IntelliJ IDEA.
 * User: Andrey
 * Date: Oct 22, 2009
 * Time: 11:47:28 AM
 * To change this template use File | Settings | File Templates.
 */
public class ExpressionStatQuery {

    private List<GeneQuery> geneQueries = new ArrayList<GeneQuery>();
    private List<Pair<ExpressionQuery,PropertyQuery>> activityQueries = new ArrayList<Pair<ExpressionQuery, PropertyQuery>>();
    private boolean facets = false;

    public ExpressionStatQuery hasGene(GeneQuery geneQuery) {
        geneQueries.add(geneQuery);
        return this;
    }

    public ExpressionStatQuery activeIn(ExpressionQuery expression, PropertyQuery property) {
        activityQueries.add(new Pair<ExpressionQuery, PropertyQuery>(expression, property));
        return this;
    }

    public List<GeneQuery> getGeneQueries() {
        return geneQueries;
    }

    public List<Pair<ExpressionQuery, PropertyQuery>> getActivityQueries() {
        return activityQueries;
    }

    public boolean isFacets() {
        return facets;
    }

    public ExpressionStatQuery setFacets(boolean facets) {
        this.facets = facets;
        return this;
    }
}