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
import uk.ac.ebi.gxa.dao.AtlasDAO;
import uk.ac.ebi.gxa.index.builder.*;

import java.io.IOException;

/**
 * An abstract IndexBuilderService, that provides convenience methods for getting and setting parameters required across
 * all SOLR index building implementations.   Implementing classes have
 * access to an {@link org.apache.solr.client.solrj.embedded.EmbeddedSolrServer} to update the index, and an {@link
 * uk.ac.ebi.gxa.dao.AtlasDAO} that provides interaction with the Atlas database (following an Atlas 2
 * schema).
 * <p/>
 * @author Miroslaw Dylag (original version)
 * @author Tony Burdett (atlas 2 revision)
 */
public abstract class IndexBuilderService {
    final private Logger log = LoggerFactory.getLogger(this.getClass());
    private AtlasDAO atlasDAO;
    private SolrServer solrServer;

    final public AtlasDAO getAtlasDAO() {
        return atlasDAO;
    }

    final public void setAtlasDAO(AtlasDAO atlasDAO) {
        this.atlasDAO = atlasDAO;
    }

    final public SolrServer getSolrServer() {
        return solrServer;
    }

    final public void setSolrServer(SolrServer solrServer) {
        this.solrServer = solrServer;
    }

    final protected Logger getLog() {
        return log;
    }

    public interface ProgressUpdater {
        void update(String progress);
    }

    /**
     * Build the index for this particular IndexBuilderService implementation. Once the index has been built, this
     * method will automatically commit any changes and release any resources held by the SOLR server.
     *
     * @param command command
     * @param progressUpdater listener for passing progress updates
     *
     * @throws IndexBuilderException if the is a problem whilst generating the index
     */
    final public void build(final IndexBuilderCommand command, final ProgressUpdater progressUpdater) throws IndexBuilderException {
        command.visit(new IndexBuilderCommandVisitor() {
            public void process(IndexAllCommand cmd) throws IndexBuilderException {
                processCommand(cmd, progressUpdater);
                finalizeCommand();
            }

            public void process(UpdateIndexForExperimentCommand cmd) throws IndexBuilderException {
                processCommand(cmd, progressUpdater);
                finalizeCommand(cmd, progressUpdater);
            }
        });
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

    public void finalizeCommand() throws IndexBuilderException {
        commit();
        //optimize();
    }

    public void finalizeCommand(UpdateIndexForExperimentCommand updateIndexForExperimentCommand, ProgressUpdater progressUpdater) throws IndexBuilderException {
        commit();
        //optimize();
    }

    /**
     * Returns index name, which this service builds
      * @return text string
     */
    public abstract String getName();
}
