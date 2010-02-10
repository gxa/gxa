package uk.ac.ebi.gxa.netcdf.generator.helper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.microarray.atlas.model.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A slice of data, organised in the way that the NetCDFs are formatted.  This means a single data slice references a
 * single experiment and array design pair, plus stores the set of assays in this experiment that utilise the given
 * array design.  It also references every sample that is upstream of each assay in this experiment - this will almost
 * always be a one to one mapping.
 *
 * @author Tony Burdett
 * @date 30-Sep-2009
 */
public class DataSlice {
    private final Experiment experiment;
    private final ArrayDesign arrayDesign;
    private List<Assay> assays;
    private List<Sample> samples;
    private Map<Integer, Map<Integer, Float>> expressionValues;
    // maps of indexed things
    private Map<Assay, List<Sample>> samplesMap;
    // maps of properties
    private Map<String, List<String>> experimentFactorMap;
    private Map<String, List<String>> sampleCharacteristicMap;

    // logger
    private Logger log = LoggerFactory.getLogger(getClass());

    public DataSlice(Experiment experiment, ArrayDesign arrayDesign) {
        this.experiment = experiment;
        this.arrayDesign = arrayDesign;
    }

    public synchronized Experiment getExperiment() {
        return experiment;
    }

    public synchronized ArrayDesign getArrayDesign() {
        return arrayDesign;
    }

    public synchronized List<Assay> getAssays() {
        if (assays != null) {
            return assays;
        }
        else {
            return new ArrayList<Assay>();
        }
    }

    public synchronized Map<Integer, String> getDesignElements() {
        return arrayDesign.getDesignElements();
    }

    public synchronized Map<Integer, List<Integer>> getGeneMapping() {
        return arrayDesign.getGenes();
    }

    public synchronized List<Sample> getSamples() {
        if (samples != null) {
            return samples;
        }
        else {
            return new ArrayList<Sample>();
        }
    }

    public synchronized Map<Integer, Map<Integer, Float>> getExpressionValues() {
        if (expressionValues != null) {
            return expressionValues;
        }
        else {
            return new HashMap<Integer, Map<Integer, Float>>();
        }
    }

    /**
     * Returns a list of samples, keyed by the assays they are related to.  The ordering of the list of samples for each
     * assay reflects the order in which they were stored: this is guaranteed to be the same ordering as the list of
     * samples.
     *
     * @return a map of lists of samples, indexed by the assay they're related to. This will be a one-to-one mapping in
     *         most cases.
     */
    public synchronized Map<Assay, List<Sample>> getSampleMappings() {
        if (samplesMap != null) {
            return samplesMap;
        }
        else {
            return new HashMap<Assay, List<Sample>>();
        }
    }

    /**
     * Returns a list of strings representing the Experiment Factor Values ("EFVs") for this data slice, indexed by the
     * Experiment Factor Name ("EF") it is declared to be a value for.  Essentially, this reflects the mappings between
     * unique property names and the set of values for all assay properties that are declared "isFactorValue() == true"
     * for this data slice.
     *
     * @return the mapping between unique EFs and the set of EFVs for this data slice
     */
    public synchronized Map<String, List<String>> getExperimentFactorMappings() {
        if (experimentFactorMap == null) {
            evaluatePropertyMappings();
        }

        return experimentFactorMap;
    }

    /**
     * Returns a list of strings representing the Sample Characteristic Values ("SCVs") for this data slice, indexed by
     * the Sample Characteristic Name ("SC") it is declared to be a value for.  Essentially, this reflects the mappings
     * between unique property names and the set of values for all sample properties for this data slice.
     *
     * @return the mapping between unique SCs and the set of SCVs for this data slice
     */
    public synchronized Map<String, List<String>> getSampleCharacteristicMappings() {
        if (sampleCharacteristicMap == null) {
            evaluatePropertyMappings();
        }

        return sampleCharacteristicMap;
    }

    /**
     * Stores a list of assays for this data slice.  This list should be the list of assays that belong to the
     * experiment/array design pair for this data slice.
     *
     * @param assays the list of assays to store
     */
    public synchronized void storeAssays(List<Assay> assays) {
        this.assays = assays;
    }

