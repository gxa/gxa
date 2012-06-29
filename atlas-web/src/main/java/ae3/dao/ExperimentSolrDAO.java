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

package ae3.dao;

import ae3.model.AtlasExperiment;
import ae3.service.experiment.AtlasExperimentQuery;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.Multimap;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.gxa.dao.ExperimentDAO;
import uk.ac.ebi.gxa.utils.EscapeUtil;
import uk.ac.ebi.microarray.atlas.model.Experiment;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

import static com.google.common.collect.Lists.transform;
import static uk.ac.ebi.gxa.exceptions.LogUtil.createUnexpected;
import static uk.ac.ebi.gxa.utils.EscapeUtil.escapeSolrValueList;

/**
 * Atlas basic model elements access class
 *
 * @author ostolop, mdylag, pashky
 */
public class ExperimentSolrDAO {
    private static final Logger log = LoggerFactory.getLogger(ExperimentSolrDAO.class);

    private SolrServer experimentSolr;
    private ExperimentDAO experimentDAO;

    public void setExperimentSolr(SolrServer experimentSolr) {
        this.experimentSolr = experimentSolr;
    }

    public void setExperimentDAO(ExperimentDAO experimentDAO) {
        this.experimentDAO = experimentDAO;
    }

    /**
     * Returns an Experiment that contains all information from index.
     *
     * @param accessionId - an experiment accession/identifier.
     * @return an Experiment that contains all information from index.
     */
    public AtlasExperiment getExperimentByAccession(String accessionId) {
        return getExperimentByQuery("accession:" + EscapeUtil.escapeSolr(accessionId));
    }

    private AtlasExperiment getExperimentByQuery(String query) {
        SolrQuery q = new SolrQuery(query);
        q.setRows(1);
        q.setFields("*");
        try {
            QueryResponse queryResponse = experimentSolr.query(q);
            SolrDocumentList documentList = queryResponse.getResults();

            if (documentList == null || documentList.size() < 1) {
                return null;
            }

            SolrDocument exptDoc = documentList.get(0);
            return AtlasExperiment.createExperiment(experimentDAO, exptDoc);
        } catch (SolrServerException e) {
            throw createUnexpected("Error querying for experiment", e);
        }
    }

    /**
     * Experiment search results class
     */
    public static class AtlasExperimentsResult {
        private List<AtlasExperiment> experiments;
        private int totalResults;
        private int startingFrom;

        /**
         * Constructor
         *
         * @param experiments  list of experiments
         * @param totalResults total number of results
         * @param startingFrom start position of the list returned in full list of found results
         */
        private AtlasExperimentsResult(List<AtlasExperiment> experiments, int totalResults, int startingFrom) {
            this.experiments = experiments;
            this.totalResults = totalResults;
            this.startingFrom = startingFrom;
        }

        /**
         * Returns total number of found results
         *
         * @return total number of found results
         */
        public int getTotalResults() {
            return totalResults;
        }

        /**
         * Returns starting position of the list
         *
         * @return starting position of the list
         */
        public int getStartingFrom() {
            return startingFrom;
        }

        /**
         * Returns number of results in returned list
         *
         * @return number of results in returned list
         */
        public int getNumberOfResults() {
            return experiments.size();
        }

        public List<AtlasExperiment> getAtlasExperiments() {
            return experiments;
        }

        public List<Experiment> getExperiments() {
            return transform(experiments,
                    new Function<AtlasExperiment, Experiment>() {
                        @Override
                        public Experiment apply(@Nonnull AtlasExperiment experiment) {
                            return experiment.getExperiment();
                        }
                    });
        }
    }

    /**
     * Search experiments by SOLR query
     *
     * It seems this method is used only by API
     *
     * @param query SOLR query string
     * @return experiments matching the query
     */
    public AtlasExperimentsResult getExperimentsByQuery(AtlasExperimentQuery query) {
        return getExperimentsByQuery(toSolrQuery(query, "accession", SolrQuery.ORDER.asc));
    }

    public AtlasExperimentsResult getExperimentsByQuery(String query, int start, int rows, String sort, SolrQuery.ORDER order) throws DAOException{
        return getExperimentsByQuery(toSolrQuery(query, start, rows, sort, order));
    }

    private AtlasExperimentsResult getExperimentsByQuery(SolrQuery query) {
        try {
            QueryResponse queryResponse = experimentSolr.query(query);
            SolrDocumentList documentList = queryResponse.getResults();
            List<AtlasExperiment> result = new ArrayList<AtlasExperiment>();

            if (documentList != null) {
                for (SolrDocument exptDoc : documentList) {
                    result.add(AtlasExperiment.createExperiment(experimentDAO, exptDoc));
                }
            }

            return new AtlasExperimentsResult(result, documentList == null ? 0 : (int) documentList.getNumFound(), query.getStart());
        } catch (SolrServerException e) {
            //ToDo: try to find a way to validate query.
            log.error("Error querying for experiments", e);
            throw new DAOException("Error querying for experiments", e);
        }
    }

    static SolrQuery toSolrQuery(AtlasExperimentQuery query, String sort, SolrQuery.ORDER order) {
        final List<String> parts = new ArrayList<String>();

        if (query.isListAll()) {
            parts.add("*:*");
        } else {
            if (query.hasExperimentKeywords()) {
                String vals = escapeSolrValueList(query.getExperimentKeywords());
                parts.add(new StringBuilder()
                        .append("(")
                        .append(" accession:(").append(vals).append(") ")
                        .append(" id:(").append(vals).append(") ")
                        .append(" ").append(vals)
                        .append(" )").toString());
            }

            if (query.hasFactors()) {
                parts.add(new StringBuilder()
                        .append("a_properties:(").append(escapeSolrValueList(query.getFactors())).append(")").toString());
            }

            if (query.hasAnyFactorValues()) {
                parts.add(new StringBuilder()
                        .append("a_allvalues:(").append(escapeSolrValueList(query.getAnyFactorValues())).append(")").toString());
            } else if (query.hasFactorValues()) {
                Multimap<String, String> factorValues = query.getFactorValues();
                StringBuilder sb = new StringBuilder();
                for (String factor : factorValues.keySet()) {
                    sb.append("a_property_").append(factor).append(":(").append(escapeSolrValueList(factorValues.get(factor))).append(")");
                }
                parts.add(sb.toString());
            }
        }
        return toSolrQuery(Joiner.on(" AND ").join(parts), query.getStart(), query.getRows(), sort, order);
    }

    static SolrQuery toSolrQuery(String query, int start, int rows, String sort, SolrQuery.ORDER order) {

        SolrQuery q = new SolrQuery();

        q.setQuery(appendAccession(query));
        q.setRows(rows);
        q.setStart(start);
        q.setFields("*");
        q.setSortField(sort, order);
        return q;
    }

    static String appendAccession(String query) {
        Pattern pattern = Pattern.compile("[eE]-\\w{4}-\\d+");
        List<String> accessions = new LinkedList<String>();
        final Iterable<String> items = Splitter.on(" ").split(query);
        for (String item : items) {
            if(pattern.matcher(item).matches()) {
                accessions.add(item);
            }
        }
        for (String accession : accessions) {
            query = query.concat(" accession:" + accession);
        }
        
        return query;
    }
}
