package uk.ac.ebi.ae3.indexbuilder.service;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.core.CoreContainer;
import org.apache.solr.core.SolrCore;
import org.xml.sax.SAXException;
import uk.ac.ebi.ae3.indexbuilder.Constants;
import uk.ac.ebi.ae3.indexbuilder.IndexException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;

/**
 * SolrEmbeddedIndex provides methods which help initialize, close SolrServer ,
 * SolrCore etc. The class creates instances SolrCore for genes and experiments
 * indexes.
 *
 * @author Miroslaw Dylag
 * @version 1.0 2008-04-02
 */
@Deprecated
public class SolrEmbeddedIndex {
  private SolrServer solrServer;
  private SolrCore exptSolrCore;
  private CoreContainer multiCore;
  private String indexDir;
  private String coreName;

  private boolean init = false;

  @Deprecated
  public SolrEmbeddedIndex(String indexDir, String coreName) {
    this.indexDir = indexDir;
    this.coreName = coreName;
  }

  @Deprecated
  public void init() throws ParserConfigurationException, IOException,
      SAXException, IndexException {
    if (!init) {
      this.multiCore = new CoreContainer(indexDir, new File(
          indexDir, Constants.VAL_INDEXFILE));
      this.exptSolrCore = multiCore.getCore(coreName);
      this.solrServer = new EmbeddedSolrServer(multiCore, coreName);
      init = true;
    }
    else {
      throw new IndexException("Index was initialized. " +
          "Try invoke dispose and init");
    }
  }

  @Deprecated
  public void dispose() {
    exptSolrCore.close();
    multiCore.shutdown();
    init = false;
  }

  @Deprecated
  public boolean isInit() {
    return init;
  }

  @Deprecated
  public void commit() throws SolrServerException, IOException {
    solrServer.optimize();
    solrServer.commit();
  }

  @Deprecated
  public void addDoc(SolrInputDocument doc)
      throws SolrServerException, IOException {
    solrServer.add(doc);
  }

  @Deprecated
  public long getCount(String queryStr) throws SolrServerException {
    SolrQuery q = new SolrQuery(queryStr);
    int start = 0;
    q.setRows(1);
    q.setStart(start);
    QueryResponse queryResponse = solrServer.query(q);
    SolrDocumentList l = queryResponse.getResults();
    return l.getNumFound();
  }

  @Deprecated
  public SolrDocumentList search(String queryStr, int start, int rows)
      throws SolrServerException {
    SolrQuery q = new SolrQuery(queryStr);
    q.setRows(rows);
    q.setStart(start);

    return solrServer.query(q).getResults();
  }
}
