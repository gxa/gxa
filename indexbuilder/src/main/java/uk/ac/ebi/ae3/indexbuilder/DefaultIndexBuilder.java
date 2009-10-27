package uk.ac.ebi.ae3.indexbuilder;

import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.core.CoreContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.xml.sax.SAXException;
import uk.ac.ebi.ae3.indexbuilder.listener.IndexBuilderEvent;
import uk.ac.ebi.ae3.indexbuilder.listener.IndexBuilderListener;
import uk.ac.ebi.ae3.indexbuilder.service.ExperimentAtlasIndexBuilderService;
import uk.ac.ebi.ae3.indexbuilder.service.GeneAtlasIndexBuilderService;
import uk.ac.ebi.ae3.indexbuilder.service.IndexBuilderService;
import uk.ac.ebi.microarray.atlas.dao.AtlasDAO;

import javax.sql.DataSource;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

/**
 * A default implementation of {@link uk.ac.ebi.ae3.indexbuilder.IndexBuilder}
 * that constructs a SOLR index in a supplied directory.  By default, this will
 * include all genes and experiments.
 *
 * @author Tony Burdett
 * @date 20-Aug-2009
 */
public class DefaultIndexBuilder
    implements IndexBuilder<File>, InitializingBean {
  // these are spring managed fields
  private DataSource dataSource;
  private File indexLocation;

  private boolean genes = true;
  private boolean experiments = true;
  private boolean pending = false;

  // these are initialised by this bean, not spring managed
  private CoreContainer coreContainer;
  private IndexBuilderService geneIndexBuilder;
  private IndexBuilderService exptIndexBuilder;

  private ExecutorService service;
  private boolean running = false;

  // logging
  private final Logger log = LoggerFactory.getLogger(getClass());

  public void setAtlasDataSource(DataSource dataSource) {
    this.dataSource = dataSource;
  }

  public DataSource getAtlasDataSource() {
    return dataSource;
  }

  public void setIndexLocation(File indexLocation) {
    this.indexLocation = indexLocation;
  }

  public File getIndexLocation() {
    return indexLocation;
  }

  public void setIncludeGenes(boolean genes) {
    this.genes = genes;
  }

  public boolean getIncludeGenes() {
    return genes;
  }

  public void setIncludeExperiments(boolean experiments) {
    this.experiments = experiments;
  }

  public boolean getIncludeExperiments() {
    return experiments;
  }

  public void setPendingMode(boolean pending) {
    this.pending = pending;
  }

  public boolean getPendingMode() {
    return pending;
  }

  public void afterPropertiesSet() throws Exception {
    // simply delegates to startup(), this allows automated spring startup
    startup();
  }

  /**
   * Starts up any resources required for building an index.  In this case, this
   * method will create a new {@link AtlasDAO} and a spring {@link JdbcTemplate}
   * in order to interact with the Atlas database.  It will then check for the
   * existence of an appropriate SOLR index from the {@link #indexLocation}
   * configured.  If the index previously exists, it will attempt to load it,
   * and if not it will unpack any required configuration elements into this
   * location ready to run a new index build.  It will the initialise an
   * embedded SOLR server ({@link org.apache.solr.client.solrj.embedded.EmbeddedSolrServer}).
   * Finally, it will initialise an {@link java.util.concurrent.ExecutorService}
   * for running index building tasks in an asynchronous, parallel manner.
   * <p/>
   * Once you have started a default index builder, it will continue to run
   * until you call {@link #shutdown()} on it.
   *
   * @throws IndexBuilderException if initialisation of this builder failed for
   *                               any reason
   */
  public void startup() throws IndexBuilderException {
    if (!running) {
      try {
        // do some initialization...

        // create a spring jdbc template
        JdbcTemplate template = new JdbcTemplate(dataSource);

        // create an atlas dao
        AtlasDAO dao = new AtlasDAO();
        dao.setJdbcTemplate(template);

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
        log.debug("Creating new SOLR container and embedded servers...");
        coreContainer = new CoreContainer();
        coreContainer.load(indexLocation.getAbsolutePath(), solr);

        // create an embedded solr server for experiments and genes from this container
        EmbeddedSolrServer exptServer =
            new EmbeddedSolrServer(coreContainer, "expt");
        EmbeddedSolrServer atlasServer =
            new EmbeddedSolrServer(coreContainer, "atlas");

        // create IndexBuilderServices for genes (atlas) and experiments
        log.debug("Creating index building services for experiments and genes");
        exptIndexBuilder =
            new ExperimentAtlasIndexBuilderService(dao, exptServer);
        geneIndexBuilder =
            new GeneAtlasIndexBuilderService(dao, atlasServer);

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
   * Shuts down any cached resources relating to multiple solr cores within the
   * Atlas Index.  You should call this whenever the application requiring index
   * building services terminates (i.e. on webapp shutdown, or when the user
   * exits the application).
   *
   * @throws IndexBuilderException if shutting down this index builder failed
   *                               for any reason
   */
  public void shutdown() throws IndexBuilderException {
    if (running) {
      log.debug("Shutting down " + getClass().getSimpleName() + "...");
      service.shutdown();
      try {
        log.debug("Waiting for termination of running jobs");
        service.awaitTermination(300, TimeUnit.SECONDS);
      }
      catch (InterruptedException e) {
        log.error("Unable to shutdown service after 5 minutes.  " +
            "There may be running or suspended IndexBuilder tasks.  " +
            "If you are sure there are no tasks still running, then this " +
            "is a non-recoverable error - you should terminate this application");
        throw new IndexBuilderException(e);
      }
      finally {
        coreContainer.shutdown();
        log.debug("Shutdown complete");
        running = false;
      }
    }
    else {
      log.warn(
          "Ignoring attempt to shutdown() a " + getClass().getSimpleName() +
              " that is not running");
    }
  }

  public void buildIndex() {
    buildIndex(null);
  }

  public void buildIndex(IndexBuilderListener listener) {
    startIndexBuild(false, listener);
    log.info("Started IndexBuilder: " +
        "Building for " +
        (experiments ? "experiments " : "") +
        (experiments && genes ? " and " : "") +
        (genes ? "atlas " : "") +
        (pending ? "(only pending experiments) " : ""));
  }

  public void updateIndex() {
    updateIndex(null);
  }

  public void updateIndex(IndexBuilderListener listener) {
    startIndexBuild(true, listener);
    log.info("Started IndexBuilder: " +
        "Updating for " +
        (experiments ? "experiments " : "") +
        (experiments && genes ? " and " : "") +
        (genes ? "atlas " : "") +
        (pending ? "(only pending experiments) " : ""));
  }

  private void startIndexBuild(final boolean updateMode,
                               final IndexBuilderListener listener) {
    final long startTime = System.currentTimeMillis();
    final List<Future<Boolean>> indexingTasks =
        new ArrayList<Future<Boolean>>();

    if (experiments) {
      indexingTasks.add(service.submit(new Callable<Boolean>() {
        public Boolean call() throws IndexBuilderException {
          log.info("Starting building of experiments index");

          exptIndexBuilder.setPendingOnly(pending);
          exptIndexBuilder.setUpdateMode(updateMode);
          exptIndexBuilder.buildIndex();

          return true;
        }
      }));
    }

    if (genes) {
      indexingTasks.add(service.submit(new Callable<Boolean>() {
        public Boolean call() throws IndexBuilderException {
          log.info("Starting building of atlas gene index");

          geneIndexBuilder.setUpdateMode(updateMode);
          geneIndexBuilder.buildIndex();

          return true;
        }
      }));
    }

    // this tracks completion, if a listener was supplied
    if (listener != null) {
      service.submit(new Runnable() {
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

          // create our completion event
          if (success) {
            listener.buildSuccess(new IndexBuilderEvent(
                runTime, TimeUnit.SECONDS));
          }
          else {
            listener.buildError(new IndexBuilderEvent(
                runTime, TimeUnit.SECONDS, observedErrors));
          }
        }
      });
    }
  }

  /**
   * This method bootstraps an empty atlas index when starting an indexbuilder
   * from scratch.  Use this is the index could not be found, and you should get
   * a ready-to-build index with all required config files
   *
   * @param indexLocation the location in which to build the index
   * @throws IOException if the resources could not be written
   */
  private void unpackAtlasIndexTemplate(File indexLocation) throws IOException {
    // configure a list of resources we need from the indexbuilder jar
    Map<String, File> resourceMap = new HashMap<String, File>();
    resourceMap.put("solr/solr.xml",
                    new File(indexLocation,
                             "solr.xml"));
    resourceMap.put("solr/atlas/conf/scripts.conf",
                    new File(indexLocation,
                             "atlas" + File.separator + "conf" +
                                 File.separator + "scripts.conf"));
    resourceMap.put("solr/atlas/conf/admin-extra.html",
                    new File(indexLocation,
                             "atlas" + File.separator + "conf" +
                                 File.separator + "admin-extra.html"));
    resourceMap.put("solr/atlas/conf/protwords.txt",
                    new File(indexLocation,
                             "atlas" + File.separator + "conf" +
                                 File.separator + "protwords.txt"));
    resourceMap.put("solr/atlas/conf/stopwords.txt",
                    new File(indexLocation,
                             "atlas" + File.separator + "conf" +
                                 File.separator + "stopwords.txt"));
    resourceMap.put("solr/atlas/conf/synonyms.txt",
                    new File(indexLocation,
                             "atlas" + File.separator + "conf" +
                                 File.separator + "synonyms.txt"));
    resourceMap.put("solr/atlas/conf/schema.xml",
                    new File(indexLocation,
                             "atlas" + File.separator + "conf" +
                                 File.separator + "schema.xml"));
    resourceMap.put("solr/atlas/conf/solrconfig.xml",
                    new File(indexLocation,
                             "atlas" + File.separator + "conf" +
                                 File.separator + "solrconfig.xml"));
    resourceMap.put("solr/expt/conf/scripts.conf",
                    new File(indexLocation,
                             "expt" + File.separator + "conf" +
                                 File.separator + "scripts.conf"));
    resourceMap.put("solr/expt/conf/admin-extra.html",
                    new File(indexLocation,
                             "expt" + File.separator + "conf" +
                                 File.separator + "admin-extra.html"));
    resourceMap.put("solr/expt/conf/protwords.txt",
                    new File(indexLocation,
                             "expt" + File.separator + "conf" +
                                 File.separator + "protwords.txt"));
    resourceMap.put("solr/expt/conf/stopwords.txt",
                    new File(indexLocation,
                             "expt" + File.separator + "conf" +
                                 File.separator + "stopwords.txt"));
    resourceMap.put("solr/expt/conf/synonyms.txt",
                    new File(indexLocation,
                             "expt" + File.separator + "conf" +
                                 File.separator + "synonyms.txt"));
    resourceMap.put("solr/expt/conf/schema.xml",
                    new File(indexLocation,
                             "expt" + File.separator + "conf" +
                                 File.separator + "schema.xml"));
    resourceMap.put("solr/expt/conf/solrconfig.xml",
                    new File(indexLocation,
                             "expt" + File.separator + "conf" +
                                 File.separator + "solrconfig.xml"));

    // write out these resources
    for (String resourceName : resourceMap.keySet()) {
      writeResourceToFile(resourceName, resourceMap.get(resourceName));
    }
  }

  /**
   * Writes a classpath resource to a file in the specified location.  You
   * should not use this to overwrite files - if you attempt this, an
   * IOException will be thrown.  Also note that an IOException is thrown if the
   * file you specify is in a new directory and the parent directories required
   * could not be created.
   *
   * @param resourceName the name of the classpath resource to copy
   * @param file         the file to write the classpath resource to
   * @throws IOException if the resource could not properly be written out, or
   *                     if the file already exists
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
