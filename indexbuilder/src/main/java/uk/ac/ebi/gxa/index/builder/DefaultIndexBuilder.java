package uk.ac.ebi.gxa.index.builder;

import org.apache.solr.core.CoreContainer;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.xml.sax.SAXException;
import uk.ac.ebi.gxa.index.builder.listener.IndexBuilderEvent;
import uk.ac.ebi.gxa.index.builder.listener.IndexBuilderListener;
import uk.ac.ebi.gxa.index.builder.service.*;
import uk.ac.ebi.gxa.utils.Pair;
import uk.ac.ebi.microarray.atlas.dao.AtlasDAO;

import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * A default implementation of {@link IndexBuilder} that constructs a SOLR index in a supplied directory.  By default,
 * this will include all genes and experiments.
 *
 * @author Tony Burdett
 * @date 20-Aug-2009
 */
public class DefaultIndexBuilder implements IndexBuilder<File>, InitializingBean {
    // these are spring managed fields
    private AtlasDAO atlasDAO;
    private File indexLocation;

    // these are initialised by this bean, not spring managed
    private CoreContainer coreContainer;

    private ExecutorService service;
    private boolean running = false;

    private List<String> includeIndices;

    private List<Pair<String,IndexBuilderService>> services;

    private List<IndexUpdateHandler> updateHandlers = new ArrayList<IndexUpdateHandler>();

    // logging
    private final Logger log = LoggerFactory.getLogger(getClass());

    static {
        IndexBuilderServiceRegistry.registerFactory(new PropertiesIndexBuilderService.Factory());
        IndexBuilderServiceRegistry.registerFactory(new ExperimentAtlasIndexBuilderService.Factory());
        IndexBuilderServiceRegistry.registerFactory(new GeneAtlasIndexBuilderService.Factory());
    }

    public void setAtlasDAO(AtlasDAO atlasDAO) {
        this.atlasDAO = atlasDAO;
    }

    public AtlasDAO getAtlasDAO() {
        return atlasDAO;
    }

    public void setIndexLocation(File indexLocation) {
        this.indexLocation = indexLocation;
    }

    public File getIndexLocation() {
        return indexLocation;
    }

    public List<String> getIncludeIndexes() {
        return includeIndices;
    }

    public void setIncludeIndexes(List<String> includeIndices) {
        this.includeIndices = includeIndices;
    }

    public void afterPropertiesSet() throws Exception {
        // simply delegates to startup(), this allows automated spring startup
        startup();
    }

    /**
     * Starts up any resources required for building an index.  In this case, this method will create a new {@link
     * AtlasDAO} and a spring {@link JdbcTemplate} in order to interact with the Atlas database.  It will then check for
     * the existence of an appropriate SOLR index from the {@link #indexLocation} configured.  If the index previously
     * exists, it will attempt to load it, and if not it will unpack any required configuration elements into this
     * location ready to run a new index build.  It will the initialise an embedded SOLR server ({@link
     * org.apache.solr.client.solrj.embedded.EmbeddedSolrServer}). Finally, it will initialise an {@link
     * java.util.concurrent.ExecutorService} for running index building tasks in an asynchronous, parallel manner.
     * <p/>
     * Once you have started a default index builder, it will continue to run until you call {@link #shutdown()} on it.
     *
     * @throws IndexBuilderException if initialisation of this builder failed for any reason
     */
    public void startup() throws IndexBuilderException {
        if (!running) {
            try {
                // do some initialization...

                // check for the presence of the index
                File solr = new File(indexLocation, "solr.xml");
                if (!solr.exists()) {
                    log.debug("No existing index - unpacking config files to " +
                            indexLocation.getAbsolutePath());
                    // no prior index, check the directory is empty?
                    if (indexLocation.exists() && indexLocation.listFiles().length > 0) {
                        String message = "Unable to unpack solr configuration files - " +
                                indexLocation.getAbsolutePath() + " is not empty. " +
                                "Please choose an empty directory to create the index";
                        log.error(message);
                        throw new IndexBuilderException(message);
                    }
                    else {
                        // unpack configuration files
                        unpackAtlasIndexTemplate(indexLocation);
                    }
                }

                // first, create a solr CoreContainer
                log.debug("Creating new SOLR container...");
                coreContainer = new CoreContainer();
                coreContainer.load(indexLocation.getAbsolutePath(), solr);

                // create IndexBuilderServices for genes (atlas) and experiments
                log.debug("Creating index building services for " + StringUtils.join(getIncludeIndexes(), ","));

                services = new ArrayList<Pair<String, IndexBuilderService>>();
                for(String name : includeIndices)
                    services.add(new Pair<String, IndexBuilderService>(
                            name,
                            IndexBuilderServiceRegistry.getFactoryByName(name).create(atlasDAO, coreContainer)
                    ));

                // finally, create an executor service for processing calls to build the index
                service = Executors.newCachedThreadPool();
                log.debug("Initialized " + getClass().getSimpleName() + " OK!");

                running = true;
            }
            catch (IOException e) {
                // wrap and rethrow as IndexBuilderException
                throw new IndexBuilderException(e);
            }
            catch (SAXException e) {
                // wrap and rethrow as IndexBuilderException
                throw new IndexBuilderException(e);
            }
            catch (ParserConfigurationException e) {
                // wrap and rethrow as IndexBuilderException
                throw new IndexBuilderException(e);
            }
        }
        else {
            log.warn("Ignoring attempt to startup() a " + getClass().getSimpleName() +
                    " that is already running");
        }
    }

