package uk.ac.ebi.microarray.atlas.netcdf.helper;

import uk.ac.ebi.microarray.atlas.model.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A slice of data, organised in the way that the NetCDFs are formatted.  This
 * means a single data slice references a single experiment and array design
 * pair, plus stores the set of assays in this experiment that utilise the given
 * array design.  It also references every sample that is upstream of each assay
 * in this experiment - this will almost always be a one to one mapping.
 *
 * @author Tony Burdett
 * @date 30-Sep-2009
 */
public class DataSlice {
  private Experiment experiment;
  private ArrayDesign arrayDesign;
  private List<Assay> assays;
//  private List<Integer> designElementIDs;
  private Map<Integer, String> designElements;
  // lists of indexed things
  private List<Sample> samples;
  private List<Gene> genes;
  private List<ExpressionAnalysis> analyses;
  private Map<Integer, Map<String, Float>> expressionValues;
  // maps of indexed things
  private Map<Assay, List<Sample>> samplesMap;
  private Map<Integer, Gene> genesMap;
  private Map<Integer, List<ExpressionAnalysis>> analysesMap;
  // maps of properties
  Map<String, List<String>> experimentFactorMap;
  Map<String, List<String>> sampleCharacteristicMap;
  Map<Assay, List<String>> assayFactorValueMap;


  public DataSlice(Experiment experiment, ArrayDesign arrayDesign) {
    this.experiment = experiment;
    this.arrayDesign = arrayDesign;
  }

  public Experiment getExperiment() {
    return experiment;
  }

  public ArrayDesign getArrayDesign() {
    return arrayDesign;
  }

  public List<Assay> getAssays() {
    if (assays != null) {
      return assays;
    }
    else {
      return new ArrayList<Assay>();
    }
  }

//  public List<Integer> getDesignElementIDs() {
//    if (designElementIDs != null) {
//      return designElementIDs;
//    }
//    else {
//      return new ArrayList<Integer>();
//    }
//  }

  public Map<Integer, String> getDesignElements() {
    if (designElements != null) {
      return designElements;
    }
    else {
      return new HashMap<Integer, String>();
    }
  }

  public List<Gene> getGenes() {
    if (genes != null) {
      return genes;
    }
    else {
      return new ArrayList<Gene>();
    }
  }

  public List<ExpressionAnalysis> getExpressionAnalyses() {
    if (analyses != null) {
      return analyses;
    }
    else {
      return new ArrayList<ExpressionAnalysis>();
    }
  }

  public List<Sample> getSamples() {
    if (samples != null) {
      return samples;
    }
    else {
      return new ArrayList<Sample>();
    }
  }

  public Map<Integer, Map<String, Float>> getExpressionValues() {
    if (expressionValues != null) {
      return expressionValues;
    }
    else {
      return new HashMap<Integer, Map<String, Float>>();
    }
  }

  /**
   * Returns a list of samples, keyed by the assays they are related to.  The
   * ordering of the list of samples for each assay reflects the order in which
   * they were stored: this is guaranteed to be the same ordering as the list of
   * samples.
   *
   * @return a map of lists of samples, indexed by the assay they're related to.
   *         This will be a one-to-one mapping in most cases.
   */
  public Map<Assay, List<Sample>> getSampleMappings() {
    if (samplesMap != null) {
      return samplesMap;
    }
    else {
      return new HashMap<Assay, List<Sample>>();
    }
  }

  /**
   * Returns a map of genes, keyed by the design element id they are annotated
   * for. This is a mapping with one-to-one cardinality - design elements cannot
   * be annotated with multiple genes, although the same gene can be annotated
   * against multiple design elements.  Note that some design elements may not
   * be annotated at all, in which case the gene will be null.
   *
   * @return a map of genes, indexed by the design element id they're related
   *         to.
   */
  public Map<Integer, Gene> getGeneMappings() {
    if (genesMap != null) {
      return genesMap;
    }
    else {
      return new HashMap<Integer, Gene>();
    }
  }

  public Map<Integer, List<ExpressionAnalysis>> getExpressionAnalysisMappings() {
    if (analysesMap != null) {
      return analysesMap;
    }
    else {
      return new HashMap<Integer, List<ExpressionAnalysis>>();
    }
  }

  /**
   * Returns a list of strings representing the Experiment Factor Values
   * ("EFVs") for this data slice, indexed by the Experiment Factor Name ("EF")
   * it is declared to be a value for.  Essentially, this reflects the mappings
   * between unique property names and the set of values for all assay
   * properties that are declared "isFactorValue() == true" for this data
   * slice.
   *
   * @return the mapping between unique EFs and the set of EFVs for this data
   *         slice
   */
  public Map<String, List<String>> getExperimentFactorMappings() {
    if (experimentFactorMap == null) {
      evaluatePropertyMappings();
    }

    return experimentFactorMap;
  }

