package uk.ac.ebi.gxa.netcdf.generator.helper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.microarray.atlas.dao.AtlasDAO;
import uk.ac.ebi.microarray.atlas.model.Experiment;
import uk.ac.ebi.microarray.atlas.model.ExpressionAnalysis;
import uk.ac.ebi.microarray.atlas.model.Gene;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;

/**
 * A device for slicing the data in a single experiment into bits that are
 * suitable for storing in NetCDFs.  Generally, this means a single slice of
 * data per array design, and each slice indexes every sample by the assay it is
 * associated with.  Only the assays appropriate for the paired array design are
 * present.  This class basically takes an AtlasDAO it can use to fetch any
 * additional data, and performs the slicing on a supplied experiment.
 *
 * @author Tony Burdett
 * @date 30-Sep-2009
 */
public class DataSlicer {
  private AtlasDAO atlasDAO;

  // logger
  private Logger log = LoggerFactory.getLogger(getClass());

  public DataSlicer(AtlasDAO atlasDAO) {
    this.atlasDAO = atlasDAO;
  }

  public AtlasDAO getAtlasDAO() {
    return atlasDAO;
  }

  public void setAtlasDAO(AtlasDAO atlasDAO) {
    this.atlasDAO = atlasDAO;
  }

  public Set<DataSlice> sliceExperiment(final Experiment experiment)
      throws DataSlicingException {
    // create a service to handle slicing tasks in parallel
    final ExecutorService service = Executors.newCachedThreadPool();

    // prefetch genes by experiment
    final Future<List<Gene>> fetchGenesTask =
        service.submit(new Callable<List<Gene>>() {
          public List<Gene> call() throws Exception {
            log.debug("Fetching genes data for " + experiment.getAccession());
            return getAtlasDAO().getGenesByExperimentAccession(
                experiment.getAccession());
          }
        });

    // prefetch expression analysis by experiment
    final Future<List<ExpressionAnalysis>> fetchAnalyticsTask =
        service.submit(new Callable<List<ExpressionAnalysis>>() {

          public List<ExpressionAnalysis> call() throws Exception {
            log.debug(
                "Fetching analytics data for " + experiment.getAccession());
            return getAtlasDAO().getExpressionAnalyticsByExperimentID(
                experiment.getExperimentID());
          }
        });

    // create dumps for things that didn't resolve to a design element for any data slice
    final Set<Gene> unmappedGenes =
        new HashSet<Gene>();
    final Set<ExpressionAnalysis> unmappedAnalytics =
        new HashSet<ExpressionAnalysis>();


    // start fetching data...

    // fetch array designs and iterate
    ExperimentSlicer exptSlicer = new ExperimentSlicer(service, experiment);
    exptSlicer.setAtlasDAO(
        getAtlasDAO());
    exptSlicer.setGeneFetchingStrategy(
        fetchGenesTask, unmappedGenes);
    exptSlicer.setAnalyticsFetchingStrategy(
        fetchAnalyticsTask, unmappedAnalytics);
    // submit this task
    Future<Set<DataSlice>> exptFetching = service.submit(exptSlicer);

    // wait for dataslicing to finish
    try {
      log.debug("Waiting for experiment slicing task to complete " +
          "(fetch arrays and populate the dataslice set)");
      Set<DataSlice> results = exptFetching.get();
      log.debug("Experiment slicing task completed");

      synchronized (unmappedGenes) {
        synchronized (unmappedAnalytics) {
          if (unmappedGenes.size() > 0 || unmappedAnalytics.size() > 0) {
            log.warn(unmappedGenes.size() + "/" + fetchGenesTask.get().size() +
                " genes and " + unmappedAnalytics.size() + "/" +
                fetchAnalyticsTask.get().size() + " expression analytics " +
                "that were recovered for " + experiment.getAccession() +
                " could not be mapped to known design elements");

            // todo - generate a log file of unmapped genes and expression analytics
          }
        }
      }

      log.debug("Returning the sliced data");
      return results;
    }
    catch (InterruptedException e) {
      throw new DataSlicingException(
          "A thread handling data slicing was interrupted", e);
    }
    catch (ExecutionException e) {
      if (e.getCause() != null) {
        throw new DataSlicingException(
            "A thread handling data slicing failed.  Caused by: " +
                (e.getMessage() == null || e.getMessage().equals("")
                    ? e.getCause().getClass().getSimpleName()
                    : e.getMessage()),
            e.getCause());
      }
      else {
        throw new DataSlicingException(
            "A thread handling data slicing failed", e);
      }
    }
    finally {
      // shutdown the service
      log.debug("Shutting down executor service in " +
          getClass().getSimpleName());

      try {
        service.shutdown();
        service.awaitTermination(60, TimeUnit.SECONDS);
        if (!service.isTerminated()) {
          //noinspection ThrowFromFinallyBlock
          throw new DataSlicingException(
              "Failed to terminate service for " + getClass().getSimpleName() +
                  " cleanly - suspended tasks were found");
        }
        else {
          log.debug("Executor service exited cleanly");
        }
      }
      catch (InterruptedException e) {
        //noinspection ThrowFromFinallyBlock
        throw new DataSlicingException(
            "Failed to terminate service for " + getClass().getSimpleName() +
                " cleanly - suspended tasks were found");
      }
    }
  }
}
