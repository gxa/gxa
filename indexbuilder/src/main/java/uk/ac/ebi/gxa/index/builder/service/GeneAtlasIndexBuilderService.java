package uk.ac.ebi.gxa.index.builder.service;

import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.common.SolrInputDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.ae3.indexbuilder.Efo;
import uk.ac.ebi.ae3.indexbuilder.ExperimentsTable;
import uk.ac.ebi.gxa.index.builder.IndexBuilderException;
import uk.ac.ebi.gxa.utils.EscapeUtil;
import uk.ac.ebi.microarray.atlas.dao.AtlasDAO;
import uk.ac.ebi.microarray.atlas.dao.LoadStage;
import uk.ac.ebi.microarray.atlas.dao.LoadStatus;
import uk.ac.ebi.microarray.atlas.model.ExpressionAnalysis;
import uk.ac.ebi.microarray.atlas.model.Gene;
import uk.ac.ebi.microarray.atlas.model.OntologyMapping;
import uk.ac.ebi.microarray.atlas.model.Property;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;

/**
 * An {@link IndexBuilderService} that generates index documents from the genes in the Atlas database, and enriches the
 * data with expression values, links to EFO and other useful measurements.
 * <p/>
 * This is a heavily modified version of an original class first adapted to Atlas purposes by Pavel Kurnosov.
 * <p/>
 * Note that this implementation does NOT support updates - regardless of whether the update flag is set to true, this
 * will rebuild the index every time.
 *
 * @author Tony Burdett
 * @date 22-Sep-2009
 */
public class GeneAtlasIndexBuilderService extends IndexBuilderService {
    protected final Logger log = LoggerFactory.getLogger(getClass());

    private static final int NUM_THREADS = 64;

    private Map<String, List<String>> ontomap =
            new HashMap<String, List<String>>();
    private Efo efo;

    public GeneAtlasIndexBuilderService(AtlasDAO atlasDAO, SolrServer solrServer) {
        super(atlasDAO, solrServer);

        // get an Efo instance that we can use to calculate class hierarchy
        efo = Efo.getEfo();
    }

    protected void createIndexDocs(boolean pendingOnly) throws IndexBuilderException {
        // do initial setup - load efo mappings and build executor service
        loadEfoMapping();
        ExecutorService tpool = Executors.newFixedThreadPool(NUM_THREADS);

        // fetch genes
        List<Gene> genes = pendingOnly
                ? getAtlasDAO().getAllPendingGenes()
                : getAtlasDAO().getAllGenes();

        // the list of futures - we need these so we can block until completion
        List<Future<UpdateResponse>> tasks =
                new ArrayList<Future<UpdateResponse>>();

        try {
            // index all genes in parallel
            for (final Gene gene : genes) {
                // for each gene, submit a new task to the executor
                tasks.add(tpool.submit(new Callable<UpdateResponse>() {

                    public UpdateResponse call() throws IOException, SolrServerException,
                            IndexBuilderException {
                        UpdateResponse response = null;
                        try {
                            // update loadmonitor - gene is indexing
                            getLog().debug("Updating load_monitor table: status = working");
                            getAtlasDAO().writeLoadDetails(
                                    gene.getIdentifier(), LoadStage.SEARCHINDEX, LoadStatus.WORKING);
                            getLog().debug("Load_monitor table updated");

                            // get the properties for all these genes
                            getLog().debug("Fetching properties for " + gene.getIdentifier());
                            getAtlasDAO().getPropertiesForGenes(Collections.singletonList(gene));
                            getLog().debug("Acquired genes for " + gene.getIdentifier());

                            getLog().debug("Updating index - adding gene " + gene.getIdentifier());

                            // create a new solr document for this gene
                            SolrInputDocument solrInputDoc = new SolrInputDocument();
                            getLog().debug("Updating index with properties for " + gene.getIdentifier());
                            // add the gene id field
                            solrInputDoc.addField("gene_id", gene.getGeneID());
                            for (Property prop : gene.getProperties()) {
                                // update with gene properties
                                String p = "gene_" +
                                        prop.getName().toLowerCase(); // fixme: hack to support known index format
                                String pv = prop.getValue();

                                getLog().trace("Updating index, gene property " + p + " = " + pv);
                                solrInputDoc.addField(p, pv);
                            }
                            getLog().debug("Properties for " + gene.getIdentifier() + " updated");

                            // add EFO counts for this gene
                            if (addEfoCounts(solrInputDoc, gene.getDesignElementIDs())) {
                                getLog().debug("Updated solr document with EFO counts");
                                // finally, add the document to the index
                                getLog().info("Finalising changes for " + gene.getIdentifier());
                                response = getSolrServer().add(solrInputDoc);

                                // update loadmonitor table - experiment has completed indexing
                                getAtlasDAO().writeLoadDetails(
                                        gene.getIdentifier(), LoadStage.SEARCHINDEX, LoadStatus.DONE);

                                return response;
                            }
                            else {
                                getLog().warn("Failed to update solr document with counts from EFO.  " +
                                        "This is caused by an absence of analytics for " + gene.getIdentifier() + ". ");
                                return null;
                            }
                        }
                        finally {
                            // if the response was set, everything completed as expected, but if it's null we got
                            // an uncaught exception, so make sure we update loadmonitor to reflect that this failed
                            if (response == null) {
                                getAtlasDAO().writeLoadDetails(
                                        gene.getIdentifier(), LoadStage.SEARCHINDEX, LoadStatus.FAILED);
                            }
                        }
                    }
                }));
            }

            // block until completion, and throw any errors
            for (Future<UpdateResponse> task : tasks) {
                try {
                    task.get();
                }
                catch (ExecutionException e) {
                    if (e.getCause() instanceof IndexBuilderException) {
                        throw (IndexBuilderException) e.getCause();
                    }
                    else {
                        throw new IndexBuilderException("An error occurred updating Atlas SOLR index", e);
                    }
                }
                catch (InterruptedException e) {
                    throw new IndexBuilderException("An error occurred updating Atlas SOLR index", e);
                }
            }
        }
        finally {
            // shutdown the service
            getLog().debug("Gene index building tasks finished, cleaning up resources and exiting");
            tpool.shutdown();
        }
    }