  /**
   * Returns a list of strings representing the Sample Characteristic Values
   * ("SCVs") for this data slice, indexed by the Sample Characteristic Name
   * ("SC") it is declared to be a value for.  Essentially, this reflects the
   * mappings between unique property names and the set of values for all sample
   * properties for this data slice.
   *
   * @return the mapping between unique SCs and the set of SCVs for this data
   *         slice
   */
  public Map<String, List<String>> getSampleCharacteristicMappings() {
    if (sampleCharacteristicMap == null) {
      evaluatePropertyMappings();
    }

    return sampleCharacteristicMap;
  }

  /**
   * Returns a list of strings representing the Experiment Factor Values
   * ("EFVs") for this data slice, indexed by the Assay it is declared to be a
   * EFV for.  This gives a binding between all observed assays and the matched
   * property values found in this data slice.  Note that the list of property
   * values is arranged according to the order of the observed properties, and
   * can contain empty strings when a property has no value.  This means the
   * ordering of property values consistently reflects the order of the known
   * properties across all assays.
   *
   * @return the mappins between assays with declared factor values, and the
   *         list of values observed.
   */
  public Map<Assay, List<String>> getAssayFactorValueMappings() {
    if (assayFactorValueMap == null) {
      evaluatePropertyMappings();
    }

    return assayFactorValueMap;
  }

  /**
   * Stores a list of assays for this data slice.  This list should be the list
   * of assays that belong to the experiment/array design pair for this data
   * slice.
   *
   * @param assays the list of assays to store
   */
  public void storeAssays(List<Assay> assays) {
    this.assays = assays;
  }

