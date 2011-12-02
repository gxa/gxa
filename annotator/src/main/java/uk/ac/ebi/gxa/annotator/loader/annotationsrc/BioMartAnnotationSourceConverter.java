package uk.ac.ebi.gxa.annotator.loader.annotationsrc;

import org.apache.commons.configuration.PropertiesConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.gxa.annotator.model.biomart.BioMartAnnotationSource;
import uk.ac.ebi.microarray.atlas.model.Organism;
import uk.ac.ebi.microarray.atlas.model.bioentity.Software;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.Properties;

import static com.google.common.io.Closeables.closeQuietly;

/**
 * User: nsklyar
 * Date: 07/11/2011
 */
public class BioMartAnnotationSourceConverter extends AnnotationSourceConverter<BioMartAnnotationSource> {

    final private Logger log = LoggerFactory.getLogger(this.getClass());

    private static final String ORGANISM_PROPNAME = "organism";
    private static final String MYSQLDBNAME_PROPNAME = "mySqlDbName";
    private static final String MYSQLDBURL_PROPNAME = "mySqlDbUrl";
    private static final String DATASET_NAME_PROPNAME = "datasetName";
    private static final String DATABASE_NAME_PROPNAME = "databaseName";

    @Override
    protected Class<BioMartAnnotationSource> getClazz() {
        return BioMartAnnotationSource.class;
    }

    public BioMartAnnotationSource editOrCreateAnnotationSource(String id, String text) throws AnnotationLoaderException {

        Reader input = new StringReader(text);
        Properties properties = new Properties();
        try {
            properties.load(input);
            //Fetch organism and software
            BioMartAnnotationSource annSrc = initAnnotationSource(id, properties);
            updateAnnotationSource(properties, annSrc);
            return annSrc;
        } catch (IOException e) {
            throw new AnnotationLoaderException("Cannot read annotation properties", e);
        } finally {
            closeQuietly(input);
        }
    }

    @Override
    protected BioMartAnnotationSource initAnnotationSource(String id, Properties properties) throws AnnotationLoaderException {
        Organism organism = organismDAO.getOrCreateOrganism(getProperty(ORGANISM_PROPNAME, properties));
        Software software = softwareDAO.findOrCreate(getProperty(SOFTWARE_NAME_PROPNAME, properties), getProperty(SOFTWARE_VERSION_PROPNAME, properties));

        BioMartAnnotationSource annSrc = fetchAnnSrcById(id);
        //Check if for given organism and software annotation source exists
        if (annSrc == null || (!annSrc.getSoftware().equals(software) || !annSrc.getOrganism().equals(organism))) {
            final BioMartAnnotationSource annotationSource = annSrcDAO.findAnnotationSource(software, organism, BioMartAnnotationSource.class);
            if (annotationSource != null) {
                throw new AnnotationLoaderException("Annotation source for organism " + organism.getName() + " and for software " +
                        software.getName() + " " + software.getVersion() + " already exists.");
            } else {
                if (annSrc == null) {
                    annSrc = new BioMartAnnotationSource(software, organism);
                } else {
                    annSrc.setOrganism(organism);
                    annSrc.setSoftware(software);
                }
            }
        }
        return annSrc;
    }

    @Override
    protected void updateExtraProperties(Properties properties, BioMartAnnotationSource annotationSource) throws AnnotationLoaderException {
        annotationSource.setUrl(getProperty(URL_PROPNAME, properties));
        annotationSource.setDatabaseName(getProperty(DATABASE_NAME_PROPNAME, properties));
        annotationSource.setDatasetName(getProperty(DATASET_NAME_PROPNAME, properties));
        annotationSource.setMySqlDbName(getProperty(MYSQLDBNAME_PROPNAME, properties));
        annotationSource.setMySqlDbUrl(getProperty(MYSQLDBURL_PROPNAME, properties));
    }

    @Override
    protected void writeExtraProperties(BioMartAnnotationSource annSrc, PropertiesConfiguration properties) {
        properties.addProperty(ORGANISM_PROPNAME, annSrc.getOrganism().getName());
        properties.addProperty(DATABASE_NAME_PROPNAME, annSrc.getDatabaseName());
        properties.addProperty(DATASET_NAME_PROPNAME, annSrc.getDatasetName());
        properties.addProperty(MYSQLDBNAME_PROPNAME, annSrc.getMySqlDbName());
        properties.addProperty(MYSQLDBURL_PROPNAME, annSrc.getMySqlDbUrl());
    }

}
