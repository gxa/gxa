/*
 * Copyright 2008-2010 Microarray Informatics Team, EMBL-European Bioinformatics Institute
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *
 * For further details of the Gene Expression Atlas project, including source code,
 * downloads and documentation, please see:
 *
 * http://gxa.github.com/gxa
 */

package uk.ac.ebi.gxa.analytics.generator.service;

import uk.ac.ebi.gxa.analytics.compute.AtlasComputeService;
import uk.ac.ebi.gxa.analytics.compute.ComputeException;
import uk.ac.ebi.gxa.analytics.compute.ComputeTask;
import uk.ac.ebi.gxa.analytics.generator.AnalyticsGeneratorException;
import uk.ac.ebi.gxa.analytics.generator.listener.AnalyticsGeneratorListener;
import uk.ac.ebi.gxa.dao.AtlasDAO;
import uk.ac.ebi.gxa.dao.LoadStage;
import uk.ac.ebi.gxa.dao.LoadStatus;
import uk.ac.ebi.gxa.netcdf.reader.NetCDFProxy;
import uk.ac.ebi.microarray.atlas.model.Experiment;
import uk.ac.ebi.rcloud.server.RServices;
import uk.ac.ebi.rcloud.server.RType.RChar;
import uk.ac.ebi.rcloud.server.RType.RObject;

import java.io.*;
import java.rmi.RemoteException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.*;

/**
 * Javadocs go here!
 *
 * @author Tony Burdett
 * @date 28-Sep-2009
 */
public class ExperimentAnalyticsGeneratorService extends AnalyticsGeneratorService<File> {
    private static final int NUM_THREADS = 32;

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
        List<Future> tasks =
                new ArrayList<Future>();

        // start the timer
        timer.start();

        // call the start procedure with "null" parameter, as we're doing all
        try {
            // simply call start procedure
            getAtlasDAO().startExpressionAnalytics(null);
        }
        catch (Exception e) {
            getLog().error("Failing analytics run for all experiments: " +
                    "failed to run initialising management procedure.\n{}", e);
            for (Experiment experiment : experiments) {
                getAtlasDAO().writeLoadDetails(
                        experiment.getAccession(), LoadStage.RANKING, LoadStatus.FAILED);
            }
            return;
        }

        // the first error encountered whilst generating analytics, if any
        Exception firstError = null;

