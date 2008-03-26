package uk.ac.ebi.ae3.indexbuilder.service;

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.solr.client.solrj.SolrServerException;
import org.dom4j.DocumentException;
import org.xml.sax.SAXException;

import uk.ac.ebi.ae3.indexbuilder.IndexBuilderException;
import uk.ac.ebi.ae3.indexbuilder.dao.ExperimentJdbcDao;
import uk.ac.ebi.ae3.indexbuilder.model.Experiment;

public class IndexBuilderFromDb extends IndexBuilderService
{
	
	private ExperimentJdbcDao experimentDao;
	public IndexBuilderFromDb(ConfigurationService confService)
	{
		super(confService);
	}

	@Override
	public void buildIndex() throws IOException, SolrServerException,
			ParserConfigurationException, SAXException, IndexBuilderException
	{
		//Get Data from Database
		Collection<Experiment> colExp=experimentDao.getExperiments();
		Iterator<Experiment> it=colExp.iterator();
		while (it.hasNext())
		{
			Experiment exp=it.next();

		}
		Experiment exp = new Experiment();
		exp.setId(1098587655);
		try
		{
			exp=experimentDao.getExperiment(exp);
		}
		catch (DocumentException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		
	}

	public ExperimentJdbcDao getExperimentDao()
	{
		return experimentDao;
	}

	public void setExperimentDao(ExperimentJdbcDao experimentDao)
	{
		this.experimentDao = experimentDao;
	}


}
