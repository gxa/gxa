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

package uk.ac.ebi.gxa.annotator.loader;

import org.junit.Test;
import uk.ac.ebi.gxa.annotator.model.BioMartAnnotationSource;
import uk.ac.ebi.microarray.atlas.model.Organism;
import uk.ac.ebi.microarray.atlas.model.bioentity.BioEntityProperty;
import uk.ac.ebi.microarray.atlas.model.bioentity.BioEntityType;
import uk.ac.ebi.microarray.atlas.model.bioentity.Software;

import java.util.Collection;
import java.util.List;

import static junit.framework.Assert.*;

/**
 * User: nsklyar
 * Date: 12/10/2011
 */
public class BETypeExternalAttributesHandlerTest {

    @Test
    public void testGetMartBEIdentifiers() throws Exception {
        Annotator.BETypeExternalAttributesHandler handler =
                new Annotator.BETypeExternalAttributesHandler(getAnnotationSource());
        List<String> martBEIdentifiers = handler.getExternalBEIdentifiers();
        assertEquals(2, martBEIdentifiers.size());
        assertTrue(martBEIdentifiers.contains("gene"));
        assertTrue(martBEIdentifiers.contains("transcript"));
    }

    @Test
    public void testCheckOrder() throws Exception {
        Annotator.BETypeExternalAttributesHandler handler =
                new Annotator.BETypeExternalAttributesHandler(getAnnotationSource());
        List<BioEntityType> types = handler.getTypes();
        assertEquals(2, types.size());

        List<String> martBEIdentifiers = handler.getExternalBEIdentifiers();
        if (types.get(0).getIdentifierProperty().getName().equals("ensgene")) {
            assertEquals("gene", martBEIdentifiers.get(0));
        } else if (types.get(0).getIdentifierProperty().getName().equals("enstranscript")) {
            assertEquals("transcript", martBEIdentifiers.get(0));
        } else {
            fail();
        }
    }

    @Test
    public void testGetBioEntityProperties() throws Exception{
         Annotator.BETypeExternalAttributesHandler handler =
                new Annotator.BETypeExternalAttributesHandler(getAnnotationSource());
        final Collection<BioEntityProperty> bioEntityProperties = handler.getBioEntityProperties();
        assertEquals(3, bioEntityProperties.size());
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
