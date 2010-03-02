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

package uk.ac.ebi.gxa.index.builder;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.core.CoreContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;
import org.xml.sax.SAXException;
import uk.ac.ebi.gxa.dao.AtlasDAOTestCase;
import uk.ac.ebi.gxa.index.SolrContainerFactory;
import uk.ac.ebi.gxa.index.builder.listener.IndexBuilderEvent;
import uk.ac.ebi.gxa.index.builder.listener.IndexBuilderListener;
import uk.ac.ebi.gxa.index.builder.service.ExperimentAtlasIndexBuilderService;
import uk.ac.ebi.gxa.index.builder.service.IndexBuilderService;
import uk.ac.ebi.gxa.utils.FileUtil;

import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.logging.LogManager;

/**
 * A test case fo assessing whether the DefaultIndexBuilder class initializes correctly and can run a build of the index
 * over some dummy data from the test database.  Note that only very simple checks are run to ensure that some data has
 * gone into the index.  Precise implementations tests should be done on the individual index building services, not
 * this class.  This test is just to ensure clean startup and shutdown of the main index builder.
 *
 * @author Junit Generation Plugin for Maven, written by Tony Burdett
 * @date 07-10-2009
 */
public class TestDefaultIndexBuilder extends AtlasDAOTestCase {
    private File indexLocation;
    private DefaultIndexBuilder indexBuilder;

    private CoreContainer coreContainer;
    private SolrServer exptServer;

    private boolean buildFinished = false;
    private final Logger log = LoggerFactory.getLogger(getClass());

    public void setUp() throws Exception {
        super.setUp();

        try {
            LogManager.getLogManager()
                    .readConfiguration(this.getClass().getClassLoader().getResourceAsStream("logging.properties"));
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        SLF4JBridgeHandler.install();

        indexLocation =
                new File("target" + File.separator + "test" + File.separator + "index");

        System.out.println("Extracting index to " + indexLocation.getAbsolutePath());
        createSOLRServers();

        ExperimentAtlasIndexBuilderService eaibs = new ExperimentAtlasIndexBuilderService();
        eaibs.setAtlasDAO(getAtlasDAO());
        eaibs.setSolrServer(exptServer);

        indexBuilder = new DefaultIndexBuilder();
        indexBuilder.setIncludeIndexes(Collections.singletonList("experiments"));
        indexBuilder.setServices(Collections.<IndexBuilderService>singletonList(eaibs));
    }

    public void tearDown() throws Exception {
        super.tearDown();

        // shutdown the indexBuilder and coreContainer if its not already been done
        indexBuilder.shutdown();
        if (coreContainer != null) {
            coreContainer.shutdown();
        }

        // delete the index
        if (indexLocation.exists() && !FileUtil.deleteDirectory(indexLocation)) {
            log.warn("Failed to delete " + indexLocation.getAbsolutePath());
        }

        indexLocation = null;
        indexBuilder = null;
    }

    public void createSOLRServers() {
        try {
            SolrContainerFactory solrContainerFactory = new SolrContainerFactory();
            solrContainerFactory.setAtlasIndex(indexLocation);
            solrContainerFactory.setTemplatePath("solr");

            coreContainer = solrContainerFactory.createContainer();

            // create an embedded solr server for experiments and genes from this container
            exptServer = new EmbeddedSolrServer(coreContainer, "expt");
        }
        catch (IOException e) {
            e.printStackTrace();
            fail();
        }
        catch (SAXException e) {
            e.printStackTrace();
            fail();
        }
        catch (ParserConfigurationException e) {
            e.printStackTrace();
            fail();
        }
    }

    public void testStartup() {
        try {
            indexBuilder.startup();

            // now try a repeat startup
            indexBuilder.startup();
        }
        catch (IndexBuilderException e) {
            e.printStackTrace();
            fail();
        }
    }

    public void testShutdown() {
        try {
            // try shutdown without startup
            indexBuilder.shutdown();

            // now startup
            indexBuilder.startup();
            
            // just check shutdown occurs cleanly, without throwing an exception
            indexBuilder.shutdown();
        }
        catch (IndexBuilderException e) {
            e.printStackTrace();
            fail();
        }
    }

    public void testBuildIndex() {
        try {
            indexBuilder.startup();

            // run buildIndex
            indexBuilder.buildIndex(new IndexBuilderListener() {
                public void buildSuccess(IndexBuilderEvent event) {
                    try {
                        // now query the index for the stuff that is in the test DB

                        SolrQuery q = new SolrQuery("*:*");
                        q.setRows(10);
                        q.setFields("");
                        q.addSortField("id", SolrQuery.ORDER.asc);


                        QueryResponse queryResponse = exptServer.query(q);
                        SolrDocumentList documentList = queryResponse.getResults();

                        if (documentList == null || documentList.size() < 1) {
                            fail("No experiments available");
                        }

                        // just check we have 2 experiments - as this is the number in our dataset
                        int expected = getDataSet().getTable("A2_EXPERIMENT").getRowCount();
                        int actual = documentList.size();
                        assertEquals("Wrong number of docs: expected " + expected +
                                ", actual " + actual, expected, actual);
                    } catch (Exception e) {
                        fail();
                    } finally {
                        buildFinished = true;
                    }
                }

                public void buildError(IndexBuilderEvent event) {
                    fail();
                    buildFinished = true;
                }

                public void buildProgress(String progressStatus) {}
            });

            while(buildFinished != true) {
                synchronized(this) { wait(100); };
            }
        }
        catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }
}
