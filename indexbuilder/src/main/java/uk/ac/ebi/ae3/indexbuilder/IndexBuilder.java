package uk.ac.ebi.ae3.indexbuilder;
import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.cli2.Argument;
import org.apache.commons.cli2.CommandLine;
import org.apache.commons.cli2.Group;
import org.apache.commons.cli2.builder.ArgumentBuilder;
import org.apache.commons.cli2.builder.DefaultOptionBuilder;
import org.apache.commons.cli2.builder.GroupBuilder;
import org.apache.commons.cli2.commandline.Parser;
import org.apache.commons.cli2.option.DefaultOption;
import org.apache.commons.cli2.util.HelpFormatter;
import org.apache.commons.configuration.CompositeConfiguration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.core.MultiCore;
import org.xml.sax.SAXException;

import uk.ac.ebi.ae3.indexbuilder.magetab.MageTabDocument;
import uk.ac.ebi.ae3.indexbuilder.magetab.MageTabParser;
import uk.ac.ebi.ae3.indexbuilder.magetab.MageTabParserFactory;
import uk.ac.ebi.ae3.indexbuilder.service.ConfigurationService;

/**
 * User: ostolop, mdylag
 * @version $Id$
 * Date: 15-Feb-2008
 * <p/>
 * EBI Microarray Informatics Team (c) 2008
 */
public class IndexBuilder {
    private static final Log log = LogFactory.getLog(IndexBuilder.class);
    private ConfigurationService confService = new ConfigurationService();
   
    /**
     * DOCUMENT ME
     * @param args
     * @throws org.apache.commons.configuration.ConfigurationException 
     */
    public void parse(String[] args) throws org.apache.commons.configuration.ConfigurationException
    {
        //System.out.println("Usage: IndexBuilder <indexDir> <idfFile>\nidfFile can be a directory, in which case all *.idf.txt and *.sdrf.txt will be parsed.");
    	confService.parseAndConfigure(args);
    }
    /**
     * DOCUMENT ME
     * @throws SAXException 
     * @throws IOException 
     * @throws ParserConfigurationException 
     * @throws SolrServerException 
     */
    public void run() throws ParserConfigurationException, IOException, SAXException, SolrServerException 
    {
        
        //read configuration
    		;
            MultiCore.getRegistry().load(confService.getIndexDir(), new File(confService.getIndexDir(), "multicore.xml"));
            SolrServer solr = new EmbeddedSolrServer("expt");

            File idf = new File(confService.getMageDir());
            if (idf.isDirectory()) {
                File[] idfs = idf.listFiles(new FilenameFilter() {
                    public boolean accept(File dir, String name) {
                        return name.endsWith(".idf.txt");
                    }
                }
                );

                for (File nextidf : idfs) {
                    log.info("Indexing " + nextidf.getAbsolutePath());
                    indexMageTab(nextidf, solr);
                }
            } else {
                indexMageTab(idf, solr);
            }

            UpdateResponse response = solr.commit();
            response = solr.optimize();
            
            MultiCore.getRegistry().shutdown();
    	
    }
    
    private static void indexMageTab(File idfFile, SolrServer solr) throws IOException, SolrServerException {
        MageTabParser mtp = MageTabParserFactory.getParser();
        MageTabDocument mtd = mtp.parseIDF(new FileReader(idfFile));

        SolrInputDocument doc = new SolrInputDocument();

        addMageTabFields(doc, mtd.getFields());

        File sdrfFile = new File(idfFile.getAbsolutePath().replace(".idf.txt", ".sdrf.txt"));
        if (sdrfFile.exists()) {
            log.info("Found " + sdrfFile.getAbsolutePath());
            MageTabDocument mtd_sdrf = mtp.parseSDRF(new FileReader(sdrfFile));
            addMageTabFields(doc, mtd_sdrf.getFields());
        }

        doc.addField("exp_accession", idfFile.getName().replace(".idf.txt",""));
        UpdateResponse response = solr.add(doc);
    }

    private static void addMageTabFields(SolrInputDocument doc, Map<String, List<String>> mtFields) {
        for (Map.Entry<String, List<String>> entry : mtFields.entrySet()) {
            String fieldName = entry.getKey();
            List<String> fieldValues = entry.getValue();

            for ( String val : fieldValues ) {
                doc.addField(fieldName, val);
            }
        }
    }
    
    public static void main(String[] args) 
    {
    	try
    	{
    		IndexBuilder indexBuilder=new IndexBuilder();
    		indexBuilder.parse(args);
    		indexBuilder.run();
    	} catch (ConfigurationException e) {
            log.error(e);    		
    	} catch (java.io.IOException e) {
            log.error(e);
        } catch (SolrServerException e) {
            log.error(e);
        } catch (ParserConfigurationException e) {
            log.error(e);
        } catch (SAXException e) {
            log.error(e);
        }
    }
	
    
}
