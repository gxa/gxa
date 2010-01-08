package uk.ac.ebi.gxa.analytics.generator.service;

import org.kchine.r.RList;
import org.kchine.r.server.RServices;
import uk.ac.ebi.gxa.analytics.compute.AtlasComputeService;
import uk.ac.ebi.gxa.analytics.compute.ComputeTask;
import uk.ac.ebi.gxa.analytics.generator.AnalyticsGeneratorException;
import uk.ac.ebi.gxa.netcdf.reader.NetCDFProxy;
import uk.ac.ebi.microarray.atlas.dao.AtlasDAO;
import uk.ac.ebi.microarray.atlas.dao.LoadStage;
import uk.ac.ebi.microarray.atlas.dao.LoadStatus;
import uk.ac.ebi.microarray.atlas.model.Experiment;

import java.io.*;
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
public class ExperimentAnalyticsGeneratorService extends AnalyticsGeneratorService<File> {
    private static final int NUM_THREADS = 64;

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

        // the list of futures - we need these so we can block until completion
        List<Future<Boolean>> tasks =
                new ArrayList<Future<Boolean>>();

        try {
            // process each experiment to build the netcdfs
            for (final Experiment experiment : experiments) {
                // run each experiment in parallel
                tasks.add(tpool.submit(new Callable<Boolean>() {

                    public Boolean call() throws Exception {
                        boolean success = false;
                        try {
                            // update loadmonitor - experiment is indexing
                            getAtlasDAO().writeLoadDetails(
                                    experiment.getAccession(), LoadStage.RANKING, LoadStatus.WORKING);

                            createAnalyticsForExperiment(experiment.getAccession());
                            success = true;

                            // update loadmonitor - experiment is indexing
                            getAtlasDAO().writeLoadDetails(
                                    experiment.getAccession(), LoadStage.RANKING, LoadStatus.DONE);

                            return success;
                        }
                        finally {
                            // if success if true, everything completed as expected, but if it's false we got
                            // an uncaught exception, so make sure we update loadmonitor to reflect that this failed
                            if (!success) {
                                getAtlasDAO().writeLoadDetails(
                                        experiment.getAccession(), LoadStage.RANKING, LoadStatus.FAILED);
                            }
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

    protected void createAnalyticsForExperiment(String experimentAccession) throws AnalyticsGeneratorException {
        getLog().info("Generating analytics for experiment " + experimentAccession);

        // work out where the NetCDF(s) are located
        final Experiment experiment = getAtlasDAO().getExperimentByAccession(experimentAccession);
        File[] netCDFs = getRepositoryLocation().listFiles(new FilenameFilter() {

            public boolean accept(File file, String name) {
                return name.matches("^" + experiment.getExperimentID() + "_[0-9]+(_ratios)?\\.nc$");
            }
        });

        for (final File netCDF : netCDFs) {
            ComputeTask<RList> computeAnalytics = new ComputeTask<RList>() {
                public RList compute(RServices rs) throws RemoteException {
                    try {
                        // first, make sure we load the R code that runs the analytics
                        rs.sourceFromBuffer(getRCodeFromResource("R/analytics.R"));

                        // fixme: this MUST be on the same file system where the workers run
                        return (RList) rs.getObject("computeAnalytics('" + netCDF.getAbsolutePath() + "')");
                    }
                    catch (IOException e) {
                        e.printStackTrace();
                        throw new RemoteException("Unable to load R source from R/analytics.R");
                    }
                }
            };

            // now run this compute task
            getAtlasComputeService().computeTask(computeAnalytics);

            // computeAnalytics writes analytics data back to NetCDF, so now read back from NetCDF to database
            NetCDFProxy proxy = new NetCDFProxy(netCDF);

            try {
                // get unique factor values for the expression value matrix
                String[] uefvs = proxy.getUniqueFactorValues();

                // uefvs is indexed by 

            }
            catch (IOException e) {
                throw new AnalyticsGeneratorException("Could not access NetCDF at " + netCDF.getAbsolutePath(), e);
            }
        }

        getLog().info("Finalising analytics changes for " + experimentAccession);
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
}
