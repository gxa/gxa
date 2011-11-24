package uk.ac.ebi.gxa.annotator.loader.biomart;

import org.junit.Test;
import uk.ac.ebi.gxa.annotator.model.biomart.BioMartAnnotationSource;
import uk.ac.ebi.microarray.atlas.model.Organism;
import uk.ac.ebi.microarray.atlas.model.bioentity.BioEntityProperty;
import uk.ac.ebi.microarray.atlas.model.bioentity.BioEntityType;
import uk.ac.ebi.microarray.atlas.model.bioentity.Software;

import java.util.List;

import static junit.framework.Assert.*;

/**
 * User: nsklyar
 * Date: 12/10/2011
 */
public class BioMartAnnotatorTest {

    @Test
    public void testGetMartBEIdentifiersAndNames() throws Exception {
        BioMartAnnotator.BETypeMartAttributesHandler handler =
                new BioMartAnnotator.BETypeMartAttributesHandler(getAnnotationSource());

        List<String> identifiersAndNames = handler.getMartBEIdentifiersAndNames();
        assertEquals(4, identifiersAndNames.size());
        String first = identifiersAndNames.get(0);
        if (first.equals("gene"))
            assertEquals("symbol", identifiersAndNames.get(1));
        if (first.equals("transcript"))
            assertEquals("identifier",identifiersAndNames.get(1));
        else
            fail();
    }

    @Test
    public void testGetMartBEIdentifiers() throws Exception {
        BioMartAnnotator.BETypeMartAttributesHandler handler =
                new BioMartAnnotator.BETypeMartAttributesHandler(getAnnotationSource());
        List<String> martBEIdentifiers = handler.getMartBEIdentifiers();
        assertEquals(2, martBEIdentifiers.size());
        assertTrue(martBEIdentifiers.contains("gene"));
        assertTrue(martBEIdentifiers.contains("transcript"));
    }

    @Test
    public void testCheckOrder() throws Exception {
        BioMartAnnotator.BETypeMartAttributesHandler handler =
                new BioMartAnnotator.BETypeMartAttributesHandler(getAnnotationSource());
        List<BioEntityType> types = handler.getTypes();
        assertEquals(2, types.size());

        List<String> martBEIdentifiers = handler.getMartBEIdentifiers();
        List<String> martBEIdentifiersAndNames = handler.getMartBEIdentifiersAndNames();
        if (types.get(0).getIdentifierProperty().getName().equals("ensgene")) {
            assertEquals("gene", martBEIdentifiers.get(0));
            assertEquals("gene", martBEIdentifiersAndNames.get(0));
            assertEquals("symbol", martBEIdentifiersAndNames.get(1));
        } else if (types.get(0).getIdentifierProperty().getName().equals("enstranscript")) {
            assertEquals("transcript", martBEIdentifiers.get(0));
            assertEquals("transcript", martBEIdentifiersAndNames.get(0));
            assertEquals("identifier", martBEIdentifiersAndNames.get(1));
        } else {
            fail();
        }
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
        BioEntityProperty nameProp = new BioEntityProperty(null, "name");
        BioEntityProperty idenProp = new BioEntityProperty(null, "identifier");

        annotationSource.addExternalProperty("gene", geneProp);
        annotationSource.addExternalProperty("transcript", transProp);
        annotationSource.addExternalProperty("symbol", nameProp);
        annotationSource.addExternalProperty("identifier", idenProp);

        BioEntityType type1 = new BioEntityType(null, "ensgene", 1, geneProp, nameProp);
        BioEntityType type2 = new BioEntityType(null, "enstranscript", 0, transProp, idenProp);

        annotationSource.addBioEntityType(type1);
        annotationSource.addBioEntityType(type2);

        return annotationSource;
    }
}
