package ae3.service;

import ae3.dao.AtlasDao;
import ae3.dao.AtlasObjectNotFoundException;
import ae3.model.AtlasExperiment;

public class AtlasDaoTest extends AtlasAbstractTest
{
	public void test_getExperiment1() throws AtlasObjectNotFoundException
	{
		  AtlasExperiment exp=AtlasDao.getExperiment("E-MEXP-980");
		  assertNotNull(exp);
		  assertNotNull(exp.getExperimentAccession());
		  assertNotNull(exp.getExperimentId());
		  assertNotNull(exp.getExperimentTypes());
	}
}
