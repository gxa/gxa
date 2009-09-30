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
  private List<Integer> designElementIDs;
  private List<Gene> genes;
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
      for (Assay assay : assays) {
        assayToSampleMapping.put(
            assay,
            getSamplesAssociatedWithAssay(assay.getAccession())
        );
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
}
