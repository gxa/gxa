package uk.ac.ebi.ae3.indexbuilder.service;

import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.core.MultiCore;
import org.xml.sax.SAXException;

import uk.ac.ebi.ae3.indexbuilder.IndexBuilder;
import uk.ac.ebi.ae3.indexbuilder.IndexBuilderException;
import uk.ac.ebi.ae3.indexbuilder.magetab.MageTabDocument;
import uk.ac.ebi.ae3.indexbuilder.magetab.MageTabParser;
import uk.ac.ebi.ae3.indexbuilder.magetab.MageTabParserFactory;
import uk.ac.ebi.ae3.indexbuilder.utils.MageTabUtils;

public class IndexBuilderService
{
	private ConfigurationService confService;
	private static final Log log = LogFactory.getLog(IndexBuilderService.class);
	/**
	 * DOCUMENT ME
	 * @param confService
	 */
	public IndexBuilderService(ConfigurationService confService)
	{
		this.confService = confService;
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
        MultiCore.getRegistry().load(confService.getIndexDir(), new File(confService.getIndexDir(), "multicore.xml"));
        SolrServer solr = new EmbeddedSolrServer(IndexBuilder.SOLR_CORE_NAME);
        Collection<File> idfFiles=MageTabUtils.getIdfFiles(confService.getMageDir());
        Iterator<File> itIdfFiles = idfFiles.iterator();
        while (itIdfFiles.hasNext())
        {
        	File idfFile=itIdfFiles.next();
            log.info("Indexing " + idfFile.getAbsolutePath());
            indexMageTab(idfFile, solr);
        	
        }
        UpdateResponse response = solr.commit();
        response = solr.optimize();
       
        MultiCore.getRegistry().shutdown();
    }
	
    private static void indexMageTab(File idfFile, SolrServer solr) throws IOException, SolrServerException, IndexBuilderException {
        MageTabParser mtp = MageTabParserFactory.getParser();
        MageTabDocument mtd = mtp.parseIDF(new FileReader(idfFile));
        
        SolrInputDocument doc = new SolrInputDocument();

        addMageTabFields(doc, mtd.getFields());
        File sdrfFile = MageTabUtils.getSdrfFileForIdf(idfFile);
        //File sdrfFile = new File(idfFile.getAbsolutePath().replace(IndexBuilder.IDF_EXTENSION, IndexBuilder.SDRF_EXTENSION));
        if (sdrfFile.exists()) {
            log.info("Found " + sdrfFile.getAbsolutePath());
            MageTabDocument mtd_sdrf = mtp.parseSDRF(new FileReader(sdrfFile));
            addMageTabFields(doc, mtd_sdrf.getFields());
        }

        doc.addField("exp_accession", idfFile.getName().replace(IndexBuilder.IDF_EXTENSION,""));
        log.info("ALALALALALALALL" + idfFile.getName().replace(IndexBuilder.IDF_EXTENSION,""));
        UpdateResponse response = solr.add(doc);
    }

    private static void addMageTabFields(SolrInputDocument doc, Map<String, List<String>> mtFields) {
        for (Map.Entry<String, List<String>> entry : mtFields.entrySet()) {
            String fieldName = entry.getKey();
            List<String> fieldValues = entry.getValue();

            for ( String val : fieldValues ) {
                doc.addField(fieldName, val);
                log.info(fieldName);
            }
        }
    }

}
