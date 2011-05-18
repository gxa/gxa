package uk.ac.ebi.gxa.loader.bioentity;

import junit.framework.TestCase;
import org.junit.Test;

import java.net.URLEncoder;
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
    public void testValidateAttributeNames() throws Exception {
        List<String> missing = bmService.validateAttributeNames("http://plants.ensembl.org/biomart/martservice?", "athaliana_eg_gene",
                Arrays.asList(new String[]{"ddd", "name_1006"}));

        assertEquals(1, missing.size());
        assertEquals("ddd", missing.get(0));
    }

    @Test
    public void testGetPropertyForOrganismURL() throws Exception {
        String encode = URLEncoder.encode("<?xml version=\"1.0\" encoding=\"UTF-8\"?><!DOCTYPE Query><Query  virtualSchemaName = \"fungi_mart_9\" formatter = \"TSV\" header = \"0\" uniqueRows = \"0\" count = \"\" datasetConfigVersion = \"0.7\" ><Dataset name = \"scerevisiae_eg_gene\" interface = \"default\" ><Attribute name = \"ensembl_gene_id\" /><Attribute name = \"ensembl_transcript_id\" /></Dataset></Query>", "US-ASCII");
        System.out.println("encode = " + encode);
    }


}
