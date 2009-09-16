package uk.ac.ebi.ae3.indexbuilder;

import uk.ac.ebi.ae3.indexbuilder.service.IndexBuilderService;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.beans.factory.xml.XmlBeanFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;

/**
 * A default implementation of {@link uk.ac.ebi.ae3.indexbuilder.IndexBuilder}
 * that constructs a SOLR index in a supplied directory.  By default, this will
 * include all genes and experiments.
 *
 * @author Tony Burdett
 * @date 20-Aug-2009
 */
public class DefaultIndexBuilder implements IndexBuilder<File> {
  private boolean genes = true;
  private boolean experiments = true;
  private boolean pending = false;

  private File indexLocation;

  // logging
  private static final Logger log =
      LoggerFactory.getLogger(DefaultIndexBuilder.class);

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

  public boolean getncludeExperiments() {
    return experiments;
  }

  public void setPendingMode(boolean pending) {
    this.pending = pending;
  }

  public boolean getPendingMode() {
    return pending;
  }

  public void buildIndex() throws IndexBuilderException {
    runIndexBuild(false);
  }

  public void updateIndex() throws IndexBuilderException {
    runIndexBuild(true);
  }

  private void runIndexBuild(boolean updateMode) throws IndexBuilderException {
    PropertyPlaceholderConfigurer conf = new PropertyPlaceholderConfigurer();
    conf.setLocation(new ClassPathResource("indexbuilder.properties"));

    XmlBeanFactory appContext = new XmlBeanFactory(new ClassPathResource(
        "app-context.xml"));
    conf.postProcessBeanFactory(appContext);

    log.info("Will build indexes: " +
        (experiments ? "experiments " : "") +
        (genes ? "gene" : "") +
        (updateMode ? " (update mode)" : "") +
        (pending ? "(only pending experiments)" : ""));

    try {
      if (experiments) {
        log.info("Building experiments index");
        IndexBuilderService exptIBS = (IndexBuilderService) appContext
            .getBean(Constants.exptIndexBuilderServiceID);
        exptIBS.setCreateOnlyPendingExps(pending);
        exptIBS.setUpdateMode(updateMode);
        exptIBS.buildIndex();
      }


      if (genes) {
        log.info("Building atlas gene index");
        IndexBuilderService geneIBS = (IndexBuilderService) appContext
            .getBean(Constants.geneIndexBuilderServiceID);
        geneIBS.setUpdateMode(updateMode);
        geneIBS.buildIndex();
      }
    }
    catch (Exception e) {
      throw new IndexBuilderException(
          "Something went wrong whilst building the index", e);
    }
    catch (IndexException e) {
      throw new IndexBuilderException(
          "Something went wrong whilst building the index", e);
    }
  }
}
