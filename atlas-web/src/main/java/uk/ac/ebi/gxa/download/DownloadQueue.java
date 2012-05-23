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
import uk.ac.ebi.gxa.exceptions.LogUtil;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.concurrent.*;

import static java.io.File.createTempFile;
import static java.lang.Math.min;

/**
 * @author Olga Melnichuk
 */
public class DownloadQueue {

    protected final static Logger log = LoggerFactory.getLogger(DownloadQueue.class);

    private static final long ONE_HOUR = 60*60*1000;

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

    public int getProgress(String token) throws TaskExecutionException {
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
    }

    private boolean expired(String token, File file) {
        FutureTaskKeeper keeper = results.get(token);
        if (keeper == null) {
            throw LogUtil.createUnexpected("FutureTaskKeeper was null");
        }
        long ago = System.currentTimeMillis() - keeper.getLastAccess();
        if (ago > ONE_HOUR) {
            tidyUp(token, file);
            return true;
        }
        return false;
    }

    public void addDsvDownloadTask(final String token, Collection<? extends DsvDocumentCreator> dsvDocumentCreators) throws IOException {
        TaskProgress progress = new TaskProgress();
        final File file = createTmpFile(token);

        DsvDownloadTask task = new DsvDownloadTask(file,
                dsvDocumentCreators,
                progress);

        Future<DownloadTaskResult> f = taskExecutor.submit(task);
        FutureTaskKeeper keeper = new FutureTaskKeeper(f, progress);
        if (results.putIfAbsent(token, keeper) == null) {
            janitorService.schedule(new JanitorService.Janitor() {
                public boolean keepOnCleaning() {
                    return !expired(token, file);
                }
            });
        } else {
            f.cancel(true);
        }
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

        public int getProgress() throws TaskExecutionException {
            if (future.isDone()) {
                getResult().checkNoErrors();
            }
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
                log.error("Task execution error:", e.getCause());
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

    private File createTmpFile(String token) throws IOException {
        return createTempFile(token.substring(0, min(token.length(), 40)), ".zip");
    }

    private void clear(File file) {
        if (file != null && file.exists()) {
            log.debug("Removing {}...", file);
            if (!file.delete()) {
                log.warn("Can't remove: {}", file);
            }
        }
    }

    private void tidyUp(String token, File file) {
        log.debug("Task result expired; sweep.. " + token);
        cancel(token);
        results.remove(token, results.get(token));
        clear(file);
    }
}
