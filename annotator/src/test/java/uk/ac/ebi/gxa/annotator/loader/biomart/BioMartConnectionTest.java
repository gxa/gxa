package uk.ac.ebi.gxa.annotator.loader.biomart;

import junit.framework.TestCase;
import org.apache.commons.io.FileUtils;
import org.junit.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
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
        bmService = new BioMartConnection("http://plants.ensembl.org/biomart/martservice?", "plants", "athaliana_eg_gene");
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
        bmService = new BioMartConnection("http://plants.ensembl.org/biomart/martservice?", "plants", "athaliana_eg_gene");
        boolean isValid = bmService.isValidDataSetName();
        assertTrue(isValid);

        bmService = new BioMartConnection("http://plants.ensembl.org/biomart/martservice?", "plants", "wrong_name");
        boolean isValid2 = bmService.isValidDataSetName();
        assertFalse(isValid2);
    }

    @Test
    public void testValidateAttributeNames() throws Exception {
        bmService = new BioMartConnection("http://plants.ensembl.org/biomart/martservice?", "plants", "athaliana_eg_gene");
        Set<String> attributes = new HashSet<String>();
        attributes.add("ddd");
        attributes.add("name_1006");

        Collection<String> missing = bmService.validateAttributeNames(attributes);

        assertEquals(1, missing.size());
        assertTrue(missing.contains("ddd"));

    }

    @Test
    public void testGetPropertyForOrganismURL() throws Exception {
        bmService = new BioMartConnection("http://plants.ensembl.org/biomart/martservice?", "plants", "athaliana_eg_gene");

        URL attributesURL = bmService.getAttributesURL(Arrays.asList(new String[]{"ensembl_gene_id", "ensembl_transcript_id", "external_gene_id"}));
        assertEquals("http://plants.ensembl.org/biomart/martservice?query=%3C%3Fxml+version%3D%221.0%22+encoding%3D%22UTF-8%22%3F%3E%3C%21DOCTYPE+Query%3E%3CQuery++virtualSchemaName+%3D+%22plants_mart_10%22+formatter+%3D+%22TSV%22+header+%3D+%220%22+uniqueRows+%3D+%221%22+count+%3D+%22%22+%3E%3CDataset+name+%3D+%22athaliana_eg_gene%22+interface+%3D+%22default%22+%3E%3CAttribute+name+%3D+%22ensembl_gene_id%22+%2F%3E%3CAttribute+name+%3D+%22ensembl_transcript_id%22+%2F%3E%3CAttribute+name+%3D+%22external_gene_id%22+%2F%3E%3C%2FDataset%3E%3C%2FQuery%3E",
                attributesURL.toString());

    }

    @Test
    public void testReadFromURL() throws Exception {
        bmService = new BioMartConnection("http://plants.ensembl.org/biomart/martservice?", "plants", "athaliana_eg_gene");
        URL attributesURL = bmService.getAttributesURL(Arrays.asList(new String[]{"ensembl_gene_id", "ensembl_transcript_id", "external_gene_id"}));
        HttpURLConnection uc = (HttpURLConnection) attributesURL.openConnection();
        int code = uc.getResponseCode();
        String response = uc.getResponseMessage();
        System.out.println("HTTP/1.x " + code + " " + response);

        Object o = attributesURL.getContent();
        System.out.println("I got a " + o.getClass().getName());

        int contentLength = uc.getContentLength();
        if (contentLength < 0)
            System.out.println("Could not determine file size.");
        else
            System.out.println(contentLength);


//        for (int j = 1; ; j++) {
//            String header = uc.getHeaderField(j);
//            String key = uc.getHeaderFieldKey(j);
//            if (header == null || key == null)
//                break;
//            System.out.println(uc.getHeaderFieldKey(j) + ": " + header);
//        }
//        InputStream in = new BufferedInputStream(uc.getInputStream());
//
//        Reader r = new InputStreamReader(in);
//        int c;
//        while ((c = r.read()) != -1) {
//            System.out.print((char) c);
//        }

    }

    @Test
    public void testGetReader() throws Exception {
        bmService = new BioMartConnection("http://www.ensembl.org/biomart/martservice?", "ensembl", "mmusculus_gene_ensembl");
        URL url = bmService.getAttributesURL(Arrays.asList(new String[]{"ensembl_gene_id", "ensembl_transcript_id", "name_1006"}));
        try {
            /*
            * Get a connection to the URL and start up
            * a buffered reader.
            */
            long startTime = System.currentTimeMillis();

            System.out.println("Connecting to biomart site...\n");

            url.openConnection();
            InputStream reader = url.openStream();

            /*
            * Setup a buffered file writer to write
            * out what we read from the website.
            */
            FileOutputStream writer = new FileOutputStream("biomart_out.txt");
            byte[] buffer = new byte[153600];
            int totalBytesRead = 0;
            int bytesRead = 0;

            System.out.println("Reading ZIP file 150KB blocks at a time.\n");

            while ((bytesRead = reader.read(buffer)) > 0) {
                writer.write(buffer, 0, bytesRead);
                buffer = new byte[153600];
                totalBytesRead += bytesRead;
            }

            long endTime = System.currentTimeMillis();

            System.out.println("Done. " + (new Integer(totalBytesRead).toString()) + " bytes read (" + (new Long(endTime - startTime).toString()) + " millseconds).\n");
            writer.close();
            reader.close();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    @Test
    public void testGetReader1() throws Exception {
//        bmService = new BioMartConnection("http://www.ensembl.org/biomart/martservice?", "ensembl", "mmusculus_gene_ensembl");
//        URL url = bmService.getAttributesURL(Arrays.asList(new String[]{"ensembl_gene_id", "ensembl_transcript_id", "name_1006"}));
        bmService = new BioMartConnection("http://plants.ensembl.org/biomart/martservice?", "plants", "athaliana_eg_gene");
        URL url = bmService.getAttributesURL(Arrays.asList(new String[]{"ensembl_gene_id", "ensembl_transcript_id", "external_gene_id"}));
        
        System.out.println("Connecting to biomart site...\n");

        long startTime = System.currentTimeMillis();

        FileUtils.copyURLToFile(url, new File("athaliana_eg_gene", "external_gene_id.txt"));
        long endTime = System.currentTimeMillis();
        System.out.println("Done. " +  (new Long(endTime - startTime).toString()) + " millseconds).\n");
        
        File file = new File("athaliana_eg_gene", "external_gene_id.txt");
        System.out.println("file.length() = " + file.length());
    }
}
