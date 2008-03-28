/**
 * EBI Microarray Informatics Team (c) 2007-2008
 */
package uk.ac.ebi.ae3.indexbuilder.service;

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.solr.common.SolrInputDocument;
import org.dom4j.DocumentException;
import org.xml.sax.SAXException;

import uk.ac.ebi.ae3.indexbuilder.dao.ExperimentJdbcDao;
import uk.ac.ebi.ae3.indexbuilder.model.Experiment;
import uk.ac.ebi.ae3.indexbuilder.utils.XmlUtil;

public class IndexBuilderFromDb extends IndexBuilderService
{
	
	private ExperimentJdbcDao experimentDao;
	
	public IndexBuilderFromDb(ConfigurationService confService) throws ParserConfigurationException, IOException, SAXException
	{
		super(confService);
	}

	@Override
	protected void createIndexDocs() throws Exception
	{
			Collection<Experiment> colExp=experimentDao.getExperiments();
		
			Iterator<Experiment> it=colExp.iterator();
			while (it.hasNext())
			{
				Experiment exp=it.next();
				String xml=experimentDao.getExperimentAsXml(exp);
				SolrInputDocument doc = null;
				doc = XmlUtil.createSolrInputDoc(xml);
				if (doc!=null)
				{
					response = solr.add(doc);
					System.out.println("add ");
				}

			}

		
	}
	
	private void addField()
	{
		
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
