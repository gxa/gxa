package uk.ac.ebi.ae3.indexbuilder;
import org.apache.lucene.analysis.WhitespaceAnalyzer;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.KeywordAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Fieldable;
import org.apache.lucene.index.IndexWriter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.lang.StringUtils;
import org.apache.solr.client.solrj.SolrRequest;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.core.SolrCore;
import org.apache.solr.core.MultiCore;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.xml.sax.SAXException;

import uk.ac.ebi.ae3.indexbuilder.magetab.MageTabDocument;
import uk.ac.ebi.ae3.indexbuilder.magetab.MageTabParser;
import uk.ac.ebi.ae3.indexbuilder.magetab.MageTabParserFactory;


import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.FilenameFilter;
import java.util.Map;
import java.util.List;


import javax.xml.parsers.ParserConfigurationException;

/**
 * User: ostolop
 * Date: 15-Feb-2008
 * <p/>
 * EBI Microarray Informatics Team (c) 2008
 */
public class IndexBuilder {
    private static final Log log = LogFactory.getLog(IndexBuilder.class);

    public static void main(String[] args) {
        System.out.println("Usage: IndexBuilder <indexDir> <idfFile>\nidfFile can be a directory, in which case all *.idf.txt and *.sdrf.txt will be parsed.");

        if (args.length != 2) System.exit(0);

        String indexDir = args[0];
        String idfFile = args[1];

        log.info("Using " + indexDir + " index dir, " + idfFile + " idf.");

        try {
            MultiCore.getRegistry().load(indexDir, new File(indexDir, "multicore.xml"));
            SolrServer solr = new EmbeddedSolrServer("expt");

            File idf = new File(idfFile);
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
}
