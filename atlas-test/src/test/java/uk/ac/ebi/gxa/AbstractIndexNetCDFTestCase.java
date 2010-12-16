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
import uk.ac.ebi.gxa.efo.Efo;
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
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.logging.LogManager;

/**
 * Test case that creates Solr indices and NetCDFs from DB unit test.
 */
public abstract class AbstractIndexNetCDFTestCase extends AtlasDAOTestCase {
    private final Logger log = LoggerFactory.getLogger(getClass());

    private File indexLocation;
    private SolrServer exptServer;
    private SolrServer atlasServer;
    private DefaultIndexBuilder indexBuilder;
    private CoreContainer coreContainer;
    private AtlasNetCDFDAO atlasNetCDFDAO;
    private boolean solrBuildFinished;

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

    private void generateNetCDFs() throws NetCDFCreatorException, InterruptedException, IOException {
        atlasNetCDFDAO = new AtlasNetCDFDAO();
        atlasNetCDFDAO.setAtlasDataRepo(new File(getTestClassesPath(), "netcdfs"));
    }

    /**
     * We assume test classes are deployed in a file system; moreover, we assume the path will be available.
     * This might be not a case with non-Sun JVMs; hope maven won't ever jar the test classes, though.
     *
     * @return the {@link java.io.File} representing the test classes directory.
     */
    private File getTestClassesPath() {
        return new File(this.getClass().getClassLoader().getResource(".").getPath());
    }

    protected void tearDown() throws Exception {
        super.tearDown();

        // shutdown the indexBuilder and coreContainer if its not already been done
        indexBuilder.shutdown();
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

        log.debug("Extracting index to {}", indexLocation.getAbsolutePath());
        createSOLRServers();

        ExperimentAtlasIndexBuilderService eaibs = new ExperimentAtlasIndexBuilderService();
        eaibs.setAtlasDAO(getAtlasDAO());
        eaibs.setSolrServer(exptServer);

        GeneAtlasIndexBuilderService gaibs = new GeneAtlasIndexBuilderService();
        gaibs.setAtlasDAO(getAtlasDAO());
        gaibs.setSolrServer(atlasServer);
        AtlasProperties atlasProperties = new AtlasProperties();
        ResourceFileStorage storage = new ResourceFileStorage();
        storage.setResourcePath("atlas.properties");
        atlasProperties.setStorage(storage);
        gaibs.setAtlasProperties(atlasProperties);

        Efo efo = new Efo();
        efo.setUri(new URI("resource:META-INF/efo.owl"));
        //efo.load();
        gaibs.setEfo(efo);

        indexBuilder = new DefaultIndexBuilder();
        indexBuilder.setIncludeIndexes(Arrays.asList("experiments", "genes"));
        indexBuilder.setServices(Arrays.asList(eaibs, gaibs));

        indexBuilder.startup();
        indexBuilder.doCommand(new IndexAllCommand(), new IndexBuilderListener() {
            public void buildSuccess(IndexBuilderEvent event) {
                solrBuildFinished = true;
            }

            public void buildError(IndexBuilderEvent event) {
                solrBuildFinished = true;
                fail("Failed to build Solr Indexes");
            }

            public void buildProgress(String progressStatus) {
            }
        });

        while (!solrBuildFinished) {
            Thread.sleep(100);
        }
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
