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
import uk.ac.ebi.gxa.index.builder.IndexAllCommand;
import uk.ac.ebi.gxa.index.builder.IndexBuilderException;
import uk.ac.ebi.gxa.index.builder.UpdateIndexForExperimentCommand;
import uk.ac.ebi.gxa.utils.EscapeUtil;
import uk.ac.ebi.microarray.atlas.model.*;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
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

        // fetch all experiments - check if we want all or only the pending ones
        final List<Experiment> experiments = getAtlasDAO().getAllExperiments();

        final int total = experiments.size();
        final AtomicInteger num = new AtomicInteger(0);
        Collection<Callable<Boolean>> tasks = transform(experiments, new Function<Experiment, Callable<Boolean>>() {
            @Override
            public Callable<Boolean> apply(@Nonnull final Experiment experiment) {
                return new Callable<Boolean>() {
                    public Boolean call() throws IOException, SolrServerException {
                        boolean result = processExperiment(experiment.getAccession());
                        int processed = num.incrementAndGet();
                        progressUpdater.update(processed + "/" + total);
                        return result;
                    }
                };
            }
        });

        // the first error encountered whilst building the index, if any
        Exception firstError = null;

        try {
            final List<Future<Boolean>> results = executor.invokeAll(tasks);

            // block until completion, and throw the first error we see
            for (Future<Boolean> task : results) {
                try {
                    task.get();
                } catch (ExecutionException e) {
                    // print the stacktrace, but swallow this exception to rethrow at the very end
                    getLog().error("An error occurred whilst building the Experiments index:\n" + e.getMessage(), e.getCause());
                    if (firstError == null) {
                        firstError = e;
                    }
                }
            }

            // if we have encountered an exception, throw the first error
            if (firstError != null) {
                throw new IndexBuilderException("An error occurred whilst building the Experiments index", firstError);
            }
        } catch (InterruptedException e) {
            throw new IndexBuilderException("Interrupted while building the Experiments index", e);
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
            Experiment experiment = getAtlasModel().getExperimentByAccession(accession);
            processExperiment(experiment.getAccession());
            progressUpdater.update("1/1");
        } catch (SolrServerException e) {
            throw new IndexBuilderException(e);
        } catch (IOException e) {
            throw new IndexBuilderException(e);
        }
    }

    //changed scope to public to make test case
    public boolean processExperiment(String accession) throws SolrServerException, IOException {
        getAtlasDAO().startSession();
        try {
            final Experiment experiment = getAtlasDAO().getExperimentByAccession(accession);
            // Create a new solr document
            SolrInputDocument solrInputDoc = new SolrInputDocument();

            getLog().info("Updating index - adding experiment {}", experiment.getAccession());

            solrInputDoc.addField("id", experiment.getId());
            solrInputDoc.addField("accession", experiment.getAccession());
            solrInputDoc.addField("description", experiment.getDescription());
            solrInputDoc.addField("pmid", experiment.getPubmedId());
            solrInputDoc.addField("abstract", experiment.getAbstract());
            solrInputDoc.addField("loaddate", experiment.getLoadDate());
            solrInputDoc.addField("releasedate", experiment.getReleaseDate());

            assAssayInformation(experiment, solrInputDoc);

            addSampleInformation(solrInputDoc, experiment);

            addAssetInformation(solrInputDoc, experiment);

            getLog().info("Finalising changes for {}", experiment);
            getSolrServer().add(solrInputDoc);

            return true;
        } finally {
            getAtlasDAO().finishSession();
        }
    }

    private void assAssayInformation(Experiment experiment, SolrInputDocument solrInputDoc) {
        if (experiment.getAssays().isEmpty()) {
            getLog().trace("No assays present for {}", experiment);
        }

        Set<String> assayPropertyNames = new HashSet<String>();
        Set<String> arrayDesignAccessions = new LinkedHashSet<String>();
        for (Assay assay : experiment.getAssays()) {
            if (assay.hasNoProperties()) {
                getLog().trace("No properties present for {} ({})", assay);
            }
            for (AssayProperty prop : assay.getProperties()) {
                solrInputDoc.addField("a_property_" + prop.getName(), prop.getValue());
                assayPropertyNames.add(prop.getName());
            }
            arrayDesignAccessions.add(assay.getArrayDesign().getAccession());
        }
        solrInputDoc.addField("a_properties", assayPropertyNames);
        solrInputDoc.addField("platform", on(",").join(arrayDesignAccessions));
    }

    private void addSampleInformation(SolrInputDocument solrInputDoc, Experiment experiment) {
        Set<String> samplePropertyNames = new HashSet<String>();
        for (Sample sample : experiment.getSamples()) {
            if (sample.hasNoProperties()) {
                getLog().trace("No properties present for {}", sample);
            }

            // get sample properties and values
            for (SampleProperty prop : sample.getProperties()) {
                solrInputDoc.addField("s_property_" + prop.getName(), prop.getValue());
                samplePropertyNames.add(prop.getName());
            }
        }
        solrInputDoc.addField("s_properties", samplePropertyNames);
        solrInputDoc.addField("numSamples", experiment.getSamples().size());
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
