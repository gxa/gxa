package uk.ac.ebi.gxa.loader.bioentity;

import au.com.bytecode.opencsv.CSVReader;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.gxa.loader.AtlasLoaderException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;

import static com.google.common.io.Closeables.closeQuietly;

/**
 * User: nsklyar
 * Date: 14/04/2011
 */
public class BioMartDAO {
    // logging
    final private Logger log = LoggerFactory.getLogger(this.getClass());

    private static final String DATA_SET = "$DATA_SET";
    private static final String PROP_NAME = "$PROP_NAME";

    private final static String PROPERTY_QUERY = "query=%3C?xml%20version=%221.0%22%20encoding=%22UTF-8%22?%3E%3C!DOCTYPE%20Query%3E%3CQuery%20%20virtualSchemaName%20=%20%22default%22%20formatter%20=%20%22TSV%22%20header%20=%20%220%22%20uniqueRows%20=%20%220%22%20count%20=%20%22%22%20datasetConfigVersion%20=%20%220.6%22%20%3E%3CDataset%20name%20=%20%22" + DATA_SET + "%22%20interface%20=%20%22default%22%20%3E%3CAttribute%20name%20=%20%22ensembl_gene_id%22%20/%3E%3CAttribute%20name%20=%20%22ensembl_transcript_id%22%20/%3E%3CAttribute%20name%20=%20%22" + PROP_NAME + "%22%20/%3E%3C/Dataset%3E%3C/Query%3E";
    private final static String REGISTRY_QUERY = "type=registry";

        private static final String URL_LOCATION = "http://www.ensembl.org/biomart/martservice?";
//    private static final String URL_LOCATION = "http://www.biomart.org/biomart/martservice?";
    private static final String ENS_DB = "database=\"ensembl_mart_";

    public String getEnsemblVersion() {
        URL url = null;
        BufferedReader bufferedReader = null;
        String version = null;
        try {
            url = new URL(URL_LOCATION + REGISTRY_QUERY);

            InputStream inputStream = url.openStream();
            bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            String line;

            while ((line = bufferedReader.readLine()) != null) {
                int ensPosition;
                if ((ensPosition = line.indexOf(ENS_DB)) > -1) {
                    version = line.substring(ensPosition + ENS_DB.length(), line.indexOf("\"", ensPosition + ENS_DB.length()));
                    System.out.println("version = " + version);
                }
            }


        } catch (MalformedURLException e) {
            log.error("Problem when reading registry " + url, e);
        } catch (IOException e) {
            log.error("Problem when reading registry " + url, e);
        } finally {
            log.info("Finished reading from " + url + ", closing");
            closeQuietly(bufferedReader);
        }
        return version;
    }

    /**
     * @param ensOrganism
     * @param ensProperty
     * @return null - in a case of BioMart error and if ensProperty doesn't exist for a given ensOrganism
     */
    public URL getPropertyForOrganismURL(String ensOrganism, String ensProperty) {
        URL url = null;
        String location = URL_LOCATION + PROPERTY_QUERY.replace(DATA_SET, ensOrganism).replace(PROP_NAME, ensProperty);

        BufferedReader bufferedReader = null;
        try {
            url = new URL(location);

            InputStream inputStream = url.openStream();
            bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            String line = bufferedReader.readLine();

            if (StringUtils.isEmpty(line) || line.contains("Exception") || line.contains("ERROR")) {
                log.info("Cannot get property " + ensProperty + " for organism " + ensOrganism);
                url = null;
            }

        } catch (MalformedURLException e) {
            log.error("Problem when reading bioentity annotations from " + location, e);
        } catch (IOException e) {
            log.error("Problem when reading bioentity annotations from " + location, e);
        } finally {
            log.info("Finished reading from " + location + ", closing");
            closeQuietly(bufferedReader);
        }
        return url;
    }

    /**
     * @param ensOrganism
     * @param ensProperty
     * @return null - in a case of BioMart error and if ensProperty doesn't exist for a given ensOrganism
     */
    public CSVReader getPropertyForOrganism(String ensOrganism, String ensProperty) {
        String location = URL_LOCATION + PROPERTY_QUERY.replace(DATA_SET, ensOrganism).replace(PROP_NAME, ensProperty);
        CSVReader csvReader = null;
        BufferedReader bufferedReader = null;
        try {
            URL url = new URL(location);

            InputStream inputStream = url.openStream();
            bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            String line = bufferedReader.readLine();

            if (StringUtils.isNotEmpty(line) && !line.contains("Exception") && !line.contains("ERROR")) {
                csvReader = new CSVReader(new InputStreamReader(url.openStream()), '\t', '"');
            } else {
                log.info("Cannot get property " + ensProperty + " for organism " + ensOrganism);
            }

        } catch (MalformedURLException e) {
            log.error("Problem when reading bioentity annotations from " + location, e);
        } catch (IOException e) {
            log.error("Problem when reading bioentity annotations from " + location, e);
        } finally {
            log.info("Finished reading from " + location + ", closing");
            closeQuietly(bufferedReader);
            closeQuietly(csvReader);
        }
        return csvReader;
    }

    public static void main(String[] args) throws AtlasLoaderException {
        BioMartDAO dao = new BioMartDAO();
//        String ensemblVersion = dao.getEnsemblVersion();

        CSVReader propertyForOrganism = dao.getPropertyForOrganism("hsapiens_gene_ensembl", "ensembl_peptide_id");
        System.out.println("propertyuniprot_sptrembl, uniprot_swissprot_accessionForOrganism = " + propertyForOrganism);

  
    }
}
