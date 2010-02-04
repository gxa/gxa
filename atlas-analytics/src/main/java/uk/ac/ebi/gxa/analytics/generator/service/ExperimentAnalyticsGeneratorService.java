package uk.ac.ebi.gxa.analytics.generator.service;

import org.kchine.r.server.RServices;
import uk.ac.ebi.gxa.analytics.compute.AtlasComputeService;
import uk.ac.ebi.gxa.analytics.compute.ComputeException;
import uk.ac.ebi.gxa.analytics.compute.ComputeTask;
import uk.ac.ebi.gxa.analytics.generator.AnalyticsGeneratorException;
import uk.ac.ebi.gxa.dao.AtlasDAO;
import uk.ac.ebi.gxa.dao.LoadStage;
import uk.ac.ebi.gxa.dao.LoadStatus;
import uk.ac.ebi.gxa.netcdf.reader.NetCDFProxy;
import uk.ac.ebi.microarray.atlas.model.Experiment;

import java.io.*;
import java.rmi.RemoteException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/**
 * Javadocs go here!
 *
 * @author Tony Burdett
 * @date 28-Sep-2009
 */
public class ExperimentAnalyticsGeneratorService extends AnalyticsGeneratorService<File> {
    private static final int NUM_THREADS = 8;

    public ExperimentAnalyticsGeneratorService(AtlasDAO atlasDAO,
                                               File repositoryLocation,
                                               AtlasComputeService atlasComputeService) {
        super(atlasDAO, repositoryLocation, atlasComputeService);
    }

