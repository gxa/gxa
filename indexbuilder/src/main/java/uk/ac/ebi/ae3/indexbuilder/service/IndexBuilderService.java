package uk.ac.ebi.ae3.indexbuilder.service;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrInputDocument;
import org.xml.sax.SAXException;

import uk.ac.ebi.ae3.indexbuilder.IndexBuilderException;

public abstract class IndexBuilderService
{

	private ConfigurationService confService;
	/** */
	protected static final Log log = LogFactory.getLog(IndexBuilderService.class);
	public static final String ACCESION_NUMBER="exp_accession";
	public static final String TITLE="Investigation Title";
	public static final String SPECIE="Characteristics [Organism]";
	public static final String[] idfFields={TITLE,"Experiment Description","Person Last Name","Person First Name","Experimental Design"};
	public static final String[] sdrfFields={SPECIE,"Array Design REF","Protocol REF","Characteristics[CellLine]",
											  "Factor Value [EF1](genotype)","Publication Title","Publication Author List","Publication Status","Publication Status Term Source REF"};


	public IndexBuilderService(ConfigurationService confService)
	{
		this.confService = confService;
	}

    /**
     * DOCUMENT ME
     * @param doc       - 
     * @param mtFields  -
     * @param idxfields - 
     */
    protected static void addMageTabFields(SolrInputDocument doc, Map<String, List<String>> mtFields, String[] idxfields) {
        for (Map.Entry<String, List<String>> entry : mtFields.entrySet()) {
            String fieldName = entry.getKey();
            List<String> fieldValues = entry.getValue();

            for ( String val : fieldValues ) {
            	if (existsInIndex(fieldName, idxfields))
            	{
            		doc.addField(fieldName, val);
            	}
            }
        }
    }

    protected static boolean existsInIndex(final String field, final String[] idxFields)
    {
    	for (String val : idxFields)
    	{
    		if (val.equals(field))
    			return true;
    	}
    	return false;
    }

	public abstract void buildIndex() throws IOException, SolrServerException, ParserConfigurationException, SAXException, IndexBuilderException;

	public ConfigurationService getConfService()
	{
		return confService;
	}

}
