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
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.gxa.utils.EscapeUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static uk.ac.ebi.gxa.exceptions.LogUtil.logUnexpected;

/**
 * Atlas basic model elements access class
 *
 * @author ostolop, mdylag, pashky
 */
public class ExperimentSolrDAO {
    private static final Logger log = LoggerFactory.getLogger(ExperimentSolrDAO.class);

    private SolrServer experimentSolr;

    public void setExperimentSolr(SolrServer experimentSolr) {
        this.experimentSolr = experimentSolr;
    }

    /**
     * Retrieve experiment by ID
     *
     * @param id experiment ID
     * @return experiment if found, null if not
     */
    public AtlasExperiment getExperimentById(long id) {
        return getExperimentByQuery("id:" + id);
    }

    /**
     * Returns an AtlasExperiment that contains all information from index.
     *
     * @param accessionId - an experiment accession/identifier.
     * @return an AtlasExperiment that contains all information from index.
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
            return AtlasExperiment.createExperiment(exptDoc);
        } catch (SolrServerException e) {
            throw logUnexpected("Error querying for experiment", e);
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
         * Returns list of experiments
         *
         * @return list of experiments
         */
        public List<AtlasExperiment> getExperiments() {
            return experiments;
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
    }

    public AtlasExperimentsResult getExperimentsByQuery(String query, int start, int rows) {
        return getExperimentsByQuery(query, start, rows, "accession", SolrQuery.ORDER.asc);
    }

    /**
     * Search experiments by SOLR query
     *
     * @param query     SOLR query string
     * @param start     starting position
     * @param rows      number of rows to fetch
     * @param accession
     * @param asc
     * @return experiments matching the query
     */
    public AtlasExperimentsResult getExperimentsByQuery(String query, int start, int rows, String accession, SolrQuery.ORDER asc) {
        SolrQuery q = new SolrQuery(query);
        q.setRows(rows);
        q.setStart(start);
        q.setFields("*");
        q.setSortField(accession, asc);

        try {
            QueryResponse queryResponse = experimentSolr.query(q);
            SolrDocumentList documentList = queryResponse.getResults();
            List<AtlasExperiment> result = new ArrayList<AtlasExperiment>();

            if (documentList != null)
                for (SolrDocument exptDoc : documentList)
                    result.add(AtlasExperiment.createExperiment(exptDoc));

            return new AtlasExperimentsResult(result, documentList == null ? 0 : (int) documentList.getNumFound(), start);
        } catch (SolrServerException e) {
            throw logUnexpected("Error querying for experiments", e);
        }
    }

    /**
     * List all experiments
     *
     * @param ids
     * @return list of all experiments with UP/DOWN expressions
     */
    public Collection<AtlasExperiment> getExperiments(Collection<Long> ids) {
        List<AtlasExperiment> result = new ArrayList<AtlasExperiment>();
        for (long id : ids) {
            AtlasExperiment atlasExp = getExperimentById(id);
            if (atlasExp != null)
                result.add(atlasExp);
            else
                throw logUnexpected("Failed to find experiment: " + id + " in Solr experiment index!");
        }
        return result;
    }
}
