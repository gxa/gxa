package uk.ac.ebi.microarray.atlas.netcdf.service;

import uk.ac.ebi.microarray.atlas.dao.AtlasDAOTestCase;

import java.io.File;

/**
 * Javadocs go here.
 *
 * @author Junit Generation Plugin for Maven, written by Tony Burdett
 * @date 07-10-2009
 */
public abstract class NetCDFGeneratorServiceTestCase extends AtlasDAOTestCase {
  private File repoLocation;

  private NetCDFGeneratorService netCDFGenerator;

  public NetCDFGeneratorService getNetCDFGenerator() {
    return netCDFGenerator;
  }

  public void setNetCDFGenerator(NetCDFGeneratorService netCDFGenerator) {
    this.netCDFGenerator = netCDFGenerator;
  }

  public File getRepoLocation() {
    return repoLocation;
  }

  public void setUp() throws Exception {
    super.setUp();

    repoLocation = new File("test" + File.separator + "netcdfs");
    if (!repoLocation.exists()) {
      repoLocation.mkdirs();
    }
  }

  public void tearDown() {
    // delete the index
    if (!deleteDirectory(repoLocation)) {
      fail("Failed to delete " + repoLocation.getAbsolutePath());
    }

    repoLocation = null;
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
