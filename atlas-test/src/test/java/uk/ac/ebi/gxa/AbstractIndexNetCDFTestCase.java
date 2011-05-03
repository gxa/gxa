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

package uk.ac.ebi.gxa;

import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.core.CoreContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;
import org.xml.sax.SAXException;
import uk.ac.ebi.gxa.dao.AtlasDAOTestCase;
import uk.ac.ebi.gxa.index.SolrContainerFactory;
import uk.ac.ebi.gxa.index.builder.DefaultIndexBuilder;
import uk.ac.ebi.gxa.index.builder.IndexAllCommand;
import uk.ac.ebi.gxa.index.builder.IndexBuilderException;
import uk.ac.ebi.gxa.index.builder.listener.IndexBuilderEvent;
import uk.ac.ebi.gxa.index.builder.listener.IndexBuilderListener;
import uk.ac.ebi.gxa.index.builder.service.ExperimentAtlasIndexBuilderService;
import uk.ac.ebi.gxa.index.builder.service.GeneAtlasIndexBuilderService;
import uk.ac.ebi.gxa.netcdf.generator.NetCDFCreatorException;
import uk.ac.ebi.gxa.netcdf.reader.AtlasNetCDFDAO;
import uk.ac.ebi.gxa.properties.AtlasProperties;
import uk.ac.ebi.gxa.properties.ResourceFileStorage;
import uk.ac.ebi.gxa.utils.FileUtil;

import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.LogManager;

/**
 * Test case that creates Solr indices and NetCDFs from DB unit test.
 */
public abstract class AbstractIndexNetCDFTestCase extends AtlasDAOTestCase {
    private File indexLocation;
    private SolrServer exptServer;
    private SolrServer atlasServer;
    private DefaultIndexBuilder indexBuilder;
    private CoreContainer coreContainer;
    private File netCDFRepoLocation;
    private AtlasNetCDFDAO atlasNetCDFDAO;

    private boolean solrBuildFinished;

    private final Logger log = LoggerFactory.getLogger(getClass());

    protected void setUp() throws Exception {
        super.setUp();

        try {
            LogManager.getLogManager()
                    .readConfiguration(this.getClass().getClassLoader().getResourceAsStream("logging.properties"));
        } catch (Exception e) {
            e.printStackTrace();
        }
        SLF4JBridgeHandler.install();

        buildSolrIndexes();
        generateNetCDFs();
    }

    private void generateNetCDFs() throws NetCDFCreatorException, InterruptedException {
        final File classPath = new File(this.getClass().getClassLoader().getResource("").getPath());
        netCDFRepoLocation = new File(classPath, "netcdfs");
        atlasNetCDFDAO = new AtlasNetCDFDAO();
        atlasNetCDFDAO.setAtlasDataRepo(netCDFRepoLocation);
    }


    protected void tearDown() throws Exception {
        super.tearDown();

        // delete the repo
        if (netCDFRepoLocation.exists()) FileUtil.deleteDirectory(netCDFRepoLocation);

        netCDFRepoLocation = null;

        // shutdown the indexBuilder and coreContainer if its not already been done
        if (coreContainer != null) {
            coreContainer.shutdown();
        }

        // delete the index
        if (indexLocation.exists()) FileUtil.deleteDirectory(indexLocation);

        indexLocation = null;
        indexBuilder = null;
    }

    public AtlasNetCDFDAO getNetCDFDAO() {
        return atlasNetCDFDAO;
    }

    public SolrServer getSolrServerExpt() {
        return exptServer;
    }

    public SolrServer getSolrServerAtlas() {
        return atlasServer;
    }

    private void buildSolrIndexes() throws InterruptedException, IndexBuilderException, URISyntaxException, IOException, SAXException, ParserConfigurationException {
        indexLocation = new File(new File("target", "test"), "index");

        log.debug("Extracting index to " + indexLocation.getAbsolutePath());
        createSOLRServers();

        ExperimentAtlasIndexBuilderService eaibs = new ExperimentAtlasIndexBuilderService();
        eaibs.setAtlasDAO(getAtlasDAO());
        eaibs.setAtlasModel(getAtlasModel());
        eaibs.setSolrServer(exptServer);
        eaibs.setExecutor(executor());

        GeneAtlasIndexBuilderService gaibs = new GeneAtlasIndexBuilderService();
        gaibs.setAtlasDAO(getAtlasDAO());
        gaibs.setSolrServer(atlasServer);
        gaibs.setBioEntityDAO(getBioEntityDAO());
        AtlasProperties atlasProperties = new AtlasProperties();
        ResourceFileStorage storage = new ResourceFileStorage();
        storage.setResourcePath("atlas.properties");
        atlasProperties.setStorage(storage);
        gaibs.setAtlasProperties(atlasProperties);
        gaibs.setExecutor(executor());

        indexBuilder = new DefaultIndexBuilder();
        indexBuilder.setIncludeIndexes(Arrays.asList("experiments", "genes"));
        indexBuilder.setServices(Arrays.asList(eaibs, gaibs));
        indexBuilder.setExecutor(executor());

        indexBuilder.doCommand(new IndexAllCommand(), new IndexBuilderListener() {
            public void buildSuccess() {
                solrBuildFinished = true;
            }

            public void buildError(IndexBuilderEvent event) {
                solrBuildFinished = true;
                for (Throwable t : event.getErrors()) {
                    t.printStackTrace();
                }
                fail("Failed to build Solr Indexes: " + event.getErrors());
            }

            public void buildProgress(String progressStatus) {
            }
        });

        while (!solrBuildFinished) {
            synchronized (this) {
                wait(100);
            }
        }
    }

    private ExecutorService executor() {
        return Executors.newSingleThreadExecutor();
    }

    private void createSOLRServers() throws IOException, SAXException, ParserConfigurationException {
        SolrContainerFactory solrContainerFactory = new SolrContainerFactory();
        solrContainerFactory.setAtlasIndex(indexLocation);
        solrContainerFactory.setTemplatePath("solr");

        coreContainer = solrContainerFactory.createContainer();

        // create an embedded solr server for experiments and genes from this container
        exptServer = new EmbeddedSolrServer(coreContainer, "expt");
        atlasServer = new EmbeddedSolrServer(coreContainer, "atlas");
    }
}
