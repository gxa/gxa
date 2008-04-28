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

import uk.ac.ebi.ae3.indexbuilder.dao.ExperimentDwJdbcDao;
import uk.ac.ebi.ae3.indexbuilder.dao.ExperimentJdbcDao;
import uk.ac.ebi.ae3.indexbuilder.model.Experiment;
import uk.ac.ebi.ae3.indexbuilder.utils.XmlUtil;
/**
 * 
 * Class description goes here.
 *
 * @version 	1.0 2008-04-01
 * @author 	Miroslaw Dylag
 */
public class IndexBuilderFromDb extends IndexBuilderService
{
    	/** */
	private ExperimentJdbcDao experimentDao;
	private ExperimentDwJdbcDao experimentDwDao;
	
	/**
	 * 
	 * @param confService
	 * @throws ParserConfigurationException
	 * @throws IOException
	 * @throws SAXException
	 */
	public IndexBuilderFromDb(ConfigurationService confService) throws ParserConfigurationException, IOException, SAXException
	{
		super(confService);
	}

	/**
	 * 
	 */
	@Override
	protected void createIndexDocs() throws Exception
	{
			Collection<Experiment> colExp=experimentDao.getExperiments();		
			Iterator<Experiment> it=colExp.iterator();
			while (it.hasNext())
			{
				Experiment exp=it.next();
				String xml=experimentDao.getExperimentAsXml(exp);
				String xmlDw=experimentDwDao.getExperimentAsXml(exp);
				System.out.println("DW xml: " + xmlDw);
				SolrInputDocument doc = null;
				doc = XmlUtil.createSolrInputDoc(xml);
				XmlUtil.addExperimentFromDW(xmlDw, doc);
				if (doc!=null)
				{
					getSolrEmbededIndex().addDoc(doc);
				}

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

	public ExperimentDwJdbcDao getExperimentDwDao()
	{
		return experimentDwDao;
	}

	public void setExperimentDwDao(ExperimentDwJdbcDao experimentDwDao)
	{
		this.experimentDwDao = experimentDwDao;
	}
	


}
