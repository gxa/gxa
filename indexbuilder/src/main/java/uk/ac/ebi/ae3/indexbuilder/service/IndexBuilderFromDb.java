package uk.ac.ebi.ae3.indexbuilder.service;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.solr.client.solrj.SolrServerException;
import org.xml.sax.SAXException;

import uk.ac.ebi.ae3.indexbuilder.IndexBuilderException;

public class IndexBuilderFromDb extends IndexBuilderService
{
	
	public IndexBuilderFromDb(ConfigurationService confService)
	{
		super(confService);
	}

	@Override
	public void buildIndex() throws IOException, SolrServerException,
			ParserConfigurationException, SAXException, IndexBuilderException
	{
		//Get Data from Database
	}
}
