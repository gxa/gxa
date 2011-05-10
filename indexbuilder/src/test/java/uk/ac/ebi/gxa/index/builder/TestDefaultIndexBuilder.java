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
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.core.CoreContainer;
import org.dbunit.dataset.DataSetException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;
import org.xml.sax.SAXException;
import uk.ac.ebi.gxa.dao.AtlasDAOTestCase;
import uk.ac.ebi.gxa.index.SolrContainerFactory;
import uk.ac.ebi.gxa.index.builder.listener.IndexBuilderAdapter;
import uk.ac.ebi.gxa.index.builder.listener.IndexBuilderEvent;
import uk.ac.ebi.gxa.index.builder.service.ExperimentAtlasIndexBuilderService;
import uk.ac.ebi.gxa.index.builder.service.IndexBuilderService;
import uk.ac.ebi.gxa.utils.FileUtil;

import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.logging.LogManager;

import static java.util.concurrent.Executors.newSingleThreadExecutor;

/**
 * A test case fo assessing whether the DefaultIndexBuilder class initializes correctly and can run a build of the index
 * over some dummy data from the test database.  Note that only very simple checks are run to ensure that some data has
 * gone into the index.  Precise implementations tests should be done on the individual index building services, not
 * this class.  This test is just to ensure clean startup and shutdown of the main index builder.
 */
public class TestDefaultIndexBuilder extends AtlasDAOTestCase {
    private File indexLocation;
    private DefaultIndexBuilder indexBuilder;

    private CoreContainer coreContainer;
    private SolrServer exptServer;

    private CountDownLatch buildFinished = new CountDownLatch(1);
    private final Logger log = LoggerFactory.getLogger(getClass());

    public void setUp() throws Exception {
        super.setUp();

        try {
            LogManager.getLogManager()
                    .readConfiguration(this.getClass().getClassLoader().getResourceAsStream("logging.properties"));
        } catch (Exception e) {
            e.printStackTrace();
        }
        SLF4JBridgeHandler.install();

        indexLocation = new File(new File("target", "test"), "index");

        System.out.println("Extracting index to " + indexLocation.getAbsolutePath());
        createSOLRServers();

        ExperimentAtlasIndexBuilderService eaibs = new ExperimentAtlasIndexBuilderService();
        eaibs.setAtlasDAO(atlasDAO);
        eaibs.setAtlasModel(atlasModel);

        eaibs.setSolrServer(exptServer);
        eaibs.setExecutor(newSingleThreadExecutor());

        indexBuilder = new DefaultIndexBuilder();
        indexBuilder.setIncludeIndexes(Collections.singletonList("experiments"));
        indexBuilder.setServices(Collections.<IndexBuilderService>singletonList(eaibs));
        indexBuilder.setExecutor(newSingleThreadExecutor());
    }

    public void tearDown() throws Exception {
        super.tearDown();

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

    public void createSOLRServers() throws IOException, SAXException, ParserConfigurationException {
        SolrContainerFactory solrContainerFactory = new SolrContainerFactory();
        solrContainerFactory.setAtlasIndex(indexLocation);
        solrContainerFactory.setTemplatePath("solr");

        coreContainer = solrContainerFactory.createContainer();

        // create an embedded solr server for experiments and genes from this container
        exptServer = new EmbeddedSolrServer(coreContainer, "expt");

    }

    public void testBuildIndex() throws InterruptedException, IndexBuilderException {
        // run buildIndex
        indexBuilder.doCommand(new IndexAllCommand(), new IndexBuilderAdapter() {
            @Override
            public void buildSuccess() {
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
                } catch (DataSetException e) {
                    e.printStackTrace();
                    fail(e.getMessage());
                } catch (SolrServerException e) {
                    e.printStackTrace();
                    fail(e.getMessage());
                } catch (Exception e) {
                    e.printStackTrace();
                    fail(e.getMessage());
                } finally {
                    buildFinished.countDown();
                }
            }

            @Override
            public void buildError(IndexBuilderEvent event) {
                buildFinished.countDown();
                fail("Build error: " + event);
            }
        });

        buildFinished.await();
    }
}