    protected void createAnalytics() throws AnalyticsGeneratorException {
        // do initial setup - build executor service
        ExecutorService tpool = Executors.newFixedThreadPool(NUM_THREADS);

        // fetch experiments - check if we want all or only the pending ones
        List<Experiment> experiments = getPendingOnly()
                ? getAtlasDAO().getAllExperimentsPendingAnalytics()
                : getAtlasDAO().getAllExperiments();

        // if we're computing all analytics, some might not be pending, so reset them to pending up front
        if (!getPendingOnly()) {
            for (Experiment experiment : experiments) {
                getAtlasDAO().writeLoadDetails(
                        experiment.getAccession(), LoadStage.RANKING, LoadStatus.PENDING);
            }
        }

        // create a timer, so we can track time to generate analytics
        final AnalyticsTimer timer = new AnalyticsTimer(experiments);

        // the list of futures - we need these so we can block until completion
        List<Future<Boolean>> tasks =
                new ArrayList<Future<Boolean>>();

        // start the timer
        timer.start();

        try {
            // process each experiment to build the netcdfs
            for (final Experiment experiment : experiments) {
                // run each experiment in parallel
                tasks.add(tpool.submit(new Callable<Boolean>() {

                    public Boolean call() throws Exception {
                        long start = System.currentTimeMillis();
                        try {
                            createAnalyticsForExperiment(experiment.getAccession());
                            return true;
                        }
                        finally {
                            timer.completed(experiment.getExperimentID());

                            long end = System.currentTimeMillis();
                            String total = new DecimalFormat("#.##").format((end - start) / 1000);
                            String estimate = new DecimalFormat("#.##").format(timer.getCurrentEstimate() / 60000);

                            getLog().info("\n\tAnalytics for " + experiment.getAccession() +
                                    " created in " + total + "s." +
                                    "\n\tCompleted " + timer.getCompletedExperimentCount() + "/" +
                                    timer.getTotalExperimentCount() + "." +
                                    "\n\tEstimated time remaining: " + estimate + " mins.");
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
                    e.printStackTrace();
                    if (e.getCause() instanceof AnalyticsGeneratorException) {
                        throw (AnalyticsGeneratorException) e.getCause();
                    }
                    else {
                        throw new AnalyticsGeneratorException(
                                "An error occurred updating Analytics", e);
                    }
                }
                catch (InterruptedException e) {
                    e.printStackTrace();
                    throw new AnalyticsGeneratorException(
                            "An error occurred updating Analytics", e);
                }
            }
        }
        finally {
            // shutdown the service
            getLog().debug("Shutting down executor service in " + getClass().getSimpleName());

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
                e.printStackTrace();
                //noinspection ThrowFromFinallyBlock
                throw new AnalyticsGeneratorException(
                        "Failed to terminate service for " + getClass().getSimpleName() +
                                " cleanly - suspended tasks were found");
            }
        }
    }

    protected void createAnalyticsForExperiment(String experimentAccession) throws AnalyticsGeneratorException {
        getLog().info("Generating analytics for experiment " + experimentAccession);

        boolean success = false;
        try {
            // update loadmonitor - experiment is indexing
            getAtlasDAO().writeLoadDetails(
                    experimentAccession, LoadStage.RANKING, LoadStatus.WORKING);

            // first, delete old analytics for this experiment
            getAtlasDAO().deleteExpressionAnalytics(experimentAccession);

            // work out where the NetCDF(s) are located
            final Experiment experiment = getAtlasDAO().getExperimentByAccession(experimentAccession);
            File[] netCDFs = getRepositoryLocation().listFiles(new FilenameFilter() {

                public boolean accept(File file, String name) {
                    return name.matches("^" + experiment.getExperimentID() + "_[0-9]+(_ratios)?\\.nc$");
                }
            });

            if (netCDFs.length == 0) {
                throw new AnalyticsGeneratorException("No NetCDF files present for " + experimentAccession);
            }

            int count = 0;
            for (final File netCDF : netCDFs) {
                count++;
                ComputeTask<Void> computeAnalytics = new ComputeTask<Void>() {
                    public Void compute(RServices rs) throws RemoteException {
                        try {
                            // first, make sure we load the R code that runs the analytics
                            rs.sourceFromBuffer(getRCodeFromResource("R/analytics.R"));

                            // note - the netCDF file MUST be on the same file system where the workers run
                            getLog().debug("Starting compute task for " + netCDF.getAbsolutePath());
                            rs.getObject("computeAnalytics(\"" + netCDF.getAbsolutePath() + "\")");
                            getLog().debug("Completed compute task for " + netCDF.getAbsolutePath());

                            // todo - handle the returned results - RChar, array of codes?
                            return null;
                        }
                        catch (IOException e) {
                            e.printStackTrace();
                            throw new RemoteException("Unable to load R source from R/analytics.R");
                        }
                    }
                };

                // now run this compute task
                try {
                    getAtlasComputeService().computeTask(computeAnalytics);
                    getLog().debug("Compute task " + count + "/" + netCDFs.length + " for " + experimentAccession +
                            " has completed.");
                }
                catch (ComputeException e) {
                    getLog().error("Computation of analytics for " + netCDF.getAbsolutePath() + " failed: " +
                            e.getMessage());
                    e.printStackTrace();
                    success = false;
                }

                // computeAnalytics writes analytics data back to NetCDF, so now read back from NetCDF to database
                NetCDFProxy proxy = new NetCDFProxy(netCDF);

                // get unique factor values for the expression value matrix
                try {
                    int[] designElements = proxy.getDesignElements();
                    String[] uefvs = proxy.getUniqueFactorValues();

                    // uefvs is list of unique EF||EFV pairs - separate by splitting on ||
                    getLog().debug("Writing analytics for " + experimentAccession + " to the database...");
                    int uefvIndex = 0;
                    for (String uefv : uefvs) {
                        String[] values = uefv.split("\\|\\|"); // sheesh, crazy java regexing!
                        String ef = values[0];
                        if (values.length > 1) {
                            String efv = values[1];

                            double[] pValues = proxy.getPValuesForUniqueFactorValue(uefvIndex);
                            double[] tStatistics = proxy.getTStatisticsForUniqueFactorValue(uefvIndex);

                            // write values
                            getLog().trace("Writing analytics for experiment: " + experimentAccession + "; " +
                                    "EF: " + ef + "; EFV: " + efv);

                            try {
                                getAtlasDAO().writeExpressionAnalytics(
                                        experimentAccession, ef, efv, designElements, pValues, tStatistics);
                            }
                            catch (RuntimeException e) {
                                getLog().error("Writing analytics data for experiment: " + experimentAccession + "; " +
                                        "EF: " + ef + "; EFV: " + efv + " failed with errors: " + e.getMessage());
                                e.printStackTrace();
                                success = false;
                            }
                        }

                        // increment uefvIndex
                        uefvIndex++;
                    }
                }
                catch (IOException e) {
                    getLog().error("Unable to read from analytics at " + netCDF.getAbsolutePath());
                    e.printStackTrace();
                    success = false;
                }
            }
            success = true;
        }
        finally {
            getLog().info("Finalising analytics changes for " + experimentAccession);

            // update loadmonitor - experiment has completed analytics
            if (success) {
                getAtlasDAO().writeLoadDetails(
                        experimentAccession, LoadStage.RANKING, LoadStatus.DONE);
            }
            else {
                getAtlasDAO().writeLoadDetails(
                        experimentAccession, LoadStage.RANKING, LoadStatus.FAILED);
            }
        }
    }

    private String getRCodeFromResource(String resourcePath) throws IOException {
        // open a stream to the resource
        InputStream in = getClass().getClassLoader().getResourceAsStream(resourcePath);

        // create a reader to read in code
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));

        StringBuffer sb = new StringBuffer();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line).append("\n");
        }

        return sb.toString();
    }

    private class AnalyticsTimer {
        private int[] experimentIDs;
        private boolean[] completions;
        private int completedCount;
        private long startTime;
        private long lastEstimate;

        public AnalyticsTimer(List<Experiment> experiments) {
            experimentIDs = new int[experiments.size()];
            completions = new boolean[experiments.size()];
            int i = 0;
            for (Experiment exp : experiments) {
                experimentIDs[i] = exp.getExperimentID();
                completions[i] = false;
                i++;
            }

        }

        public synchronized AnalyticsTimer start() {
            startTime = System.currentTimeMillis();
            return this;
        }

        public synchronized AnalyticsTimer completed(int experimentID) {
            for (int i = 0; i < experimentIDs.length; i++) {
                if (experimentIDs[i] == experimentID) {
                    if (!completions[i]) {
                        completions[i] = true;
                        completedCount++;
                    }
                    break;
                }
            }

            // calculate estimate of time
            long timeWorking = System.currentTimeMillis() - startTime;
            lastEstimate = (timeWorking / completedCount) * (completions.length - completedCount);

            return this;
        }

        public synchronized long getCurrentEstimate() {
            return lastEstimate;
        }

        public synchronized int getCompletedExperimentCount() {
            return completedCount;
        }

        public synchronized int getTotalExperimentCount() {
            return completions.length;
        }
    }
}
