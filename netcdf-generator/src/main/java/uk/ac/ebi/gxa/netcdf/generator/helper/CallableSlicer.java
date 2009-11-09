package uk.ac.ebi.gxa.netcdf.generator.helper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.microarray.atlas.dao.AtlasDAO;
import uk.ac.ebi.microarray.atlas.model.ExpressionAnalysis;
import uk.ac.ebi.microarray.atlas.model.Gene;

import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

/**
 * An implementation of Callable that performs data slciing tasks common in the
 * constructiuon of NetCDF files.  THis abstract class contains methods to set
 * the required resources (namely, an {@link ExecutorService} and and {@link
 * uk.ac.ebi.microarray.atlas.dao.AtlasDAO} as well as the prefetched data,
 * genes and analytics.  As implementations may wish to update the collections
 * of genes and expression analytics that are not mapped to any design elements,
 * these fields are available
 *
 * @author Tony Burdett
 * @date 28-Oct-2009
 */
public abstract class CallableSlicer<T> implements Callable<T> {
  // service for running task
  private final ExecutorService service;

  // required DAO
  private AtlasDAO atlasDAO;

  // logger
  private Logger log = LoggerFactory.getLogger(getClass());

  /**
   * The {@link java.util.concurrent.Future} task that populates the list of
   * genes for any given experiment.
   */
  protected Future<List<Gene>> fetchGenesTask;
  /**
   * The {@link java.util.concurrent.Future} task that populates the list of
   * expression analytics for any given experiment.
   */
  protected Future<List<ExpressionAnalysis>> fetchAnalyticsTask;

  /**
   * The set of genes that cannot be resolved to any design elements present in
   * the database.
   */
  protected Set<Gene> unmappedGenes;
  /**
   * The set of expression analytics that cannot be resolved to any design
   * elements present in the database.
   */
  protected Set<ExpressionAnalysis> unmappedAnalytics;

  public CallableSlicer(ExecutorService service) {
    this.service = service;
  }

  public ExecutorService getService() {
    return service;
  }

  public Logger getLog() {
    return log;
  }

  public AtlasDAO getAtlasDAO() {
    return atlasDAO;
  }

  public void setAtlasDAO(AtlasDAO atlasDAO) {
    this.atlasDAO = atlasDAO;
  }

  /**
   * Sets the "strategy" for fetching genes.  This consists of the {@link
   * Future} that is asynchronously fetching gene data, and the set that should
   * store all genes that cannot be mapped to design elements in the database.
   *
   * @param fetchGenesTask the task for fetching genes, running asynchronously
   * @param unmappedGenes  the set in which to store any unmapped genes
   */
  public void setGeneFetchingStrategy(Future<List<Gene>> fetchGenesTask,
                                      Set<Gene> unmappedGenes) {
    this.fetchGenesTask = fetchGenesTask;
    this.unmappedGenes = unmappedGenes;
  }

  /**
   * Sets the "strategy" for fetching expression analytics.  This consists of
   * the {@link Future} that is asynchronously fetching expression data, and the
   * set that should store all expression analytics that cannot be mapped to
   * design elements in the database.
   *
   * @param fetchAnalyticsTask the task for fetching analytics, running
   *                           asynchronously
   * @param unmappedAnalytics  the set in which to store any unmapped analytics
   */
  public void setAnalyticsFetchingStrategy(
      Future<List<ExpressionAnalysis>> fetchAnalyticsTask,
      Set<ExpressionAnalysis> unmappedAnalytics) {
    this.fetchAnalyticsTask = fetchAnalyticsTask;
    this.unmappedAnalytics = unmappedAnalytics;
  }
}
