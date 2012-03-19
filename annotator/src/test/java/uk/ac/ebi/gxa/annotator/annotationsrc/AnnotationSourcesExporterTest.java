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

package uk.ac.ebi.gxa.annotator.annotationsrc;

import com.google.common.collect.Lists;
import org.junit.Test;

import java.util.Collection;

import static junit.framework.Assert.assertEquals;

/**
 * User: nsklyar
 * Date: 19/03/2012
 */
public class AnnotationSourcesExporterTest {
    private static final String separator = "\n$$$\n";

    @Test
    public void testJoinAsText() throws Exception {
        final String result = AnnotationSourcesExporter.joinAsText(Lists.newArrayList("first", "second"), "biomart", separator);
        assertEquals("Type: biomart\n" +
                "first" +
                separator +
                "Type: biomart\n" +
                "second", result);
    }

    @Test
    public void testJoinAll() throws Exception {
        final String s = AnnotationSourcesExporter.joinAll("\n$$$\n"
                , AnnotationSourcesExporter.joinAsText(Lists.newArrayList("first", "second"), "biomart", separator)
                , AnnotationSourcesExporter.joinAsText(Lists.newArrayList("first", "second"), "genesig", separator));
        assertEquals("Type: biomart\n" +
                "first" +
                separator +
                "Type: biomart\n" +
                "second" +
                separator +
                "Type: genesig\n" +
                "first" +
                separator +
                "Type: genesig\n" +
                "second", s);
    }

    @Test
    public void testGetStringSourcesOfType() throws Exception {
        final Collection<String> genesig = AnnotationSourcesExporter.getStringSourcesOfType("Type: biomart\n" +
                "first" +
                separator +
                "Type: biomart\n" +
                "second" +
                separator +
                "Type: genesig\n" +
                "first", "genesig", separator);
        assertEquals(1, genesig.size());
        assertEquals("first", genesig.iterator().next());
    }
}
