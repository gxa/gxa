package uk.ac.ebi.microarray.atlas.loader.cache;

import uk.ac.ebi.microarray.atlas.loader.model.Assay;
import uk.ac.ebi.microarray.atlas.loader.model.Experiment;
import uk.ac.ebi.microarray.atlas.loader.model.Sample;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * A cache of objects that need to be loaded into the Atlas DB.  This
 * temporarily stores objects during parsing
 *
 * @author Tony Burdett
 * @date 26-Aug-2009
 */
public class AtlasLoadCache {
  private Map<String, Experiment> experimentsByAcc;
  private Map<String, Assay> assaysByAcc;
  private Map<String, Sample> samplesByAcc;

  public AtlasLoadCache() {
    this.experimentsByAcc = new HashMap<String, Experiment>();
    this.assaysByAcc = new HashMap<String, Assay>();
    this.samplesByAcc = new HashMap<String, Sample>();
  }

  public synchronized void addExperiment(Experiment experiment) {
    if (experiment.getAccession() == null) {
      throw new NullPointerException(
          "Cannot add experiment with null accession!");
    }
    experimentsByAcc.put(experiment.getAccession(), experiment);
  }

  public synchronized Experiment fetchExperiment(String accession) {
    return experimentsByAcc.get(accession);
  }

  public synchronized Collection<Experiment> fetchAllExperiments() {
    return experimentsByAcc.values();
  }

  public synchronized void addAssay(Assay assay) {
    if (assay.getAccession() == null) {
      throw new NullPointerException(
          "Cannot add experiment with null accession!");
    }
    assaysByAcc.put(assay.getAccession(), assay);
  }

  public synchronized Assay fetchAssay(String accession) {
    return assaysByAcc.get(accession);
  }

  public synchronized Collection<Assay> fetchAllAssays() {
    return assaysByAcc.values();
  }

  public synchronized void addSample(Sample sample) {
    if (sample.getAccession() == null) {
      throw new NullPointerException("Cannot add sample with null accession!");
    }
    samplesByAcc.put(sample.getAccession(), sample);
  }

  public synchronized Sample fetchSample(String accession) {
    return samplesByAcc.get(accession);
  }

  public synchronized Collection<Sample> fetchAllSamples() {
    return samplesByAcc.values();
  }

  public synchronized void clear() {
    experimentsByAcc.clear();
    assaysByAcc.clear();
    samplesByAcc.clear();
  }
}
