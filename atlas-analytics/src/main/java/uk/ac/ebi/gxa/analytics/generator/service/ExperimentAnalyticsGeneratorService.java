package uk.ac.ebi.gxa.analytics.generator.service;

import org.kchine.r.RDataFrame;
import org.kchine.r.server.RServices;
import server.DirectJNI;
import uk.ac.ebi.gxa.R.AtlasRFactory;
import uk.ac.ebi.gxa.analytics.generator.AnalyticsGeneratorException;
import uk.ac.ebi.microarray.atlas.dao.AtlasDAO;
import uk.ac.ebi.microarray.atlas.model.Experiment;

import java.io.*;
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
                                               AtlasRFactory atlasRFactory) {
        super(atlasDAO, repositoryLocation, atlasRFactory);
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
                            createAnalyticsForExperiment(experiment.getAccession());
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

    protected void createAnalyticsForExperiment(String experimentAccession) throws AnalyticsGeneratorException {
        try {
            getLog().info("Generating analytics for experiment " + experimentAccession);

            // create a R service - DirectJNI gets an R service on the local machine
            RServices rs = DirectJNI.getInstance().getRServices();

            // load the R code that runs the analytics
            rs.sourceFromResource("analytics.R");

            // work out where the NetCDF(s) are located
            final Experiment experiment = getAtlasDAO().getExperimentByAccession(experimentAccession);
            File[] netCDFs = getRepositoryLocation().listFiles(new FilenameFilter() {

                public boolean accept(File file, String name) {
                    return name.matches("^" + experiment.getExperimentID() + "_[0-9]+(_ratios)?\\.nc$");
                }
            });

            for (File netCDF : netCDFs) {
                String callSim = "computeAnalytics(" + netCDF + "')";
                RDataFrame analytics = (RDataFrame) rs.getObject(callSim);

                analytics.getRowNames(); // todo - write analytics back to the database
            }

            getLog().info("Finalising analytics changes for " + experimentAccession);
        }
        catch (IOException e) {
            throw new AnalyticsGeneratorException(e);
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
            sb.append(line);
        }

        String rCode = sb.toString();
        System.out.println(rCode);

        return rCode;
    }
}