    private void calcChildren(String currentId, Map<String, UpDnSet> efoupdn) {
        UpDnSet current = efoupdn.get(currentId);
        if (current == null) {
            current = new UpDnSet();
            efoupdn.put(currentId, current);
        }
        else if (current.processed) {
            return;
        }

        for (Efo.Term child : efo.getTermChildren(currentId)) {
            calcChildren(child.getId(), efoupdn);
            current.addChild(efoupdn.get(child.getId()));
        }

        current.processed = true;
    }

    private <T> Set<T> union(Set<T> a, Set<T> b) {
        Set<T> x = new HashSet<T>();
        if (a != null) {
            x.addAll(a);
        }
        if (b != null) {
            x.addAll(b);
        }
        return x;
    }

    private boolean addEfoCounts(SolrInputDocument solrDoc, Set<Integer> designElementIDs) {
        Map<String, UpDnSet> efoupdn = new HashMap<String, UpDnSet>();
        Map<String, UpDn> efvupdn = new HashMap<String, UpDn>();
        Set<Long> upexp = new HashSet<Long>();
        Set<Long> dnexp = new HashSet<Long>();
        Map<String, Set<String>> upefv = new HashMap<String, Set<String>>();
        Map<String, Set<String>> dnefv = new HashMap<String, Set<String>>();

        ExperimentsTable expTable = new ExperimentsTable();

        boolean wasresult = false;
        for (int designElementID : designElementIDs) {
            getLog().debug("Fetching expression analytics for design element: " + designElementID);
            List<ExpressionAnalysis> expressionAnalytics =
                    getAtlasDAO().getExpressionAnalyticsByDesignElementID(designElementID);
            getLog().debug(
                    "Acquired " + expressionAnalytics.size() + " analytics for design element: " + designElementID);

            for (ExpressionAnalysis expressionAnalytic : expressionAnalytics) {
                Long experimentId = (long) expressionAnalytic.getExperimentID();
                if (experimentId == 0) {
                    log.error("Found experimentId=0 for design element " + designElementID);
                    continue;
                }

                wasresult = true;

                boolean isUp = expressionAnalytic.getTStatistic() > 0;
                double pval = expressionAnalytic.getPValAdjusted();
                final String ef = expressionAnalytic.getEfName();
                final String efv = expressionAnalytic.getEfvName();

                List<String> accessions =
                        ontomap.get(experimentId + "_" + ef + "_" + efv);

                String efvid = EscapeUtil.encode(ef, efv);
                if (!efvupdn.containsKey(efvid)) {
                    efvupdn.put(efvid, new UpDn());
                }
                if (isUp) {
                    efvupdn.get(efvid).cup += 1;
                    efvupdn.get(efvid).pup = Math.min(efvupdn.get(efvid).pup, pval);
                    if (!upefv.containsKey(ef)) {
                        upefv.put(ef, new HashSet<String>());
                    }
                    upefv.get(ef).add(efv);
                }
                else {
                    efvupdn.get(efvid).cdn += 1;
                    efvupdn.get(efvid).pdn = Math.min(efvupdn.get(efvid).pdn, pval);
                    if (!dnefv.containsKey(ef)) {
                        dnefv.put(ef, new HashSet<String>());
                    }
                    dnefv.get(ef).add(efv);
                }

                if (accessions != null) {
                    for (String acc : accessions) {
                        String accId = EscapeUtil.encode(acc);

                        if (!efoupdn.containsKey(accId)) {
                            efoupdn.put(accId, new UpDnSet());
                        }
                        if (isUp) {
                            efoupdn.get(accId).up.add(experimentId);
                            efoupdn.get(accId).minpvalUp =
                                    Math.min(efoupdn.get(accId).minpvalUp, pval);
                        }
                        else {
                            efoupdn.get(accId).dn.add(experimentId);
                            efoupdn.get(accId).minpvalDn =
                                    Math.min(efoupdn.get(accId).minpvalDn, pval);
                        }
                    }
                }

                if (isUp) {
                    upexp.add(experimentId);
                }
                else {
                    dnexp.add(experimentId);
                }

                String[] accs;
                if (accessions != null) {
                    accs = accessions.toArray(new String[accessions.size()]);
                }
                else {
                    accs = new String[0];
                }
                expTable.add(ef, efv, accs, experimentId, isUp, pval);
            }
        }

        if (!wasresult) {
            return false;
        }

        solrDoc.addField("exp_info", expTable.serialize());

        for (String rootId : efo.getRootIds()) {
            calcChildren(rootId, efoupdn);
        }

        storeEfoCounts(solrDoc, efoupdn);
        storeEfvCounts(solrDoc, efvupdn);
        storeExperimentIds(solrDoc, upexp, dnexp);
        storeEfvs(solrDoc, upefv, dnefv);

        return true;
    }

