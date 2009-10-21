package uk.ac.ebi.microarray.atlas.netcdf.helper;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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
  //  private List<Sample> samples;
  private List<Integer> designElementIDs;
  private Map<Integer, Gene> genes;
  private Map<Integer, List<ExpressionAnalysis>> analysesByDesignElementID;
  private Map<String, List<Sample>> samplesByAssayAcc;
  private Map<Assay, List<Sample>> assayToSampleMapping;

  private Log log = LogFactory.getLog(getClass());

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

  public List<Integer> getDesignElementIDs() {
    if (designElementIDs != null) {
      return designElementIDs;
    }
    else {
      return new ArrayList<Integer>();
    }
  }

  public Map<Integer, Gene> getGenes() {
    if (genes != null) {
      return genes;
    }
    else {
      return new HashMap<Integer, Gene>();
    }
  }

  public Map<Integer, List<ExpressionAnalysis>> getExpressionAnalyses() {
    if (analysesByDesignElementID != null) {
      return analysesByDesignElementID;
    }
    else {
      return new HashMap<Integer, List<ExpressionAnalysis>>();
    }
  }

  public List<Sample> getSamples() {
    // create arraylist
    List<Sample> samples = new ArrayList<Sample>();

    // add all samples
    if (samplesByAssayAcc != null) {
      for (String assayAcc : samplesByAssayAcc.keySet()) {
        for (Sample candidate : samplesByAssayAcc.get(assayAcc)) {
          // lookup sample to see if we already have one with this accession in the list
          boolean present = false;
          for (Sample sample : samples) {
            if (sample.getAccession().equals(candidate.getAccession())) {
              present = true;
              break;
            }
          }

          // don't add duplicates
          if (!present) {
            samples.add(candidate);
          }
        }
      }
    }

    return samples;
  }

  public List<Sample> getSamplesAssociatedWithAssay(String assayAccession) {
    if (samplesByAssayAcc != null &&
        samplesByAssayAcc.containsKey(assayAccession)) {
      return samplesByAssayAcc.get(assayAccession);
    }
    else {
      return new ArrayList<Sample>();
    }
  }

  public Map<Assay, List<Sample>> getAssayToSampleMapping() {
    if (assayToSampleMapping == null) {
      assayToSampleMapping =
          new HashMap<Assay, List<Sample>>();
      if (assays != null) {
        for (Assay assay : assays) {
          assayToSampleMapping.put(
              assay,
              getSamplesAssociatedWithAssay(assay.getAccession())
          );
        }
      }
    }

    return assayToSampleMapping;
  }

  public void storeAssays(List<Assay> assays) {
    this.assays = assays;
  }

  public void storeDesignElementIDs(List<Integer> designElementIDs) {
    this.designElementIDs = designElementIDs;
  }

  /**
   * Stores the given collection of genes, indexed by the design element to
   * which they belong.  If a list of design element ids has already been
   * stored, the map supplied should have the same set of keys as those found in
   * the list of design elements, and should be indexed the same.
   *
   * @param designElementID the design element id for this gene
   * @param gene            the gene to store, indexed by the design element id
   */
  public void storeGene(Integer designElementID,
                        Gene gene) {
    if (genes == null) {
      genes = new HashMap<Integer, Gene>();
    }

    if (designElementIDs != null &&
        !designElementIDs.contains(designElementID)) {
      log.warn("Cannot store gene " + gene.getIdentifier() +
          ": design element ID " + designElementID + " not found!");
    }
    else {
      genes.put(designElementID, gene);
    }
  }

  /**
   * Stores the given collection of ExpressionAnalysis objects, indexed  by the
   * design element id to which they belong.  If a list of design element ids
   * has already been stored, the supplied design element id should be found in
   * this map.
   *
   * @param designElementID the design element id for this collection of
   *                        analyses
   * @param analyses        the list of analyses to store.
   */
  public void storeExpressionAnalyses(Integer designElementID,
                                      List<ExpressionAnalysis> analyses) {
    if (analysesByDesignElementID == null) {
      analysesByDesignElementID =
          new HashMap<Integer, List<ExpressionAnalysis>>();
    }

    if (designElementIDs != null &&
        !designElementIDs.contains(designElementID)) {
      log.warn("Design element ID: " + designElementID + " not found!");
    }
    else {
      if (analysesByDesignElementID.containsKey(designElementID)) {
        analysesByDesignElementID.get(designElementID).addAll(analyses);
      }
      else {
        analysesByDesignElementID.put(designElementID, analyses);
      }
    }
  }

  public void storeSamplesAssociatedWithAssay(String assayAccession,
                                              List<Sample> samples) {
    if (samplesByAssayAcc == null) {
      samplesByAssayAcc = new HashMap<String, List<Sample>>();
    }

    if (samplesByAssayAcc.containsKey(assayAccession)) {
      samplesByAssayAcc.get(assayAccession).addAll(samples);
    }
    else {
      samplesByAssayAcc.put(assayAccession, samples);
    }
  }

  public void reset() {
    // reset any lists that are lazily created after storing
    this.assays = null;
    this.designElementIDs = null;
    this.genes = null;
    this.samplesByAssayAcc = null;
    this.assayToSampleMapping = null;
  }

  public String toString() {
    return "NetCDF_DataSlice{" +
        "E:" + experiment.getAccession() + "_" +
        "A:" + arrayDesign.getAccession() + '}';
  }
}
