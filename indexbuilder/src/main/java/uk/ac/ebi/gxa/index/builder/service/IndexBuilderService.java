/*
 * Copyright 2008-2010 Microarray Informatics Team, EMBL-European Bioinformatics Institute
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *
 * For further details of the Gene Expression Atlas project, including source code,
 * downloads and documentation, please see:
 *
 * http://gxa.github.com/gxa
 */

package uk.ac.ebi.gxa.index.builder.service;

import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.gxa.index.builder.IndexBuilderException;
import uk.ac.ebi.gxa.index.builder.listener.IndexBuilderListener;
import uk.ac.ebi.gxa.dao.AtlasDAO;

import java.io.IOException;
import java.util.Collection;

/**
 * An abstract IndexBuilderService, that provides convenience methods for getting and setting parameters required across
 * all SOLR index building implementations.  This class contains a single method, {@link #buildIndex(ProgressUpdater)} that
 * clients should use to construct the different types of index in a consistent manner.  Implementing classes have
 * access to an {@link org.apache.solr.client.solrj.embedded.EmbeddedSolrServer} to update the index, and an {@link
 * uk.ac.ebi.gxa.dao.AtlasDAO} that provides interaction with the Atlas database (following an Atlas 2
 * schema).
 * <p/>
 * All implementing classes should implement the method {@link #createIndexDocs(ProgressUpdater)} which contains the logic for
 * constructing the relevant parts of the index for each implementation.  Implementations do not need to be concerned
 * with the SOLR index lifecycle, as this is handled by this abstract classes and {@link
 * uk.ac.ebi.gxa.index.builder.IndexBuilder} implementations.
 *
 * @author Miroslaw Dylag (original version)
 * @author Tony Burdett (atlas 2 revision)
 */
public abstract class IndexBuilderService {
    private final Logger log = LoggerFactory.getLogger(this.getClass());
    private AtlasDAO atlasDAO;
    private SolrServer solrServer;

    public AtlasDAO getAtlasDAO() {
        return atlasDAO;
    }

    public void setAtlasDAO(AtlasDAO atlasDAO) {
        this.atlasDAO = atlasDAO;
    }

    public SolrServer getSolrServer() {
        return solrServer;
    }

    public void setSolrServer(SolrServer solrServer) {
        this.solrServer = solrServer;
    }

    protected Logger getLog() {
        return log;
    }

    public interface ProgressUpdater {
        void update(String progress);
    }

    /**
     * Build the index for this particular IndexBuilderService implementation. Once the index has been built, this
     * method will automatically commit any changes and release any resources held by the SOLR server.
     *
     * @param progressUpdater listener for passing progress updates
     *
     * @throws IndexBuilderException if the is a problem whilst generating the index
     */
    public void buildIndex(ProgressUpdater progressUpdater) throws IndexBuilderException {
        try {
            getSolrServer().deleteByQuery("*:*");
            createIndexDocs(progressUpdater);
            getSolrServer().commit();
            getSolrServer().optimize();
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
     * Update the index for a selection of document ids for this particular IndexBuilderService implementation.
     * Once the index has been built, this method will automatically commit any changes and release any
     * resources held by the SOLR server.
     *
     * @param docIds document id's to update
     * @param progressUpdater listener for passing progress updates
     *
     * @throws IndexBuilderException if the is a problem whilst generating the index
     */
    public void updateIndex(Collection<Long> docIds, ProgressUpdater progressUpdater) throws IndexBuilderException {
        try {
            updateIndexDocs(docIds, progressUpdater);
            getSolrServer().commit();
            getSolrServer().optimize();
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
     * Generate the required documents for the SOLR index, as appropriate to this implementation.  This method blocks
     * until all index documents have been created.
     * <p/>
     * Implementations are free to define their own optimization strategy, and it is acceptable to use asynchronous
     * operations.
     *
     * @param progressUpdater instance of {@link ProgressUpdater} to track progress     *
     * @throws uk.ac.ebi.gxa.index.builder.IndexBuilderException
     *          if there is a problem whilst trying to generate the index documents
     */
    protected abstract void createIndexDocs(ProgressUpdater progressUpdater) throws IndexBuilderException;

    /**
     * Generate/update only documents for a selection of id's.
     *
     * @param docIds document id's to update
     * @param progressUpdater instance of {@link ProgressUpdater} to track progress
     * @throws IndexBuilderException thrown if an error occurs
     */
    protected abstract void updateIndexDocs(Collection<Long> docIds, ProgressUpdater progressUpdater) throws IndexBuilderException;

    /**
     * Returns index name, which this service builds
      * @return text string
     */
    public abstract String getName();
}
