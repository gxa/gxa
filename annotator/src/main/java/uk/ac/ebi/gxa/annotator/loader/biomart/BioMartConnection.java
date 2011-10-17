/*
 * Copyright 2008-2011 Microarray Informatics Team, EMBL-European Bioinformatics Institute
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

package uk.ac.ebi.gxa.annotator.loader.biomart;

import au.com.bytecode.opencsv.CSVReader;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.gxa.exceptions.LogUtil;

import java.io.*;
import java.net.URL;
import java.net.URLEncoder;
import java.util.*;

import static com.google.common.io.Closeables.closeQuietly;

/**
 * User: nsklyar
 * Date: 14/04/2011
 */
public class BioMartConnection {
    final private Logger log = LoggerFactory.getLogger(this.getClass());

    private static final String MART_NAME_PH = "$MART_NAME";

    private final static String REGISTRY_QUERY = "type=registry";
    private final static String ATTRIBUTES_QUERY = "type=attributes&dataset=";
    private final static String DATASETLIST_QUERY = "type=datasets&mart=";

    private static final String MART_DB = "database=\"" + MART_NAME_PH + "_mart_";

    private static final String DATA_SET_PH = "$DATA_SET";
    private static final String PROP_NAME_PH = "$PROP_NAME";
    private static final String VIRTUAL_SCHEMA_PH = "$VIRTUAL_SCHEMA";
    private static final String ATTRIBUTES_PH = "$ATTRIBUTES";

    private static final String PROPERTY_QUERY =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                    "<!DOCTYPE Query>" +
                    "<Query  virtualSchemaName = \"" + VIRTUAL_SCHEMA_PH + "\" formatter = \"TSV\" header = \"0\" uniqueRows = \"1\" count = \"\" >" +
                    "<Dataset name = \"" + DATA_SET_PH + "\" interface = \"default\" >" +
                    ATTRIBUTES_PH +
                    "</Dataset>" +
                    "</Query>";

    private static final String ATTRIBUTE = "<Attribute name = \"" + PROP_NAME_PH + "\" />";
    private String martUrl;
    private String datasetName;
    private String databaseName;

    private String bioMartName;
    private String serverVirtualSchema;

    public BioMartConnection(String martUrl, String databaseName, String datasetName) throws BioMartAccessException {
        this.martUrl = martUrl;
        this.datasetName = datasetName;
        this.databaseName = databaseName;
        fetchInfoFromRegistry();
    }


    public String getOnlineMartVersion() throws BioMartAccessException {
        URL url = null;
        BufferedReader bufferedReader = null;
        String version = null;
        try {
            url = getMartURL(martUrl + REGISTRY_QUERY);

            InputStream inputStream = url.openStream();
            bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            String line;

            while ((line = bufferedReader.readLine()) != null) {
                String martDb = MART_DB.replace(MART_NAME_PH, databaseName);
                if ((line.indexOf(martDb)) > -1) {
                    version = parseOutValue(martDb, line);
                    break;
                }
            }
        } catch (IOException e) {
            throw new BioMartAccessException("Problem when reading registry " + url, e);
        } finally {
            log.info("Finished reading from " + url + ", closing");
            closeQuietly(bufferedReader);
        }
        return version;
    }

    public Collection<String> validateAttributeNames(Set<String> properties) throws BioMartAccessException {
        List<String> missingAttrs = new ArrayList<String>();

        String location = martUrl + ATTRIBUTES_QUERY + datasetName;
        URL url = getMartURL(location);

        CSVReader csvReader = null;
        try {
            csvReader = new CSVReader(new InputStreamReader(url.openStream()), '\t', '"');

            Set<String> martAttributes = new HashSet<String>();
            String[] line;
            while ((line = csvReader.readNext()) != null) {
                if (line.length < 1 || line[0].contains("Exception")) {
                    throw new IOException("Cannot get attributes for " + datasetName);
                }
                martAttributes.add(line[0]);
            }

            for (String attribute : properties) {
                if (!martAttributes.contains(attribute)) {
                    missingAttrs.add(attribute);
                }
            }

        } catch (IOException e) {
            throw new BioMartAccessException("Cannot read attribures from " + location, e);
        } finally {
            log.info("Finished reading from " + url + ", closing");
            closeQuietly(csvReader);
        }

        return missingAttrs;
    }

