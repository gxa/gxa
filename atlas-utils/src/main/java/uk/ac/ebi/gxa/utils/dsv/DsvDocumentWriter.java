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

package uk.ac.ebi.gxa.utils.dsv;

import java.io.Closeable;
import java.io.IOException;
import java.util.Iterator;

/**
 * @author Olga Melnichuk
 */
public class DsvDocumentWriter implements Closeable {

    private static final int NOTIFY_DELAY = 10;

    private ProgressListener progressListener;
    private final DsvWriter dsvWriter;

    public DsvDocumentWriter(DsvWriter dsvWriter) {
        this(dsvWriter, null);
    }

    public DsvDocumentWriter(DsvWriter dsvWriter, ProgressListener progressListener) {
        this.progressListener = progressListener;
        this.dsvWriter = dsvWriter;
    }

    public final void write(DsvDocument doc) throws IOException {
        dsvWriter.write(doc.getHeader());
        Iterator<String[]> rowIterator = doc.getRowIterator();

        int max = doc.getTotalRowCount();
        int processed = 0;
        while (rowIterator.hasNext()) {
            dsvWriter.write(rowIterator.next());
            notifyProgress(processed++, max);
        }
        dsvWriter.flush();
        notifyProgress(max, max);
    }

    public void close() throws IOException {
        dsvWriter.close();
    }

    private void notifyProgress(int processed, int max) {
        if (progressListener == null || (max - processed) % NOTIFY_DELAY != 0) {
            return;
        }
        progressListener.setProgress(processed, max);
    }

    public interface ProgressListener {
        public void setProgress(int curr, int max);
    }
}
