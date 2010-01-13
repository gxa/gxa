package uk.ac.ebi.gxa.index.builder.service;

import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.core.CoreContainer;
import uk.ac.ebi.gxa.index.builder.IndexBuilderException;
import uk.ac.ebi.gxa.utils.EscapeUtil;
import uk.ac.ebi.microarray.atlas.dao.AtlasDAO;
import uk.ac.ebi.microarray.atlas.dao.LoadStage;
import uk.ac.ebi.microarray.atlas.dao.LoadStatus;
import uk.ac.ebi.microarray.atlas.model.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.net.URLEncoder;

/**
 * An {@link IndexBuilderService} that generates index documents from the experiments in the Atlas database.
 * <p/>
 * Note that this implementation does NOT support updates - regardless of whether the update flag is set to true, this
 * will rebuild the index every time.
 *
 * @author Tony Burdett
 * @date 22-Sep-2009
 */
public class ExperimentAtlasIndexBuilderService extends IndexBuilderService {
    private static final int NUM_THREADS = 64;

    public ExperimentAtlasIndexBuilderService(AtlasDAO atlasDAO, SolrServer solrServer) {
        super(atlasDAO, solrServer);
    }

    protected void createIndexDocs(boolean pendingOnly) throws IndexBuilderException {
        // do initial setup - build executor service
        ExecutorService tpool = Executors.newFixedThreadPool(NUM_THREADS);

        // fetch experiments - check if we want all or only the pending ones
        List<Experiment> experiments = pendingOnly
                ? getAtlasDAO().getAllExperimentsPendingIndexing()
                : getAtlasDAO().getAllExperiments();

        // the list of futures - we need these so we can block until completion
        List<Future<UpdateResponse>> tasks = new ArrayList<Future<UpdateResponse>>();

        try {
            for (final Experiment experiment : experiments) {
                tasks.add(tpool.submit(new Callable<UpdateResponse>() {
                    public UpdateResponse call() throws IOException, SolrServerException {
                        UpdateResponse response = null;
                        try {
                            // update loadmonitor - experiment is indexing
                            getAtlasDAO().writeLoadDetails(
                                    experiment.getAccession(), LoadStage.SEARCHINDEX, LoadStatus.WORKING);

                            // Create a new solr document
                            SolrInputDocument solrInputDoc = new SolrInputDocument();

                            getLog().info("Updating index - adding experiment " + experiment.getAccession());
                            getLog().debug("Adding standard fields for experiment stats");

                            solrInputDoc.addField("id", experiment.getExperimentID());
                            solrInputDoc.addField("accession", experiment.getAccession());
                            solrInputDoc.addField("description", experiment.getDescription());

                            // now, fetch assays for this experiment
                            List<Assay> assays =
                                    getAtlasDAO().getAssaysByExperimentAccession(experiment.getAccession());
                            if (assays.size() == 0) {
                                getLog().warn("No assays present for " +
                                        experiment.getAccession());
                            }

                            for (Assay assay : assays) {
                                // get assay properties and values
                                getLog().debug("Getting properties for assay " + assay.getAssayID());
                                if (assay.getProperties().size() == 0) {
                                    getLog().warn("No properties present for assay " + assay.getAssayID() +
                                            " (" + experiment.getAccession() + ")");
                                }

                                for (Property prop : assay.getProperties()) {
                                    String p = prop.getName();
                                    String pv = prop.getValue();

                                    getLog().trace("Updating index, assay property " + p + " = " + pv);
                                    solrInputDoc.addField("a_property_" + p, pv);
                                    getLog().trace("Wrote " + p + " = " + pv);
                                }
                            }

                            // now get samples
                            List<Sample> samples =
                                    getAtlasDAO().getSamplesByExperimentAccession(experiment.getAccession());
                            if (samples.size() == 0) {
                                getLog().warn("No samples present for experiment " + experiment.getAccession());
                            }

                            for (Sample sample : samples) {
                                // get assay properties and values
                                getLog().debug("Getting properties for sample " + sample.getSampleID());
                                if (sample.getProperties().size() == 0) {
                                    getLog().warn("No properties present for sample " + sample.getSampleID() +
                                            " (" + experiment.getAccession() + ")");
                                }

                                // get sample properties and values
                                for (Property prop : sample.getProperties()) {
                                    String p = prop.getName();
                                    String pv = prop.getValue();

                                    getLog().trace("Updating index, sample property " + p + " = " + pv);
                                    solrInputDoc.addField("s_property_" + p, pv);
                                    getLog().trace("Wrote " + p + " = " + pv);
                                }
                            }

                            // now, fetch atlas counts for this experiment
                            getLog().debug("Evaluating atlas counts for " + experiment.getAccession());
                            List<AtlasCount> atlasCounts = getAtlasDAO().getAtlasCountsByExperimentID(
                                    experiment.getExperimentID());
                            getLog().debug(experiment.getAccession() + " has " + atlasCounts.size() +
                                    " atlas count objects");
                            for (AtlasCount count : atlasCounts) {
                                // efvid is concatenation of ef and efv
                                String efvid = EscapeUtil.encode(count.getProperty(), count.getPropertyValue());
                                // field name is efvid_up / efvid_dn depending on expression
                                String fieldname = "c_" + efvid + "_" + (count.getUpOrDown().equals("-1") ? "dn" : "up");

                                // add a field:
                                // key is the fieldname, value is the total count
                                getLog().debug("Updating index with atlas count data... key: " + fieldname + "; " +
                                        "value: " + count.getGeneCount());
                                solrInputDoc.addField(fieldname, count.getGeneCount());
                            }

                            // finally, add the document to the index
                            getLog().info("Finalising changes for " + experiment.getAccession());
                            response = getSolrServer().add(solrInputDoc);

                            // update loadmonitor table - experiment has completed indexing
                            getAtlasDAO().writeLoadDetails(
                                    experiment.getAccession(), LoadStage.SEARCHINDEX, LoadStatus.DONE);

                            return response;
                        }
                        finally {
                            // if the response was set, everything completed as expected, but if it's null we got
                            // an uncaught exception, so make sure we update loadmonitor to reflect that this failed
                            if (response == null) {
                                getAtlasDAO().writeLoadDetails(
                                        experiment.getAccession(), LoadStage.SEARCHINDEX, LoadStatus.FAILED);
                            }

                            // perform an explicit garbage collection to make sure all refs to large datasets are cleaned up
                            System.gc();
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
                        throw new IndexBuilderException("An error occurred updating Experiments SOLR index", e);
                    }
                }
                catch (InterruptedException e) {
                    throw new IndexBuilderException("An error occurred updating Experiments SOLR index", e);
                }
            }
        }
        finally {
            // shutdown the service
            getLog().info("Experiment index building tasks finished, cleaning up resources and exiting");
            tpool.shutdown();
        }
    }

    public static class Factory implements IndexBuilderService.Factory {
        public IndexBuilderService create(AtlasDAO atlasDAO, CoreContainer coreContainer) {
            return new ExperimentAtlasIndexBuilderService(atlasDAO, new EmbeddedSolrServer(coreContainer, "expt"));
        }

        public String getName() {
            return "experiments";
        }

        public String[] getConfigFiles() {
            return getBasicConfigFilesForCore("expt");
        }
    }
}
