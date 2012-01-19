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
import uk.ac.ebi.microarray.atlas.model.Organism;
import uk.ac.ebi.microarray.atlas.model.bioentity.BioEntityProperty;
import uk.ac.ebi.microarray.atlas.model.bioentity.BioEntityType;
import uk.ac.ebi.microarray.atlas.model.bioentity.Software;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;


/**
 * @author Olga Melnichuk
 * @version 1/17/12 11:39 AM
 */
public class AnnotationSourceTest {

    @Test
    public void testExternalName2TypeMap() {
        BioMartAnnotationSource annotSource = getAnnotationSource();
        Map<String, BioEntityType> name2Type = annotSource.getExternalName2TypeMap();
        assertEquals(2, name2Type.size());
        assertTrue(name2Type.containsKey("gene"));
        assertTrue(name2Type.containsKey("transcript"));
    }

    @Test
    public void testNonIdentifierProperties() throws AnnotationException {
        BioMartAnnotationSource annotSource = getAnnotationSource();
        List<BioEntityProperty> properties = annotSource.getNonIdentifierProperties();
        assertEquals(3, properties.size());
        assertTrue(properties.contains(new BioEntityProperty(null, "go")));
        assertTrue(properties.contains(new BioEntityProperty(null, "identifier")));
        assertTrue(properties.contains(new BioEntityProperty(null, "name")));
    }

    @Test
    public void testBioEntityTypeGetter() {
        BioMartAnnotationSource annotSource = getAnnotationSource();
        assertNotNull(annotSource.getBioEntityType("ensgene"));
        assertNotNull(annotSource.getBioEntityType("enstranscript"));
        assertNull(annotSource.getBioEntityType("a type"));
    }

    private BioMartAnnotationSource getAnnotationSource() {
        Software software = new Software("plants", "8");
        Organism organism = new Organism(null, "arabidopsis thaliana");

        BioMartAnnotationSource annotationSource = new BioMartAnnotationSource(software, organism);
        annotationSource.setDatabaseName("plant");
        annotationSource.setDatasetName("athaliana_eg_gene");
        annotationSource.setUrl("http://plants.ensembl.org/biomart/martservice?");

        BioEntityProperty geneProp = new BioEntityProperty(null, "ensgene");
        BioEntityProperty transProp = new BioEntityProperty(null, "enstranscript");
        BioEntityProperty geneNameProp = new BioEntityProperty(null, "name");
        BioEntityProperty transNameProp = new BioEntityProperty(null, "identifier");
        BioEntityProperty goProp = new BioEntityProperty(null, "go");

        annotationSource.addExternalProperty("gene", geneProp);
        annotationSource.addExternalProperty("transcript", transProp);
        annotationSource.addExternalProperty("symbol", geneNameProp);
        annotationSource.addExternalProperty("identifier", transNameProp);
        annotationSource.addExternalProperty("go_id", goProp);

        BioEntityType type1 = new BioEntityType(null, "ensgene", 1, geneProp, geneNameProp);
        BioEntityType type2 = new BioEntityType(null, "enstranscript", 0, transProp, transNameProp);

        annotationSource.addBioEntityType(type1);
        annotationSource.addBioEntityType(type2);

        return annotationSource;
    }
}
