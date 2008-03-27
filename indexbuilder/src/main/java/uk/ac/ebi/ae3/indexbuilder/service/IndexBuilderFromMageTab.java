/**
 * EBI Microarray Informatics Team (c) 2007-2008
 */
package uk.ac.ebi.ae3.indexbuilder.service;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.core.MultiCore;
import org.xml.sax.SAXException;

import uk.ac.ebi.ae3.indexbuilder.IndexBuilderException;
import uk.ac.ebi.ae3.indexbuilder.magetab.MageTabDocument;
import uk.ac.ebi.ae3.indexbuilder.magetab.MageTabParser;
import uk.ac.ebi.ae3.indexbuilder.magetab.MageTabParserFactory;
import uk.ac.ebi.ae3.indexbuilder.utils.MageTabUtils;

public class IndexBuilderFromMageTab extends IndexBuilderService
{
	/** */
	/**
	 * DOCUMENT ME
	 * @param confService
	 */
	public IndexBuilderFromMageTab(ConfigurationService confService)
	{
		super(confService);
	}
	
	/**
	 * DOCUMENT ME
	 * @throws SolrServerException 
	 * @throws IOException 
	 * @throws SAXException 
	 * @throws ParserConfigurationException 
	 */
	public void buildIndex() throws IOException, SolrServerException, ParserConfigurationException, SAXException, IndexBuilderException
	{
		//String fileAndPath=FilenameUtils.concat(confService.getIndexDir(), "multicore.xml");
        MultiCore.getRegistry().load(getConfService().getIndexDir(), new File(getConfService().getIndexDir(), ConfigurationService.VAL_INDEXFILE));
        SolrServer solr = new EmbeddedSolrServer(ConfigurationService.SOLR_CORE_NAME);
        Collection<File> idfFiles=MageTabUtils.getIdfFiles(getConfService().getMageDir());
        Iterator<File> itIdfFiles = idfFiles.iterator();
        while (itIdfFiles.hasNext())
        {
        	File idfFile=itIdfFiles.next();
            IndexBuilderService.log.info("Indexing " + idfFile.getAbsolutePath());
            indexMageTab(idfFile, solr);
        	
        }
        UpdateResponse response = solr.commit();
        response = solr.optimize();
       
        MultiCore.getRegistry().shutdown();
    }
	
	/**
	 * DOCUMENT ME
	 * @param idfFile
	 * @param solr
	 * @throws IOException
	 * @throws SolrServerException
	 * @throws IndexBuilderException
	 */
    private static void indexMageTab(File idfFile, SolrServer solr) throws IOException, SolrServerException, IndexBuilderException {
        MageTabParser mtp = MageTabParserFactory.getParser();
        
        //Parse IDF file
        MageTabDocument mtd = mtp.parseIDF(new FileReader(idfFile));
        
        SolrInputDocument doc = new SolrInputDocument();

        addMageTabFields(doc, mtd.getFields(), idfFields);
        File sdrfFile = MageTabUtils.getSdrfFileForIdf(idfFile);
        //File sdrfFile = new File(idfFile.getAbsolutePath().replace(IndexBuilder.IDF_EXTENSION, IndexBuilder.SDRF_EXTENSION));
        if (sdrfFile.exists()) {
            IndexBuilderService.log.info("Found " + sdrfFile.getAbsolutePath());
            //Parse SDRF file
            MageTabDocument mtd_sdrf = mtp.parseSDRF(new FileReader(sdrfFile));
            addMageTabFields(doc, mtd_sdrf.getFields(), sdrfFields);
        }

        doc.addField(ConfigurationService.ACCESION_NUMBER, idfFile.getName().replace(ConfigurationService.IDF_EXTENSION,""));
        UpdateResponse response = solr.add(doc);
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

    
    /**
     * 
     */
	
}
