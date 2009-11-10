package uk.ac.ebi.gxa.analytics.generator.service;

import ucar.nc2.NetcdfFileWriteable;
import uk.ac.ebi.microarray.atlas.dao.AtlasDAO;
import uk.ac.ebi.microarray.atlas.model.ArrayDesign;
import uk.ac.ebi.microarray.atlas.model.Experiment;
import uk.ac.ebi.gxa.analytics.generator.AnalyticsGeneratorException;

import java.io.File;
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/**
 * Javadocs go here!
 *
 * @author Tony Burdett
 * @date 28-Sep-2009
 */
public class ExperimentAnalyticsGeneratorService
    extends AnalyticsGeneratorService<File> {
  private static final int NUM_THREADS = 64;

  public ExperimentAnalyticsGeneratorService(AtlasDAO atlasDAO,
                                          File repositoryLocation) {
    super(atlasDAO, repositoryLocation);
  }

  protected void createAnalytics() throws AnalyticsGeneratorException {
    // do initial setup - build executor service
    ExecutorService tpool = Executors.newFixedThreadPool(NUM_THREADS);

    // fetch experiments - check if we want all or only the pending ones
    List<Experiment> experiments = getPendingOnly()
        ? getAtlasDAO().getAllExperimentsPendingAnalytics()
        : getAtlasDAO().getAllExperiments();

    // the list of futures - we need these so we can block until completion
    List<Future<Boolean>> tasks =
        new ArrayList<Future<Boolean>>();

    try {
      // process each experiment to build the netcdfs
      for (final Experiment experiment : experiments) {
        // run each experiment in parallel
        tasks.add(tpool.submit(new Callable<Boolean>() {

          public Boolean call() throws Exception {
            try {
              getLog().info("Generating analytics - experiment " +
                  experiment.getAccession());

                // TODO: Generate Analytics
                // something like

                /*
                AtlasComputeService svc = new AtlasComputeService();
                RDataFrame analytics = svc.computeTask(new ComputeTask<RDataFrame>() {
                    public RDataFrame compute(RServices R) throws RemoteException {
                        String callSim = "computeAnalytics(" + experiment.getSourceAnalytics() + "')";
                        return (RDataFrame) R.getObject(callSim);
                    }
                });

                */

                // write analytics RDataFrame to DB/NetCDF...

              getLog().info("Finalising analytics changes for " +
                  experiment.getAccession());
              return true;
            }
            catch (Exception e) {
              e.printStackTrace();
              throw e;
            }
          }
        }));
      }

      // block until completion, and throw any errors
      for (Future<Boolean> task : tasks) {
        try {
          task.get();
        }
        catch (ExecutionException e) {
          if (e.getCause() instanceof AnalyticsGeneratorException) {
            throw (AnalyticsGeneratorException) e.getCause();
          }
          else {
            throw new AnalyticsGeneratorException(
                "An error occurred updating Analytics", e);
          }
        }
        catch (InterruptedException e) {
          throw new AnalyticsGeneratorException(
              "An error occurred updating Analytics", e);
        }
      }
    }
    finally {
      // shutdown the service
      getLog().debug("Shutting down executor service in " +
          getClass().getSimpleName());

      try {
        tpool.shutdown();
        tpool.awaitTermination(60, TimeUnit.SECONDS);
        if (!tpool.isTerminated()) {
          //noinspection ThrowFromFinallyBlock
          throw new AnalyticsGeneratorException(
              "Failed to terminate service for " + getClass().getSimpleName() +
                  " cleanly - suspended tasks were found");
        }
        else {
          getLog().debug("Executor service exited cleanly");
        }
      }
      catch (InterruptedException e) {
        //noinspection ThrowFromFinallyBlock
        throw new AnalyticsGeneratorException(
            "Failed to terminate service for " + getClass().getSimpleName() +
                " cleanly - suspended tasks were found");
      }
    }
  }

  protected void createAnalyticsForExperiment(String experimentAccession)
      throws AnalyticsGeneratorException {
    // do initial setup - build executor service
    ExecutorService tpool = Executors.newFixedThreadPool(NUM_THREADS);

    // fetch experiment - ignore pending if explicitly building by accession
    Experiment experiment = getAtlasDAO().
        getExperimentByAccession(experimentAccession);

    // the list of futures - we need these so we can block until completion
    List<Future<Boolean>> tasks =
        new ArrayList<Future<Boolean>>();

    try {
      getLog().info("Generating analytics - experiment " +
          experiment.getAccession());

        // TODO: Generate Analytics
        // something like

        /*
        final RServices rs = DirectJNI.getInstance().getRServices();

                String simSrc = getRCodeFromResource("/analytics.R");
                R.sourceFromBuffer(simSrc);

        String callSim = "computeAnalytics(" + experiment.getSourceNetCDF() + "')";
        RDataFrame analytics = (RDataFrame) rs.getObject(callSim);

        AtlasComputeService svc = new AtlasComputeService();
        RDataFrame analytics = svc.computeTask(new ComputeTask<RDataFrame>() {
            public RDataFrame compute(RServices R) throws RemoteException {
                String callSim = "computeAnalytics(" + experiment.getSourceNetCDF() + "')";
                return (RDataFrame) R.getObject(callSim);
            }
        });

        */

      // read values form NetCDF --> DB ...

      // block until completion, and throw any errors
      for (Future<Boolean> task : tasks) {
        try {
          task.get();
        }
        catch (ExecutionException e) {
          if (e.getCause() instanceof AnalyticsGeneratorException) {
            throw (AnalyticsGeneratorException) e.getCause();
          }
          else {
            throw new AnalyticsGeneratorException(
                "An error occurred updating Analyticss", e);
          }
        }
        catch (InterruptedException e) {
          throw new AnalyticsGeneratorException(
              "An error occurred updating Analyticss", e);
        }
      }
    }
    finally {
      // shutdown the service
      getLog().debug("Shutting down executor service in " +
          getClass().getSimpleName() + " (" + tpool.toString() + ") for " +
          experimentAccession);

      try {
        tpool.shutdown();
        tpool.awaitTermination(60, TimeUnit.SECONDS);
        if (!tpool.isTerminated()) {
          //noinspection ThrowFromFinallyBlock
          throw new AnalyticsGeneratorException(
              "Failed to terminate service for " + getClass().getSimpleName() +
                  " cleanly - suspended tasks were found");
        }
        else {
          getLog().debug("Executor service exited cleanly");
        }
      }
      catch (InterruptedException e) {
        //noinspection ThrowFromFinallyBlock
        throw new AnalyticsGeneratorException(
            "Failed to terminate service for " + getClass().getSimpleName() +
                " cleanly - suspended tasks were found");
      }
    }
  }

  private NetcdfFileWriteable createAnalytics(Experiment experiment,
                                           ArrayDesign arrayDesign)
      throws IOException {
    // repository location exists?
    if (!getRepositoryLocation().exists()) {
      if (!getRepositoryLocation().mkdirs()) {
        throw new IOException("Could not read create directory at " +
            getRepositoryLocation().getAbsolutePath());
      }
    }

    String netcdfName =
        experiment.getExperimentID() + "_" +
            arrayDesign.getArrayDesignID() + ".nc";
    String netcdfPath =
        new File(getRepositoryLocation(), netcdfName).getAbsolutePath();
    NetcdfFileWriteable netcdfFile =
        NetcdfFileWriteable.createNew(netcdfPath, false);

    // add metadata global attributes
    netcdfFile.addGlobalAttribute(
        "CreateAnalytics_VERSION",
        versionDescriptor);
    netcdfFile.addGlobalAttribute(
        "experiment_accession",
        experiment.getAccession());
//    netcdfFile.addGlobalAttribute(
//        "quantitationType",
//        qtType); // fixme: quantitation type lookup required
    netcdfFile.addGlobalAttribute(
        "ADaccession",
        arrayDesign.getAccession());
    netcdfFile.addGlobalAttribute(
        "ADname",
        arrayDesign.getName());

    return netcdfFile;
  }
}
