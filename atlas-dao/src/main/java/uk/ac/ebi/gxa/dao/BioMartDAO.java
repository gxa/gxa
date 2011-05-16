package uk.ac.ebi.gxa.dao;

import au.com.bytecode.opencsv.CSVReader;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.google.common.io.Closeables.closeQuietly;

/**
 * User: nsklyar
 * Date: 14/04/2011
 */
public class BioMartDAO {
    // logging
    final private Logger log = LoggerFactory.getLogger(this.getClass());

    private static final String DATA_SET_PH = "$DATA_SET";
    private static final String PROP_NAME_PH = "$PROP_NAME";
    private static final String MART_NAME_PH = "$MART_NAME";

    private final static String PROPERTY_QUERY = "query=%3C?xml%20version=%221.0%22%20encoding=%22UTF-8%22?%3E%3C!DOCTYPE%20Query%3E%3CQuery%20%20virtualSchemaName%20=%20%22default%22%20formatter%20=%20%22TSV%22%20header%20=%20%220%22%20uniqueRows%20=%20%220%22%20count%20=%20%22%22%20datasetConfigVersion%20=%20%220.6%22%20%3E%3CDataset%20name%20=%20%22" + DATA_SET_PH + "%22%20interface%20=%20%22default%22%20%3E%3CAttribute%20name%20=%20%22ensembl_gene_id%22%20/%3E%3CAttribute%20name%20=%20%22ensembl_transcript_id%22%20/%3E%3CAttribute%20name%20=%20%22" + PROP_NAME_PH + "%22%20/%3E%3C/Dataset%3E%3C/Query%3E";
    private final static String REGISTRY_QUERY = "type=registry";
    private final static String ATTRIBUTES_QUERY = "type=attributes&dataset=";

    private static final String URL_LOCATION = "http://www.ensembl.org/biomart/martservice?";
    //    private static final String URL_LOCATION = "http://www.biomart.org/biomart/martservice?";
    private static final String MART_DB = "database=\""+ MART_NAME_PH +"_mart_";

//    public String getEnsemblVersion() {
//        URL url = null;
//        BufferedReader bufferedReader = null;
//        String version = null;
//        try {
//            url = new URL(URL_LOCATION + REGISTRY_QUERY);
//
//            InputStream inputStream = url.openStream();
//            bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
//            String line;
//
//            while ((line = bufferedReader.readLine()) != null) {
//                int ensPosition;
//                if ((ensPosition = line.indexOf(MART_DB)) > -1) {
//                    version = line.substring(ensPosition + MART_DB.length(), line.indexOf("\"", ensPosition + MART_DB.length()));
//                    System.out.println("version = " + version);
//                }
//            }
//
//
//        } catch (MalformedURLException e) {
//            log.error("Problem when reading registry " + url, e);
//        } catch (IOException e) {
//            log.error("Problem when reading registry " + url, e);
//        } finally {
//            log.info("Finished reading from " + url + ", closing");
//            closeQuietly(bufferedReader);
//        }
//        return version;
//    }

    public String getDataSetVersion(String martUrl, String martName) throws AtlasDataAccessException{
        URL url = null;
        BufferedReader bufferedReader = null;
        String version = null;
        try {
            url = getURL(martUrl + REGISTRY_QUERY);

            InputStream inputStream = url.openStream();
            bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            String line;

            while ((line = bufferedReader.readLine()) != null) {
                int ensPosition;
                String martDb = MART_DB.replace(MART_NAME_PH, martName);
                if ((ensPosition = line.indexOf(martDb)) > -1) {
                    version = line.substring(ensPosition + martDb.length(), line.indexOf("\"", ensPosition + martDb.length()));
                }
            }
        } catch (IOException e) {
            throw new AtlasDataAccessException("Problem when reading registry " + url, e) ;
        } finally {
            log.info("Finished reading from " + url + ", closing");
            closeQuietly(bufferedReader);
        }
        return version;
    }

    public List<String> validateAttributeNames(String martUrl, String ensOrganismName, List<String> attributes) throws AtlasDataAccessException {
        List<String> missingAttrs = new ArrayList<String>();

        String location = martUrl + ATTRIBUTES_QUERY + ensOrganismName;
        URL url = getURL(location);

        try {            
            CSVReader csvReader = new CSVReader(new InputStreamReader(url.openStream()), '\t', '"');

            Set<String> martAttributes = new HashSet<String>();
            String[] line;
            while ((line = csvReader.readNext()) != null) {
                if (line.length < 1 || line[0].contains("Exception")) {
                    throw new IOException("Cannot get attributes for " + ensOrganismName );
                }
                martAttributes.add(line[0]);
            }

            for (String attribute : attributes) {
                if (!martAttributes.contains(attribute)) {
                    missingAttrs.add(attribute);
                }
            }

        } catch (IOException e) {
            throw new AtlasDataAccessException("Cannot read attribures from " + location, e);
        }

        return missingAttrs;
    }

    /**
     * @param ensOrganism
     * @param ensProperty
     * @return null - in a case of BioMart error and if ensProperty doesn't exist for a given ensOrganism
     */
    public URL getPropertyForOrganismURL(String ensOrganism, String ensProperty) throws AtlasDataAccessException {

        String location = URL_LOCATION + PROPERTY_QUERY.replace(DATA_SET_PH, ensOrganism).replace(PROP_NAME_PH, ensProperty);

        return getURL(location);
    }

    /**
     * @param ensOrganism
     * @param ensProperty
     * @return null - in a case of BioMart error and if ensProperty doesn't exist for a given ensOrganism
     */
    public CSVReader getPropertyForOrganism(String ensOrganism, String ensProperty) {
        String location = URL_LOCATION + PROPERTY_QUERY.replace(DATA_SET_PH, ensOrganism).replace(PROP_NAME_PH, ensProperty);
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

    private URL getURL(String location) throws AtlasDataAccessException {

         URL url = null;
         BufferedReader bufferedReader = null;
         try {
             url = new URL(location);

             InputStream inputStream = url.openStream();
             bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
             String line = bufferedReader.readLine();

             if (StringUtils.isEmpty(line) || line.contains("Exception") || line.contains("ERROR")) {
                 throw new AtlasDataAccessException("Data from " + location + "contain error " + line);
             }

         } catch (IOException e) {
             throw new AtlasDataAccessException("Cannot load data from " + location, e);
         } finally {
             log.info("Finished reading from " + location + ", closing");
             closeQuietly(bufferedReader);
         }
         return url;
     }
}
