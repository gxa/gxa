package uk.ac.ebi.microarray.atlas.netcdf.helper;

import ucar.nc2.NetcdfFileWriteable;
import ucar.nc2.Variable;
import uk.ac.ebi.microarray.atlas.dao.AtlasDAOTestCase;
import uk.ac.ebi.microarray.atlas.model.Experiment;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Javadocs go here.
 *
 * @author Junit Generation Plugin for Maven, written by Tony Burdett
 * @date 07-10-2009
 */
public class TestNetCDFWriter extends AtlasDAOTestCase {
  private File repositoryLocation;

  private Map<DataSlice, NetcdfFileWriteable> dataSlices;

  private NetCDFWriter writer;

  public void setUp() throws Exception {
    super.setUp();

    repositoryLocation = new File(
        "target" + File.separator + "test" + File.separator + "netcdfs");
    if (!repositoryLocation.exists()) {
      repositoryLocation.mkdirs();
    }
    dataSlices = new HashMap<DataSlice, NetcdfFileWriteable>();

    DataSlicer dataSlicer = new DataSlicer(getAtlasDAO());
    Experiment experiment =
        getAtlasDAO().getExperimentByAccession("E-ABCD-1234");
    Set<DataSlice> slices = dataSlicer.sliceExperiment(experiment);

    // map slices for the given experiment
    for (DataSlice dataSlice : slices) {
      String netcdfName =
          experiment.getExperimentID() + "_" +
              dataSlice.getArrayDesign().getArrayDesignID() + ".nc";
      File f = new File(repositoryLocation, netcdfName);
      String netcdfPath = f.getAbsolutePath();
      NetcdfFileWriteable netcdfFile =
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

      dataSlices.put(dataSlice, netcdfFile);
    }

    writer = new NetCDFWriter();
  }

  public void tearDown() throws Exception {
    super.tearDown();

    repositoryLocation = null;
    dataSlices = null;
    writer = null;
  }

  public void testWriteNetCDF() {
    try {
      for (DataSlice dataSlice : dataSlices.keySet()) {
        System.out
            .println("Writing NetCDF for " + dataSlice.toString() + "...\n");

        NetcdfFileWriteable netcdfFile = dataSlices.get(dataSlice);

        // format the netcdf
        writer.writeNetCDF(netcdfFile, dataSlice);

        // todo - now profile the netcdf against dataset

        for (Variable v : netcdfFile.getVariables()) {
          System.out.println("Next variable: " + v.toString());
        }

        System.out.println("-------------\n");
      }
    }
    catch (Exception e) {
      e.printStackTrace();
      fail();
    }
  }
}
