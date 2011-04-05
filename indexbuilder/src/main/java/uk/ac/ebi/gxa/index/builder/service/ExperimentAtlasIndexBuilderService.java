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

package uk.ac.ebi.gxa.index.builder.service;

import com.google.common.base.Function;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrInputDocument;
import uk.ac.ebi.gxa.dao.LoadStage;
import uk.ac.ebi.gxa.dao.LoadStatus;
import uk.ac.ebi.gxa.index.builder.IndexAllCommand;
import uk.ac.ebi.gxa.index.builder.IndexBuilderException;
import uk.ac.ebi.gxa.index.builder.UpdateIndexForExperimentCommand;
import uk.ac.ebi.gxa.utils.Deque;
import uk.ac.ebi.gxa.utils.EscapeUtil;
import uk.ac.ebi.microarray.atlas.model.*;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import static com.google.common.base.Joiner.on;
import static com.google.common.collect.Collections2.transform;

/**
 * An {@link IndexBuilderService} that generates index documents from the experiments in the Atlas database.
 * <p/>
 * Note that this implementation does NOT support updates - regardless of whether the update flag is set to true, this
 * will rebuild the index every time.
 *
 * @author Tony Burdett
 */
public class ExperimentAtlasIndexBuilderService extends IndexBuilderService {
    private ExecutorService executor;

    public void setExecutor(ExecutorService executor) {
        this.executor = executor;
    }

    @Override
    public void processCommand(final IndexAllCommand indexAll, final ProgressUpdater progressUpdater) throws IndexBuilderException {
        super.processCommand(indexAll, progressUpdater);

        // fetch all public curated experiments - check if we want all or only the pending ones
        Collection<Experiment> experiments = getAtlasDAO().getPublicCuratedExperiments();

        // if we're computing all analytics, some might not be pending, so reset them to pending up front
        for (Experiment experiment : experiments) {
            getAtlasDAO().writeLoadDetails(
                    experiment.getAccession(), LoadStage.SEARCHINDEX, LoadStatus.PENDING);
        }

        // the list of futures - we need these so we can block until completion
        Deque<Future<Boolean>> tasks = new Deque<Future<Boolean>>(10);

        // the first error encountered whilst building the index, if any
        Exception firstError = null;

        try {
            final int total = experiments.size();
            final AtomicInteger num = new AtomicInteger(0);
            for (final Experiment experiment : experiments) {
                tasks.offerLast(executor.submit(new Callable<Boolean>() {
                    public Boolean call() throws IOException, SolrServerException {
                        boolean result = processExperiment(experiment);
                        int processed = num.incrementAndGet();
                        progressUpdater.update(processed + "/" + total);
                        return result;
                    }
                }));
            }

            experiments.clear();

            // block until completion, and throw the first error we see
            while (true) {
                try {
                    Future<Boolean> task = tasks.poll();
                    if (task == null) {
                        break;
                    }
                    task.get();
                } catch (Exception e) {
                    // print the stacktrace, but swallow this exception to rethrow at the very end
                    getLog().error("An error occurred whilst building the Experiments index:\n{}", e);
                    if (firstError == null) {
                        firstError = e;
                    }
                }
            }

            // if we have encountered an exception, throw the first error
            if (firstError != null) {
                throw new IndexBuilderException("An error occurred whilst building the Experiments index", firstError);
            }
        } finally {
            // shutdown the service
            getLog().info("Experiment index building tasks finished, cleaning up resources and exiting");
        }
    }

    @Override
    public void processCommand(UpdateIndexForExperimentCommand cmd, ProgressUpdater progressUpdater) throws IndexBuilderException {
        super.processCommand(cmd, progressUpdater);
        String accession = cmd.getAccession();

        getLog().info("Updating index for experiment " + accession);
        try {
            progressUpdater.update("0/1");
            getSolrServer().deleteByQuery("accession:" + EscapeUtil.escapeSolr(accession));
            Experiment experiment = getAtlasDAO().getExperimentByAccession(accession);
            processExperiment(experiment);
            progressUpdater.update("1/1");
        } catch (SolrServerException e) {
            throw new IndexBuilderException(e);
        } catch (IOException e) {
            throw new IndexBuilderException(e);
        }
    }

