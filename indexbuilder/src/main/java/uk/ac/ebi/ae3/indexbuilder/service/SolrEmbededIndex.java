/**
 * 
 */
package uk.ac.ebi.ae3.indexbuilder.service;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.PhraseQuery;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.core.MultiCore;
import org.apache.solr.core.SolrCore;
import org.apache.solr.request.LocalSolrQueryRequest;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.request.SolrQueryResponse;
import org.apache.solr.request.SolrRequestHandler;
import org.apache.solr.search.DocIterator;
import org.apache.solr.search.DocList;
import org.xml.sax.SAXException;

import uk.ac.ebi.ae3.indexbuilder.Constants;
import uk.ac.ebi.ae3.indexbuilder.IndexException;

/**
 * 
 * SolrEmbededIndex provides methods which help initialize, close SolrServer , SolrCore etc.
 * The class creates instances SolrCore for genes and experiments indexes.
 * @version 	1.0 2008-04-02
 * @author 	Miroslaw Dylag
 */
public class SolrEmbededIndex {
    /** The handle to the SolrServer */
    private SolrServer solrServer;
    /** The handle to the SolrCore. The core name is "expt" */
    private SolrCore exptSolrCore;
    /** The handle to the {@link MultiCore} */
    private MultiCore multiCore;
    /** The directory to the "multicore.xml" file */
    private String indexDir;
    /** Status of initialization SolrServer*/
    private boolean init = false;
    /**
     * Constructs a new instance of this class.
     * @param indexDir - a directory to where is the file multicore.xml 
     */
    public SolrEmbededIndex(String indexDir)
    {
	this.indexDir = indexDir;
    }
    
    /**
     * 
     * @throws ParserConfigurationException
     * @throws IOException
     * @throws SAXException
     * @throws IndexException 
     */
    public void init() throws ParserConfigurationException, IOException, SAXException, IndexException
    {
     if (!init)
     {
       this.multiCore = new MultiCore(indexDir, new File(indexDir, Constants.VAL_INDEXFILE));
       this.exptSolrCore = multiCore.getCore(Constants.SOLR_CORE_NAME_EXPT);		
       this.solrServer = new EmbeddedSolrServer(exptSolrCore);
       init = true;
     }
     else
     {
	 throw new IndexException("Index was initialized. Try invoke dispose and init");
     }
	
    }
    /**
     * Disposes of the Solr resources. The method shutdown multicore and close all SolrCore
     * instances.
     */
    public void dispose()
    {
       exptSolrCore.close();
       multiCore.shutdown();
       init = false;
    }

    public boolean isInit() {
        return init;
    }

    public void commit() throws SolrServerException, IOException
    {
	solrServer.optimize();
	solrServer.commit();
    }
    
    public void addDoc(SolrInputDocument doc) throws SolrServerException, IOException
    {
	solrServer.add(doc);
    }

    public long getCount(String queryStr) throws SolrServerException
    {
	
    	SolrQuery q = new SolrQuery(queryStr);
    	long count=0;
    	int start = 0 ;
    	q.setRows(1);
    	q.setStart(start);
    	QueryResponse queryResponse = solrServer.query(q);
    	SolrDocumentList l=queryResponse.getResults();
    	count=l.getNumFound();
	
    	return count;
	
    }

    public SolrDocumentList search(String queryStr, int start, int rows) throws SolrServerException
    {
    	SolrQuery q = new SolrQuery(queryStr);
    	//QueryParser parser = new QueryParser();
        //q.setHighlight(true);
    	
    	q.setRows(rows);
        //q.addHighlightField(ConfigurationService.FIELD_AEEXP_ACCESSION);
        //q.addHighlightField(ConfigurationService.FIELD_EXP_DESC_TEXT);
        /*q.addHighlightField("gene_goterm");
        q.addHighlightField("gene_interproterm");
        q.addHighlightField("gene_keyword");
        q.addHighlightField("gene_name");
        q.addHighlightField("gene_synonym");*/
        //q.setHighlightSnippets(100);
    	q.setStart(start);

    	QueryResponse queryResponse = solrServer.query(q);
    	Map<String,Map<String,List<String>>> map = 	queryResponse.getHighlighting();
    	
    	SolrDocumentList l=queryResponse.getResults();
    	return l;
    }
    

    
    /*public DocList search(String query) throws CorruptIndexException, IOException
    {
	SolrRequestHandler handler = exptSolrCore.getRequestHandler("");
	HashMap map = new HashMap();
	//LocalSolrQueryRequest.emptyArgs
	SolrQueryRequest queryRequest = new LocalSolrQueryRequest(exptSolrCore, query, "standard",0,10000,map);
	SolrQueryResponse queryResponse = new SolrQueryResponse();
	this.exptSolrCore.execute(handler, queryRequest, queryResponse);
	DocList docs = (DocList) queryResponse.getValues().get("response");
	IndexReader reader = this.exptSolrCore.getSearcher().get().getReader();
	DocIterator it = docs.iterator();
	while (it.hasNext())
	{
	  Document doc = reader.document(it.next());
	  if (doc.getField("desc_text")!= null)
	  {
	     System.out.println("Gogog");
	  }
	}
	queryRequest.close();
	return docs;

    }*/
}
