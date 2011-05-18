package uk.ac.ebi.gxa.loader.bioentity;

import au.com.bytecode.opencsv.CSVReader;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.microarray.atlas.model.bioentity.BioMartAnnotationSource;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.google.common.io.Closeables.closeQuietly;

/**
 * User: nsklyar
 * Date: 14/04/2011
 */
public class BioMartService {
    // logging
    final private Logger log = LoggerFactory.getLogger(this.getClass());

    public static final String MART_NAME_PH = "$MART_NAME";

    private final static String REGISTRY_QUERY = "type=registry";
    private final static String ATTRIBUTES_QUERY = "type=attributes&dataset=";

    private static final String MART_DB = "database=\""+ MART_NAME_PH +"_mart_";


    public String getDataSetVersion(String martUrl, String martName) throws BioMartAccessException {
        URL url = null;
        BufferedReader bufferedReader = null;
        String version = null;
        try {
            url = getPropertyURL(martUrl + REGISTRY_QUERY);

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
            throw new BioMartAccessException("Problem when reading registry " + url, e) ;
        } finally {
            log.info("Finished reading from " + url + ", closing");
            closeQuietly(bufferedReader);
        }
        return version;
    }

    public List<String> validateAttributeNames(String martUrl, String ensOrganismName, Collection<String> attributes) throws BioMartAccessException {
        List<String> missingAttrs = new ArrayList<String>();

        String location = martUrl + ATTRIBUTES_QUERY + ensOrganismName;
        URL url = getPropertyURL(location);

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
            throw new BioMartAccessException("Cannot read attribures from " + location, e);
        }

        return missingAttrs;
    }

   /**
     * @param location
     * @return null - in a case of BioMart error and if ensProperty doesn't exist for a given ensOrganism
    * @throws BioMartAccessException
     */
    public URL getPropertyURL(String location) throws BioMartAccessException {

         URL url = null;
         BufferedReader bufferedReader = null;
         try {
             url = new URL(location);

             InputStream inputStream = url.openStream();
             bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
             String line = bufferedReader.readLine();

             if (StringUtils.isEmpty(line) || line.contains("Exception") || line.contains("ERROR")) {
                 throw new BioMartAccessException("Data from " + location + "contain error " + line);
             }

         } catch (IOException e) {
             throw new BioMartAccessException("Cannot load data from " + location, e);
         } finally {
             log.info("Finished reading from " + location + ", closing");
             closeQuietly(bufferedReader);
         }
         return url;
     }
}
