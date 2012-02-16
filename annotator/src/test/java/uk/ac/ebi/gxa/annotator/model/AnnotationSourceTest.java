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

package uk.ac.ebi.gxa.annotator.model;

import org.junit.Test;
import uk.ac.ebi.gxa.annotator.AnnotationException;
import uk.ac.ebi.microarray.atlas.model.bioentity.BioEntityProperty;
import uk.ac.ebi.microarray.atlas.model.bioentity.BioEntityType;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;
import static uk.ac.ebi.gxa.annotator.AnnotationSourceFactory.newBioMartAnnotationSource;


/**
 * @author Olga Melnichuk
 * @version 1/17/12 11:39 AM
 */
public class AnnotationSourceTest {

    @Test
    public void testExternalName2TypeMap() {
        BioMartAnnotationSource annotSource = newAnnotationSource();
        Map<String, BioEntityType> name2Type = annotSource.getExternalName2TypeMap();
        assertEquals(2, name2Type.size());
        assertTrue(name2Type.containsKey("gene"));
        assertTrue(name2Type.containsKey("transcript"));
    }

    @Test
    public void testNonIdentifierExternalProperties() throws AnnotationException {
        BioMartAnnotationSource annotSource = newAnnotationSource();
        List<ExternalBioEntityProperty> properties = annotSource.getNonIdentifierExternalProperties();
        assertEquals(3, properties.size());
        assertTrue(properties.contains(new ExternalBioEntityProperty("go_id", new BioEntityProperty(null, "go"), annotSource)));
        assertTrue(properties.contains(new ExternalBioEntityProperty("identifier", new BioEntityProperty(null, "identifier"), annotSource)));
        assertTrue(properties.contains(new ExternalBioEntityProperty("symbol", new BioEntityProperty(null, "name"), annotSource)));
    }

    @Test
    public void testBioEntityTypeGetter() {
        BioMartAnnotationSource annotSource = newAnnotationSource();
        assertNotNull(annotSource.getBioEntityType("ensgene"));
        assertNotNull(annotSource.getBioEntityType("enstranscript"));
        assertNull(annotSource.getBioEntityType("a type"));
    }

    private static BioMartAnnotationSource newAnnotationSource() {
        return newBioMartAnnotationSource()
                .type("ensgene", "ensgene", "name1")
                .type("enstranscript", "enstranscript", "identifier")
                .property("ensgene", "gene")
                .property("enstranscript", "transcript")
                .property("name", "symbol")
                .property("identifier", "identifier")
                .property("go", "go_id")
                .create();
    }

}