        try {
            // process each experiment to build the netcdfs
            for (final Experiment experiment : experiments) {
                // run each experiment in parallel
                tasks.add(tpool.submit(new Callable() {

                    public Void call() throws Exception {
                        long start = System.currentTimeMillis();
                        try {
                            generateExperimentAnalytics(experiment.getAccession(), null);
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

                        return null;
                    }
                }));
            }

            // block until completion, and throw the first error we see
            for (Future task : tasks) {
                try {
                    task.get();
                }
                catch (Exception e) {
                    // print the stacktrace, but swallow this exception to rethrow at the very end
                    getLog().error("An error occurred whilst generating analytics:\n{}", e);
                    if (firstError == null) {
                        firstError = e;
                    }
                }
            }

            // if we have encountered an exception, throw the first error
            if (firstError != null) {
                throw new AnalyticsGeneratorException("An error occurred whilst generating analytics", firstError);
            }
        }
        finally {
            // call the ending procedure
            try {
                // simply call start procedure
                getAtlasDAO().finaliseExpressionAnalytics(null);
            }
            catch (Exception e) {
                getLog().error("Failing analytics run for all experiments: " +
                        "failed to run finalising management procedure.\n{}", e);
                for (Experiment experiment : experiments) {
                    getAtlasDAO().writeLoadDetails(
                            experiment.getAccession(), LoadStage.RANKING, LoadStatus.FAILED);
                }
            }

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
                //noinspection ThrowFromFinallyBlock
                throw new AnalyticsGeneratorException(
                        "Failed to terminate service for " + getClass().getSimpleName() +
                                " cleanly - suspended tasks were found", e);
            }
        }
    }

    protected void createAnalyticsForExperiment(String experimentAccession, AnalyticsGeneratorListener listener) throws AnalyticsGeneratorException {
        try {
            // simply call start procedure
            getAtlasDAO().startExpressionAnalytics(experimentAccession);
        }
        catch (Exception e) {
            getLog().error("Failing analytics for {}: failed to run initialising management procedure.\n{}",
                           experimentAccession, e);
            getAtlasDAO().writeLoadDetails(
                    experimentAccession, LoadStage.RANKING, LoadStatus.FAILED);
            return;
        }

        // then generateExperimentAnalytics
        generateExperimentAnalytics(experimentAccession, listener);

        try {
            // finally call end procedure
            getAtlasDAO().finaliseExpressionAnalytics(experimentAccession);
        }
        catch (Exception e) {
            getLog().error("Failing analytics for {}: failed to run finalising management procedure.\n{}",
                           experimentAccession, e);
            getAtlasDAO().writeLoadDetails(
                    experimentAccession, LoadStage.RANKING, LoadStatus.FAILED);
        }
    }

    private void generateExperimentAnalytics(String experimentAccession, AnalyticsGeneratorListener listener)
            throws AnalyticsGeneratorException {
        getLog().info("Generating analytics for experiment " + experimentAccession);

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

        if (netCDFs == null || netCDFs.length == 0) {
            throw new AnalyticsGeneratorException("No NetCDF files present for " + experimentAccession);
        }

        final List<String> analysedEFs = new LinkedList<String>();

        int count = 0;
        for (final File netCDF : netCDFs) {
            count++;
            ComputeTask<Void> computeAnalytics = new ComputeTask<Void>() {
                public Void compute(RServices rs) throws ComputeException {
                    try {
                        // first, make sure we load the R code that runs the analytics
                        rs.sourceFromBuffer(getRCodeFromResource("R/analytics.R"));

                        // note - the netCDF file MUST be on the same file system where the workers run
                        getLog().debug("Starting compute task for " + netCDF.getAbsolutePath());
                        RObject r = rs.getObject("computeAnalytics(\"" + netCDF.getAbsolutePath() + "\")");
                        getLog().debug("Completed compute task for " + netCDF.getAbsolutePath());

                        if(r instanceof RChar) {
                            String[] efs        = ((RChar) r).getNames();
                            String[] analysedOK = ((RChar) r).getValue();

                           if(efs != null)
                               for(int i = 0; i < efs.length; i++) {
                                   getLog().debug("Performed analytics computation for netcdf {}: {} was {}", new Object[] {netCDF.getAbsolutePath(), efs[i], analysedOK[i]});

                                   if("OK".equals(analysedOK[i]))
                                       analysedEFs.add(efs[i]);
                               }

                            for(String rc : analysedOK) {
                                if(rc.contains("Error"))
                                    throw new ComputeException(rc);
                            }
                        } else
                            throw new ComputeException("Analytics returned unrecognized status of class " + r.getClass().getSimpleName() + ", string value: " + r.toString());
                    } catch (RemoteException e) {
                        throw new ComputeException("Problem communicating with R service", e);
                    } catch (IOException e) {
                        throw new ComputeException("Unable to load R source from R/analytics.R", e);
                    }
                    return null;
                }
            };

            NetCDFProxy proxy = null;

            // now run this compute task
            try {
                listener.buildProgress("Computing analytics for " + experimentAccession);
                getAtlasComputeService().computeTask(computeAnalytics);
                getLog().debug("Compute task " + count + "/" + netCDFs.length + " for " + experimentAccession +
                        " has completed.");

                if(analysedEFs.size()==0) {
                    listener.buildWarning("No analytics were computed for this experiment!");
                    return;
                }

                // computeAnalytics writes analytics data back to NetCDF, so now read back from NetCDF to database
                proxy = new NetCDFProxy(netCDF);

                // get unique factor values for the expression value matrix
                long[] designElements = proxy.getDesignElements();
                String[] uefvs = proxy.getUniqueFactorValues();

                listener.buildProgress("Writing analytics for " + experimentAccession + " to the database...");

                // uefvs is list of unique EF||EFV pairs - separate by splitting on ||
                getLog().debug("Writing analytics for " + experimentAccession + " to the database...");
                int uefvIndex = 0;
                for (String uefv : uefvs) {
                    String[] values = uefv.split("\\|\\|"); // sheesh, crazy java regexing!
                    String ef = values[0];

                    for (int i = 1; i < values.length; i++) {
                        String efv = values[i];

                        float[] pValues     = proxy.getPValuesForUniqueFactorValue(uefvIndex);
                        float[] tStatistics = proxy.getTStatisticsForUniqueFactorValue(uefvIndex);

                        // filter out all elements from de, t and p where p > 0.05 (not differentially expressed)
			int k = 0;
			for (float p : pValues) if(p <= 0.05) k++; 

                        if(0 == k) {
                            listener.buildProgress("No d.e. genes found for EF: " + ef + "; EFV: " + efv);
                            getLog().trace("No d.e. genes found for EF: " + ef + "; EFV: " + efv);
                            continue;
                        }

                        long[]  deDE = new long[k];
                        float[] deP  = new float[k];
                        float[] deT  = new float[k];

                        k = 0;
                        for(int j = 0; j < pValues.length; j++) {
                            if(pValues[j] <= 0.05) {
                                deDE[k] = designElements[j];
                                deP[k]  = pValues[j];
                                deT[k]  = tStatistics[j];

                                k++;
                            }
                        }

                        // write values
                        listener.buildProgress("Writing analytics for experiment: " + experimentAccession + "; " +
                                "EF: " + ef + "; EFV: " + efv);

                        getLog().trace("Writing analytics for experiment: " + experimentAccession + "; " +
                                "EF: " + ef + "; EFV: " + efv);

                        try {
                            getAtlasDAO().writeExpressionAnalytics(experimentAccession, ef, efv, deDE, deP, deT);
                        } catch (RuntimeException e) {
                            throw new AnalyticsGeneratorException("Writing analytics data for experiment: " + experimentAccession + "; " +
                                    "EF: " + ef + "; EFV: " + efv + " failed with errors: " + e.getMessage(), e);
                        }
                    }

                    // increment uefvIndex
                    uefvIndex++;
                }
            }
            catch (IOException e) {
                throw new AnalyticsGeneratorException("Unable to read from analytics at " + netCDF.getAbsolutePath(), e);
            }
            catch (ComputeException e) {
                throw new AnalyticsGeneratorException("Computation of analytics for " + netCDF.getAbsolutePath() + " failed: " + e.getMessage(), e);
            }
            catch (AnalyticsGeneratorException e) {
                throw e;
            }
            catch (Exception e) {
                throw new AnalyticsGeneratorException("An error occurred while generating analytics for " + netCDF.getAbsolutePath(), e);
            } finally {
                if(proxy != null) {
                    try {
                        proxy.close();
                    } catch (IOException e) {
                        getLog().error("Failed to close NetCDF proxy for " + netCDF.getAbsolutePath());
                    }
                }
            }
        }
    }

    private String getRCodeFromResource(String resourcePath) throws ComputeException {
        // open a stream to the resource
        InputStream in = getClass().getClassLoader().getResourceAsStream(resourcePath);

        // create a reader to read in code
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));

        StringBuilder sb = new StringBuilder();
        String line;

        try {
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
        } catch (IOException e) {
            throw new ComputeException("Error while reading in R code from " + resourcePath, e);
        } finally {
            if (null != in) {
                try {
                    in.close();
                } catch (IOException e) {
                    getLog().error("Failed to close input stream", e);
                }
            }
        }

        return sb.toString();
    }

    private class AnalyticsTimer {
        private long[] experimentIDs;
        private boolean[] completions;
        private int completedCount;
        private long startTime;
        private long lastEstimate;

        public AnalyticsTimer(List<Experiment> experiments) {
            experimentIDs = new long[experiments.size()];
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

        public synchronized AnalyticsTimer completed(long experimentID) {
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
