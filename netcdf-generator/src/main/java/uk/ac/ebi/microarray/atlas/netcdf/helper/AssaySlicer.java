package uk.ac.ebi.microarray.atlas.netcdf.helper;

import uk.ac.ebi.microarray.atlas.model.ArrayDesign;
import uk.ac.ebi.microarray.atlas.model.Assay;
import uk.ac.ebi.microarray.atlas.model.Experiment;
import uk.ac.ebi.microarray.atlas.model.Sample;

import java.util.List;
import java.util.concurrent.ExecutorService;

/**
 * Javadocs go here!
 *
 * @author Tony Burdett
 * @date 28-Oct-2009
 */
public class AssaySlicer extends CallableSlicer<Void> {
  // required initial resources
  private final Experiment experiment;
  private final ArrayDesign arrayDesign;
  private final DataSlice dataSlice;

  public AssaySlicer(ExecutorService service, Experiment experiment,
                     ArrayDesign arrayDesign, DataSlice dataSlice) {
    super(service);
    this.experiment = experiment;
    this.arrayDesign = arrayDesign;
    this.dataSlice = dataSlice;
  }

  public Void call() throws Exception {
    // fetch assays for this array
    getLog().debug("Fetching assay data for " + arrayDesign.getAccession());
    List<Assay> assays = getAtlasDAO().getAssaysByExperimentAndArray(
        experiment.getAccession(),
        arrayDesign.getAccession());
    // and store
    dataSlice.storeAssays(assays);

    getLog().debug("Fetching samples data for each assay on " +
        arrayDesign.getAccession());
    for (Assay assay : assays) {
      // fetch samples for this assay
      List<Sample> samples = getAtlasDAO().getSamplesByAssayAccession(
          assay.getAccession());
      for (Sample sample : samples) {
        // and store
        dataSlice.storeSample(assay, sample);
      }
    }

    getLog().debug(
        "Assay and Sample data for " + arrayDesign.getAccession() + " stored");
    return null;
  }
}