    private void storeEfvs(SolrInputDocument solrDoc,
                           Map<String, Set<String>> upefv,
                           Map<String, Set<String>> dnefv) {
        for (Map.Entry<String, Set<String>> e : upefv.entrySet()) {
            for (String i : e.getValue()) {
                solrDoc.addField("efvs_up_" + EscapeUtil.encode(e.getKey()), i);
            }
        }

        for (Map.Entry<String, Set<String>> e : dnefv.entrySet()) {
            for (String i : e.getValue()) {
                solrDoc.addField("efvs_dn_" + EscapeUtil.encode(e.getKey()), i);
            }
        }

        for (String factor : union(upefv.keySet(), dnefv.keySet())) {
            for (String i : union(upefv.get(factor), dnefv.get(factor))) {
                solrDoc.addField("efvs_ud_" + EscapeUtil.encode(factor), i);
            }
        }
    }

    private void storeExperimentIds(SolrInputDocument solrDoc, Set<Long> upexp,
                                    Set<Long> dnexp) {
        for (Long i : upexp) {
            solrDoc.addField("exp_up_ids", i);
        }
        for (Long i : dnexp) {
            solrDoc.addField("exp_dn_ids", i);
        }

        for (Long i : union(upexp, dnexp)) {
            solrDoc.addField("exp_ud_ids", i);
        }
    }

    private void storeEfoCounts(SolrInputDocument solrDoc,
                                Map<String, UpDnSet> efoupdn) {
        for (Map.Entry<String, UpDnSet> e : efoupdn.entrySet()) {
            String accession = e.getKey();
            UpDnSet ud = e.getValue();

            ud.childrenUp.addAll(ud.up);
            ud.childrenDn.addAll(ud.dn);

            int cup = ud.childrenUp.size();
            int cdn = ud.childrenDn.size();

            double pup = Math.min(ud.minpvalChildrenUp, ud.minpvalUp);
            double pdn = Math.min(ud.minpvalChildrenDn, ud.minpvalDn);

            if (cup > 0) {
                solrDoc.addField("cnt_efo_" + accession + "_up", cup);
                solrDoc.addField("minpval_efo_" + accession + "_up", pup);
            }
            if (cdn > 0) {
                solrDoc.addField("cnt_efo_" + accession + "_dn", cdn);
                solrDoc.addField("minpval_efo_" + accession + "_dn", pdn);
            }
            if (ud.up.size() > 0) {
                solrDoc.addField("cnt_efo_" + accession + "_s_up", ud.up.size());
                solrDoc.addField("minpval_efo_" + accession + "_s_up", ud.minpvalUp);
            }
            if (ud.dn.size() > 0) {
                solrDoc.addField("cnt_efo_" + accession + "_s_dn", ud.dn.size());
                solrDoc.addField("minpval_efo_" + accession + "_s_dn", ud.minpvalDn);
            }

            if (cup > 0) {
                solrDoc.addField("s_efo_" + accession + "_up",
                                 shorten(cup * (1.0 - pup) - cdn * (1.0 - pdn)));
            }
            if (cdn > 0) {
                solrDoc.addField("s_efo_" + accession + "_dn",
                                 shorten(cdn * (1.0 - pdn) - cup * (1.0 - pup)));
            }
            if (cup + cdn > 0) {
                solrDoc.addField("s_efo_" + accession + "_ud",
                                 shorten(cup * (1.0 - pup) + cdn * (1.0 - pdn)));
            }

            if (cup > 0) {
                solrDoc.addField("efos_up", accession);
            }
            if (cdn > 0) {
                solrDoc.addField("efos_dn", accession);
            }
            if (cup + cdn > 0) {
                solrDoc.addField("efos_ud", accession);
            }
        }
    }

