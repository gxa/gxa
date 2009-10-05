package uk.ac.ebi.microarray.atlas.dao;

import uk.ac.ebi.microarray.atlas.model.Experiment;

import java.util.List;

/**
 * Actual tests for ATlasDAO, extends AtlasDAOTestCase which does all the handy
 * instantiation of a basic, in memory DB.
 *
 * @author Tony Burdett
 * @date 05-Oct-2009
 */
public class TestAtlasDAO extends AtlasDAOTestCase {
  // actual testing methods start here
  public void testGetAllExperiments() {
    try {
      List<Experiment> experiments = getAtlasDAO().getAllExperiments();

      assertTrue("Wrong number of experiments", experiments.size() == 1);
      for (Experiment exp : experiments) {
        assertTrue("Experiment has wrong ID (" + exp.getExperimentID() + ")",
                   exp.getExperimentID().equals("1"));
        assertTrue("Experiment has wrong accession",
                   exp.getAccession().equals("E-ABCD-1234"));
      }
    }
    catch (Exception e) {
      e.printStackTrace();
      fail();
    }
  }
}
