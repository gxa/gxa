package uk.ac.ebi.microarray.atlas.netcdf;

import org.dbunit.dataset.ITable;
import uk.ac.ebi.microarray.atlas.dao.AtlasDAOTestCase;

import java.io.File;

/**
 * Javadocs go here.
 *
 * @author Junit Generation Plugin for Maven, written by Tony Burdett
 * @date 07-10-2009
 */
public class TestDefaultNetCDFGenerator extends AtlasDAOTestCase {
  private DefaultNetCDFGenerator netCDFGenerator;
  private File repoLocation;

  public void setUp() throws Exception {
    super.setUp();

    repoLocation = new File("test" + File.separator + "netcdfs");

    netCDFGenerator = new DefaultNetCDFGenerator();
    netCDFGenerator.setAtlasDataSource(getDataSource());
    netCDFGenerator.setRepositoryLocation(repoLocation);
  }

  public void tearDown() throws Exception {
    super.tearDown();

    netCDFGenerator.shutdown();

    // delete the repo
    if (repoLocation.exists() && !deleteDirectory(repoLocation)) {
      fail("Failed to delete " + repoLocation.getAbsolutePath());
    }
    repoLocation = null;
  }

  public void testStartup() {
    try {
      // start the netcdf generator
      netCDFGenerator.startup();

      // check the net cdf directory is created
      assertTrue("NetCDF repository wasn't created", repoLocation.exists());

      // try repeat startups
      netCDFGenerator.startup();
    }
    catch (NetCDFGeneratorException e) {
      e.printStackTrace();
      fail();
    }
  }

  public void testShutdown() {
    try {
      // shutdown the netcdf generator without starting up
      netCDFGenerator.shutdown();

      // startup
      netCDFGenerator.startup();

      // and now shutdown
      netCDFGenerator.shutdown();

      // nothing to test
    }
    catch (NetCDFGeneratorException e) {
      e.printStackTrace();
      fail();
    }
  }

  public void testGenerateNetCDFs() {
    try {
      netCDFGenerator.startup();
      netCDFGenerator.generateNetCDFs();
    }
    catch (Exception e) {
      e.printStackTrace();
      fail();
    }
  }

  public void testGenerateNetCDFsForExperiment() {
    try {
      // get the first experiment from our test dataset
      ITable expts = getDataSet().getTable("A2_EXPERIMENT");

      if (expts.getRowCount() > 0) {
        String exptAccession = expts.getValue(0, "accession").toString();

        netCDFGenerator.startup();
        netCDFGenerator.generateNetCDFsForExperiment(exptAccession);
      }
    }
    catch (Exception e) {
      e.printStackTrace();
      fail();
    }
  }

  private boolean deleteDirectory(File directory) {
    boolean success = true;
    if (directory.exists()) {
      for (File file : directory.listFiles()) {
        if (file.isDirectory()) {
          success = success && deleteDirectory(file);
        }
        else {
          success = success && file.delete();
        }
      }
    }
    return success && directory.delete();
  }
}
