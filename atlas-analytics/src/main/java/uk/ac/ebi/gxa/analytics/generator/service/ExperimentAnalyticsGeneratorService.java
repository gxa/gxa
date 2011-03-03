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
import uk.ac.ebi.gxa.netcdf.reader.AtlasNetCDFDAO;
import uk.ac.ebi.gxa.netcdf.reader.NetCDFProxy;
import uk.ac.ebi.microarray.atlas.model.Experiment;
import uk.ac.ebi.rcloud.server.RServices;
import uk.ac.ebi.rcloud.server.RType.RChar;
import uk.ac.ebi.rcloud.server.RType.RObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.rmi.RemoteException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static com.google.common.io.Closeables.closeQuietly;

public class ExperimentAnalyticsGeneratorService extends AnalyticsGeneratorService {
    private static final int NUM_THREADS = 32;
    // http://download.oracle.com/docs/cd/B12037_01/java.101/b10979/ref.htm - jdbc NUMBER type does not
    // comply with the IEEE 754 standard for floating-point arithmetic, hence float precision  in jdbc is
    // lower tha nthat used in java. To prevent pValues inserted from NetCDFs via java/JDBC into Oracle
    // loosing their precision in JDBC (i.e. being turned to 0), we use this constant to set them a reasonably low
    // (form Atlas statistics point of view) low enough value that is acceptable to both java and jdbc.
    private static final Float MIN_PVALUE_FOR_SOLR_INDEX = 10E-22f;

    public ExperimentAnalyticsGeneratorService(AtlasDAO atlasDAO,
                                               AtlasNetCDFDAO atlasNetCDFDAO,
                                               AtlasComputeService atlasComputeService) {
        super(atlasDAO, atlasNetCDFDAO, atlasComputeService);
    }

