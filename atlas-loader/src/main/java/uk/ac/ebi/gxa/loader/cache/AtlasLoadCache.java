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
import uk.ac.ebi.gxa.loader.datamatrix.DataMatrixStorage;
import uk.ac.ebi.gxa.utils.Pair;
import uk.ac.ebi.microarray.atlas.model.ArrayDesignBundle;
import uk.ac.ebi.microarray.atlas.model.Sample;
import uk.ac.ebi.gxa.Experiment;
import uk.ac.ebi.gxa.Assay;

import java.net.URL;
import java.util.*;

/**
 * A cache of objects that need to be loaded into the Atlas DB.  This temporarily stores objects during parsing
 *
 * @author Tony Burdett
 */
public class AtlasLoadCache {
    private static final Logger log = LoggerFactory.getLogger(AtlasLoadCache.class);

    private Experiment experiment;
    private ArrayDesignBundle arrayDesignBundle;
    private Map<String, Assay> assaysByAcc = new HashMap<String, Assay>();
    private Map<String, Sample> samplesByAcc = new HashMap<String, Sample>();
    private Map<String, DataMatrixFileBuffer> dataMatrixBuffers = new HashMap<String, DataMatrixFileBuffer>();
    private Map<String, DataMatrixStorage.ColumnRef> assayDataMap = new HashMap<String, DataMatrixStorage.ColumnRef>();
    private Collection<String> availQTypes;

    private Map<String, List<String>> arrayDesignToDesignElements = new HashMap<String, List<String>>();

    //storage for pval, tsta for netcdf->netcdf transfer
    Map<Pair<String, String>, DataMatrixStorage.ColumnRef> pvalMap = new HashMap<Pair<String, String>, DataMatrixStorage.ColumnRef>();
    Map<Pair<String, String>, DataMatrixStorage.ColumnRef> tstatMap = new HashMap<Pair<String, String>, DataMatrixStorage.ColumnRef>();

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
    public synchronized void setExperiment(Experiment experiment) {
        if (experiment == null) {
            throw new NullPointerException("Experiment is null");
        }

        if (experiment.getAccession() == null) {
            throw new NullPointerException("Cannot add experiment with null accession!");
        }

        if (this.experiment != null && this.experiment.getAccession() != null && !this.experiment.equals(experiment)) {
            log.error("Experiment already set, old = " + this.experiment.getAccession() + ", new = " +
                    experiment.getAccession());
            throw new IllegalArgumentException("Attempting to override experiment already set");
        } else {
            this.experiment = experiment;
        }
        notifyAll();
    }

    /**
     * Retrieves all stored experiments from this cache.
     *
     * @return the collection of stored experiments
     */
    public synchronized Experiment fetchExperiment() {
        return this.experiment;
    }

    /**
     * Retrieves an experiment from the load cache with the given accession, if present.  If there is no experiment with
     * the given accession, null is returned.
     *
     * @param accession the accession of the experiment to fetch
     * @return the experiment, if present, or null if there is no experiment with this accession
     */
    public synchronized Experiment fetchExperiment(String accession) {
        return experiment != null && experiment.getAccession().equals(accession)
                ? experiment
                : null;
    }

    /**
     * Adds a specially constructed data bundle that ships array design data into the database.  Unlike other objects,
     * this does not use the standard atlas object model but instead creates a new data bundle with structures ready to
     * ship to the loading stored procedure.
     *
     * @param arrayDesign the array design "bundle" loader object to store in the cache
     * @deprecated
     */
    public synchronized void setArrayDesignBundle(ArrayDesignBundle arrayDesign) {
        if (arrayDesign.getAccession() == null) {
            throw new NullPointerException("Cannot add array design bundle without first setting the accession");
        }

        if (this.arrayDesignBundle != null && this.arrayDesignBundle.getAccession() != null) {
            throw new IllegalArgumentException("Attempting to override array design bundle already set");
        } else {
            this.arrayDesignBundle = arrayDesign;
        }
        notifyAll();
    }

    /**
     * Retrieves all stored array design bundles from this cache.
     *
     * @return the collection of stored samples
     * @deprecated
     */
    public synchronized ArrayDesignBundle fetchArrayDesignBundle() {
        return arrayDesignBundle;
    }