    /**
     * Shuts down any cached resources relating to multiple solr cores within the Atlas Index.  You should call this
     * whenever the application requiring index building services terminates (i.e. on webapp shutdown, or when the user
     * exits the application).
     *
     * @throws IndexBuilderException if shutting down this index builder failed for any reason
     */
    public void shutdown() throws IndexBuilderException {
        if (running) {
            log.debug("Shutting down " + getClass().getSimpleName() + "...");
            service.shutdown();
            try {
                log.debug("Waiting for termination of running jobs");
                service.awaitTermination(60, TimeUnit.SECONDS);

                if (!service.isTerminated()) {
                    // try and halt immediately
                    List<Runnable> tasks = service.shutdownNow();
                    service.awaitTermination(15, TimeUnit.SECONDS);
                    // if it's STILL not terminated...
                    if (!service.isTerminated()) {
                        StringBuffer sb = new StringBuffer();
                        sb.append(
                                "Unable to cleanly shutdown index building service.\n");
                        if (tasks.size() > 0) {
                            sb.append("The following tasks are still active or suspended:\n");
                            for (Runnable task : tasks) {
                                sb.append("\t").append(task.toString()).append("\n");
                            }
                        }
                        sb.append(
                                "There are running or suspended index building tasks. " +
                                        "If execution is complete, or has failed to exit " +
                                        "cleanly following an error, you should terminate this " +
                                        "application");
                        log.error(sb.toString());
                        throw new IndexBuilderException(sb.toString());
                    }
                    else {
                        // it worked second time round
                        log.debug("Shutdown complete");
                    }
                }
                else {
                    log.debug("Shutdown complete");
                }
            }
            catch (InterruptedException e) {
                log.error("The application was interrupted whilst waiting to be shutdown.  " +
                        "There may be tasks still running or suspended.");
                throw new IndexBuilderException(e);
            }
            finally {
                coreContainer.shutdown();
                running = false;
            }
        }
        else {
            log.warn("Ignoring attempt to shutdown() a " + getClass().getSimpleName() + " that is not running");
        }
    }

    public void buildIndex() {
        buildIndex(null);
    }

    public void buildIndex(IndexBuilderListener listener) {
        startIndexBuild(listener, false);
        log.info("Started IndexBuilder: " + "Building for " + StringUtils.join(getIncludeIndexes(), ","));
    }

    public void updateIndex() {
        updateIndex(null);
    }

    public void updateIndex(IndexBuilderListener listener) {
        startIndexBuild(listener, true);
        log.info("Started IndexBuilder: " +
                "Updating for " + StringUtils.join(getIncludeIndexes(), ","));
    }

    public void registerIndexUpdateHandler(IndexUpdateHandler handler) {
        if(!updateHandlers.contains(handler))
            updateHandlers.add(handler);
    }

    public void unregisterIndexUpdateHandler(IndexUpdateHandler handler) {
        updateHandlers.remove(handler);
    }

    private void notifyUpdateHandlers() {
        log.info("Index updated, notifying webapp to reload caches");
        for(IndexUpdateHandler handler : updateHandlers)
            handler.onIndexUpdate(this);
    }

    private void startIndexBuild(final IndexBuilderListener listener, final boolean pending) {
        final long startTime = System.currentTimeMillis();
        final List<Future<Boolean>> indexingTasks =
                new ArrayList<Future<Boolean>>();

        for(final Pair<String, IndexBuilderService> ibService : services) {
            indexingTasks.add(service.submit(new Callable<Boolean>() {
                public Boolean call() throws IndexBuilderException {
                    try {
                        log.info("Starting building of index: " + ibService.getFirst());
                        ibService.getSecond().buildIndex(pending);
                        return true;
                    }
                    catch (Exception e) {
                        log.error("Caught unchecked exception: " + e.getMessage());
                        e.printStackTrace();
                        return false;
                    }
                }
            }));
        }

        // this tracks completion, if a listener was supplied
        if (listener != null) {
            new Thread(new Runnable() {
                public void run() {
                    boolean success = true;
                    List<Throwable> observedErrors = new ArrayList<Throwable>();

                    // wait for expt and gene indexes to build
                    for (Future<Boolean> indexingTask : indexingTasks) {
                        try {
                            success = success && indexingTask.get();
                        }
                        catch (Exception e) {
                            observedErrors.add(e);
                            success = false;
                        }
                    }

                    // now we've finished - get the end time, calculate runtime and fire the event
                    long endTime = System.currentTimeMillis();
                    long runTime = (endTime - startTime) / 1000;

                    notifyUpdateHandlers();
                    
                    // create our completion event
                    if (success) {
                        listener.buildSuccess(new IndexBuilderEvent(runTime, TimeUnit.SECONDS));
                    }
                    else {
                        listener.buildError(new IndexBuilderEvent(runTime, TimeUnit.SECONDS, observedErrors));
                    }
                }
            }).start();
        }
    }

    /**
     * This method bootstraps an empty atlas index when starting an indexbuilder from scratch.  Use this is the index
     * could not be found, and you should get a ready-to-build index with all required config files
     *
     * @param indexLocation the location in which to build the index
     * @throws IOException if the resources could not be written
     */
    private void unpackAtlasIndexTemplate(File indexLocation) throws IOException {
        // configure a list of resources we need from the indexbuilder jar
        writeResourceToFile("solr/solr.xml", new File(indexLocation, "solr.xml"));

        for(String factory : IndexBuilderServiceRegistry.getAvailableFactories())
            for(String fileName : IndexBuilderServiceRegistry.getFactoryByName(factory).getConfigFiles())
                writeResourceToFile("solr/" + fileName, new File(indexLocation, fileName.replaceAll("/", File.separator)));
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
}
    