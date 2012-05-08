/*
 * Copyright 2008-2012 Microarray Informatics Team, EMBL-European Bioinformatics Institute
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

package uk.ac.ebi.gxa.download;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.gxa.download.dsv.DsvDocumentCreator;
import uk.ac.ebi.gxa.download.dsv.DsvDownloadTask;

import java.util.Collection;
import java.util.concurrent.*;

/**
 * @author Olga Melnichuk
 */
public class DownloadQueue {

    protected final static Logger log = LoggerFactory.getLogger(DownloadQueue.class);

    private final ExecutorService taskExecutor;
    private final JanitorService janitorService;

    private final ConcurrentMap<String, FutureTaskKeeper> results = new ConcurrentHashMap<String, FutureTaskKeeper>();

    public DownloadQueue(ExecutorService taskExecutor, JanitorService janitorService) {
        this.taskExecutor = taskExecutor;
        this.janitorService = janitorService;
    }

    public void shutdown() {
        taskExecutor.shutdown();
    }

    public DownloadTaskResult getResult(String token) {
        FutureTaskKeeper keeper = results.get(token);
        if (keeper == null) {
            return null;
        }
        return keeper.getResult();
    }

    public int getProgress(String token) {
        FutureTaskKeeper keeper = results.get(token);
        if (keeper == null) {
            return -1;
        }
        return keeper.getProgress();
    }

    public void cancel(String token) {
        FutureTaskKeeper keeper = results.get(token);
        if (keeper == null) {
            return;
        }
        keeper.cancel();
        results.remove(token, keeper);
    }

    private boolean expired(String token) {
        FutureTaskKeeper keeper = results.get(token);
        if (keeper == null) {
            return false;
        }
        long ago = System.currentTimeMillis() - keeper.getLastAccess();
        if (ago > 60*60*1000) { //TODO an hour
            log.debug("Task result expired; sweep.. " + token);
            cancel(token);
            //TODO should we remove temporary fileS?
            return true;
        }
        return false;
    }

    public void addDsvDownloadTask(final String token, Collection<? extends DsvDocumentCreator> dsvDocumentCreators) {
        TaskProgress progress = new TaskProgress();
        DsvDownloadTask task = new DsvDownloadTask(token,
                dsvDocumentCreators,
                progress);

        Future<DownloadTaskResult> f = taskExecutor.submit(task);
        FutureTaskKeeper keeper = new FutureTaskKeeper(f, progress);
        if (results.putIfAbsent(token, keeper) == null) {
            janitorService.schedule(new JanitorService.Janitor() {
                public boolean keepOnCleaning() {
                    return !expired(token);
                }
            });
        } else {
            f.cancel(true);
        }
        //TODO return false ?
    }

    private static class FutureTaskKeeper {
        private final Future<DownloadTaskResult> future;
        private final TaskProgress progress;
        private volatile long lastAccess;

        public FutureTaskKeeper(Future<DownloadTaskResult> future, TaskProgress progress) {
            this.future = future;
            this.progress = progress;
            this.lastAccess = System.currentTimeMillis();
        }

        public void cancel() {
            if (!future.isDone()) {
                future.cancel(true);
            }
        }

        public int getProgress() {
            return progress.getPercentage();
        }

        public long getLastAccess() {
            return lastAccess;
        }

        public DownloadTaskResult getResult() {
            try {
                lastAccess = System.currentTimeMillis();
                return future.get();
            } catch (InterruptedException e) {
                //TODO interrupted status ???
                return DownloadTaskResult.error(e);
            } catch (ExecutionException e) {
                return DownloadTaskResult.error(e.getCause());
            }
        }
    }

    private static class TaskProgress implements TaskProgressListener {
        private volatile int percentage;

        @Override
        public void onTaskProgress(int curr, int max) {
            percentage = curr * 100 / max;
        }

        public int getPercentage() {
            return percentage;
        }
    }
}
