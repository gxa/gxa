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
import org.springframework.beans.factory.InitializingBean;
import uk.ac.ebi.gxa.index.builder.listener.IndexBuilderEvent;
import uk.ac.ebi.gxa.index.builder.listener.IndexBuilderListener;
import uk.ac.ebi.gxa.index.builder.service.IndexBuilderService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.*;

/**
 * A default implementation of {@link IndexBuilder} that constructs a SOLR index in a supplied directory.  By default,
 * this will include all genes and experiments.
 *
 * @author Tony Burdett
 * @date 20-Aug-2009
 */
public class DefaultIndexBuilder implements IndexBuilder, InitializingBean {

    private ExecutorService service;
    private boolean running = false;

    private List<String> includeIndexes;

    private List<IndexBuilderService> services;

    private List<IndexBuilderEventHandler> eventHandlers = new ArrayList<IndexBuilderEventHandler>();

    // logging
    private final Logger log = LoggerFactory.getLogger(getClass());

    public List<String> getIncludeIndexes() {
        return includeIndexes;
    }

    public void setIncludeIndexes(List<String> includeIndices) {
        this.includeIndexes = includeIndices;
    }

    public List<IndexBuilderService> getServices() {
        return services;
    }

    public void setServices(List<IndexBuilderService> services) {
        this.services = services;
    }

    public void afterPropertiesSet() throws Exception {
        // simply delegates to startup(), this allows automated spring startup
        startup();
    }

    /**
     * Starts up any resources required for building an index. It will initialise an {@link
     * java.util.concurrent.ExecutorService} for running index building tasks in an asynchronous, parallel manner.
     * <p/>
     * Once you have started a default index builder, it will continue to run until you call {@link #shutdown()} on it.
     *
     * @throws IndexBuilderException if initialisation of this builder failed for any reason
     */
    public void startup() throws IndexBuilderException {
        if (!running) {
            // finally, create an executor service for processing calls to build the index
            service = Executors.newCachedThreadPool();
            log.debug("Initialized " + getClass().getSimpleName() + " OK!");

            running = true;
        }
        else {
            log.warn("Ignoring attempt to startup() a " + getClass().getSimpleName() +
                    " that is already running");
        }
    }

    /**
     * Shuts down any cached resources relating to multiple solr cores within the Atlas Index.  You should call this
     * whenever the application requiring index building services terminates (i.e. on webapp shutdown, or when the user
     * exits the application).
     *
     * @throws IndexBuilderException if shutting down this index builder failed for any reason
     */
    public void shutdown() throws IndexBuilderException {
        if (running) {
            log.debug("Shutting down " + getClass().getSimpleName() + "...");
            service.shutdown();
            try {
                log.debug("Waiting for termination of running jobs");
                service.awaitTermination(60, TimeUnit.SECONDS);

                if (!service.isTerminated()) {
                    // try and halt immediately
                    List<Runnable> tasks = service.shutdownNow();
                    service.awaitTermination(15, TimeUnit.SECONDS);
                    // if it's STILL not terminated...
                    if (!service.isTerminated()) {
                        StringBuffer sb = new StringBuffer();
                        sb.append(
                                "Unable to cleanly shutdown index building service.\n");
                        if (tasks.size() > 0) {
                            sb.append("The following tasks are still active or suspended:\n");
                            for (Runnable task : tasks) {
                                sb.append("\t").append(task.toString()).append("\n");
                            }
                        }
                        sb.append(
                                "There are running or suspended index building tasks. " +
                                        "If execution is complete, or has failed to exit " +
                                        "cleanly following an error, you should terminate this " +
                                        "application");
                        log.error(sb.toString());
                        throw new IndexBuilderException(sb.toString());
                    }
                    else {
                        // it worked second time round
                        log.debug("Shutdown complete");
                    }
                }
                else {
                    log.debug("Shutdown complete");
                }
            }
            catch (InterruptedException e) {
                log.error("The application was interrupted whilst waiting to be shutdown.  " +
                        "There may be tasks still running or suspended.");
                throw new IndexBuilderException(e);
            }
            finally {
                running = false;
            }
        }
        else {
            log.warn("Ignoring attempt to shutdown() a " + getClass().getSimpleName() + " that is not running");
        }
    }

    public void doCommand(final IndexBuilderCommand command, final IndexBuilderListener listener) {
        log.info("Started IndexBuilder: " + command.toString() + "Building for " + StringUtils.join(getIncludeIndexes(), ","));

        if(includeIndexes.isEmpty()) {
            log.info("Nothing to build");
            return;
        }

        final long startTime = System.currentTimeMillis();
        final List<Future<Boolean>> indexingTasks =
                new ArrayList<Future<Boolean>>();

        notifyBuildStartHandlers();

        final Map<String, String> progressMap = new HashMap<String, String>();

        for (final IndexBuilderService service : services) {
            if(includeIndexes.contains(service.getName())) {
                indexingTasks.add(this.service.submit(new Callable<Boolean>() {
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
                        }
                        catch (Exception e) {
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
                    }
                    catch (Exception e) {
                        observedErrors.add(e);
                        success = false;
                    }
                }

                // now we've finished - get the end time, calculate runtime and fire the event
                long endTime = System.currentTimeMillis();
                long runTime = (endTime - startTime) / 1000;

                final IndexBuilderEvent builderEvent = success ?
                        new IndexBuilderEvent(runTime, TimeUnit.SECONDS)
                        :
                        new IndexBuilderEvent(runTime, TimeUnit.SECONDS, observedErrors);

                notifyBuildFinishHandlers(builderEvent);

                // create our completion event
                if (listener != null) {
                    if (success) {
                        listener.buildSuccess(builderEvent);
                    }
                    else {
                        listener.buildError(builderEvent);
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

    private void notifyBuildFinishHandlers(IndexBuilderEvent event) {
        log.info("Index updated, notifying listeners");
        for (IndexBuilderEventHandler handler : eventHandlers) {
            handler.onIndexBuildFinish(this, event);
        }
    }

    private void notifyBuildStartHandlers() {
        log.info("Index build started, notifying listeners");
        for (IndexBuilderEventHandler handler : eventHandlers) {
            handler.onIndexBuildStart(this);
        }
    }
}
    