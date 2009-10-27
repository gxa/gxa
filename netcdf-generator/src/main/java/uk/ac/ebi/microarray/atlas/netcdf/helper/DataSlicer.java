package uk.ac.ebi.microarray.atlas.netcdf.helper;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import uk.ac.ebi.microarray.atlas.dao.AtlasDAO;
import uk.ac.ebi.microarray.atlas.model.*;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
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

  private Log log = LogFactory.getLog(getClass());

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
    Future<Set<DataSlice>> fetchLevelOne = service.submit(
        new Callable<Set<DataSlice>>() {

          public Set<DataSlice> call() throws Exception {
            log.debug(
                "Fetching array design data for " + experiment.getAccession());
            List<ArrayDesign> arrays = getAtlasDAO()
                .getArrayDesignByExperimentAccession(experiment.getAccession());

            // the set of level two fetch tasks - per array design
            Set<Future<DataSlice>> fetchLevelTwo =
                new HashSet<Future<DataSlice>>();

            for (final ArrayDesign arrayDesign : arrays) {
              fetchLevelTwo.add(service.submit(new Callable<DataSlice>() {

                public DataSlice call() throws Exception {
                  // the set of level three fetch tasks - per array design
                  Set<Future> fetchLevelThree = new HashSet<Future>();

                  // create a new data slice, for this experiment and arrayDesign
                  final DataSlice dataSlice =
                      new DataSlice(experiment, arrayDesign);

                  synchronized (fetchLevelThree) {
                    fetchLevelThree.add(service.submit(new Callable<Void>() {
                      public Void call() throws Exception {
                        // fetch assays for this array
                        log.debug(
                            "Fetching assay data for " +
                                arrayDesign.getAccession());
                        List<Assay> assays = getAtlasDAO()
                            .getAssaysByExperimentAndArray(
                                experiment.getAccession(),
                                arrayDesign.getAccession());
                        // and store
                        dataSlice.storeAssays(assays);

                        log.debug("Fetching samples data for each assay on " +
                            arrayDesign.getAccession());
                        for (Assay assay : assays) {
                          // fetch samples for this assay
                          List<Sample> samples = getAtlasDAO()
                              .getSamplesByAssayAccession(assay.getAccession());
                          for (Sample sample : samples) {
                            // and store
                            dataSlice.storeSample(assay, sample);
                          }
                        }

                        log.debug("Assay and Sample data for " +
                            arrayDesign.getAccession() + " stored");
                        return null;
                      }
                    }));
                  }

                  // fetch expression values for this array
                  synchronized (fetchLevelThree) {
                    fetchLevelThree.add(service.submit(new Callable<Void>() {
                      public Void call() throws Exception {
                        log.debug(
                            "Fetching expression values for " +
                                experiment.getAccession() +
                                " and " + arrayDesign.getAccession());
                        Map<Integer, Map<Integer, Float>> expressionValues =
                            getAtlasDAO()
                                .getExpressionValuesByExperimentAndArray(
                                    experiment.getExperimentID(),
                                    arrayDesign.getArrayDesignID());
                        // and store
                        dataSlice.storeExpressionValues(expressionValues);

                        log.debug("Expression Value data for " +
                            arrayDesign.getAccession() + " stored");
                        return null;
                      }
                    }));
                  }

                  // fetch design elements specific to this array design
                  synchronized (fetchLevelThree) {
                    fetchLevelThree.add(service.submit(new Callable<Void>() {
                      public Void call() throws Exception {
                        log.debug(
                            "Fetching design element data for " +
                                arrayDesign.getAccession());
                        Map<Integer, String> designElements = getAtlasDAO()
                            .getDesignElementsByArrayAccession(
                                arrayDesign.getAccession());
                        // and store
                        dataSlice.storeDesignElements(designElements);

                        // genes for this experiment were prefetched -
                        // compare to design elements and store, correctly indexed
                        // fetch design elements specific to this array design
                        log.debug("Indexing gene data by design element ID " +
                            "for " + arrayDesign.getAccession());
                        fetchGenesTask.get();
                        log.debug("Gene data for " +
                            arrayDesign.getAccession() + " acquired");
                        for (Gene gene : fetchGenesTask.get()) {
                          // check this gene maps to a stored design element
                          if (dataSlice.getDesignElements()
                              .containsKey(gene.getDesignElementID())) {
                            dataSlice
                                .storeGene(gene.getDesignElementID(), gene);

                            // remove from the unmapped list if necessary
                            synchronized (unmappedGenes) {
                              if (unmappedGenes.contains(gene)) {
                                unmappedGenes.remove(gene);
                              }
                            }
                          }
                          else {
                            // exclude this gene - design element not resolvable,
                            // or maybe it's just from a different array design
                            synchronized (unmappedGenes) {
                              unmappedGenes.add(gene);
                            }
                          }
                        }

                        // expression analyses for this experiment were prefetched -
                        // compare to design elements and store, correctly indexed
                        log.debug("Indexing analytics data by design element " +
                            "ID for " + arrayDesign.getAccession());
                        fetchAnalyticsTask.get();
                        log.debug("Analytics data for " +
                            arrayDesign.getAccession() + " acquired");
                        for (ExpressionAnalysis analysis :
                            fetchAnalyticsTask.get()) {
                          if (dataSlice.getDesignElements()
                              .containsKey(analysis.getDesignElementID())) {
                            dataSlice.storeExpressionAnalysis(
                                analysis.getDesignElementID(), analysis);

                            // remove from the unmapped list if necessary
                            synchronized (unmappedAnalytics) {
                              if (unmappedAnalytics.contains(analysis)) {
                                unmappedAnalytics.remove(analysis);
                              }
                            }
                          }
                          else {
                            // exclude this gene - design element not resolvable,
                            // or maybe it's just from a different array design
                            synchronized (unmappedAnalytics) {
                              unmappedAnalytics.add(analysis);
                            }
                          }
                        }

                        log.debug("Design Element/Gene/Analytics data for " +
                            arrayDesign.getAccession() + " stored");
                        return null;
                      }
                    }));
                  }

                  // block until all level three tasks are complete
                  synchronized (fetchLevelThree) {
                    log.debug("Waiting for all level three tasks to complete " +
                        "(modify each dataslice with required data)");
                    for (Future task : fetchLevelThree) {
                      try {
                        task.get();
                      }
                      catch (InterruptedException e) {
                        throw new DataSlicingException(
                            "A thread handling data slicing was interrupted",
                            e);
                      }
                      catch (ExecutionException e) {
                        e.printStackTrace();
                        if (e.getCause() != null) {
                          throw new DataSlicingException(
                              "A thread handling data slicing failed.  Caused by: " +
                                  (e.getMessage() == null ||
                                      e.getMessage().equals("")
                                      ? e.getCause().getClass().getSimpleName()
                                      : e.getMessage()),
                              e.getCause());
                        }
                        else {
                          throw new DataSlicingException(
                              "A thread handling data slicing failed", e);
                        }
                      }
                    }
                    log.debug("Level three tasks to populate " + dataSlice
                        + " completed");
                  }

                  // now evaluate property mappings
                  log.debug("Evaluating property/value/assay " +
                      "indices for " + dataSlice.toString());
                  dataSlice.evaluatePropertyMappings();

                  // save this dataslice
                  log.debug("Compiled dataslice... " + dataSlice.toString());
                  return dataSlice;
                }
              }));
            }

            // wait for level two fetch tasks to complete
            Set<DataSlice> dataSlices = new HashSet<DataSlice>();
            synchronized (fetchLevelTwo) {
              log.debug("Waiting for all level two tasks to complete " +
                  "(create and populate each dataslice with experiment " +
                  "and array data)");
              for (Future<DataSlice> task : fetchLevelTwo) {
                dataSlices.add(task.get());
                log.debug("Level two task for " + task.get() + " complete");
              }
            }

            // and return
            log.debug("Compiled the set of all dataslices for " +
                experiment.getAccession());
            return dataSlices;
          }
        });

    // wait for dataslicing to finish
    try {
      log.debug("Waiting for level one task to complete " +
          "(fetch arrays and populate the dataslice set)");
      Set<DataSlice> results = fetchLevelOne.get();
      log.debug("Level one task completed");

      if (unmappedGenes.size() > 0 || unmappedAnalytics.size() > 0) {
        log.warn(unmappedGenes.size() + "/" + fetchGenesTask.get().size() +
            " genes and " + unmappedAnalytics.size() + "/" +
            fetchAnalyticsTask.get().size() + " expression analytics " +
            "that were recovered for " + experiment.getAccession() +
            " could " +
            "not be mapped to known design elements");

        // todo - generate a log file of unmapped genes and expression analytics
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
  }
}
