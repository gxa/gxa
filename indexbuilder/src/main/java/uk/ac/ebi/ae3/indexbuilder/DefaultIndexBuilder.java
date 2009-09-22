package uk.ac.ebi.ae3.indexbuilder;

import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.core.CoreContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.jdbc.core.JdbcTemplate;
import uk.ac.ebi.ae3.indexbuilder.dao.AtlasDAO;
import uk.ac.ebi.ae3.indexbuilder.service.ExperimentAtlasIndexBuilder;
import uk.ac.ebi.ae3.indexbuilder.service.GeneAtlasIndexBuilder;
import uk.ac.ebi.ae3.indexbuilder.service.IndexBuilderService;

import javax.sql.DataSource;
import java.io.File;

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
  private DataSource dataSource;
  private File indexLocation;

  private boolean genes = true;
  private boolean experiments = true;
  private boolean pending = false;

  private CoreContainer coreContainer;
  private IndexBuilderService geneIndexBuilder;
  private IndexBuilderService exptIndexBuilder;

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

    // first, create a solr CoreContainer
    File solr = new File(indexLocation, "solr.xml");
    coreContainer = new CoreContainer();
    coreContainer.load(indexLocation.getAbsolutePath(), solr);

    // create an embedded solr server for experiments and genes from this container
    EmbeddedSolrServer exptServer =
        new EmbeddedSolrServer(coreContainer, "experiments");
    EmbeddedSolrServer atlasServer =
        new EmbeddedSolrServer(coreContainer, "atlas");

    // create IndexBuilderServices for genes (atlas) and experiments
    geneIndexBuilder = new GeneAtlasIndexBuilder(dao, exptServer);
    exptIndexBuilder = new ExperimentAtlasIndexBuilder(dao, atlasServer);

    // finally, create an executor service for processing calls to build the index
    // todo - create a service so that index building is parallelised
  }

  public void buildIndex() throws IndexBuilderException {
    runIndexBuild(false);
  }

  public void updateIndex() throws IndexBuilderException {
    runIndexBuild(true);
  }

  /**
   * Shuts down any cached resources relating to multiple solr cores within the
   * Atlas Index.  You should call this whenever the application requiring index
   * building services terminates (i.e. on webapp shutdown, or when the user
   * exits the application).
   */
  public void shutdownIndex() {
    coreContainer.shutdown();
  }

  private void runIndexBuild(boolean updateMode) throws IndexBuilderException {
    log.info("Will build indexes: " +
        (experiments ? "experiments " : "") +
        (genes ? "gene" : "") +
        (updateMode ? " (update mode)" : "") +
        (pending ? "(only pending experiments)" : ""));

    if (experiments) {
      log.info("Building experiments index");

      exptIndexBuilder.setPendingOnly(pending);
      exptIndexBuilder.setUpdateMode(updateMode);
      exptIndexBuilder.buildIndex();
    }


    if (genes) {
      log.info("Building atlas gene index");

      geneIndexBuilder.setUpdateMode(updateMode);
      geneIndexBuilder.buildIndex();
    }
  }
}
