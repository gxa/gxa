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

package uk.ac.ebi.gxa.index.builder;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.gxa.index.builder.listener.IndexBuilderEvent;
import uk.ac.ebi.gxa.index.builder.listener.IndexBuilderListener;
import uk.ac.ebi.gxa.index.builder.service.IndexBuilderService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

/**
 * A default implementation of {@link IndexBuilder} that constructs a SOLR index in a supplied directory.  By default,
 * this will include all genes and experiments.
 *
 * @author Tony Burdett
 */
public class DefaultIndexBuilder implements IndexBuilder {
    private ExecutorService executor;
    private List<String> includeIndexes;
    private List<IndexBuilderService> services;
    private List<IndexBuilderEventHandler> eventHandlers = new ArrayList<IndexBuilderEventHandler>();
    private final Logger log = LoggerFactory.getLogger(getClass());

    public List<String> getIncludeIndexes() {
        return includeIndexes;
    }

    public void setExecutor(ExecutorService executor) {
        this.executor = executor;
    }

    public void setIncludeIndexes(List<String> includeIndices) {
        this.includeIndexes = includeIndices;
    }

    public void setServices(List<IndexBuilderService> services) {
        this.services = services;
    }

    public void doCommand(final IndexBuilderCommand command, final IndexBuilderListener listener) {
        log.info("Started IndexBuilder: " + command.toString() + "Building for " + StringUtils.join(getIncludeIndexes(), ","));

        if (includeIndexes.isEmpty()) {
            log.info("Nothing to build");
            return;
        }

        final List<Future<Boolean>> indexingTasks =
                new ArrayList<Future<Boolean>>();

        notifyBuildStartHandlers();

        final Map<String, String> progressMap = new HashMap<String, String>();

        for (final IndexBuilderService service : services) {
            if (includeIndexes.contains(service.getName())) {
                indexingTasks.add(this.executor.submit(new Callable<Boolean>() {
                    public Boolean call() throws IndexBuilderException {
                        try {
                            log.info("Starting building of index: " + service.getName());
                            final IndexBuilderService.ProgressUpdater updater = new IndexBuilderService.ProgressUpdater() {
                                public void update(String progress) {
                                    synchronized (progressMap) {
                                        progressMap.put(service.getName(), progress);
                                        StringBuilder sb = new StringBuilder();
                                        for (Map.Entry<String, String> p : progressMap.entrySet()) {
                                            if (sb.length() > 0)
                                                sb.append(", ");
                                            sb.append(p.getKey()).append(": ").append(p.getValue());
                                        }
                                        if (sb.length() > 0)
                                            sb.insert(0, "Processed ");
                                        if (listener != null)
                                            listener.buildProgress(sb.toString());
                                    }
                                }
                            };
                            service.build(command, updater);
                            return true;
                        } catch (Exception e) {
                            log.error("Caught unchecked exception: " + e.getMessage(), e);
                            return false;
                        }
                    }
                }));
            }
        }

        // this tracks completion, if a listener was supplied
        new Thread(new Runnable() {
            public void run() {

                boolean success = true;
                List<Throwable> observedErrors = new ArrayList<Throwable>();

                // wait for expt and gene indexes to build
                for (Future<Boolean> indexingTask : indexingTasks) {
                    try {
                        success = indexingTask.get() && success;
                    } catch (Exception e) {
                        observedErrors.add(e);
                        success = false;
                    }
                }

                // now we've finished - get the end time, calculate runtime and fire the event

                notifyBuildFinishHandlers();

                // create our completion event
                if (listener != null) {
                    if (success) {
                        listener.buildSuccess();
                    } else {
                        listener.buildError(new IndexBuilderEvent(observedErrors));
                    }
                }
            }
        }).start();
    }

    public void registerIndexBuildEventHandler(IndexBuilderEventHandler handler) {
        if (!eventHandlers.contains(handler)) {
            eventHandlers.add(handler);
        }
    }

    public void unregisterIndexBuildEventHandler(IndexBuilderEventHandler handler) {
        eventHandlers.remove(handler);
    }

    private void notifyBuildFinishHandlers() {
        log.info("Index updated, notifying listeners");
        for (IndexBuilderEventHandler handler : eventHandlers) {
            handler.onIndexBuildFinish();
        }
    }

    private void notifyBuildStartHandlers() {
        log.info("Index build started, notifying listeners");
        for (IndexBuilderEventHandler handler : eventHandlers) {
            handler.onIndexBuildStart();
        }
    }
}
    