    //changed scope to public to make test case
    public boolean processExperiment(Experiment experiment) throws SolrServerException, IOException {
        boolean result = false;
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
            solrInputDoc.addField("pmid", experiment.getPubmedID());
            solrInputDoc.addField("abstract", experiment.getArticleAbstract());
            solrInputDoc.addField("loaddate", experiment.getLoadDate());
            solrInputDoc.addField("releasedate", experiment.getReleaseDate());


            // now, fetch assays for this experiment
            long start = System.currentTimeMillis();
            List<Assay> assays =
                    getAtlasDAO().getAssaysByExperimentAccession(experiment.getAccession());
            if (assays.size() == 0) {
                getLog().trace("No assays present for " +
                        experiment.getAccession());
            }
            getLog().debug("Retrieved: " + assays.size() + " assays for experiment: " + experiment.getAccession() + " in: " + (System.currentTimeMillis() - start) + " ms");

            Set<String> assayProps = new HashSet<String>();
            Set<String> arrayDesigns = new LinkedHashSet<String>();

            start = System.currentTimeMillis();
            for (Assay assay : assays) {
                // get assay properties and values
                getLog().debug("Getting properties for assay " + assay.getAssayID());
                if (assay.hasNoProperties()) {
                    getLog().trace("No properties present for assay " + assay.getAssayID() +
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

                arrayDesigns.add(assay.getArrayDesignAccession());
            }
            getLog().info("Updated index with assay properties for: " + assays.size() + " assays for experiment: " + experiment.getAccession() + " in: " + (System.currentTimeMillis() - start) + " ms");

            solrInputDoc.addField("a_properties", assayProps);

            start = System.currentTimeMillis();
            // now get samples
            List<Sample> samples =
                    getAtlasDAO().getSamplesByExperimentAccession(experiment.getAccession());
            if (samples.size() == 0) {
                getLog().trace("No samples present for experiment " + experiment.getAccession());
            }
            getLog().debug("Retrieved: " + samples.size() + " samples for experiment: " + experiment.getAccession() + " in: " + (System.currentTimeMillis() - start) + " ms");

            start = System.currentTimeMillis();
            Set<String> sampleProps = new HashSet<String>();
            for (Sample sample : samples) {
                // get assay properties and values
                getLog().debug("Getting properties for sample " + sample.getSampleID());
                if (sample.hasNoProperties()) {
                    getLog().trace("No properties present for sample " + sample.getSampleID() +
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
            getLog().info("Updated index with sample properties for: " + assays.size() + " samples for experiment: " + experiment.getAccession() + " in: " + (System.currentTimeMillis() - start) + " ms");


            solrInputDoc.addField("s_properties", sampleProps);

            solrInputDoc.addField("platform", on(",").join(arrayDesigns));
            solrInputDoc.addField("numSamples", String.format("%d", samples.size()));
            solrInputDoc.addField("numIndividuals", null); //TODO:
            solrInputDoc.addField("studyType", null); //TODO:

            start = System.currentTimeMillis();
            addAssetInformation(solrInputDoc, experiment);
            getLog().info("Added asset info for experiment: " + experiment.getAccession() + " in: " + (System.currentTimeMillis() - start) + " ms");


            // finally, add the document to the index
            getLog().info("Finalising changes for " + experiment.getAccession());
            start = System.currentTimeMillis();
            getSolrServer().add(solrInputDoc);
            getLog().info("Added Solr doc to index for  experiment: " + experiment.getAccession() + " in: " + (System.currentTimeMillis() - start) + " ms");

            // update loadmonitor table - experiment has completed indexing
            getAtlasDAO().writeLoadDetails(
                    experiment.getAccession(), LoadStage.SEARCHINDEX, LoadStatus.DONE);

            return result = true;
        } finally {
            // if the response was set, everything completed as expected, but if it's null we got
            // an uncaught exception, so make sure we update loadmonitor to reflect that this failed
            if (!result) {
                getAtlasDAO().writeLoadDetails(
                        experiment.getAccession(), LoadStage.SEARCHINDEX, LoadStatus.FAILED);
            }
        }
    }

    private void addAssetInformation(SolrInputDocument solrInputDoc, Experiment experiment) {
        //asset captions stored as indexed multy-value property
        //asset filenames is comma-separated list for now
        for (Asset a : experiment.getAssets()) {
            solrInputDoc.addField("assetCaption", a.getName());
            solrInputDoc.addField("assetDescription", a.getDescription());
        }
        solrInputDoc.addField("assetFileInfo", on(",").join(transform(experiment.getAssets(),
                new Function<Asset, String>() {
                    public String apply(@Nonnull Asset a) {
                        return a.getFileName();
                    }
                })));
    }

    public String getName() {
        return "experiments";
    }
}
