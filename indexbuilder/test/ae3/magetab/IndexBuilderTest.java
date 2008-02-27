package ae3.magetab;

import junit.framework.TestCase;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.search.Searcher;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Hits;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.core.SolrCore;
import org.apache.solr.core.SolrConfig;
import org.apache.solr.schema.IndexSchema;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * User: ostolop
 * Date: 19-Feb-2008
 * <p/>
 * EBI Microarray Informatics Team (c) 2007
 */
public class IndexBuilderTest extends TestCase {
    public void testIndexSearch() throws IOException, ParseException {
        IndexReader reader = IndexReader.open("/ebi/work/tmp");
        Searcher searcher = new IndexSearcher(reader);
        Analyzer analyzer = new StandardAnalyzer();

        QueryParser parser = new QueryParser("all", analyzer);
        Query query = parser.parse("OrganismPart");
        System.out.println("Searching for: " + query.toString("all"));
        Hits hits = searcher.search(query);

        System.out.println(hits.length() + " total matching documents");

        boolean raw = true;

        final int HITS_PER_PAGE = 10;
        for (int start = 0; start < hits.length(); start += HITS_PER_PAGE) {
            int end = Math.min(hits.length(), start + HITS_PER_PAGE);
            for (int i = start; i < end; i++) {
                System.out.println("doc=" + hits.id(i) + " score=" + hits.score(i));

                Document doc = hits.doc(i);
                List<Field> fields = doc.getFields();
                for ( Field f : fields ) {
                    System.out.println( "<" + f.name() + ">" + f.stringValue() + "</" + f.name() + ">");
                }
            }
        }


        reader.close();
    }

    public void testSolr() throws SolrServerException, IOException {
        System.setProperty("solr.solr.home", "/ebi/work/solr_idxs/solr_expt");
        SolrCore core = SolrCore.getSolrCore();

        SolrServer solr = new EmbeddedSolrServer(core);
        SolrQuery  solrq = new SolrQuery();

        solrq.setQuery("leukemia");

        QueryResponse solrqr = solr.query(solrq);
        SolrDocumentList documentList = solrqr.getResults();

        SolrInputDocument sdoc = new SolrInputDocument();
        sdoc.addField("foo", "foo");
        UpdateResponse response = solr.add(sdoc);
        response.getStatus();
    }

}
