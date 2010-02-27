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
import uk.ac.ebi.gxa.index.builder.IndexBuilderException;
import uk.ac.ebi.gxa.index.builder.listener.IndexBuilderEvent;
import uk.ac.ebi.gxa.index.builder.listener.IndexBuilderListener;
import uk.ac.ebi.gxa.index.builder.service.ExperimentAtlasIndexBuilderService;
import uk.ac.ebi.gxa.index.builder.service.GeneAtlasIndexBuilderService;
import uk.ac.ebi.gxa.index.builder.service.IndexBuilderService;
import uk.ac.ebi.gxa.netcdf.generator.DefaultNetCDFGenerator;
import uk.ac.ebi.gxa.netcdf.generator.NetCDFGeneratorException;
import uk.ac.ebi.gxa.utils.FileUtil;

import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.logging.LogManager;

/**
 * Test case that creates Solr indices and NetCDFs from DB unit test.
 */
public abstract class AbstractIndexNetCDFTestCase extends AtlasDAOTestCase {
    private File indexLocation;
    private SolrServer exptServer;
    private SolrServer atlasServer;
    private DefaultIndexBuilder indexBuilder;
    private boolean buildFinished;
    private CoreContainer coreContainer;
    private File netCDFRepoLocation;
    private DefaultNetCDFGenerator netCDFGenerator;

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

    protected void tearDown() throws Exception {
        super.tearDown();

        netCDFGenerator.shutdown();

        // delete the repo
        if (netCDFRepoLocation.exists()) FileUtil.deleteDirectory(netCDFRepoLocation);

        netCDFRepoLocation = null;

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

    public File getNetCDFRepoLocation() {
        return netCDFRepoLocation;
    }

    public SolrServer getSolrServerExpt() {
        return exptServer;
    }

    public SolrServer getSolrServerAtlas() {
        return atlasServer;
    }

    private void generateNetCDFs() throws NetCDFGeneratorException {
        netCDFRepoLocation = new File(
                "target" + File.separator + "test" + File.separator + "netcdfs");

        netCDFGenerator = new DefaultNetCDFGenerator();
        netCDFGenerator.setAtlasDAO(getAtlasDAO());
        netCDFGenerator.setRepositoryLocation(netCDFRepoLocation);

        netCDFGenerator.startup();
        netCDFGenerator.generateNetCDFs();
    }

    private void buildSolrIndexes() throws InterruptedException, IndexBuilderException, URISyntaxException {
        indexLocation =
                new File("target" + File.separator + "test" + File.separator + "index");

        System.out.println("Extracting index to " + indexLocation.getAbsolutePath());
        createSOLRServers();

        ExperimentAtlasIndexBuilderService eaibs = new ExperimentAtlasIndexBuilderService();
        eaibs.setAtlasDAO(getAtlasDAO());
        eaibs.setSolrServer(exptServer);

        GeneAtlasIndexBuilderService gaibs = new GeneAtlasIndexBuilderService();
        gaibs.setAtlasDAO(getAtlasDAO());
        gaibs.setSolrServer(atlasServer);

        Efo efo = new Efo();
        efo.setUri(new URI("resource:META-INF/efo.owl"));
        //efo.load();
        gaibs.setEfo(efo);

        indexBuilder = new DefaultIndexBuilder();
        indexBuilder.setIncludeIndexes(Arrays.asList("experiments", "genes"));
        indexBuilder.setServices(Arrays.asList(eaibs, gaibs));

        indexBuilder.startup();
        indexBuilder.buildIndex(new IndexBuilderListener(){
            public void buildSuccess(IndexBuilderEvent event) {
                buildFinished = true;
            }

            public void buildError(IndexBuilderEvent event) {
                buildFinished = true;
            }

            public void buildProgress(String progressStatus) {}
        });

        while(!buildFinished) {
            synchronized(this) { wait(100); };
        }
    }

    private void createSOLRServers() {
        try {
            SolrContainerFactory solrContainerFactory = new SolrContainerFactory();
            solrContainerFactory.setAtlasIndex(indexLocation);
            solrContainerFactory.setTemplatePath("solr");

            coreContainer = solrContainerFactory.createContainer();

            // create an embedded solr server for experiments and genes from this container
            exptServer = new EmbeddedSolrServer(coreContainer, "expt");
            atlasServer = new EmbeddedSolrServer(coreContainer, "atlas");
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

}
