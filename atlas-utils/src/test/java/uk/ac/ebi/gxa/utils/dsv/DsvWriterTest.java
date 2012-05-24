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

import org.junit.Test;

import java.io.IOException;
import java.io.StringWriter;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;

/**
 * @author Olga Melnichuk
 */
public class DsvWriterTest {

    @Test
    public void writeLineTest() throws IOException {
        StringWriter sw = new StringWriter();

        DsvWriter writer = new DsvWriter(sw, DsvFormat.tsv());
        writer.writeLine("");
        writer.writeLine("a");
        writer.writeLine(asList("", ""));
        writer.writeLine(asList("", "b"));
        writer.writeLine(asList("c", "d"));

        assertEquals("\na\n\tb\nc\td\n", sw.toString());
    }
}
