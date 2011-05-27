package uk.ac.ebi.gxa.loader.bioentity;

import junit.framework.TestCase;
import org.junit.Test;
import uk.ac.ebi.microarray.atlas.model.bioentity.BioMartProperty;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * User: nsklyar
 * Date: 05/05/2011
 */
public class BioMartServiceTest extends TestCase {
    
    private BioMartService bmService;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        bmService = new BioMartService();
    }

    @Test
    public void testGetDataSetVersion() throws Exception {
        String version = bmService.getDataSetVersion("http://plants.ensembl.org/biomart/martservice?", "plant");
        boolean correctVersion = true;
        try {
            Integer.parseInt(version);
        } catch (NumberFormatException e) {
            correctVersion = false;
        }
        assertTrue("Dataset BioMart version is not correct", correctVersion);
    }

    @Test
    public void testValidateOrganismName() throws Exception {
        boolean isValid = bmService.isValidOrganismName("http://plants.ensembl.org/biomart/martservice?", "plant_mart_9", "athaliana_eg_gene");
        assertTrue(isValid);

        boolean isValid2 = bmService.isValidOrganismName("http://plants.ensembl.org/biomart/martservice?", "plant_mart_9", "athaliana__gene");
        assertFalse(isValid2);
    }

    @Test
    public void testValidateAttributeNames() throws Exception {
        List<BioMartProperty> properties= new ArrayList<BioMartProperty>();
        properties.add(new BioMartProperty(null, "ddd", null));
        properties.add(new BioMartProperty(null, "name_1006", null));
        List<BioMartProperty> missing = bmService.validateAttributeNames("http://plants.ensembl.org/biomart/martservice?", "athaliana_eg_gene",
                properties);

        assertEquals(1, missing.size());
        assertEquals("ddd", missing.get(0).getBiomartPropertyName());
    }

    @Test
    public void testGetPropertyForOrganismURL() throws Exception {
        String encode = URLEncoder.encode("<?xml version=\"1.0\" encoding=\"UTF-8\"?><!DOCTYPE Query><Query  virtualSchemaName = \"fungi_mart_9\" formatter = \"TSV\" header = \"0\" uniqueRows = \"0\" count = \"\" datasetConfigVersion = \"0.7\" ><Dataset name = \"scerevisiae_eg_gene\" interface = \"default\" ><Attribute name = \"ensembl_gene_id\" /><Attribute name = \"ensembl_transcript_id\" /></Dataset></Query>", "US-ASCII");
        System.out.println("encode = " + encode);
    }


}
