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

import uk.ac.ebi.gxa.spring.view.dsv.DsvDocument;
import uk.ac.ebi.gxa.utils.dsv.DsvFormat;
import uk.ac.ebi.gxa.download.DownloadTask;
import uk.ac.ebi.gxa.download.DownloadTaskResult;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static com.google.common.io.Closeables.closeQuietly;
import static java.io.File.createTempFile;
import static uk.ac.ebi.gxa.download.DownloadTaskResult.error;
import static uk.ac.ebi.gxa.download.DownloadTaskResult.success;

/**
 * @author Olga Melnichuk
 */
public class DsvDownloadTask implements DownloadTask {

    private final String token;
    private final DsvDocumentCreator creator;

    public DsvDownloadTask(String token, DsvDocumentCreator creator) {
        this.token = token;
        this.creator = creator;
    }

    public String getToken() {
        return token;
    }

    public DownloadTaskResult call() {
        try {
            return success(
                    write(createDsvDocument(), createTempFile(token, "tmp")));
        } catch (Exception e) {
            //TODO log error
            return error(e);
        }
    }

    private DsvDocument createDsvDocument() throws Exception {
       return creator.create();
    }

    private File write(DsvDocument doc, File file) throws IOException {
        ZipOutputStream zout = null;
        try {
            zout = new ZipOutputStream(new FileOutputStream(file));
            zout.putNextEntry(new ZipEntry("data.tab"));
            doc.write(DsvFormat.tsv(), new OutputStreamWriter(zout));
            zout.closeEntry();
            return file;
        } finally {
            closeQuietly(zout);
        }
    }
}