    private void storeEfvCounts(SolrInputDocument solrDoc,
                                Map<String, UpDn> efvupdn) {
        for (Map.Entry<String, UpDn> e : efvupdn.entrySet()) {
            String efvid = e.getKey();
            UpDn ud = e.getValue();

            int cup = ud.cup;
            int cdn = ud.cdn;
            double pvup = ud.pup;
            double pvdn = ud.pdn;

            if (cup != 0) {
                solrDoc.addField("cnt_" + efvid + "_up", cup);
                solrDoc.addField("minpval_" + efvid + "_up", pvup);
            }
            if (cdn != 0) {
                solrDoc.addField("cnt_" + efvid + "_dn", cdn);
                solrDoc.addField("minpval_" + efvid + "_dn", pvdn);
            }

            solrDoc.addField("s_" + efvid + "_up",
                             shorten(cup * (1.0 - pvup) - cdn * (1.0 - pvdn)));
            solrDoc.addField("s_" + efvid + "_dn",
                             shorten(cdn * (1.0 - pvdn) - cup * (1.0 - pvup)));
            solrDoc.addField("s_" + efvid + "_ud",
                             shorten(cup * (1.0 - pvup) + cdn * (1.0 - pvdn)));

        }
    }

    private void loadEfoMapping() {
        log.info("Fetching ontology mappings...");

        // todo - query by ontology name necessary?
        List<OntologyMapping> mappings = getAtlasDAO().getOntologyMappings();
        for (OntologyMapping mapping : mappings) {
            String mapKey = mapping.getExperimentAccession() + "_" +
                    mapping.getProperty().toLowerCase() + "_" +
                    mapping.getPropertyValue();

            if (ontomap.containsKey(mapKey)) {
                // fetch the existing array and add this term
                // fixme: should actually add ontology term accession
                ontomap.get(mapKey).add(mapping.getOntologyTerm());
            }
            else {
                // add a new array
                List<String> values = new ArrayList<String>();
                // fixme: should actually add ontology term accession
                values.add(mapping.getOntologyTerm());
                ontomap.put(mapKey, values);
            }
        }

        log.info("Ontology mappings loaded");
    }

    private short shorten(double d) {
        d = d * 256;
        if (d > Short.MAX_VALUE) {
            return Short.MAX_VALUE;
        }
        if (d < Short.MIN_VALUE) {
            return Short.MIN_VALUE;
        }
        return (short) d;
    }

    private class UpDnSet {
        Set<Long> up = new HashSet<Long>();
        Set<Long> dn = new HashSet<Long>();
        Set<Long> childrenUp = new HashSet<Long>();
        Set<Long> childrenDn = new HashSet<Long>();
        boolean processed = false;
        double minpvalUp = 1;
        double minpvalDn = 1;
        double minpvalChildrenUp = 1;
        double minpvalChildrenDn = 1;

        void addChild(UpDnSet child) {
            childrenUp.addAll(child.childrenUp);
            childrenDn.addAll(child.childrenDn);
            childrenUp.addAll(child.up);
            childrenDn.addAll(child.dn);
            minpvalChildrenDn =
                    Math.min(Math.min(minpvalChildrenDn, child.minpvalChildrenDn),
                             child.minpvalDn);
            minpvalChildrenUp =
                    Math.min(Math.min(minpvalChildrenUp, child.minpvalChildrenUp),
                             child.minpvalUp);
        }
    }

    private class UpDn {
        int cup = 0;
        int cdn = 0;
        double pup = 1;
        double pdn = 1;
    }
}