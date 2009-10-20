package uk.ac.ebi.microarray.atlas.netcdf.service;

import uk.ac.ebi.microarray.atlas.netcdf.NetCDFGeneratorException;

import java.io.File;

/**
 * Javadocs go here.
 *
 * @author Junit Generation Plugin for Maven, written by Tony Burdett
 * @date 07-10-2009
 */
public class TestExperimentNetCDFGeneratorService
    extends NetCDFGeneratorServiceTestCase {
  public void setUp() throws Exception {
    super.setUp();

    ExperimentNetCDFGeneratorService engs =
        new ExperimentNetCDFGeneratorService(getAtlasDAO(), getRepoLocation());
    engs.versionDescriptor = "Atlas NetCDF Experiment Generator Version [TEST]";
    setNetCDFGenerator(engs);
  }

  public void tearDown() throws Exception {
    super.tearDown();

    setNetCDFGenerator(null);
  }

  public void testCreateNetCDFDocs() {
    try {
      getNetCDFGenerator().createNetCDFDocs();

      System.out.println("Wrote NetCDFs to " +
          getRepoLocation().getAbsolutePath());
      System.out.println("Created NetCDFS:");
      for (File f : getRepoLocation().listFiles()) {
        System.out.println("\t" + f.getName());
      }
    }
    catch (NetCDFGeneratorException e) {
      e.printStackTrace();
      fail();
    }
  }
}
