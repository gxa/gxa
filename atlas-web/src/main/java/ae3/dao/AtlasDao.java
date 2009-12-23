package ae3.dao;

import ae3.model.AtlasExperiment;
import ae3.model.AtlasGene;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.core.CoreContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ae3.service.structuredquery.Constants;
import uk.ac.ebi.ae3.indexbuilder.Experiment;
import uk.ac.ebi.ae3.indexbuilder.ExperimentsTable;
import uk.ac.ebi.gxa.utils.EscapeUtil;

import java.util.*;

/**
 * Atlas basic model elements access class
 *
 * @author ostolop, mdylag, pashky
 */
public class AtlasDao {
    final private Logger log = LoggerFactory.getLogger(getClass());
    final private int MAX_EXPERIMENTS = 10000;

    private SolrServer solrAtlas;
    private SolrServer solrExpt;

    public AtlasDao(CoreContainer coreContainer) {
        this.solrAtlas = new EmbeddedSolrServer(coreContainer, Constants.CORE_ATLAS);
        this.solrExpt = new EmbeddedSolrServer(coreContainer, Constants.CORE_EXPT);
    }

    public AtlasDao(SolrServer solrAtlas, SolrServer solrExpt) {
        this.solrAtlas = solrAtlas;
        this.solrExpt = solrExpt;
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
            QueryResponse queryResponse = solrExpt.query(q);
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

    public List<AtlasExperiment> getExperimentsByQuery(String query, int start, int rows) {
        SolrQuery q = new SolrQuery(query);
        q.setRows(rows);
        q.setStart(start);
        q.setFields("*");

        try {
            QueryResponse queryResponse = solrExpt.query(q);
            SolrDocumentList documentList = queryResponse.getResults();
            List<AtlasExperiment> result = new ArrayList<AtlasExperiment>();

            if (documentList != null) {
                for (SolrDocument exptDoc : documentList) {
                    result.add(new AtlasExperiment(exptDoc));
                }
            }

            return result;
        }
        catch (SolrServerException e) {
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

            QueryResponse queryResponse = solrExpt.query(q);
            SolrDocumentList documentList = queryResponse.getResults();

            if (documentList == null || documentList.size() < 1) {
                return result;
            }

            for (SolrDocument exptDoc : documentList) {
                SolrQuery q1 = new SolrQuery("exp_ud_ids:" + exptDoc.getFieldValue("id"));
                q1.setRows(1);
                q1.setFields("id");

                QueryResponse qr1 = solrAtlas.query(q1);

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
            QueryResponse queryResponse = solrAtlas.query(q);
            SolrDocumentList documentList = queryResponse.getResults();

            if (documentList == null || documentList.size() == 0) {
                return new AtlasGeneResult(null, false);
            }

            return new AtlasGeneResult(new AtlasGene(documentList.get(0)), documentList.getNumFound() > 1);
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
            QueryResponse queryResponse = solrAtlas.query(q);
            SolrDocumentList documentList = queryResponse.getResults();

            for (SolrDocument d : documentList) {
                AtlasGene g = new AtlasGene(d);
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
        return getGeneByQuery(EscapeUtil.escapeSolr(gene_identifier));
    }

    public void retrieveOrthoGenes(AtlasGene atlasGene) {
        for (String orth : atlasGene.getOrthologs()) {
            AtlasGeneResult orthoGene = getGeneByIdentifier(orth);
            if (orthoGene.isFound()) {
                atlasGene.addOrthoGene(orthoGene.getGene());
            }
            else {
                log.error("Could not find ortholog " + orth + " of " + atlasGene.getGeneIdentifier());
            }

            if (orthoGene.isMulti()) {
                log.error("Multiple genes found for ortholog " + orth + " of " + atlasGene.getGeneIdentifier());
            }
        }
    }

    public List<AtlasExperiment> getRankedGeneExperiments(AtlasGene atlasGene, String ef, String efv, int minRows,
                                                          int maxRows) {
        List<AtlasExperiment> atlasExps = new ArrayList<AtlasExperiment>();

        ExperimentsTable etable = atlasGene.getExperimentsTable();
        Map<Long, Double> exps = new HashMap<Long, Double>();
        for (Experiment e : (ef != null && efv != null ? etable.findByEfEfv(ef, efv) : etable.getAll())) {
            if (exps.get(e.getId()) == null || exps.get(e.getId()) > e.getPvalue()) {
                exps.put(e.getId(), e.getPvalue());
            }
        }

        Object[] aexps = exps.entrySet().toArray();
        Arrays.sort(aexps, new Comparator<Object>() {
            public int compare(Object o1, Object o2) {
                @SuppressWarnings("unchecked")
                Map.Entry<Long, Double> e1 = (Map.Entry<Long, Double>) o1;
                @SuppressWarnings("unchecked")
                Map.Entry<Long, Double> e2 = (Map.Entry<Long, Double>) o2;
                return e1.getValue().compareTo(e2.getValue());
            }
        });

        //AZ: crashed at aexps.length=0
        for (int i = minRows > 0 ? minRows - 1 : 0;
             i < (maxRows > 0 ? (maxRows > aexps.length ? aexps.length : maxRows) : aexps.length); ++i) {
            @SuppressWarnings("unchecked")
            Long experimentId = ((Map.Entry<Long, Double>) aexps[i]).getKey();
            AtlasExperiment atlasExperiment = getExperimentById(experimentId);
            if (atlasExperiment != null) {
                atlasExperiment
                        .addHighestRankEF(atlasGene.getGeneId(), atlasGene.getHighestRankEF(experimentId).getFirst());
                atlasExps.add(atlasExperiment);
            }
        }
        return atlasExps;
    }

}
