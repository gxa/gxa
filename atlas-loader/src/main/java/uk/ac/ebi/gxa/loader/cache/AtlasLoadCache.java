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

package uk.ac.ebi.gxa.loader.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.arrayexpress2.magetab.utils.MAGETABUtils;
import uk.ac.ebi.gxa.loader.AtlasLoaderException;
import uk.ac.ebi.gxa.loader.datamatrix.DataMatrixFileBuffer;
import uk.ac.ebi.gxa.data.DataMatrixStorage;
import uk.ac.ebi.microarray.atlas.model.ArrayDesign;
import uk.ac.ebi.microarray.atlas.model.Assay;
import uk.ac.ebi.microarray.atlas.model.Experiment;
import uk.ac.ebi.microarray.atlas.model.Sample;

import java.net.URL;
import java.util.*;

/**
 * A cache of objects that need to be loaded into the Atlas DB.  This temporarily stores objects during parsing
 *
 * @author Tony Burdett
 */
public class AtlasLoadCache implements ExperimentBuilder {
    private static final Logger log = LoggerFactory.getLogger(AtlasLoadCache.class);

    private Experiment experiment;
    private Map<String, DataMatrixFileBuffer> dataMatrixBuffers = new HashMap<String, DataMatrixFileBuffer>();
    private Map<String, DataMatrixStorage.ColumnRef> assayDataMap = new HashMap<String, DataMatrixStorage.ColumnRef>();
    private Collection<String> availQTypes;

    public AtlasLoadCache() {
    }

    @Override
    public void linkAssayToSample(Assay assay, String sampleAccession) throws AtlasLoaderException {
        Sample sample = fetchSample(sampleAccession);

        if (sample == null) {
            // no sample to link to in the cache - generate error item and throw exception
            throw new AtlasLoaderException("Assay " + assay.getAccession() + " is linked to sample " +
                    sampleAccession + " but this sample is not due to be loaded. " +
                    "This assay will not be linked to a sample");
        }

        sample.addAssay(assay);
    }

    @Override
    public Sample fetchOrCreateSample(String accession) {
        Sample sample = fetchSample(accession);
        if (sample == null) {
            // create a new sample and add it to the cache
            sample = new Sample(accession);
            addSample(sample);
        }
        return sample;
    }

    public void setAvailQTypes(Collection<String> availQTypes) {
        this.availQTypes = new HashSet<String>();
        for (String qtype : availQTypes) {
            this.availQTypes.add(MAGETABUtils.digestHeader(qtype));
        }
    }

    /**
     * Adds an experiment to the cache of objects to be loaded.  Experiments are indexed by accession, so every
     * experiment in the cache should have a unique accession.  If an experiment is passed to this method with an
     * accession that is the same as one that has been previously stored, and the experiment is not the same object,
     * then an IllegalArgumentException is thrown
     *
     * @param experiment the experiment to store in the cache.
     */
    @Override
    public void setExperiment(Experiment experiment) {
        if (experiment == null) {
            throw new NullPointerException("Experiment is null");
        }

        if (this.experiment != null) {
            log.error("Experiment already set, old = {}, new = {}", this.experiment, experiment);
            throw new IllegalStateException("Attempting to override experiment already set");
        }

        this.experiment = experiment;
    }

    /**
     * Retrieves all stored experiments from this cache.
     *
     * @return the collection of stored experiments
     */
    @Override
    public Experiment fetchExperiment() {
        return this.experiment;
    }

    /**
     * Adds an assay to the cache of objects to be loaded.  Assays are indexed by accession, so every assay in the cache
     * should have a unique accession.  If an assay is passed to this method with an accession that is the same as one
     * that has been previously stored, and the assay is not the same object, then an IllegalArgumentException is
     * thrown
     *
     * @param assay the assay to store in the cache.
     */
    @Override
    public void addAssay(Assay assay) {
        experiment.addAssay(assay);
    }

    /**
     * Retrieves an assay from the load cache with the given accession, if present.  If there is no assay with the given
     * accession, null is returned.
     *
     * @param accession the accession of the assay to fetch
     * @return the assay, if present, or null if there is no assay with this accession
     */
    @Override
    public Assay fetchAssay(String accession) {
        return experiment.getAssay(accession);
    }

    /**
     * Retrieves all stored assays from this cache.
     *
     * @return the collection of stored assays
     */
    @Override
    public Collection<Assay> fetchAllAssays() {
        return experiment.getAssays();
    }

    public DataMatrixFileBuffer getDataMatrixFileBuffer(URL url, String fileName) throws AtlasLoaderException {
        return getDataMatrixFileBuffer(url, fileName, true);
    }

    public DataMatrixFileBuffer getDataMatrixFileBuffer(URL url, String fileName, boolean hasQtTypes)
            throws AtlasLoaderException {

        String filePath = url.toExternalForm();
        if (fileName != null) {
            filePath += fileName;
        }
        DataMatrixFileBuffer buffer = dataMatrixBuffers.get(filePath);
        if (buffer == null) {
            buffer = new DataMatrixFileBuffer(url, fileName, availQTypes, hasQtTypes);
            dataMatrixBuffers.put(filePath, buffer);
        }
        return buffer;
    }

    /**
     * Adds an sample to the cache of objects to be loaded.  Samples are indexed by accession, so every sample in the
     * cache should have a unique accession. If an sample is passed to this method with an accession that is the same as
     * one that has been previously stored, and the sample is not the same object, then an IllegalArgumentException is
     * thrown
     *
     * @param sample the sample to store in the cache.
     */
    @Override
    public void addSample(Sample sample) {
        experiment.addSample(sample);
    }

    /**
     * Retrieves an sample from the load cache with the given accession, if present.  If there is no sample with the
     * given accession, null is returned.
     *
     * @param accession the accession of the sample to fetch
     * @return the sample, if present, or null if there is no sample with this accession
     */
    @Override
    public Sample fetchSample(String accession) {
        return experiment.getSample(accession);
    }

    /**
     * Retrieves all stored samples from this cache.
     *
     * @return the collection of stored samples
     */
    @Override
    public Collection<Sample> fetchAllSamples() {
        return experiment.getSamples();
    }

    public void setAssayDataMatrixRef(Assay assay, DataMatrixStorage buffer, int columnIndex) {
        assayDataMap.put(assay.getAccession(), new DataMatrixStorage.ColumnRef(buffer, columnIndex));
    }

    public Map<String, DataMatrixStorage.ColumnRef> getAssayDataMap() {
        return assayDataMap;
    }
}
