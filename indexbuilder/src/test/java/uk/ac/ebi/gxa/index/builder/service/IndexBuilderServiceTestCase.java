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
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.core.CoreContainer;
import org.slf4j.bridge.SLF4JBridgeHandler;
import uk.ac.ebi.gxa.dao.AtlasDAOTestCase;
import uk.ac.ebi.gxa.index.SolrContainerFactory;
import uk.ac.ebi.gxa.utils.FileUtil;

import java.io.*;
import java.util.logging.LogManager;

/**
 * Javadocs go here!
 *
 * @author Tony Burdett
 * @date 08-Oct-2009
 */
public abstract class IndexBuilderServiceTestCase extends AtlasDAOTestCase {
    private File indexLocation;
    private CoreContainer coreContainer;
    private SolrServer exptSolrServer;
    private SolrServer atlasSolrServer;

    public void setUp() throws Exception {
        super.setUp();

        // sort out logging
        try {
            LogManager.getLogManager()
                    .readConfiguration(this.getClass().getClassLoader().getResourceAsStream("logging.properties"));
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        SLF4JBridgeHandler.install();

        // configure registries

        // locate index
        indexLocation = new File("target" + File.separator + "test" +
                File.separator + "index");

        System.out.println("Extracting index to " + indexLocation.getAbsolutePath());

        SolrContainerFactory factory = new SolrContainerFactory();
        factory.setAtlasIndex(indexLocation);
        factory.setTemplatePath("solr");
        // first, create a solr CoreContainer
        coreContainer = factory.createContainer();

        // create an embedded solr server for experiments and genes from this container
        exptSolrServer = new EmbeddedSolrServer(coreContainer, "expt");
        atlasSolrServer = new EmbeddedSolrServer(coreContainer, "atlas");
    }

    public void tearDown() throws Exception {
        super.tearDown();

        if (coreContainer != null) {
            coreContainer.shutdown();
        }

        // delete the index
        if (!FileUtil.deleteDirectory(indexLocation)) {
            fail("Failed to delete " + indexLocation.getAbsolutePath());
        }

        indexLocation = null;
    }

    public SolrServer getExptSolrServer() {
        return exptSolrServer;
    }

    public SolrServer getAtlasSolrServer() {
        return atlasSolrServer;
    }
}
