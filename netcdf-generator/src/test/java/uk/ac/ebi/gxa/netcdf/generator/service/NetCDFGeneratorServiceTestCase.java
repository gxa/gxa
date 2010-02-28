package uk.ac.ebi.gxa.netcdf.generator.service;

import uk.ac.ebi.gxa.dao.AtlasDAOTestCase;
import uk.ac.ebi.gxa.netcdf.generator.service.NetCDFGeneratorService;
import uk.ac.ebi.gxa.utils.FileUtil;

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

    repoLocation = new File(
        System.getProperty("java.io.tmpdir") + File.separator + "test" + File.separator + "netcdfs");
    if (!repoLocation.exists()) {
      repoLocation.mkdirs();
    }
  }

  public void tearDown() throws Exception {
    super.tearDown();

    // delete the repo
    if (!FileUtil.deleteDirectory(repoLocation)) {
      fail("Failed to delete " + repoLocation.getAbsolutePath());
    }

    repoLocation = null;
  }
}
