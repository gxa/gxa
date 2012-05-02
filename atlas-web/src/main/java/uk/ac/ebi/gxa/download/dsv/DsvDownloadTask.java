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

import uk.ac.ebi.gxa.download.DownloadTaskResult;
import uk.ac.ebi.gxa.download.TaskProgressListener;
import uk.ac.ebi.gxa.utils.dsv.DsvDocument;
import uk.ac.ebi.gxa.utils.dsv.DsvDocumentWriter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.concurrent.Callable;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static com.google.common.io.Closeables.closeQuietly;
import static java.io.File.createTempFile;
import static uk.ac.ebi.gxa.download.DownloadTaskResult.error;
import static uk.ac.ebi.gxa.download.DownloadTaskResult.success;
import static uk.ac.ebi.gxa.utils.dsv.DsvFormat.tsv;

/**
 * @author Olga Melnichuk
 */
public class DsvDownloadTask implements Callable<DownloadTaskResult> {

    private static final String CONTENT_TYPE = "application/octet-stream";

    private final String token;
    private final DsvDocumentCreator creator;
    private final TaskProgressListener listener;

    public DsvDownloadTask(String token, DsvDocumentCreator creator, TaskProgressListener listener) {
        this.token = token;
        this.creator = creator;
        this.listener = listener;
    }

    public DownloadTaskResult call() {
        try {
            return success(
                    write(createDsvDocument(), createTempFile(token, ".zip")),
                    CONTENT_TYPE);
        } catch (IOException e) {
            //TODO log error
            return error(e);
        } catch (DsvDocumentCreateException e) {
            //TODO log error
            return error(e);
        }
    }

    private void notifyTaskProgress(int curr, int max) {
        if (listener != null) {
            listener.onTaskProgress(curr, max);
        }
    }

    private DsvDocument createDsvDocument() throws DsvDocumentCreateException {
       return creator.create();
    }

    private File write(DsvDocument doc, File file) throws IOException {
        DsvDocumentWriter writer = null;
        try {
            ZipOutputStream zout = new ZipOutputStream(new FileOutputStream(file));
            zout.putNextEntry(new ZipEntry("data.tab"));

            writer = new DsvDocumentWriter(tsv().newWriter(new OutputStreamWriter(zout)), new DsvDocumentWriter.ProgressListener() {
                @Override
                public void setProgress(int curr, int max) {
                    notifyTaskProgress(max + curr, 2 * max);
                }
            });
            writer.write(doc);

            zout.closeEntry();
            return file;
        } finally {
            closeQuietly(writer);
        }
    }
}
