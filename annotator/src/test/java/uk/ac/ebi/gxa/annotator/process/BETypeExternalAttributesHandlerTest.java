package uk.ac.ebi.gxa.annotator.process;

import org.junit.Test;
import uk.ac.ebi.gxa.annotator.model.biomart.BioMartAnnotationSource;
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
