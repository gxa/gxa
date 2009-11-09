package uk.ac.ebi.gxa.netcdf.generator.helper;

import uk.ac.ebi.microarray.atlas.model.ArrayDesign;
import uk.ac.ebi.microarray.atlas.model.Experiment;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

/**
 * Javadocs go here!
 *
 * @author Tony Burdett
 * @date 28-Oct-2009
 */
public class ArrayDesignSlicer extends CallableSlicer<DataSlice> {
  // required initial resources
  private final Experiment experiment;
  private final ArrayDesign arrayDesign;

  public ArrayDesignSlicer(ExecutorService service, Experiment experiment,
                           ArrayDesign arrayDesign) {
    super(service);
    this.experiment = experiment;
    this.arrayDesign = arrayDesign;
  }

  public DataSlice call() throws Exception {
    // the set of level three fetch tasks - per array design
    Set<Future> dataFetching = new HashSet<Future>();

    // create a new data slice, for this experiment and arrayDesign
    final DataSlice dataSlice = new DataSlice(experiment, arrayDesign);

    // run nested fetch tasks
    synchronized (dataFetching) {
      AssaySlicer assaySlicer = new AssaySlicer(
          getService(), experiment, arrayDesign, dataSlice);
      assaySlicer.setAtlasDAO(
          getAtlasDAO());

      dataFetching.add(getService().submit(assaySlicer));
    }

    synchronized (dataFetching) {
      ExpressionValueSlicer evSlicer = new ExpressionValueSlicer(
          getService(), experiment, arrayDesign, dataSlice);
      evSlicer.setAtlasDAO(
          getAtlasDAO());

      dataFetching.add(getService().submit(evSlicer));
    }

    // fetch design elements specific to this array design
    synchronized (dataFetching) {
      DesignElementSlicer deSlicer = new DesignElementSlicer(
          getService(), arrayDesign, dataSlice);
      deSlicer.setAtlasDAO(
          getAtlasDAO());
      deSlicer.setGeneFetchingStrategy(
          fetchGenesTask, unmappedGenes);
      deSlicer.setAnalyticsFetchingStrategy(
          fetchAnalyticsTask, unmappedAnalytics);

      dataFetching.add(getService().submit(deSlicer));
    }

    // block until all data fetching tasks are complete
    synchronized (dataFetching) {
      getLog().debug("Waiting for all data slicing tasks to complete " +
          "(modify each dataslice with required data)");
      for (Future task : dataFetching) {
        try {
          task.get();
        }
        catch (InterruptedException e) {
          throw new DataSlicingException(
              "A thread handling data slicing was interrupted", e);
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
      getLog().debug(
          "Data slicing tasks to populate " + dataSlice + " completed");
    }

    // now evaluate property mappings
    getLog().debug("Evaluating property/value/assay indices for " +
        dataSlice.toString());
    dataSlice.evaluatePropertyMappings();

    // save this dataslice
    getLog().debug("Compiled dataslice... " + dataSlice.toString());
    return dataSlice;
  }
}
