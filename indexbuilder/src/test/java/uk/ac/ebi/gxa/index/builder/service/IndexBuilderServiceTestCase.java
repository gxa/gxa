package uk.ac.ebi.gxa.index.builder.service;

import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.core.CoreContainer;
import org.slf4j.bridge.SLF4JBridgeHandler;
import uk.ac.ebi.gxa.dao.AtlasDAOTestCase;
import uk.ac.ebi.gxa.index.SolrContainerFactory;

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
        if (!deleteDirectory(indexLocation)) {
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

    private boolean deleteDirectory(File directory) {
        boolean success = true;
        if (directory.exists()) {
            for (File file : directory.listFiles()) {
                if (file.isDirectory()) {
                    success = success && deleteDirectory(file);
                }
                else {
                    success = success && file.delete();
                }
            }
        }
        return success && directory.delete();
    }
}
