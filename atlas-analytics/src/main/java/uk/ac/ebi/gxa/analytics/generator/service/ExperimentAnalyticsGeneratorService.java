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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.gxa.Experiment;
import uk.ac.ebi.gxa.Model;
import uk.ac.ebi.gxa.analytics.compute.AtlasComputeService;
import uk.ac.ebi.gxa.analytics.compute.ComputeException;
import uk.ac.ebi.gxa.analytics.compute.ComputeTask;
import uk.ac.ebi.gxa.analytics.generator.AnalyticsGeneratorException;
import uk.ac.ebi.gxa.analytics.generator.listener.AnalyticsGeneratorListener;
import uk.ac.ebi.gxa.dao.AtlasDAO;
import uk.ac.ebi.gxa.netcdf.reader.AtlasNetCDFDAO;
import uk.ac.ebi.gxa.netcdf.reader.NetCDFDescriptor;
import uk.ac.ebi.gxa.netcdf.reader.NetCDFProxy;
import uk.ac.ebi.rcloud.server.RServices;
import uk.ac.ebi.rcloud.server.RType.RChar;
import uk.ac.ebi.rcloud.server.RType.RObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.rmi.RemoteException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import static com.google.common.io.Closeables.closeQuietly;

public class ExperimentAnalyticsGeneratorService {
    private final Model atlasModel;
    private final AtlasDAO atlasDAO;
    private final AtlasNetCDFDAO atlasNetCDFDAO;
    private final AtlasComputeService atlasComputeService;

    private final Logger log = LoggerFactory.getLogger(this.getClass());
    private ExecutorService executor;

    public ExperimentAnalyticsGeneratorService(Model atlasModel, AtlasDAO atlasDAO, AtlasNetCDFDAO atlasNetCDFDAO, AtlasComputeService atlasComputeService, ExecutorService executor) {
        this.atlasModel = atlasModel;
        this.atlasDAO = atlasDAO;
        this.atlasNetCDFDAO = atlasNetCDFDAO;
        this.atlasComputeService = atlasComputeService;
        this.executor = executor;
    }