    /**
     * Retrieves an array design bundle from the load cache with the given accession, if present.  If there is no array
     * design bundle with the given accession, null is returned.
     *
     * @param accession the accession of the sample to fetch
     * @return the sample, if present, or null if there is no sample with this accession
     * @deprecated
     */
    public synchronized ArrayDesignBundle fetchArrayDesignBundle(String accession) {
        return arrayDesignBundle != null && arrayDesignBundle.getAccession().equals(accession)
                ? arrayDesignBundle
                : null;
    }

    /**
     * Adds an assay to the cache of objects to be loaded.  Assays are indexed by accession, so every assay in the cache
     * should have a unique accession.  If an assay is passed to this method with an accession that is the same as one
     * that has been previously stored, and the assay is not the same object, then an IllegalArgumentException is
     * thrown
     *
     * @param assay the assay to store in the cache.
     */
    public synchronized void addAssay(Assay assay) {
        if (assay.getAccession() == null) {
            throw new NullPointerException(
                    "Cannot add experiment with null accession!");
        }
        if (assaysByAcc.containsKey(assay.getAccession()) &&
                assaysByAcc.get(assay.getAccession()) != assay) {
            throw new IllegalArgumentException("Attempting to store a new " +
                    "assay with a non-unique accession");
        } else {
            assaysByAcc.put(assay.getAccession(), assay);
        }
        notifyAll();
    }

    /**
     * Retrieves an assay from the load cache with the given accession, if present.  If there is no assay with the given
     * accession, null is returned.
     *
     * @param accession the accession of the assay to fetch
     * @return the assay, if present, or null if there is no assay with this accession
     */
    public synchronized Assay fetchAssay(String accession) {
        return assaysByAcc.get(accession);
    }

    /**
     * Retrieves all stored assays from this cache.
     *
     * @return the collection of stored assays
     */
    public synchronized Collection<Assay> fetchAllAssays() {
        return assaysByAcc.values();
    }

    public synchronized DataMatrixFileBuffer getDataMatrixFileBuffer(URL url, String fileName) throws AtlasLoaderException {
        return getDataMatrixFileBuffer(url, fileName, true);
    }

    public synchronized DataMatrixFileBuffer getDataMatrixFileBuffer(URL url, String fileName, boolean hasQtTypes)
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
    public synchronized void addSample(Sample sample) {
        if (sample.getAccession() == null) {
            throw new NullPointerException("Cannot add sample with null accession!");
        }
        if (samplesByAcc.containsKey(sample.getAccession()) &&
                samplesByAcc.get(sample.getAccession()) != sample) {
            throw new IllegalArgumentException("Attempting to store a new " +
                    "experiment with a non-unique accession");
        } else {
            samplesByAcc.put(sample.getAccession(), sample);
        }
        notifyAll();
    }

    /**
     * Retrieves an sample from the load cache with the given accession, if present.  If there is no sample with the
     * given accession, null is returned.
     *
     * @param accession the accession of the sample to fetch
     * @return the sample, if present, or null if there is no sample with this accession
     */
    public synchronized Sample fetchSample(String accession) {
        return samplesByAcc.get(accession);
    }

    /**
     * Retrieves all stored samples from this cache.
     *
     * @return the collection of stored samples
     */
    public synchronized Collection<Sample> fetchAllSamples() {
        return samplesByAcc.values();
    }

    public synchronized void setAssayDataMatrixRef(Assay assay, DataMatrixStorage buffer, int columnIndex) {
        assayDataMap.put(assay.getAccession(), new DataMatrixStorage.ColumnRef(buffer, columnIndex));
    }

    public synchronized Map<String, DataMatrixStorage.ColumnRef> getAssayDataMap() {
        return assayDataMap;
    }

    public Map<String, List<String>> getArrayDesignToDesignElements() {
        return arrayDesignToDesignElements;
    }

    public void setDesignElements(String arrayDesign, List<String> designElements) {
        arrayDesignToDesignElements.put(arrayDesign, designElements);
    }

    /**
     * Clears the cache.  All objects currently stored in this cache will be removed.
     */
    public synchronized void clear() {
        // set single params as null
        experiment = null;
        arrayDesignBundle = null;
        // clear collections
        assaysByAcc.clear();
        samplesByAcc.clear();
        // clear all our data matrix file buffers
        dataMatrixBuffers.clear();
        notifyAll();
    }

    public void setPvalDataMap(Map<Pair<String, String>, DataMatrixStorage.ColumnRef> pvalMap) {
        this.pvalMap = pvalMap;
    }

    public void setTstatDataMap(Map<Pair<String, String>, DataMatrixStorage.ColumnRef> tstatMap) {
        this.tstatMap = tstatMap;
    }
}
