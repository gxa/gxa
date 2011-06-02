package uk.ac.ebi.gxa.loader.bioentity;

import au.com.bytecode.opencsv.CSVReader;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.gxa.dao.AnnotationSourceDAO;
import uk.ac.ebi.gxa.dao.SoftwareDAO;
import uk.ac.ebi.microarray.atlas.model.bioentity.AnnotationSource;
import uk.ac.ebi.microarray.atlas.model.bioentity.BioMartAnnotationSource;
import uk.ac.ebi.microarray.atlas.model.bioentity.BioMartProperty;
import uk.ac.ebi.microarray.atlas.model.bioentity.CurrentAnnotationSource;
import uk.ac.ebi.microarray.atlas.model.bioentity.Software;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
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
    private final static String DATASETLIST_QUERY = "type=datasets&mart=";

    private static final String MART_DB = "database=\"" + MART_NAME_PH + "_mart_";

    private AnnotationSourceDAO annSrcDAO;
    private SoftwareDAO softwareDAO;

    public void setAnnSrcDAO(AnnotationSourceDAO annSrcDAO) {
        this.annSrcDAO = annSrcDAO;
    }

    public void setSoftwareDAO(SoftwareDAO softwareDAO) {
        this.softwareDAO = softwareDAO;
    }

    public List<BioMartAnnotationSourceView> getBioMartAnnSrcs() {
        List<BioMartAnnotationSourceView> viewSources = new ArrayList<BioMartAnnotationSourceView>();
        Collection<BioMartAnnotationSource> currentAnnSrcs = annSrcDAO.getCurrentAnnotationSourcesOfType(BioMartAnnotationSource.class);
        for (BioMartAnnotationSource annSrc : currentAnnSrcs) {
            try {
                String newVersion = getDataSetVersion(annSrc.getUrl(), annSrc.getDatasetName());
                if (annSrc.getSoftware().getVersion().equals(newVersion)) {
                    viewSources.add(new BioMartAnnotationSourceView(annSrc, annSrc.getDisplayName()));
                } else {
                    //check if AnnotationSource exists for new version
                    Software newSoftware = softwareDAO.find(annSrc.getSoftware().getName(), newVersion);
                    if (newSoftware ==null) {
                        newSoftware = new Software(null, annSrc.getSoftware().getName(), newVersion);
                    }

                    BioMartAnnotationSource newAnnSrc = annSrcDAO.findAnnotationSource(newSoftware, annSrc.getOrganism(), BioMartAnnotationSource.class);
                    //create and Save new AnnotationSource
                    if (newAnnSrc == null) {
                        newAnnSrc = annSrc.createCopy(newSoftware);
                        annSrcDAO.save(newAnnSrc);
                    }
                    ValidationReport validationReport = validateAnnotationSource(newAnnSrc);
                    BioMartAnnotationSourceView annSrcView = new BioMartAnnotationSourceView(newAnnSrc, annSrc.getDisplayName());
                    annSrcView.setValidationReport(validationReport);
                    viewSources.add(annSrcView);
                }

            } catch (BioMartAccessException e) {
                log.error("Problem when fetching version for " + annSrc.getSoftware().getName(), e);
            }
        }

        return viewSources;
    }

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
                String martDb = MART_DB.replace(MART_NAME_PH, martName);
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

    public List<BioMartProperty> validateAttributeNames(String martUrl, String ensOrganismName, List<BioMartProperty> attributes) throws BioMartAccessException {
        List<BioMartProperty> missingAttrs = new ArrayList<BioMartProperty>();

        String location = martUrl + ATTRIBUTES_QUERY + ensOrganismName;
        URL url = getPropertyURL(location);

        try {
            CSVReader csvReader = new CSVReader(new InputStreamReader(url.openStream()), '\t', '"');

            Set<String> martAttributes = new HashSet<String>();
            String[] line;
            while ((line = csvReader.readNext()) != null) {
                if (line.length < 1 || line[0].contains("Exception")) {
                    throw new IOException("Cannot get attributes for " + ensOrganismName);
                }
                martAttributes.add(line[0]);
            }

            for (BioMartProperty attribute : attributes) {
                if (!martAttributes.contains(attribute.getName())) {
                    missingAttrs.add(attribute);
                }
            }

        } catch (IOException e) {
            throw new BioMartAccessException("Cannot read attribures from " + location, e);
        }

        return missingAttrs;
    }

    public boolean isValidOrganismName(String bmUrl, String bmName, String organismName) throws BioMartAccessException {

        String location = bmUrl + DATASETLIST_QUERY + bmName;
        URL url = getPropertyURL(location);

        try {
            CSVReader csvReader = new CSVReader(new InputStreamReader(url.openStream()), '\t', '"');

            String[] line;
            while ((line = csvReader.readNext()) != null) {
                if (line.length < 1 || line[0].contains("Exception")) {
                    throw new IOException("Cannot get validate organism " + organismName + " on " + location);
                }
                if (line.length >1 && organismName.equals(line[1])) {
                    return true;
                }
            }

        } catch (IOException e) {
            throw new BioMartAccessException("Cannot read attribures from " + location, e);
        }

        return false;
    }

    public ValidationReport validateAnnotationSource(BioMartAnnotationSource annSrc) throws BioMartAccessException {
        ValidationReport validationReport = new ValidationReport();
        fillAnnSrcFromRegistry(annSrc);
        //validate org_name
        if (!isValidOrganismName(annSrc.getUrl(), annSrc.getBioMartName(), annSrc.getDatasetName())) {
            validationReport.setOrganismName(annSrc.getDatasetName());
        }
        //validate properties
        validationReport.setMissingProperties(validateAttributeNames(annSrc.getUrl(), annSrc.getDatasetName(), annSrc.getBioMartProperties()));
        return validationReport;
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

            if (line.contains("Exception") || line.contains("ERROR")) {
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

    private void fillAnnSrcFromRegistry(BioMartAnnotationSource annSrc) throws BioMartAccessException {
        URL url = null;
        BufferedReader bufferedReader = null;
        String nameProp = "name=\"";
        String virtSchProp = "serverVirtualSchema=\"";
        try {
            url = getPropertyURL(annSrc.getUrl() + REGISTRY_QUERY);

            InputStream inputStream = url.openStream();
            bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            String line;

            while ((line = bufferedReader.readLine()) != null) {
                String martDb = MART_DB.replace(MART_NAME_PH, annSrc.getSoftware().getName());
                if ((line.indexOf(martDb)) > -1) {
                    annSrc.setBioMartName(parseOutValue(nameProp, line));
                    annSrc.setServerVirtualSchema(parseOutValue(virtSchProp, line));
                }
            }
        } catch (IOException e) {
            throw new BioMartAccessException("Problem when reading registry " + url, e);
        } finally {
            log.info("Finished reading from " + url + ", closing");
            closeQuietly(bufferedReader);
        }
    }

    private String parseOutValue(String nameProp, String line) {
        return line.substring(line.indexOf(nameProp) + nameProp.length(), line.indexOf("\"", line.indexOf(nameProp) + nameProp.length()));
    }

    public static class BioMartAnnotationSourceView {
        private BioMartAnnotationSource annSrc;
        private String currentName;
        private ValidationReport validationReport;

        public BioMartAnnotationSourceView(BioMartAnnotationSource annSrc, String currentName) {
            this.annSrc = annSrc;
            this.currentName = currentName;
        }

        public BioMartAnnotationSource getAnnSrc() {
            return annSrc;
        }

        public String getCurrentName() {
            return currentName;
        }

        public ValidationReport getValidationReport() {
            return validationReport;
        }

        public void setValidationReport(ValidationReport validationReport) {
            this.validationReport = validationReport;
        }
    }

    public static class ValidationReport {
        private String organismName;
        private List<BioMartProperty> missingProperties;

        public ValidationReport() {
        }

        public String getOrganismName() {
            return organismName;
        }

        public void setOrganismName(String organismName) {
            this.organismName = organismName;
        }

        public String getSummary() {
            StringBuilder sb = new StringBuilder();
            sb.append(organismName + " ");
            sb.append(missingProperties);
            return sb.toString();
        }

        public List<BioMartProperty> getMissingProperties() {
            return missingProperties;
        }

        public void setMissingProperties(List<BioMartProperty> missingProperties) {
            this.missingProperties = missingProperties;
        }

        public boolean isValid() {
            return StringUtils.isEmpty(organismName) && CollectionUtils.isEmpty(missingProperties);
        }
    }
}
