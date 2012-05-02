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

import uk.ac.ebi.gxa.download.dsv.DsvDocumentCreator;
import uk.ac.ebi.gxa.download.dsv.DsvDownloadTask;

import java.util.concurrent.*;

/**
 * @author Olga Melnichuk
 */
public class DownloadQueue {

    private final ExecutorService taskExecutor;
    private final JanitorService janitorService;

    private final ConcurrentMap<String, FutureTaskResult> results = new ConcurrentHashMap<String, FutureTaskResult>();

    public DownloadQueue(ExecutorService taskExecutor, JanitorService janitorService) {
        this.taskExecutor = taskExecutor;
        this.janitorService = janitorService;
    }

    public void shutdown() {
        taskExecutor.shutdown();
    }

    public DownloadTaskResult getResult(String token) {
        FutureTaskResult result = results.get(token);
        if (result == null) {
            return null;
        }
        return result.getResult();
    }

    public int getProgress(String token) {
        FutureTaskResult result = results.get(token);
        if (result == null) {
            return -1;
        }
        return result.getProgress();
    }

    public void cancel(String token) {
        FutureTaskResult result = results.get(token);
        if (result == null) {
            return;
        }
        result.cancel();
        results.remove(token, result);
    }

    private boolean expired(String token) {
        FutureTaskResult result = results.get(token);
        if (result == null) {
            return false;
        }
        long ago = System.currentTimeMillis() - result.getLastAccess();
        if (ago > 60*60*1000) { //TODO an hour
            cancel(token);
            //TODO should we remove temporary fileS?
            return true;
        }
        return false;
    }

    public void addDsvDownloadTask(final String token, DsvDocumentCreator dsvDocumentCreator) {
        TaskProgress progress = new TaskProgress();
        DsvDownloadTask task = new DsvDownloadTask(token,
                dsvDocumentCreator,
                progress);

        results.putIfAbsent(token, new FutureTaskResult(taskExecutor.submit(task), progress));
        janitorService.schedule(new JanitorService.Janitor() {
            public boolean keepOnCleaning() {
                return !expired(token);
            }
        });
    }

    private static class FutureTaskResult {
        private final Future<DownloadTaskResult> future;
        private final TaskProgress progress;
        private volatile long lastAccess;

        public FutureTaskResult(Future<DownloadTaskResult> future, TaskProgress progress) {
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
