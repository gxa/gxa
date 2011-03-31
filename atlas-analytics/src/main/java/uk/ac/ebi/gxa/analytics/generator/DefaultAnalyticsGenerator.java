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

package uk.ac.ebi.gxa.analytics.generator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.gxa.analytics.generator.listener.AnalyticsGenerationEvent;
import uk.ac.ebi.gxa.analytics.generator.listener.AnalyticsGeneratorListener;
import uk.ac.ebi.gxa.analytics.generator.service.ExperimentAnalyticsGeneratorService;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

/**
 * A default implementation of {@link AnalyticsGenerator} that creates Atlas analytics in the database.
 *
 * @author Misha Kapushesky
 */
public class DefaultAnalyticsGenerator implements AnalyticsGenerator {
    private ExperimentAnalyticsGeneratorService analyticsService;

    private ExecutorService executor;

    // logging
    private final Logger log =
            LoggerFactory.getLogger(DefaultAnalyticsGenerator.class);

    public void setAnalyticsService(ExperimentAnalyticsGeneratorService analyticsService) {
        this.analyticsService = analyticsService;
    }

    public void setExecutor(ExecutorService executor) {
        this.executor = executor;
    }

    public void startup() throws AnalyticsGeneratorException {
    }

    public void shutdown() throws AnalyticsGeneratorException {
    }

    public void generateAnalytics() {
        generateAnalytics(null);
    }

    public void generateAnalytics(final AnalyticsGeneratorListener listener) {
        generateAnalyticsForExperiment(null, listener);
    }

    public void generateAnalyticsForExperiment(String experimentAccession) {
        generateAnalyticsForExperiment(experimentAccession, null);
    }

    public void generateAnalyticsForExperiment(
            final String experimentAccession,
            final AnalyticsGeneratorListener listener) {
        final List<Future<Boolean>> buildingTasks =
                new ArrayList<Future<Boolean>>();

        buildingTasks.add(executor.submit(new Callable<Boolean>() {
            public Boolean call() throws AnalyticsGeneratorException {
                try {
                    if (listener != null)
                        listener.buildProgress("Processing...");

                    if (experimentAccession == null) {
                        log.info("Starting analytics generations for all experiments");
                        analyticsService.generateAnalytics();
                        log.info("Finished analytics generations for all experiments");
                    } else {
                        analyticsService.createAnalyticsForExperiment(experimentAccession, listener);
                    }

                    return true;
                } catch (AnalyticsGeneratorException e) {
                    throw e;
                } catch (Exception e) {
                    throw new AnalyticsGeneratorException("Error", e);
                }

            }
        }));

        // this tracks completion, if a listener was supplied
        if (listener != null) {
            new Thread(new Runnable() {
                public void run() {
                    boolean success = true;
                    List<Throwable> observedErrors = new ArrayList<Throwable>();

                    // wait for expt and gene indexes to build
                    for (Future<Boolean> buildingTask : buildingTasks) {
                        try {
                            success = buildingTask.get() && success;
                        } catch (InterruptedException e) {
                            log.error("Interrupted", e);
                        } catch (ExecutionException e) {
                            observedErrors.add(e.getCause() != null ? e.getCause() : e);
                            success = false;
                        } catch (Throwable e) {
                            observedErrors.add(e);
                            success = false;
                        }
                    }

                    // create our completion event
                    if (success) {
                        listener.buildSuccess();
                    } else {
                        listener.buildError(new AnalyticsGenerationEvent(observedErrors));
                    }
                }
            }).start();
        }
    }
}
