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

import java.io.Writer;
import java.util.Arrays;
import java.util.Iterator;

/**
 * @author Olga Melnichuk
 */
public abstract class DsvFormat {

    private final char delimiter;
    private final String contentType;
    private final String ext;

    public DsvFormat(char delimiter, String contentType, String ext) {
        this.delimiter = delimiter;
        this.contentType = contentType;
        this.ext = ext;
    }
    
    public DsvFormat(char delimiter, String contentType) {
        this(delimiter, contentType, "");
    }

    public String getContentType() {
        return contentType;
    }

    public DsvWriter newWriter(Writer writer) {
        return new DsvWriter(writer, this);
    }

    public String fileName(String fileName) {
        return fileName + ext;
    }

    String joinValues(Iterable<String> values) {
        StringBuilder sb = new StringBuilder();
        Iterator<String> iterator = values.iterator();
        while(iterator.hasNext()) {
           sb.append(sanitizeFieldValue(iterator.next()));
            if (iterator.hasNext()) {
                sb.append(delimiter);
            }
        }
        return sb.toString();
    }

    abstract String sanitizeFieldValue(String value);

    public static TsvFormat tsv() {
        return new TsvFormat();
    }

    public static CsvFormat csv() {
        return new CsvFormat();
    }
}
