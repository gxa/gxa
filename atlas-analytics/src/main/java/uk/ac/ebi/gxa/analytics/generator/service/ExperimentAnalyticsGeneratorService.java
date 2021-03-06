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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ebi.gxa.R.compute.AtlasComputeService;
import uk.ac.ebi.gxa.R.compute.ComputeException;
import uk.ac.ebi.gxa.R.compute.ComputeTask;
import uk.ac.ebi.gxa.R.compute.RUtil;
import uk.ac.ebi.gxa.analytics.generator.AnalyticsGeneratorException;
import uk.ac.ebi.gxa.analytics.generator.listener.AnalyticsGenerationEvent;
import uk.ac.ebi.gxa.analytics.generator.listener.AnalyticsGeneratorListener;
import uk.ac.ebi.gxa.dao.AtlasDAO;
import uk.ac.ebi.gxa.dao.exceptions.RecordNotFoundException;
import uk.ac.ebi.gxa.data.AtlasDataDAO;
import uk.ac.ebi.gxa.data.AtlasDataException;
import uk.ac.ebi.gxa.data.ExperimentWithData;
import uk.ac.ebi.gxa.properties.AtlasProperties;
import uk.ac.ebi.microarray.atlas.model.ArrayDesign;
import uk.ac.ebi.microarray.atlas.model.Experiment;
import uk.ac.ebi.rcloud.server.RServices;
import uk.ac.ebi.rcloud.server.RType.RChar;
import uk.ac.ebi.rcloud.server.RType.RObject;

import java.io.IOException;
import java.rmi.RemoteException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import static com.google.common.io.Closeables.closeQuietly;

public class ExperimentAnalyticsGeneratorService {
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private AtlasDAO atlasDAO;
    @Autowired
    private AtlasDataDAO atlasDataDAO;
    @Autowired
    private AtlasComputeService atlasComputeService;
    @Autowired
    private ExecutorService executor;
    @Autowired
    private AtlasProperties atlasProperties;

    // for CGLIB only
    ExperimentAnalyticsGeneratorService() {
    }

    public ExperimentAnalyticsGeneratorService(AtlasDAO atlasDAO, AtlasDataDAO atlasDataDAO,
                                               AtlasComputeService atlasComputeService, ExecutorService executor,
                                               AtlasProperties atlasProperties) {
        this.atlasDAO = atlasDAO;
        this.atlasDataDAO = atlasDataDAO;
        this.atlasComputeService = atlasComputeService;
        this.executor = executor;
        this.atlasProperties = atlasProperties;
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void generateAnalytics() throws AnalyticsGeneratorException {
        // do initial setup - build executor service

        // fetch experiments - check if we want all or only the pending ones
        List<Experiment> experiments = atlasDAO.getAllExperiments();

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
                        createAnalyticsForExperiment(experiment.getAccession(), new LogAnalyticsGeneratorListener());
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

    private class LogAnalyticsGeneratorListener implements AnalyticsGeneratorListener {
        public void buildSuccess() {
        }

        public void buildError(AnalyticsGenerationEvent event) {
        }

        public void buildProgress(String progressStatus) {
            log.info(progressStatus);
        }

        public void buildWarning(String message) {
            log.warn(message);
        }
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void createAnalyticsForExperiment(
            String experimentAccession,
            AnalyticsGeneratorListener listener) throws AnalyticsGeneratorException, RecordNotFoundException {
        log.info("Generating analytics for experiment " + experimentAccession);

        final Experiment experiment = atlasDAO.getExperimentByAccession(experimentAccession);
        final Collection<ArrayDesign> arrayDesigns = experiment.getArrayDesigns();
        Iterator<ArrayDesign> iter = arrayDesigns.iterator();
        if (atlasProperties.getHideGxaContentExperiments().contains(experimentAccession) ||
                iter.hasNext() && iter.next().getAccession().equals("A-ENST-X")) {
            listener.buildWarning("No analytics were computed for " + experimentAccession + " as this is an RNA-seq or 2-colour experiment.");
            return;
        }
        final ExperimentWithData ewd = atlasDataDAO.createExperimentWithData(experiment);
        if (arrayDesigns.isEmpty()) {
            throw new AnalyticsGeneratorException("No array designs present for " + experiment);
        }
        final List<String> analysedEFs = new ArrayList<String>();
        int count = 0;
        try {
            ewd.updateDataToNewestVersion();
        } catch (AtlasDataException e) {
            throw new AnalyticsGeneratorException(e);
        }
        try {
            for (ArrayDesign ad : arrayDesigns) {
                count++;

                if (!factorsAvailable(ewd, ad)) {
                    listener.buildWarning("No analytics were computed for " + experimentAccession + "/" + ad.getAccession() + " as it contained no factors or characteristics!");
                    return;
                }
                final String dataPathForR = ewd.getDataPathForR(ad);
                final String statisticsPathForR = ewd.getStatisticsPathForR(ad);
                ComputeTask<Void> computeAnalytics = new ComputeTask<Void>() {
                    public Void compute(RServices rs) throws ComputeException {
                        try {
                            // first, make sure we load the R code that runs the analytics
                            rs.sourceFromBuffer(RUtil.getRCodeFromResource("R/analytics.R"));

                            // note - the netCDF file MUST be on the same file system where the workers run
                            log.debug("Starting compute task for " + statisticsPathForR);
                            RObject r = rs.getObject("computeAnalytics(\"" + dataPathForR + "\", \"" + statisticsPathForR + "\")");
                            log.debug("Completed compute task for " + statisticsPathForR);

                            if (r instanceof RChar) {
                                String[] efs = ((RChar) r).getNames();
                                String[] analysedOK = ((RChar) r).getValue();

                                if (efs != null)
                                    for (int i = 0; i < efs.length; i++) {
                                        log.info("Performed analytics computation for netcdf {}: {} was {}", new Object[]{statisticsPathForR, efs[i], analysedOK[i]});

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

                // now run this compute task
                try {
                    listener.buildProgress("Creating analytics file for " + experimentAccession + "/" + ad.getAccession());
                    ewd.getStatisticsCreator(ad).createNetCdf();
                    listener.buildProgress("Computing analytics for " + experimentAccession + "/" + ad.getAccession());
                    // computeAnalytics writes analytics data back to NetCDF
                    atlasComputeService.computeTask(computeAnalytics);
                    log.debug("Compute task " + count + "/" + arrayDesigns.size() + " for " + experimentAccession + " has completed.");

                    if (analysedEFs.size() == 0) {
                        listener.buildWarning("No analytics were computed for this experiment!");
                    }
                } catch (AtlasDataException e) {
                    throw new AnalyticsGeneratorException("Computation of analytics for " + experimentAccession + "/" + ad.getAccession() + " failed: " + e.getMessage(), e);
                } catch (ComputeException e) {
                    throw new AnalyticsGeneratorException("Computation of analytics for " + experimentAccession + "/" + ad.getAccession() + " failed: " + e.getMessage(), e);
                } catch (Exception e) {
                    throw new AnalyticsGeneratorException("An error occurred while generating analytics for " + experimentAccession + "/" + ad.getAccession(), e);
                }
            }
        } finally {
            closeQuietly(ewd);
        }
    }


    private boolean factorsAvailable(ExperimentWithData ewd, ArrayDesign ad) throws AnalyticsGeneratorException {
        try {
            return ewd.getFactors(ad).length > 0;
        } catch (AtlasDataException e) {
            throw new AnalyticsGeneratorException("Failed to open " + ewd.getExperiment().getAccession() + "/" + ad.getAccession() + " to check if it contained factors", e);
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
