package uk.ac.ebi.gxa.index.builder.service;

import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import uk.ac.ebi.gxa.index.builder.*;

import java.io.IOException;

/**
 * Created by IntelliJ IDEA.
 * User: rpetry
 * Date: Nov 2, 2010
 * Time: 11:46:17 AM
 * This class wraps Index building functionality specific to Solr indexes
 */
public abstract class SolrIndexBuilderService extends IndexBuilderService {
    private SolrServer solrServer;

    final public SolrServer getSolrServer() {
        return solrServer;
    }

    final public void setSolrServer(SolrServer solrServer) {
        this.solrServer = solrServer;
    }

    final protected void deleteAll() throws IndexBuilderException {
        try {
            getSolrServer().deleteByQuery("*:*");
        } catch (IOException e) {
            throw new IndexBuilderException(e);
        }
        catch (SolrServerException e) {
            throw new IndexBuilderException(e);
        }

    }

    final protected void commit() throws IndexBuilderException {
        try {
            getSolrServer().commit();
        } catch (IOException e) {
            throw new IndexBuilderException(
                    "Cannot commit changes to the SOLR server", e);
        }
        catch (SolrServerException e) {
            throw new IndexBuilderException(
                    "Cannot commit changes to the SOLR server - server threw exception",
                    e);
        }

    }

    final protected void optimize() throws IndexBuilderException {
        try {
            getSolrServer().optimize();
        } catch (IOException e) {
            throw new IndexBuilderException(
                    "Cannot commit changes to the SOLR server", e);
        }
        catch (SolrServerException e) {
            throw new IndexBuilderException(
                    "Cannot commit changes to the SOLR server - server threw exception",
                    e);
        }

    }

    public void processCommand(IndexAllCommand indexAll, ProgressUpdater progressUpdater) throws IndexBuilderException {
        deleteAll();
    }

    public void processCommand(UpdateIndexForExperimentCommand updateIndexForExperimentCommand, ProgressUpdater progressUpdater) throws IndexBuilderException {
    }

    public void finalizeCommand(IndexAllCommand indexAll, ProgressUpdater progressUpdater) throws IndexBuilderException {
        commit();
        optimize();
    }

    public void finalizeCommand(UpdateIndexForExperimentCommand updateIndexForExperimentCommand, ProgressUpdater progressUpdater) throws IndexBuilderException {
        commit();
        optimize();
    }

    /**
     * Returns index name, which this service builds
      * @return text string
     */
    public abstract String getName();
}
