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

package uk.ac.ebi.gxa.download.dsv;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.gxa.download.DownloadTaskResult;
import uk.ac.ebi.gxa.download.TaskProgressListener;
import uk.ac.ebi.gxa.utils.dsv.DsvDocumentWriter;
import uk.ac.ebi.gxa.utils.dsv.DsvRowIterator;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static com.google.common.io.Closeables.closeQuietly;
import static uk.ac.ebi.gxa.download.DownloadTaskResult.error;
import static uk.ac.ebi.gxa.download.DownloadTaskResult.success;
import static uk.ac.ebi.gxa.utils.dsv.DsvFormat.tsv;

/**
 * @author Olga Melnichuk
 */
public class DsvDownloadTask implements Callable<DownloadTaskResult> {

    protected final static Logger log = LoggerFactory.getLogger(DsvDownloadTask.class);

    private static final String CONTENT_TYPE = "application/octet-stream";

    private final File file;
    private final List<DsvDocumentCreator> creators = new ArrayList<DsvDocumentCreator>();
    private final TaskProgressListener listener;

    public DsvDownloadTask(File file, Collection<? extends DsvDocumentCreator> creators, TaskProgressListener listener) {
        this.file = file;
        this.creators.addAll(creators);
        this.listener = listener;
    }

    public DownloadTaskResult call() {
        try {
            return success(createZip(file), CONTENT_TYPE);
        } catch (IOException e) {
            log.error("DSV download task execution I/O error", e);
            return error(e);
        } catch (DsvDocumentCreateException e) {
            log.error("DSV download task execution error", e);
            return error(e.getCause());
        }
    }

    private void notifyTaskProgress(int curr, int max) {
        log.debug("notifyTaskProgress({},{})", curr, max);
        if (listener != null) {
            listener.onTaskProgress(curr, max);
        }
    }

    private File createZip(File file) throws IOException, DsvDocumentCreateException {
        ZipOutputStream zout = null;
        boolean emptyZip = true;
        try {
            zout = new ZipOutputStream(new FileOutputStream(file));
            MultiDocProgressListener listener = new MultiDocProgressListener(creators.size());

            for (DsvDocumentCreator docCreator : creators) {
                log.info("Creating " + docCreator.getName() + "...");
                long start = System.currentTimeMillis();
                DsvRowIterator doc = docCreator.create();
                long dur = (int) (System.currentTimeMillis() - start)/1000;
                if (dur > 0)
                    log.info("Created " + docCreator.getName() + " in: " + dur + " s");
                zout.putNextEntry(new ZipEntry(docCreator.getName() + ".tab"));
                (new DsvDocumentWriter(tsv().newWriter(new OutputStreamWriter(zout)), listener.next())).write(doc);
            }
            zout.closeEntry();
            emptyZip = false;
            listener.done();
            return file;
        } finally {
            if (!emptyZip)
                closeQuietly(zout);
        }
    }

    private class MultiDocProgressListener implements DsvDocumentWriter.ProgressListener{
        private final int size;
        private static final int globalMax = 100;
        private int idx = -1;

        private MultiDocProgressListener(int size) {
            this.size = size;
        }

        public DsvDocumentWriter.ProgressListener next() {
            idx++;
            return this;
        }

        @Override
        public void setProgress(int curr, int max) {
            double from = 1.0 / size * idx;
            int c = (int) Math.floor(globalMax *(from + 1.0 * curr / size / max));
            notifyTaskProgress(c, globalMax);
        }

        public void done() {
            notifyTaskProgress(globalMax, globalMax);
        }
    }
}
