package uk.ac.ebi.microarray.atlas.netcdf.model;

import uk.ac.ebi.microarray.atlas.model.ArrayDesign;
import uk.ac.ebi.microarray.atlas.model.Assay;
import uk.ac.ebi.microarray.atlas.model.Experiment;

import java.util.List;

/**
 * A data slice that needs to be processed into a NetCDF.  Each dataslice is
 * primarly indexed around a single ArrayDesign, with links to the Experiment
 * that references it.  All Assays in this experiment that use this array design
 * should also be stored in this DataSlice.  The {@link
 * ucar.nc2.NetcdfFileWriteable} that this data slice is to be encrypted in is
 * also stored.
 * <p/>
 * You should have one data slice per experiment/array design pair.  For a
 * single experiment with multiple array designs, you should have multiple
 * dataslices.  Each dataslice represents one array design, and the assays
 * present that utilise that array design.
 *
 * @author Tony Burdett
 * @date 29-Sep-2009
 */
public class DataSlice {
  private Experiment experiment;
  private ArrayDesign arrayDesign;
  private List<Assay> assays;

  public DataSlice(Experiment experiment,
                   ArrayDesign arrayDesign,
                   List<Assay> assays) {
    this.experiment = experiment;
    this.arrayDesign = arrayDesign;
    this.assays = assays;
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
}
