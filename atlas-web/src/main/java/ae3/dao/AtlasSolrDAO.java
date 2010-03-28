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
import ae3.model.AtlasGene;
import org.apache.solr.common.params.FacetParams;
import uk.ac.ebi.gxa.utils.FilterIterator;
import uk.ac.ebi.gxa.utils.StringUtil;
import uk.ac.ebi.gxa.utils.EmptyIterator;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.gxa.index.GeneExpressionAnalyticsTable;
import uk.ac.ebi.gxa.utils.EscapeUtil;
import uk.ac.ebi.gxa.properties.AtlasProperties;
import uk.ac.ebi.microarray.atlas.model.ExpressionAnalysis;

import java.util.*;

/**
 * Atlas basic model elements access class
 *
 * @author ostolop, mdylag, pashky
 */
public class AtlasSolrDAO {
    final private Logger log = LoggerFactory.getLogger(getClass());
    final private int MAX_EXPERIMENTS = 10000;

    private SolrServer solrServerAtlas;
    private SolrServer solrServerExpt;
    private AtlasProperties atlasProperties;

    public void setSolrServerAtlas(SolrServer solrServerAtlas) {
        this.solrServerAtlas = solrServerAtlas;
    }

    public void setSolrServerExpt(SolrServer solrServerExpt) {
        this.solrServerExpt = solrServerExpt;
    }

    public void setAtlasProperties(AtlasProperties atlasProperties) {
        this.atlasProperties = atlasProperties;
    }

    /**
     * Retrieve experiment by ID
     *
     * @param experiment_id_key experiment ID
     * @return experiment if found, null if not
     */
    public AtlasExperiment getExperimentById(String experiment_id_key) {
        return getExperimentByQuery("id:" + EscapeUtil.escapeSolr(experiment_id_key));
    }

    /**
     * Retrieve experiment by ID
     *
     * @param experiment_id_key experiment ID
     * @return experiment if found, null if not
     */
    public AtlasExperiment getExperimentById(long experiment_id_key) {
        return getExperimentById(String.valueOf(experiment_id_key));
    }

    /**
     * Returns an AtlasExperiment that contains all information from index.
     *
     * @param accessionId - an experiment accession/identifier.
     * @return
     */
    public AtlasExperiment getExperimentByAccession(String accessionId) {
        return getExperimentByQuery("accession:" + EscapeUtil.escapeSolr(accessionId));
    }

    private AtlasExperiment getExperimentByQuery(String query) {
        SolrQuery q = new SolrQuery(query);
        q.setRows(1);
        q.setFields("*");
        try {
            QueryResponse queryResponse = solrServerExpt.query(q);
            SolrDocumentList documentList = queryResponse.getResults();

            if (documentList == null || documentList.size() < 1) {
                return null;
            }

            SolrDocument exptDoc = documentList.get(0);
            return new AtlasExperiment(exptDoc);
        }
        catch (SolrServerException e) {
            throw new RuntimeException("Error querying for experiment", e);
        }
    }

    public static class AtlasExperimentsResult {
        private List<AtlasExperiment> experiments;
        private int totalResults;
        private int startingFrom;

        private AtlasExperimentsResult(List<AtlasExperiment> experiments, int totalResults, int startingFrom) {
            this.experiments = experiments;
            this.totalResults = totalResults;
            this.startingFrom = startingFrom;
        }

        public List<AtlasExperiment> getExperiments() {
            return experiments;
        }

        public int getTotalResults() {
            return totalResults;
        }

        public int getStartingFrom() {
            return startingFrom;
        }

        public int getNumberOfResults() {
            return experiments.size();
        }
    }