    public boolean isValidDataSetName() throws BioMartAccessException {

        String location = martUrl + DATASETLIST_QUERY + bioMartName;
        URL url = getMartURL(location);

        CSVReader csvReader = null;
        try {
            csvReader = new CSVReader(new InputStreamReader(url.openStream()), '\t', '"');

            String[] line;
            while ((line = csvReader.readNext()) != null) {
                if (line.length < 1 || line[0].contains("Exception")) {
                    throw new IOException("Cannot get validate organism " + datasetName + " on " + location);
                }
                if (line.length > 1 && datasetName.equals(line[1])) {
                    return true;
                }
            }

        } catch (IOException e) {
            throw new BioMartAccessException("Cannot read data from " + location, e);
        } finally {
            closeQuietly(csvReader);
        }

        return false;
    }

    public String getDatasetName() {
        return datasetName;
    }

    /**
     * @param location
     * @return URL to biomart data
     * @throws BioMartAccessException if URL cannot be open or data contain an error
     */
    private URL getMartURL(String location) throws BioMartAccessException {

        URL url = null;
        BufferedReader bufferedReader = null;
        try {
            url = new URL(location);

            InputStream inputStream = url.openStream();
            bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            String line = bufferedReader.readLine();

            if (line == null || line.contains("Exception") || line.contains("ERROR")) {
                throw new BioMartAccessException("Data from " + location + " contain error " + line);
            }

        } catch (IOException e) {
            throw new BioMartAccessException("Cannot load data from " + location, e);
        } finally {
            log.info("Finished reading from " + location + ", closing");
            closeQuietly(bufferedReader);
        }
        return url;
    }

    public URL getAttributesURL(Collection<String> attributes) throws BioMartAccessException {
        return getMartURL(getAttributesURLLocation(attributes));
    }


    private String parseOutValue(String nameProp, String line) {
        return line.substring(line.indexOf(nameProp) + nameProp.length(), line.indexOf("\"", line.indexOf(nameProp) + nameProp.length()));
    }

    private String getAttributesURLLocation(Collection<String> attributes) {
        StringBuffer attributesSB = new StringBuffer();
        for (String attribute : attributes) {
            attributesSB.append(ATTRIBUTE.replace(PROP_NAME_PH, attribute));
        }
        try {
            return martUrl + "query=" + URLEncoder.encode(PROPERTY_QUERY.replace(DATA_SET_PH, datasetName).
                    replace(ATTRIBUTES_PH, attributesSB.toString()).replace(VIRTUAL_SCHEMA_PH, serverVirtualSchema), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw LogUtil.createUnexpected("Failed while trying to encode URL ", e);
        }
    }

    private void fetchInfoFromRegistry() throws BioMartAccessException {

        URL url = null;
        BufferedReader bufferedReader = null;
        String nameProp = "name=\"";
        String virtSchProp = "serverVirtualSchema=\"";
        try {
            url = getMartURL(martUrl + REGISTRY_QUERY);

            InputStream inputStream = url.openStream();
            bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            String line;

            while ((line = bufferedReader.readLine()) != null) {
                String martDb = MART_DB.replace(MART_NAME_PH, databaseName);
                if ((line.indexOf(martDb)) > -1) {
                    bioMartName = parseOutValue(nameProp, line);
                    serverVirtualSchema = parseOutValue(virtSchProp, line);
                }
            }
        } catch (IOException e) {
            throw new BioMartAccessException("Problem when reading registry " + url, e);
        } finally {
            log.info("Finished reading from " + url + ", closing");
            closeQuietly(bufferedReader);
        }

        if (StringUtils.isEmpty(bioMartName) || StringUtils.isEmpty(serverVirtualSchema)) {
            throw new BioMartAccessException("Problem when reading registry. Check annotation source configuration. " + url);
        }
    }
}
