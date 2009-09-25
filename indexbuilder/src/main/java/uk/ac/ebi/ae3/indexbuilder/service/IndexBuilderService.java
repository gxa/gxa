/**
 * EBI Microarray Informatics Team (c) 2007-2008
 */
package uk.ac.ebi.ae3.indexbuilder.service;

import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.ae3.indexbuilder.IndexBuilderException;
import uk.ac.ebi.microarray.atlas.dao.AtlasDAO;

import java.io.IOException;

/**
 * An abstract IndexBuilderService, that provides convenience methods for
 * getting and setting parameters required across all SOLR index building
 * implementations.  This class contains a single method, {@link #buildIndex()}
 * that clients should use to construct the different types of index in a
 * consistent manner.  Implementing classes have access to an {@link
 * org.apache.solr.client.solrj.embedded.EmbeddedSolrServer} to update the
 * index, and an {@link uk.ac.ebi.microarray.atlas.dao.AtlasDAO} that provides
 * interaction with the Atlas database (following an Atlas 2 schema).
 * <p/>
 * All implementing classes should implement the method {@link
 * #createIndexDocs()} which contains the logic for constructing the relevant
 * parts of the index for each implementation.  Implementations do not need to
 * be concerned with the SOLR index lifecycle, as this is handled by this
 * abstract classes and {@link uk.ac.ebi.ae3.indexbuilder.IndexBuilder}
 * implementations.
 *
 * @author mdylag
 * @author Tony Burdett (atlas 2 revision)
 */
public abstract class IndexBuilderService {
  private AtlasDAO atlasDAO;
  private EmbeddedSolrServer solrServer;

  private boolean updateMode = false;
  private boolean pendingExps = false;

  private final Logger log = LoggerFactory.getLogger(this.getClass());

  public IndexBuilderService(AtlasDAO atlasDAO,
                             EmbeddedSolrServer solrServer) {
    this.atlasDAO = atlasDAO;
    this.solrServer = solrServer;
  }

  @Deprecated
  public IndexBuilderService() throws Exception {

  }

  public boolean getUpdateMode() {
    return updateMode;
  }

  public void setUpdateMode(boolean updateMode) {
    this.updateMode = updateMode;
  }

  public void setPendingOnly(boolean pending) {
    this.pendingExps = pending;
  }

  public boolean getPendingOnly() {
    return this.pendingExps;
  }

  protected AtlasDAO getAtlasDAO() {
    return atlasDAO;
  }

  protected EmbeddedSolrServer getSolrServer() {
    return solrServer;
  }

  protected Logger getLog() {
    return log;
  }

  @Deprecated
  private SolrEmbeddedIndex solrEmbeddedIndex;

  @Deprecated
  public SolrEmbeddedIndex getSolrEmbeddedIndex() {
    return solrEmbeddedIndex;
  }

  @Deprecated
  public void setSolrEmbeddedIndex(SolrEmbeddedIndex solrEmbeddedIndex) {
    this.solrEmbeddedIndex = solrEmbeddedIndex;
  }

  /**
   * Build the index for this particular IndexBuilderService implementation.
   * Once the index has been built, this method will automatically commit any
   * changes and release any resources held by the SOLR server.
   *
   * @throws IndexBuilderException if the is a problem whilst generating the
   *                               index
   */
  public void buildIndex() throws IndexBuilderException {
    try {
      createIndexDocs();
      solrServer.commit();
    }
    catch (IOException e) {
      throw new IndexBuilderException(
          "Cannot commit changes to the SOLR server", e);
    }
    catch (SolrServerException e) {
      throw new IndexBuilderException(
          "Cannot commit changes to the SOLR server - server threw exception",
          e);
    }
  }

  /**
   * Generate the required documents for the SOLR index, as appropriate to this
   * implementation.  This method blocks until all index documents have been
   * created.
   * <p/>
   * Implementations are free to define their own optimization strategy, and it
   * is acceptable to use asynchronous operations.
   *
   * @throws uk.ac.ebi.ae3.indexbuilder.IndexBuilderException
   *          if there is a problem whilst trying to generate the index
   *          documents
   */
  protected abstract void createIndexDocs() throws IndexBuilderException;
}