    public AtlasExperimentsResult getExperimentsByQuery(String query, int start, int rows) {
        SolrQuery q = new SolrQuery(query);
        q.setRows(rows);
        q.setStart(start);
        q.setFields("*");

        try {
            QueryResponse queryResponse = solrServerExpt.query(q);
            SolrDocumentList documentList = queryResponse.getResults();
            List<AtlasExperiment> result = new ArrayList<AtlasExperiment>();

            if (documentList != null)
                for(SolrDocument exptDoc : documentList)
                    result.add(new AtlasExperiment(exptDoc));

            return new AtlasExperimentsResult(result, documentList == null ? 0 : (int)documentList.getNumFound(), start);
        } catch (SolrServerException e) {
            throw new RuntimeException("Error querying for experiments", e);
        }
    }

    public List<AtlasExperiment> getExperiments() {
        List<AtlasExperiment> result = new ArrayList<AtlasExperiment>();

        SolrQuery q = new SolrQuery("*:*");
        q.setRows(MAX_EXPERIMENTS);
        q.setFields("");
        q.addSortField("id", SolrQuery.ORDER.asc);

        try {

            QueryResponse queryResponse = solrServerExpt.query(q);
            SolrDocumentList documentList = queryResponse.getResults();

            if (documentList == null || documentList.size() < 1) {
                return result;
            }

            for (SolrDocument exptDoc : documentList) {
                SolrQuery q1 = new SolrQuery("exp_ud_ids:" + exptDoc.getFieldValue("id"));
                q1.setRows(1);
                q1.setFields("id");

                QueryResponse qr1 = solrServerAtlas.query(q1);

                AtlasExperiment ae = new AtlasExperiment(exptDoc);

                if (qr1.getResults().isEmpty()) {
                    ae.setDEGStatus(AtlasExperiment.DEGStatus.EMPTY);
                }

                result.add(ae);
            }
        }
        catch (SolrServerException e) {
            throw new RuntimeException("Error querying for experiment", e);
        }

        return result;
    }


    public AtlasGeneResult getGeneById(String gene_id_key) {
        return getGeneByQuery("id:" + EscapeUtil.escapeSolr(gene_id_key));
    }

    public static class AtlasGeneResult {
        private AtlasGene gene;
        private boolean multi;

        private AtlasGeneResult(AtlasGene gene, boolean multi) {
            this.gene = gene;
            this.multi = multi;
        }

        public AtlasGene getGene() {
            return gene;
        }

        public boolean isMulti() {
            return multi;
        }

        public boolean isFound() {
            return gene != null;
        }
    }

    private AtlasGeneResult getGeneByQuery(String query) {
        SolrQuery q = new SolrQuery(query);
        q.setRows(1);
        q.setFields("*");
        try {
            QueryResponse queryResponse = solrServerAtlas.query(q);
            SolrDocumentList documentList = queryResponse.getResults();

            if (documentList == null || documentList.size() == 0) {
                return new AtlasGeneResult(null, false);
            }

            return new AtlasGeneResult(new AtlasGene(atlasProperties, documentList.get(0)), documentList.getNumFound() > 1);
        }
        catch (SolrServerException e) {
            throw new RuntimeException("Error querying for gene " + query, e);
        }
    }

    public List<AtlasGene> getGenes() {
        List<AtlasGene> result = new ArrayList<AtlasGene>();

        SolrQuery q = new SolrQuery("*:*");
        q.setRows(1000);
        q.setFields("name,id,identifier");
        try {
            QueryResponse queryResponse = solrServerAtlas.query(q);
            SolrDocumentList documentList = queryResponse.getResults();

            for (SolrDocument d : documentList) {
                AtlasGene g = new AtlasGene(atlasProperties, d);
                result.add(g);
            }

            return result;

        }
        catch (SolrServerException e) {
            throw new RuntimeException("Error querying list of genes");
        }
    }

    /**
     * Returns the AtlasGene corresponding to the specified gene identifier, i.e. matching one of the terms in the
     * "gene_ids" field in Solr schema.
     *
     * @param gene_identifier
     * @return AtlasGene
     */
    public AtlasGeneResult getGeneByIdentifier(String gene_identifier) {
        final String id = EscapeUtil.escapeSolr(gene_identifier);
        return getGeneByQuery("id:" + id + " identifier:" + id);
    }

