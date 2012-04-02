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

import java.io.IOException;
import java.io.Writer;

/**
 * @author Olga Melnichuk
 */
public class DsvWriter {

    private final Writer writer;

    private final DsvFormat dsvFormat;

    protected DsvWriter(Writer writer, DsvFormat dsvFormat) {
        this.dsvFormat = dsvFormat;
        this.writer = writer;
    }

    public void write(String[] values) throws IOException {
        writer.write(dsvFormat.joinValues(values));
        writer.write("\n");
    }

    public void flush() throws IOException {
        writer.flush();
    }

    public void close() throws IOException {
        writer.close();
    }
}
