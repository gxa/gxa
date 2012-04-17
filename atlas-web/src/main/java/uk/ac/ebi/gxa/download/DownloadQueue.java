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

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * @author Olga Melnichuk
 */
public class DownloadQueue {

    private final ExecutorService executorService;
    private final Map<String, Future<DownloadTaskResult>> futures = new HashMap<String, Future<DownloadTaskResult>>();

    public DownloadQueue() {
        this.executorService = Executors.newFixedThreadPool(10);
    }

    public void cancelTask(String jobId) {
        Future<DownloadTaskResult> future = futures.get(jobId);
        if (future == null) {
            return;
        }
        if (!future.isDone()) {
            if (!future.cancel(true)) {
                //TODO log.warn("Can not cancel the job: " + jobId);
            }
        }
        futures.remove(jobId);
    }

    public <T extends DownloadTask> void add(T task) {
        futures.put(task.getToken(), executorService.submit(task));
    }
}