    public List<AtlasGene> getOrthoGenes(AtlasGene atlasGene) {
        List<AtlasGene> result = new ArrayList<AtlasGene>();
        for (String orth : atlasGene.getOrthologs()) {
            AtlasGeneResult orthoGene = getGeneByIdentifier(orth);
            if (orthoGene.isFound()) {
                result.add(orthoGene.getGene());
            }
            else {
                log.error("Could not find ortholog " + orth + " of " + atlasGene.getGeneIdentifier());
            }

            if (orthoGene.isMulti()) {
                log.error("Multiple genes found for ortholog " + orth + " of " + atlasGene.getGeneIdentifier());
            }
        }
        return result;
    }

    public List<AtlasExperiment> getRankedGeneExperiments(AtlasGene atlasGene, String ef, String efv, int minRows,
                                                          int maxRows) {
        List<AtlasExperiment> atlasExps = new ArrayList<AtlasExperiment>();

        GeneExpressionAnalyticsTable etable = atlasGene.getExpressionAnalyticsTable();
        Map<Long, Float> exps = new HashMap<Long, Float>();
        for (ExpressionAnalysis e : (ef != null && efv != null ? etable.findByEfEfv(ef, efv) : etable.getAll())) {
            if (exps.get(e.getExperimentID()) == null || exps.get(e.getExperimentID()) > e.getPValAdjusted()) {
                exps.put(e.getExperimentID(), e.getPValAdjusted());
            }
        }

        Object[] aexps = exps.entrySet().toArray();
        Arrays.sort(aexps, new Comparator<Object>() {
            public int compare(Object o1, Object o2) {
                @SuppressWarnings("unchecked")
                Map.Entry<Long, Float> e1 = (Map.Entry<Long, Float>) o1;
                @SuppressWarnings("unchecked")
                Map.Entry<Long, Float> e2 = (Map.Entry<Long, Float>) o2;
                return e1.getValue().compareTo(e2.getValue());
            }
        });

        //AZ: crashed at aexps.length=0
        for (int i = minRows > 0 ? minRows - 1 : 0;
             i < (maxRows > 0 ? (maxRows > aexps.length ? aexps.length : maxRows) : aexps.length); ++i) {
            @SuppressWarnings("unchecked")
            Long experimentId = ((Map.Entry<Long, Float>) aexps[i]).getKey();
            AtlasExperiment atlasExperiment = getExperimentById(experimentId);
            if (atlasExperiment != null) {
                atlasExperiment
                        .addHighestRankEF(atlasGene.getGeneId(), atlasGene.getHighestRankEF(experimentId).getFirst());
                atlasExps.add(atlasExperiment);
            }
        }
        return atlasExps;
    }

    public Iterable<String> getExperimentSpecies(long experimentId) {
        SolrQuery q = new SolrQuery("exp_ud_ids:" + experimentId);
        q.setRows(0);
        q.setFacet(true);
        q.setFacetSort(FacetParams.FACET_SORT_COUNT);
        q.setFacetMinCount(1);
        q.addFacetField("species");
        try {
            QueryResponse qr = solrServerAtlas.query(q);
            if(qr.getFacetFields() != null && qr.getFacetFields().get(0) != null && qr.getFacetFields().get(0).getValues() != null) {
                final Iterator<FacetField.Count> iterator = qr.getFacetFields().get(0).getValues().iterator();
                return new Iterable<String>() {
                    public Iterator<String> iterator() {
                        return new FilterIterator<FacetField.Count, String>(iterator) {
                            public String map(FacetField.Count c) {
                                if(c.getName() != null)
                                    return StringUtil.upcaseFirst(c.getName());
                                return null;
                            }
                        };
                    }
                };
            }
            return EmptyIterator.emptyIterable();
        } catch (SolrServerException e) {
            throw new RuntimeException("Error querying for experiment", e);
        }
    }
}