    protected void createAnalytics(final AtlasNetCDFDAO atlasNetCDFDAO) throws AnalyticsGeneratorException {
        // do initial setup - build executor service
        ExecutorService tpool = Executors.newFixedThreadPool(NUM_THREADS);

        // fetch experiments - check if we want all or only the pending ones
        List<Experiment> experiments = getAtlasDAO().getAllExperiments();

        for (Experiment experiment : experiments) {
            getAtlasDAO().writeLoadDetails(
                    experiment.getAccession(), LoadStage.RANKING, LoadStatus.PENDING);
        }

        // create a timer, so we can track time to generate analytics
        final AnalyticsTimer timer = new AnalyticsTimer(experiments);

        // the list of futures - we need these so we can block until completion
        List<Future> tasks =
                new ArrayList<Future>();

        // start the timer
        timer.start();

        // the first error encountered whilst generating analytics, if any
        Exception firstError = null;

        try {
            // process each experiment to build the netcdfs
            for (final Experiment experiment : experiments) {
                // run each experiment in parallel
                tasks.add(tpool.<Void>submit(new Callable<Void>() {

                    public Void call() throws Exception {
                        long start = System.currentTimeMillis();
                        try {
                            generateExperimentAnalytics(experiment.getAccession(), null, atlasNetCDFDAO);
                        } finally {
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
                } catch (Exception e) {
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
        } finally {
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
                } else {
                    getLog().debug("Executor service exited cleanly");
                }
            } catch (InterruptedException e) {
                //noinspection ThrowFromFinallyBlock
                throw new AnalyticsGeneratorException(
                        "Failed to terminate service for " + getClass().getSimpleName() +
                                " cleanly - suspended tasks were found", e);
            }
        }
    }

    protected void createAnalyticsForExperiment(
            String experimentAccession,
            AnalyticsGeneratorListener listener,
            AtlasNetCDFDAO atlasNetCDFDAO) throws AnalyticsGeneratorException {
        // then generateExperimentAnalytics
        generateExperimentAnalytics(experimentAccession, listener, atlasNetCDFDAO);
    }

    private void generateExperimentAnalytics(
            String experimentAccession,
            AnalyticsGeneratorListener listener,
            AtlasNetCDFDAO atlasNetCDFDAO)
            throws AnalyticsGeneratorException {
        getLog().info("Generating analytics for experiment " + experimentAccession);

        // update loadmonitor - experiment is indexing
        getAtlasDAO().writeLoadDetails(
                experimentAccession, LoadStage.RANKING, LoadStatus.WORKING);

        // work out where the NetCDF(s) are located
        File[] netCDFs = getAtlasNetCDFDAO().listNetCDFs(experimentAccession);

        if (netCDFs.length == 0) {
            throw new AnalyticsGeneratorException("No NetCDF files present for " + experimentAccession);
        }

        final List<String> analysedEFs = new ArrayList<String>();

        int count = 0;
        for (final File netCDF : netCDFs) {
            count++;
            NetCDFProxy proxy = null;
            try {
                proxy = atlasNetCDFDAO.getNetCDFProxy(experimentAccession, netCDF.getName());
                if (proxy.getFactors().length == 0) {
                    listener.buildWarning("No analytics were computed for " + netCDF.getName() + " as it contained no factors!");
                    closeQuietly(proxy);
                    return;
                }
            } catch (IOException ioe) {
                throw new AnalyticsGeneratorException("Failed to open " + netCDF + " to check if it contained factors", ioe);
            } finally {
                closeQuietly(proxy);
            }

            ComputeTask<Void> computeAnalytics = new ComputeTask<Void>() {
                public Void compute(RServices rs) throws ComputeException {
                    try {
                        // first, make sure we load the R code that runs the analytics
                        rs.sourceFromBuffer(getRCodeFromResource("R/analytics.R"));

                        // note - the netCDF file MUST be on the same file system where the workers run
                        getLog().debug("Starting compute task for " + netCDF.getAbsolutePath());
                        RObject r = rs.getObject("computeAnalytics(\"" + netCDF.getAbsolutePath() + "\")");
                        getLog().debug("Completed compute task for " + netCDF.getAbsolutePath());

                        if (r instanceof RChar) {
                            String[] efs = ((RChar) r).getNames();
                            String[] analysedOK = ((RChar) r).getValue();

                            if (efs != null)
                                for (int i = 0; i < efs.length; i++) {
                                    getLog().debug("Performed analytics computation for netcdf {}: {} was {}", new Object[]{netCDF.getAbsolutePath(), efs[i], analysedOK[i]});

                                    if ("OK".equals(analysedOK[i]))
                                        analysedEFs.add(efs[i]);
                                }

                            for (String rc : analysedOK) {
                                if (rc.contains("Error"))
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

            proxy = null;

            // now run this compute task
            try {
                listener.buildProgress("Computing analytics for " + experimentAccession);
                // computeAnalytics writes analytics data back to NetCDF
                getAtlasComputeService().computeTask(computeAnalytics);
                getLog().debug("Compute task " + count + "/" + netCDFs.length + " for " + experimentAccession +
                        " has completed.");

                if (analysedEFs.size() == 0) {
                    listener.buildWarning("No analytics were computed for this experiment!");
                }
            } catch (ComputeException e) {
                throw new AnalyticsGeneratorException("Computation of analytics for " + netCDF.getAbsolutePath() + " failed: " + e.getMessage(), e);
            } catch (Exception e) {
                throw new AnalyticsGeneratorException("An error occurred while generating analytics for " + netCDF.getAbsolutePath(), e);
            } finally {
                closeQuietly(proxy);
            }
        }
    }

    private String getRCodeFromResource(String resourcePath) throws ComputeException {
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(getClass().getClassLoader().getResourceAsStream(resourcePath)));

            StringBuilder sb = new StringBuilder();
            for (String line; (line = reader.readLine()) != null;) {
                sb.append(line).append("\n");
            }
            return sb.toString();
        } catch (IOException e) {
            throw new ComputeException("Error while reading in R code from " + resourcePath, e);
        } finally {
            closeQuietly(reader);
        }
    }

    private static class AnalyticsTimer {
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
