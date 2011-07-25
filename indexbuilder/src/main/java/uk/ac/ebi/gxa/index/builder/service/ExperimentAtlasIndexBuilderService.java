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
import uk.ac.ebi.gxa.dao.ExperimentDAO;
import uk.ac.ebi.gxa.index.builder.IndexAllCommand;
import uk.ac.ebi.gxa.index.builder.IndexBuilderException;
import uk.ac.ebi.gxa.index.builder.UpdateIndexForExperimentCommand;
import uk.ac.ebi.gxa.utils.EscapeUtil;
import uk.ac.ebi.microarray.atlas.model.*;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

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
    private ExperimentDAO experimentDAO;

    public void setExperimentDAO(ExperimentDAO experimentDAO) {
        this.experimentDAO = experimentDAO;
    }

    @Override
    public void processCommand(final IndexAllCommand indexAll, final ProgressUpdater progressUpdater) throws IndexBuilderException {
        super.processCommand(indexAll, progressUpdater);

        getAtlasDAO().startSession();
        try {
            final List<Experiment> experiments = experimentDAO.getAll();

            final int total = experiments.size();
            int num = 0;
            for (Experiment experiment : experiments) {
                processExperiment(experiment);
                progressUpdater.update(++num + "/" + total);
            }
        } catch (IOException e) {
            throw new IndexBuilderException(e);
        } catch (SolrServerException e) {
            throw new IndexBuilderException(e);
        } finally {
            getAtlasDAO().finishSession();
        }
    }

    @Override
    public void processCommand(UpdateIndexForExperimentCommand cmd, ProgressUpdater progressUpdater) throws IndexBuilderException {
        super.processCommand(cmd, progressUpdater);
        String accession = cmd.getAccession();

        getLog().info("Updating index for experiment " + accession);

        getAtlasDAO().startSession();
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
        } finally {
            getAtlasDAO().finishSession();
        }
    }

    //changed scope to public to make test case
    public void processExperiment(Experiment experiment) throws SolrServerException, IOException {
        // Create a new solr document
        SolrInputDocument solrInputDoc = new SolrInputDocument();

        getLog().info("Updating index - adding experiment {}", experiment.getAccession());

        solrInputDoc.addField("id", experiment.getId());
        solrInputDoc.addField("accession", experiment.getAccession());
        solrInputDoc.addField("description", experiment.getDescription());
        solrInputDoc.addField("pmid", experiment.getPubmedId());
        solrInputDoc.addField("abstract", experiment.getAbstract());
        solrInputDoc.addField("loaddate", experiment.getLoadDate());

        addAssayInformation(solrInputDoc, experiment);
        addSampleInformation(solrInputDoc, experiment);
        addAssetInformation(solrInputDoc, experiment);

        getLog().info("Finalising changes for {}", experiment);
        getSolrServer().add(solrInputDoc);
    }

    private void addAssayInformation(SolrInputDocument solrInputDoc, Experiment experiment) {
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
        //asset captions stored as an indexed multivalue property
        //asset file names is comma-separated list for now
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
