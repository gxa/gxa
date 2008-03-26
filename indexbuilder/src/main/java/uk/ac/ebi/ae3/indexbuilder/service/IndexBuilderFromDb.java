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
        MultiCore.getRegistry().load(getConfService().getIndexDir(), new File(getConfService().getIndexDir(), ConfigurationService.VAL_INDEXFILE));
        org.apache.solr.client.solrj.SolrServer solr = new EmbeddedSolrServer(ConfigurationService.SOLR_CORE_NAME);

        SolrInputDocument doc = new SolrInputDocument();
        doc.addField("exp_accession", "ala");
        UpdateResponse response = solr.add(doc);
        doc.clear();
        doc.addField("exp_accession", "ala1");        
        response = solr.add(doc);

		//Get Data from Database
		/*Collection<Experiment> colExp=experimentDao.getExperiments();
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
		}*/
        response = solr.commit();
        response = solr.optimize();
       
        MultiCore.getRegistry().shutdown();

		
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
