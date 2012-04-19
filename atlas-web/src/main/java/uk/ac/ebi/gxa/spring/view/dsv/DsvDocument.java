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

package uk.ac.ebi.gxa.spring.view.dsv;

import uk.ac.ebi.gxa.utils.dsv.DsvFormat;
import uk.ac.ebi.gxa.utils.dsv.DsvWriter;

import java.io.IOException;
import java.io.Writer;
import java.util.Iterator;

/**
 * @author Olga Melnichuk
 */
public abstract class DsvDocument {

    public abstract String[] getHeader();

    public abstract Iterator<String[]> getRowIterator();

    public void write(DsvFormat format, Writer writer) throws IOException {
        DsvWriter dsvWriter = format.newWriter(writer);
        dsvWriter.write(getHeader());
        Iterator<String[]> rowIterator = getRowIterator();
        while (rowIterator.hasNext()) {
            dsvWriter.write(rowIterator.next());
        }
        dsvWriter.flush();
    }
}
