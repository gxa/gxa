/**
 * EBI Microarray Informatics Team (c) 2007-2008
 */
package uk.ac.ebi.ae3.indexbuilder.service;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.core.MultiCore;
import org.apache.solr.update.DirectUpdateHandler2;
import org.dom4j.DocumentException;
import org.xml.sax.SAXException;

import uk.ac.ebi.ae3.indexbuilder.IndexBuilderException;
import uk.ac.ebi.ae3.indexbuilder.dao.ExperimentJdbcDao;
import uk.ac.ebi.ae3.indexbuilder.model.Experiment;
import uk.ac.ebi.ae3.indexbuilder.utils.XmlUtil;

public class IndexBuilderFromDb extends IndexBuilderService
{
	
	private ExperimentJdbcDao experimentDao;
	private UpdateResponse response;
	org.apache.solr.client.solrj.SolrServer solr;
	
	public IndexBuilderFromDb(ConfigurationService confService) throws ParserConfigurationException, IOException, SAXException
	{
		super(confService);
        MultiCore.getRegistry().load(getConfService().getIndexDir(), new File(getConfService().getIndexDir(), ConfigurationService.VAL_INDEXFILE));
        this.solr = new EmbeddedSolrServer(ConfigurationService.SOLR_CORE_NAME);
	}

	@Override
	public void buildIndex() throws Exception
	{
		try
		{
			Collection<Experiment> colExp=experimentDao.getExperiments();
		
			Iterator<Experiment> it=colExp.iterator();
			while (it.hasNext())
			{
				Experiment exp=it.next();
				String xml=experimentDao.getExperimentAsXml(exp);
				SolrInputDocument doc = null;
				try
				{
					doc = XmlUtil.createSolrInputDoc(xml);
				}
				catch (DocumentException e)
				{
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				if (doc!=null)
				{
					response = solr.add(doc);
				}

			}
		}
		catch (Exception e)
		{
			throw new Exception(e);
		}		
		finally
		{
	        dispose();			
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
	
	public void dispose() throws SolrServerException, IOException
	{
        response = solr.commit();
        response = solr.optimize();
        MultiCore.getRegistry().shutdown();		
	}


}
