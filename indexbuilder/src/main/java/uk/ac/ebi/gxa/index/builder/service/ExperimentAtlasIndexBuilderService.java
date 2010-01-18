package uk.ac.ebi.gxa.index.builder.service;

import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrInputDocument;
import uk.ac.ebi.gxa.index.builder.IndexBuilderException;
import uk.ac.ebi.gxa.utils.Deque;
import uk.ac.ebi.gxa.dao.LoadStage;
import uk.ac.ebi.gxa.dao.LoadStatus;
import uk.ac.ebi.microarray.atlas.model.*;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.concurrent.*;

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
    private static final int NUM_THREADS = 32;

    protected void createIndexDocs(boolean pendingOnly) throws IndexBuilderException {
        // do initial setup - build executor service
        ExecutorService tpool = Executors.newFixedThreadPool(NUM_THREADS);

        // fetch experiments - check if we want all or only the pending ones
        List<Experiment> experiments = pendingOnly
                ? getAtlasDAO().getAllExperimentsPendingIndexing()
                : getAtlasDAO().getAllExperiments();

        // the list of futures - we need these so we can block until completion
        Deque<Future<Boolean>> tasks = new Deque<Future<Boolean>>(10);

        try {
            for (final Experiment experiment : experiments) {
                tasks.offerLast(tpool.submit(new Callable<Boolean>() {
                    public Boolean call() throws IOException, SolrServerException {
                        Boolean result = false;
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

                            Set<String> assayProps = new HashSet<String>();

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
                                    assayProps.add(p);
                                }
                            }

                            solrInputDoc.addField("a_properties", assayProps);

                            // now get samples
                            List<Sample> samples =
                                    getAtlasDAO().getSamplesByExperimentAccession(experiment.getAccession());
                            if (samples.size() == 0) {
                                getLog().warn("No samples present for experiment " + experiment.getAccession());
                            }

                            Set<String> sampleProps = new HashSet<String>();
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
                                    sampleProps.add(p);
                                }
                            }

                            solrInputDoc.addField("s_properties", sampleProps);

                            // finally, add the document to the index
                            getLog().info("Finalising changes for " + experiment.getAccession());
                            getSolrServer().add(solrInputDoc);

                            // update loadmonitor table - experiment has completed indexing
                            getAtlasDAO().writeLoadDetails(
                                    experiment.getAccession(), LoadStage.SEARCHINDEX, LoadStatus.DONE);

                            result = true;

                            return result;
                        }
                        finally {
                            // if the response was set, everything completed as expected, but if it's null we got
                            // an uncaught exception, so make sure we update loadmonitor to reflect that this failed
                            if (!result) {
                                getAtlasDAO().writeLoadDetails(
                                        experiment.getAccession(), LoadStage.SEARCHINDEX, LoadStatus.FAILED);
                            }
                        }
                    }
                }));
            }

            experiments.clear();

            // block until completion, and throw any errors
            while (true) {
                Future<Boolean> task = tasks.poll();
                if(task == null)
                    break;

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

    public String getName() {
        return "experiments";
    }
}