  /**
   * Stores the given sample, indexed by the assay to which it belongs.  If a
   * list of assays has already been stored, the supplied assay should be found
   * in this list. A DataSlicingException will be raised if there are no stored
   * assays, or if you attempt to store a Sample for an unknown assay.  It is
   * legal to store several Samples for the same assay.
   *
   * @param assay  the assay that indexes this sample
   * @param sample the sample to store for the given assay
   * @throws DataSlicingException if you store a sample for an unknown assay, or
   *                              if no assays are stored.
   */
  public void storeSample(Assay assay, Sample sample)
      throws DataSlicingException {
    if (assays == null) {
      throw new DataSlicingException("Can't store " + sample + ": " +
          "assay index has not been initialized!");
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
        throw new DataSlicingException("Can't store " + sample + ": " +
            "assay " + assay + " absent from index");
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

//  /**
//   * Stores a list of design element ids for this data slice.  This list should
//   * be the list of design elements that belong to the array design for this
//   * data slice.
//   *
//   * @param designElementIDs the list of design element ids to store
//   */
//  public void storeDesignElementIDs(List<Integer> designElementIDs) {
//    this.designElementIDs = designElementIDs;
//  }

  /**
   * Stores a map containing information about design elements for this data
   * slice.  This map should contain the list of design element ids and the
   * manufacturers design element accession.
   *
   * @param designElements
   */
  public void storeDesignElements(Map<Integer, String> designElements) {
    this.designElements = designElements;
  }

  /**
   * Stores the given gene, indexed by the design element to which it belongs.
   * If a list of design element ids has already been stored, the supplied
   * design element id should be found in this list. A DataSlicingException will
   * be raised if there are no stored design elements, or if you attempt to
   * store a Gene for an unknown design element.  It is not legal to store
   * several Genes for the same design element: if you attempt this, a
   * DataSlicingException will be thrown.
   *
   * @param designElementID the design element id for this gene
   * @param gene            the gene to store, indexed by the design element id
   * @throws DataSlicingException if you store a gene for an unknown design
   *                              element, or if no design elements are stored.
   */
  public void storeGene(
      int designElementID, Gene gene) throws DataSlicingException {
    if (designElements == null) {
      throw new DataSlicingException("Can't store " + gene + ": " +
          "design element index has not been initialized!");
    }
    else {
      // we're ok to initialize
      if (genesMap == null) {
        genesMap = new HashMap<Integer, Gene>();
      }
      if (genes == null) {
        genes = new ArrayList<Gene>();
      }

      // now check integrity
      if (!designElements.containsKey(designElementID)) {
        throw new DataSlicingException("Can't store " + gene + ": " +
            "design element " + designElementID + " absent from index");
      }
      else if (genesMap.containsKey(designElementID) &&
          genesMap.get(designElementID) != gene) {
        // can't store duplicates - one-to-one for design elements to genes
        throw new DataSlicingException("Attempting to overwrite " +
            genesMap.get(designElementID) + " with " + gene + ": " +
            "this attempt will be ignored");
      }
      else {
        // add gene to the list
        genes.add(gene);
        // and add to the map indexed by design element id
        genesMap.put(designElementID, gene);
      }

    }
  }

  /**
   * Stores an ExpressionAnalysis object, indexed  by the design element id to
   * which it belongs.  If a list of design element ids has already been stored,
   * the supplied design element id should be found in this list.  A
   * DataSlicingException will be raised if there are no stored design elements,
   * or if you attempt to store an ExpressionAnalysis for an unknown design
   * element.  It is legal to store several ExpressionAnalyses for the same
   * design element.
   *
   * @param designElementID the design element id for this collection of
   *                        analyses
   * @param analysis        the list of analyses to store.
   * @throws DataSlicingException if you store an analysis for an unknown design
   *                              element, or if no design elements are stored.
   */
  public void storeExpressionAnalysis(
      int designElementID, ExpressionAnalysis analysis)
      throws DataSlicingException {
    if (designElements == null) {
      throw new DataSlicingException("Can't store " + analysis + ": " +
          "design element index has not been initialized!");
    }
    else {
      // we're ok to initialize
      if (analysesMap == null) {
        analysesMap = new HashMap<Integer, List<ExpressionAnalysis>>();
      }
      if (analyses == null) {
        analyses = new ArrayList<ExpressionAnalysis>();
      }

      // now check integrity
      if (!designElements.containsKey(designElementID)) {
        throw new DataSlicingException("Can't store " + analysis + ": " +
            "design element " + designElementID + " absent from index");
      }
      else {
        // add analysis to the list
        analyses.add(analysis);
        // and add to the map indexed by design element id
        if (analysesMap.containsKey(designElementID)) {
          analysesMap.get(designElementID).add(analysis);
        }
        else {
          List<ExpressionAnalysis> analyses =
              new ArrayList<ExpressionAnalysis>();
          analyses.add(analysis);
          this.analysesMap.put(designElementID, analyses);
        }
      }
    }
  }

  /**
   * Stores all the expression values for a data slcie (i.e. expression values
   * for a pair of experiment and arraydesign).
   *
   * @param expressionValues the expression values to store
   */
  public void storeExpressionValues(
      Map<Integer, Map<String, Float>> expressionValues) {
    this.expressionValues = expressionValues;
  }

  public void evaluatePropertyMappings() {
    // maps property names to all values for assay properties
    experimentFactorMap = new HashMap<String, List<String>>();
    // maps property names to all values for sample properties
    sampleCharacteristicMap = new HashMap<String, List<String>>();
    // maps assays to the property values observed; duplicates allowed
    assayFactorValueMap = new HashMap<Assay, List<String>>();

    // check all assays
    for (Assay assay : assays) {
      // list of values for this assay
      List<String> observedPropertyValues = new ArrayList<String>();

      // get all assay properties
      for (Property prop : assay.getProperties()) {
        // we only care about factor values, not other properties
        // fixme: wrong for data in DB right now
//        if (prop.isFactorValue()) {
        // have we seen this property name before?
        if (experimentFactorMap.containsKey(prop.getName())) {
          // if so, add values to the existing list
          experimentFactorMap.get(prop.getName()).add(prop.getValue());
        }
        else {
          // otherwise, start a new list and add it, keyed by the new name
          List<String> propertyNames = new ArrayList<String>();
          propertyNames.add(prop.getValue());
          experimentFactorMap.put(prop.getName(), propertyNames);
        }

        // add the value to the observedProperties list
        observedPropertyValues.add(prop.getValue());
//        }
      }

      // now add all property values to the observedValues map
      assayFactorValueMap.put(assay, observedPropertyValues);
    }

    // check all samples
    for (Sample sample : samples) {
      // get all sample properties
      for (Property prop : sample.getProperties()) {
        // have we seen this property name before?
        if (sampleCharacteristicMap.containsKey(prop.getName())) {
          // if so, add values to the existing list
          sampleCharacteristicMap.get(prop.getName()).add(prop.getValue());
        }
        else {
          // otherwise, start a new list and add it, keyed by the new name
          List<String> propertyNames = new ArrayList<String>();
          propertyNames.add(prop.getValue());
          sampleCharacteristicMap.put(prop.getName(), propertyNames);
        }
      }
    }
  }

  /**
   * Clears all the data stored in this data slice.  All collections and
   * populated maps will be reset to null.
   */
  public void reset() {
    // reset any lists that are lazily created after storing
    this.experiment = null;
    this.arrayDesign = null;
    this.assays = null;
    this.designElements = null;
    // lists of indexed things
    this.samples = null;
    this.genes = null;
    this.analyses = null;
    // maps of indexed things
    this.samplesMap = null;
    this.genesMap = null;
    this.analysesMap = null;
  }

  public String toString() {
    return "NetCDF_DataSlice{" +
        "E:" + experiment.getAccession() + "_" +
        "A:" + arrayDesign.getAccession() + '}';
  }
}