    /**
     * Stores the given sample, indexed by the assay to which it belongs.  If a list of assays has already been stored,
     * the supplied assay should be found in this list. A DataSlicingException will be raised if there are no stored
     * assays, or if you attempt to store a Sample for an unknown assay.  It is legal to store several Samples for the
     * same assay.
     *
     * @param assay  the assay that indexes this sample
     * @param sample the sample to store for the given assay
     * @throws DataSlicingException if you store a sample for an unknown assay, or if no assays are stored.
     */
    public synchronized void storeSample(Assay assay, Sample sample)
            throws DataSlicingException {
        if (assays == null) {
            throw new DataSlicingException("Can't store " + sample + ": assay index has not been initialized!");
        }
        else {
            // ok to initialize
            if (samplesMap == null) {
                samplesMap = new HashMap<Assay, List<Sample>>();
            }
            if (samples == null) {
                samples = new ArrayList<Sample>();
            }

            // now check integrity
            if (!assays.contains(assay)) {
                throw new DataSlicingException("Can't store " + sample + ": assay " + assay + " absent from index");
            }
            else {
                // add sample to the list (unless it's already added)
                if (!samples.contains(sample)) {
                    samples.add(sample);
                }
                // and add to the map indexed by assayID
                if (samplesMap.containsKey(assay)) {
                    samplesMap.get(assay).add(sample);
                }
                else {
                    List<Sample> samples = new ArrayList<Sample>();
                    samples.add(sample);
                    samplesMap.put(assay, samples);
                }
            }
        }
    }

    /**
     * Stores all the expression values for a data slcie (i.e. expression values for a pair of experiment and
     * arraydesign).
     *
     * @param expressionValues the expression values to store
     */
    public synchronized void storeExpressionValues(
            Map<Integer, Map<Integer, Float>> expressionValues) {
        this.expressionValues = expressionValues;
    }

    public synchronized void evaluatePropertyMappings() {
        // maps property names to all values for assay properties
        experimentFactorMap = new HashMap<String, List<String>>();
        // maps property names to all values for sample properties
        sampleCharacteristicMap = new HashMap<String, List<String>>();

        // iterate over assays, create keys for the map
        for (Assay assay : assays) {
            // check properties for next assay
            for (Property prop : assay.getProperties()) {
                // seen this one before?
                if (!experimentFactorMap.containsKey(prop.getName())) {
                    // if not, start a new list and add it, keyed by the new name
                    List<String> propertyNames = new ArrayList<String>();
                    experimentFactorMap.put(prop.getName(), propertyNames);
                }
            }
        }

        // now check all assays for values
        for (Assay assay : assays) {
            // iterate over known properties
            for (String propName : experimentFactorMap.keySet()) {
                // do next property
                boolean located = false;
                for (Property prop : assay.getProperties()) {
                    if (prop.getName().equals(propName)) {
                        // we only care about factor values, not other properties
                        // fixme: wrong for data in DB right now
                        experimentFactorMap.get(propName).add(prop.getValue());
                        located = true;
                        break;
                    }
                }

                if (!located) {
                    // missing property value, add an empty string to ensure ordering is still correct
                    log.warn("Assay " + assay.getAccession() + " [experiment " + experiment.getAccession() +
                            "] has no property value associated with the property " + propName +
                            ".  Mapped records will be empty.");
                    experimentFactorMap.get(propName).add("");
                }
            }
        }

        // iterate over samples, create keys for the map
        for (Sample sample : samples) {
            // check properties for next sample
            for (Property prop : sample.getProperties()) {
                // seen this one before?
                if (!sampleCharacteristicMap.containsKey(prop.getName())) {
                    // if not, start a new list and add it, keyed by the new name
                    List<String> propertyNames = new ArrayList<String>();
                    sampleCharacteristicMap.put(prop.getName(), propertyNames);
                }
            }
        }

        // now check all samples for values
        for (Sample sample : samples) {
            // iterate over known properties
            for (String propName : sampleCharacteristicMap.keySet()) {
                // do next property
                boolean located = false;
                for (Property prop : sample.getProperties()) {
                    // found the property, so add its value
                    if (prop.getName().equals(propName)) {
                        sampleCharacteristicMap.get(propName).add(prop.getValue());
                        located = true;
                        break;
                    }
                }

                if (!located) {
                    // missing property value, add an empty string to ensure ordering is still correct
                    log.warn("Sample " + sample.getAccession() + " [experiment " + experiment.getAccession() +
                            "] has no property value associated with the property " + propName +
                            ".  Mapped records will be empty.");
                    sampleCharacteristicMap.get(propName).add("");
                }
            }
        }
    }

    /**
     * Clears all the data stored in this data slice.  All collections and populated maps will be reset to null.
     */
    public synchronized void reset() {
        // reset any lists that are lazily created after storing
        this.assays = null;
        // lists of indexed things
        this.samples = null;
        // maps of indexed things
        this.samplesMap = null;
    }

    public String toString() {
        return "NetCDF_DataSlice{" +
                "E:" + experiment.getAccession() + "_" +
                "A:" + arrayDesign.getAccession() + '}';
    }
}
