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

import java.io.File;

/**
 * @author Olga Melnichuk
 */
public class DownloadTaskResult {

    private final File tmpFile;

    private final Exception exception;

    private DownloadTaskResult(File file, Exception e) {
        this.tmpFile = file;
        this.exception = e;
    }

    public boolean hasErrors() {
        return exception != null;
    }

    public File getFile() {
        return tmpFile;
    }

    public static DownloadTaskResult success(File file) {
        return new DownloadTaskResult(file, null);
    }

    public static DownloadTaskResult error(Exception e) {
        return new DownloadTaskResult(null, e);
    }
}
