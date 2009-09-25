package uk.ac.ebi.ae3.indexbuilder;

import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.core.CoreContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.jdbc.core.JdbcTemplate;
import uk.ac.ebi.ae3.indexbuilder.service.ExperimentAtlasIndexBuilder;
import uk.ac.ebi.ae3.indexbuilder.service.GeneAtlasIndexBuilder;
import uk.ac.ebi.ae3.indexbuilder.service.IndexBuilderService;
import uk.ac.ebi.microarray.atlas.dao.AtlasDAO;

import javax.sql.DataSource;
import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

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

  // logging
  private static final Logger log =
      LoggerFactory.getLogger(DefaultIndexBuilder.class);

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
    // do some initialization...

    // create a spring jdbc template
    JdbcTemplate template = new JdbcTemplate(dataSource);

    // create an atlas dao
    AtlasDAO dao = new AtlasDAO();
    dao.setJdbcTemplate(template);

    // check for the presence of the index
    File solr = new File(indexLocation, "solr.xml");
    if (!solr.exists()) {
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
    coreContainer = new CoreContainer();
    coreContainer.load(indexLocation.getAbsolutePath(), solr);

    // create an embedded solr server for experiments and genes from this container
    EmbeddedSolrServer exptServer =
        new EmbeddedSolrServer(coreContainer, "expt");
    EmbeddedSolrServer atlasServer =
        new EmbeddedSolrServer(coreContainer, "atlas");

    // create IndexBuilderServices for genes (atlas) and experiments
    exptIndexBuilder = new ExperimentAtlasIndexBuilder(dao, exptServer);
    geneIndexBuilder = new GeneAtlasIndexBuilder(dao, atlasServer);

    // finally, create an executor service for processing calls to build the index
    service = Executors.newCachedThreadPool();
  }

  public void buildIndex() {
    startIndexBuild(false);
    log.info("Started IndexBuilder: " +
        "Building for " +
        (experiments ? "experiments" : "") +
        (experiments && genes ? " and genes" : "") + ", pending mode " +
        (pending ? "ON" : "OFF"));
  }

  public void updateIndex() {
    startIndexBuild(true);
  }

  /**
   * Shuts down any cached resources relating to multiple solr cores within the
   * Atlas Index.  You should call this whenever the application requiring index
   * building services terminates (i.e. on webapp shutdown, or when the user
   * exits the application).
   */
  public void shutdownIndex() {
    service.shutdown();
    try {
      service.awaitTermination(5, TimeUnit.MINUTES);
    }
    catch (InterruptedException e) {
      log.error("Unable to shutdown service, there may be suspended " +
          "IndexBuilder tasks.  This is a non-recoverable error - you should " +
          "terminate this application");
    }
    coreContainer.shutdown();
  }

  private void startIndexBuild(final boolean updateMode) {
    log.info("Will build indexes: " +
        (experiments ? "experiments " : "") +
        (experiments && genes ? " and " : "") +
        (genes ? "atlas " : "") +
        (updateMode ? "(update mode) " : "") +
        (pending ? "(only pending experiments) " : ""));

    if (experiments) {
      service.submit(new Callable<Boolean>() {
        public Boolean call() throws Exception {
          log.info("Starting building of experiments index");

          exptIndexBuilder.setPendingOnly(pending);
          exptIndexBuilder.setUpdateMode(updateMode);
          exptIndexBuilder.buildIndex();

          return true;
        }
      });
    }

    if (genes) {
      service.submit(new Callable<Boolean>() {
        public Boolean call() throws Exception {
          log.info("Starting building of atlas gene index");

          geneIndexBuilder.setUpdateMode(updateMode);
          geneIndexBuilder.buildIndex();

          return true;
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