    public void generateAnalytics() throws AnalyticsGeneratorException {
        // do initial setup - build executor service

        // fetch experiments - check if we want all or only the pending ones
        List<Experiment> experiments = atlasModel.getAllExperiments();

        // create a timer, so we can track time to generate analytics
        final AnalyticsTimer timer = new AnalyticsTimer(experiments);

        // the list of futures - we need these so we can block until completion
        List<Future> tasks =
                new ArrayList<Future>();

        // start the timer
        timer.start();

        // the first error encountered whilst generating analytics, if any
        Exception firstError = null;

        // process each experiment to build the netcdfs
        for (final Experiment experiment : experiments) {
            // run each experiment in parallel
            tasks.add(executor.<Void>submit(new Callable<Void>() {

                public Void call() throws Exception {
                    long start = System.currentTimeMillis();
                    try {
                        generateExperimentAnalytics(experiment.getAccession());
                    } finally {
                        timer.completed(experiment.getId());

                        long end = System.currentTimeMillis();
                        String total = new DecimalFormat("#.##").format((end - start) / 1000);
                        String estimate = new DecimalFormat("#.##").format(timer.getCurrentEstimate() / 60000);

                        log.info("\n\tAnalytics for " + experiment.getAccession() +
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
                log.error("An error occurred whilst generating analytics:\n{}", e);
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

    public void createAnalyticsForExperiment(
            String experimentAccession,
            AnalyticsGeneratorListener listener) throws AnalyticsGeneratorException {
        // then generateExperimentAnalytics
        log.info("Generating analytics for experiment " + experimentAccession);

        final Collection<NetCDFDescriptor> netCDFs = getNetCDFs(experimentAccession);
        final List<String> analysedEFSCs = new ArrayList<String>();
        int count = 0;
        for (NetCDFDescriptor netCDF : netCDFs) {
            count++;

            if (!factorsCharacteristicsAvailable(netCDF)) {
                listener.buildWarning("No analytics were computed for " + netCDF + " as it contained no factors or characteristics!");
                return;
            }

            final String pathForR = netCDF.getPathForR();
            ComputeTask<Void> computeAnalytics = new ComputeTask<Void>() {
                public Void compute(RServices rs) throws ComputeException {
                    try {
                        // first, make sure we load the R code that runs the analytics
                        rs.sourceFromBuffer(getRCodeFromResource("R/analytics.R"));

                        // note - the netCDF file MUST be on the same file system where the workers run
                        log.debug("Starting compute task for " + pathForR);
                        RObject r = rs.getObject("computeAnalytics(\"" + pathForR + "\")");
                        log.debug("Completed compute task for " + pathForR);

                        if (r instanceof RChar) {
                            String[] efScs = ((RChar) r).getNames();
                            String[] analysedOK = ((RChar) r).getValue();

                            if (efScs != null)
                                for (int i = 0; i < efScs.length; i++) {
                                    log.info("Performed analytics computation for netcdf {}: {} was {}", new Object[]{pathForR, efScs[i], analysedOK[i]});

                                    if ("OK".equals(analysedOK[i]))
                                        analysedEFSCs.add(efScs[i]);
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

            // now run this compute task
            try {
                listener.buildProgress("Computing analytics for " + experimentAccession);
                // computeAnalytics writes analytics data back to NetCDF
                atlasComputeService.computeTask(computeAnalytics);
                log.debug("Compute task " + count + "/" + netCDFs.size() + " for " + experimentAccession +
                        " has completed.");

                if (analysedEFSCs.size() == 0) {
                    listener.buildWarning("No analytics were computed for this experiment!");
                }
            } catch (ComputeException e) {
                throw new AnalyticsGeneratorException("Computation of analytics for " + netCDF + " failed: " + e.getMessage(), e);
            } catch (Exception e) {
                throw new AnalyticsGeneratorException("An error occurred while generating analytics for " + netCDF, e);
            }
        }
    }

    private void generateExperimentAnalytics(
            String experimentAccession)
            throws AnalyticsGeneratorException {
        log.info("Generating analytics for experiment " + experimentAccession);

        final Collection<NetCDFDescriptor> netCDFs = getNetCDFs(experimentAccession);
        final List<String> analysedEFSCs = new ArrayList<String>();
        int count = 0;
        for (NetCDFDescriptor netCDF : netCDFs) {
            count++;

            if (!factorsCharacteristicsAvailable(netCDF)) {
                log.warn("No analytics were computed for {} as it contained no factors or characteristics!", netCDF);
                return;
            }

            final String pathForR = netCDF.getPathForR();
            ComputeTask<Void> computeAnalytics = new ComputeTask<Void>() {
                public Void compute(RServices rs) throws ComputeException {
                    try {
                        // first, make sure we load the R code that runs the analytics
                        rs.sourceFromBuffer(getRCodeFromResource("R/analytics.R"));

                        // note - the netCDF file MUST be on the same file system where the workers run
                        log.debug("Starting compute task for " + pathForR);
                        RObject r = rs.getObject("computeAnalytics(\"" + pathForR + "\")");
                        log.debug("Completed compute task for " + pathForR);

                        if (r instanceof RChar) {
                            String[] efScs = ((RChar) r).getNames();
                            String[] analysedOK = ((RChar) r).getValue();

                            if (efScs != null)
                                for (int i = 0; i < efScs.length; i++) {
                                    log.info("Performed analytics computation for netcdf {}: {} was {}", new Object[]{pathForR, efScs[i], analysedOK[i]});

                                    if ("OK".equals(analysedOK[i]))
                                        analysedEFSCs.add(efScs[i]);
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

            // now run this compute task
            try {
                log.info("Computing analytics for " + experimentAccession);
                // computeAnalytics writes analytics data back to NetCDF
                atlasComputeService.computeTask(computeAnalytics);
                log.debug("Compute task " + count + "/" + netCDFs.size() + " for " + experimentAccession +
                        " has completed.");

                if (analysedEFSCs.size() == 0) {
                    log.warn("No analytics were computed for this experiment!");
                }
            } catch (ComputeException e) {
                throw new AnalyticsGeneratorException("Computation of analytics for " + netCDF + " failed: " + e.getMessage(), e);
            } catch (Exception e) {
                throw new AnalyticsGeneratorException("An error occurred while generating analytics for " + netCDF, e);
            }
        }
    }

    private Collection<NetCDFDescriptor> getNetCDFs(String experimentAccession) throws AnalyticsGeneratorException {
        Collection<NetCDFDescriptor> netCDFs = atlasNetCDFDAO.getNetCDFProxiesForExperiment(experimentAccession);
        if (netCDFs.isEmpty()) {
            throw new AnalyticsGeneratorException("No NetCDF files present for " + experimentAccession);
        }
        return netCDFs;
    }

    private boolean factorsCharacteristicsAvailable(NetCDFDescriptor netCDF) throws AnalyticsGeneratorException {
        NetCDFProxy proxy = null;
        try {
            proxy = netCDF.createProxy();
            return proxy.getFactorsAndCharacteristics().length > 0;
        } catch (IOException e) {
            throw new AnalyticsGeneratorException("Failed to open " + netCDF + " to check if it contained factors or characteristics", e);
        } finally {
            closeQuietly(proxy);
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
        private final long[] experimentIDs;
        private final boolean[] completions;
        private int completedCount;
        private long startTime;
        private long lastEstimate;

        public AnalyticsTimer(List<Experiment> experiments) {
            experimentIDs = new long[experiments.size()];
            completions = new boolean[experiments.size()];
            int i = 0;
            for (Experiment exp : experiments) {
                experimentIDs[i] = exp.getId();
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
