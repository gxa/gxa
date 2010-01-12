package uk.ac.ebi.gxa.index.builder.service;

import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.core.CoreContainer;
import org.slf4j.bridge.SLF4JBridgeHandler;
import uk.ac.ebi.microarray.atlas.dao.AtlasDAOTestCase;

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

        if (IndexBuilderServiceRegistry.getFactoryByName("properties") == null) {
            IndexBuilderServiceRegistry.registerFactory(new PropertiesIndexBuilderService.Factory());
        }
        if (IndexBuilderServiceRegistry.getFactoryByName("experiments") == null) {
            IndexBuilderServiceRegistry.registerFactory(new ExperimentAtlasIndexBuilderService.Factory());
        }
        if (IndexBuilderServiceRegistry.getFactoryByName("genes") == null) {
            IndexBuilderServiceRegistry.registerFactory(new GeneAtlasIndexBuilderService.Factory());
        }
        // locate index
        indexLocation = new File("target" + File.separator + "test" +
                File.separator + "index");

        System.out.println("Extracting index to " + indexLocation.getAbsolutePath());

        // check for the presence of the index
        File solr = new File(indexLocation, "solr.xml");
        if (!solr.exists()) {
            // no prior index, check the directory is empty?
            if (indexLocation.exists() && indexLocation.listFiles().length > 0) {
                fail();
            }
            else {
                // unpack configuration files
                unpackAtlasIndexTemplate(indexLocation);
            }
        }

        // first, create a solr CoreContainer
        coreContainer = new CoreContainer();
        coreContainer.load(indexLocation.getAbsolutePath(), solr);

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

    /**
     * This method bootstraps an empty atlas index when starting an indexbuilder from scratch.  Use this is the index
     * could not be found, and you should get a ready-to-build index with all required config files
     *
     * @param indexLocation the location in which to build the index
     * @throws java.io.IOException if the resources could not be written
     */
    private void unpackAtlasIndexTemplate(File indexLocation) throws IOException {
        // configure a list of resources we need from the indexbuilder jar
        writeResourceToFile("solr/solr.xml", new File(indexLocation, "solr.xml"));

        for (String factory : IndexBuilderServiceRegistry.getAvailableFactories()) {
            for (String fileName : IndexBuilderServiceRegistry.getFactoryByName(factory).getConfigFiles()) {
                writeResourceToFile("solr/" + fileName,
                                    new File(indexLocation, fileName.replaceAll("/", File.separator)));
            }
        }
    }

    /**
     * Writes a classpath resource to a file in the specified location.  You should not use this to overwrite files - if
     * you attempt this, an IOException will be thrown.  Also note that an IOException is thrown if the file you specify
     * is in a new directory and the parent directories required could not be created.
     *
     * @param resourceName the name of the classpath resource to copy
     * @param file         the file to write the classpath resource to
     * @throws IOException if the resource could not properly be written out, or if the file already exists
     */
    private void writeResourceToFile(String resourceName, File file)
            throws IOException {
        // make all parent dirs necessary if they don't exist
        if (!file.getParentFile().exists()) {
            if (!file.getParentFile().mkdirs()) {
                throw new IOException("Unable to make index directory " +
                        file.getParentFile() + ", do you have permission to write here?");
            }
        }

        // check the resource we're attempting to write doesn't exist
        if (file.exists()) {
            throw new IOException("The file " + file + " already exists - you " +
                    "should not attempt to overwrite an existing index.  If you wish " +
                    "to replace this index, please backup or delete the old one first");
        }

        BufferedReader reader =
                new BufferedReader(new InputStreamReader(
                        Thread.currentThread().getContextClassLoader().getResourceAsStream(
                                resourceName)));
        BufferedWriter writer =
                new BufferedWriter(new FileWriter(file));
        String line;
        while ((line = reader.readLine()) != null) {
            writer.write(line + "\n");
        }
        reader.close();
        writer.close();
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
