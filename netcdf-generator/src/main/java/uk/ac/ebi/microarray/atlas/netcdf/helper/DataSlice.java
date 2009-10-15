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
  //  private List<Sample> samples;
  private List<Integer> designElementIDs;
  private List<Gene> genes;
  private Map<Integer, List<ExpressionAnalysis>> analysesByGeneID;
  private Map<String, List<Sample>> samplesByAssayAcc;
  private Map<Assay, List<Sample>> assayToSampleMapping;

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
    return assays;
  }

  public List<Integer> getDesignElementIDs() {
    return designElementIDs;
  }

  public List<Gene> getGenes() {
    return genes;
  }

  public List<ExpressionAnalysis> getExpressionAnalyses() {
    // create arraylist
    List<ExpressionAnalysis> analyses = new ArrayList<ExpressionAnalysis>();

    // add all analyses
    if (analysesByGeneID != null) {
      for (int geneID : analysesByGeneID.keySet()) {
        for (ExpressionAnalysis candidate : analysesByGeneID.get(geneID)) {
          // todo - lookup analysis to see if we already have it in the list?
          boolean present = false;
          for (ExpressionAnalysis analysis : analyses) {
            if (analysis.getEfName().equals(candidate.getEfName()) &&
                analysis.getEfvName().equals(candidate.getEfvName())) {
              present = true;
              break;
            }
          }

          // don't add duplicates
          if (!present) {
            analyses.add(candidate);
          }
          else {
            System.out
                .println("Duplicated EF/EFV pairing for this experiment!");
          }
        }
      }
    }
    else {
      return null;
    }

    return analyses;
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
    else {
      return null;
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

  public void storeGenes(List<Gene> genes) {
    this.genes = genes;
  }

  public void storeExpressionAnalyses(Integer geneID,
                                      List<ExpressionAnalysis> analyses) {
    if (analysesByGeneID == null) {
      analysesByGeneID = new HashMap<Integer, List<ExpressionAnalysis>>();
    }

    if (analysesByGeneID.containsKey(geneID)) {
      analysesByGeneID.get(geneID).addAll(analyses);
    }
    else {
      analysesByGeneID.put(geneID, analyses);
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
