package uk.ac.ebi.gxa.loader.bioentity;

import junit.framework.TestCase;
import org.junit.Test;

import java.net.URLEncoder;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * User: nsklyar
 * Date: 05/05/2011
 */
public class BioMartConnectionTest extends TestCase {

    private BioMartConnection bmService;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    @Test
    public void testGetDataSetVersion() throws Exception {
        bmService = new BioMartConnection("http://plants.ensembl.org/biomart/martservice?", "plant", "athaliana_eg_gene");
        String version = bmService.getOnlineMartVersion();
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
        bmService = new BioMartConnection("http://plants.ensembl.org/biomart/martservice?", "plant", "athaliana_eg_gene");
        boolean isValid = bmService.isValidDataSetName();
        assertTrue(isValid);

        bmService = new BioMartConnection("http://plants.ensembl.org/biomart/martservice?", "plant", "wrong_name");
        boolean isValid2 = bmService.isValidDataSetName();
        assertFalse(isValid2);
    }

    @Test
    public void testValidateAttributeNames() throws Exception {
        bmService = new BioMartConnection("http://plants.ensembl.org/biomart/martservice?", "plant", "athaliana_eg_gene");
        Set<String> attributes = new HashSet<String>();
        attributes.add("ddd");
        attributes.add("name_1006");

        Collection<String> missing = bmService.validateAttributeNames(attributes);

        assertEquals(1, missing.size());
        assertTrue(missing.contains("ddd"));

    }

    @Test
    public void testGetPropertyForOrganismURL() throws Exception {
//        String encode = URLEncoder.encode("<?xml version=\"1.0\" encoding=\"UTF-8\"?><!DOCTYPE Query><Query  virtualSchemaName = \"fungi_mart_9\" formatter = \"TSV\" header = \"0\" uniqueRows = \"0\" count = \"\" datasetConfigVersion = \"0.7\" ><Dataset name = \"scerevisiae_eg_gene\" interface = \"default\" ><Attribute name = \"ensembl_gene_id\" /><Attribute name = \"ensembl_transcript_id\" /></Dataset></Query>", "US-ASCII");
//        System.out.println("encode = " + encode);
    }


}
