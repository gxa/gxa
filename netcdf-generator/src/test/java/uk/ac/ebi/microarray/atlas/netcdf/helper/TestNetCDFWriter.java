package uk.ac.ebi.microarray.atlas.netcdf.helper;

import ucar.nc2.NetcdfFileWriteable;
import ucar.nc2.Variable;
import uk.ac.ebi.microarray.atlas.dao.AtlasDAOTestCase;
import uk.ac.ebi.microarray.atlas.model.Experiment;

import java.io.File;
import java.util.Set;

/**
 * Javadocs go here.
 *
 * @author Junit Generation Plugin for Maven, written by Tony Burdett
 * @date 07-10-2009
 */
public class TestNetCDFWriter extends AtlasDAOTestCase {
  private File repositoryLocation;

  private DataSlice dataSlice;
  private NetcdfFileWriteable netcdfFile;

  private NetCDFWriter writer;

  public void setUp() throws Exception {
    super.setUp();

    repositoryLocation = new File("test" + File.separator + "netcdfs");

    DataSlicer dataSlicer = new DataSlicer(getAtlasDAO());
    Experiment experiment =
        getAtlasDAO().getExperimentByAccession("E-ABCD-1234");
    Set<DataSlice> slices = dataSlicer.sliceExperiment(experiment);

    // just use first slice for the given experiment
    for (DataSlice slice : slices) {
      if (slice.getArrayDesign().getAccession().equals("A-WXYZ-6789")) {
        // use this one, as it has interesting assay/samples
        dataSlice = slice;
        break;
      }
    }

    String netcdfName =
        experiment.getExperimentID() + "_" +
            dataSlice.getArrayDesign().getArrayDesignID() + ".nc";
    File f = new File(repositoryLocation, netcdfName);
    if (!repositoryLocation.exists()) {
      repositoryLocation.mkdirs();
    }
    String netcdfPath = f.getAbsolutePath();
    netcdfFile =
        NetcdfFileWriteable.createNew(netcdfPath, false);

    // add metadata global attributes
    netcdfFile.addGlobalAttribute(
        "CreateNetCDF_VERSION",
        "test-version");
    netcdfFile.addGlobalAttribute(
        "experiment_accession",
        dataSlice.getExperiment().getAccession());
    netcdfFile.addGlobalAttribute(
        "ADaccession",
        dataSlice.getArrayDesign().getAccession());
    netcdfFile.addGlobalAttribute(
        "ADname",
        dataSlice.getArrayDesign().getName());

    NetCDFFormatter formatter = new NetCDFFormatter();
    formatter.formatNetCDF(netcdfFile, dataSlice);
    netcdfFile.create();

    writer = new NetCDFWriter();
  }

  public void tearDown() throws Exception {
    super.tearDown();

    repositoryLocation = null;
    dataSlice = null;
    netcdfFile = null;
  }

  public void testWriteNetCDF() {
    try {
      // format the netcdf
      writer.writeNetCDF(netcdfFile, dataSlice);

      // todo - now profile the netcdf against dataset

      for (Variable v : netcdfFile.getVariables()) {
        System.out.println("Next variable: " + v.toString());
      }
    }
    catch (Exception e) {
      e.printStackTrace();
      fail();
    }
  }
}
