package uk.ac.ebi.gxa.annotator.loader.annotationsrc;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.gxa.annotator.model.AnnotatedArrayDesign;
import uk.ac.ebi.gxa.annotator.model.AnnotatedBioEntityProperty;
import uk.ac.ebi.gxa.annotator.model.biomart.BioMartAnnotationSource;
import uk.ac.ebi.microarray.atlas.model.ArrayDesign;
import uk.ac.ebi.microarray.atlas.model.Organism;
import uk.ac.ebi.microarray.atlas.model.bioentity.BioEntityProperty;
import uk.ac.ebi.microarray.atlas.model.bioentity.BioEntityType;
import uk.ac.ebi.microarray.atlas.model.bioentity.Software;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;

import static com.google.common.collect.Sets.difference;
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
    private static final String BIOMARTPROPERTY_PROPNAME = "biomartProperty";
    private static final String ARRAYDESIGN_PROPNAME = "arrayDesign";


    public BioMartAnnotationSource editOrCreateAnnotationSource(String id, String text) throws AnnotationLoaderException {

        Reader input = new StringReader(text);
        Properties properties = new Properties();
        try {
            properties.load(input);
            //Fetch organism and software
            Organism organism = organismDAO.getOrCreateOrganism(getProperty(ORGANISM_PROPNAME, properties));
            Software software = softwareDAO.findOrCreate(getProperty(SOFTWARE_NAME_PROPNAME, properties), getProperty(SOFTWARE_VERSION_PROPNAME, properties));

            BioMartAnnotationSource annSrc = fetchAnnSrc(id);
            //Check if for given organism and software annotation source exists
            if (annSrc == null || (!annSrc.getSoftware().equals(software) || !annSrc.getOrganism().equals(organism))) {
                final BioMartAnnotationSource annotationSource = annSrcDAO.findAnnotationSource(software, organism, BioMartAnnotationSource.class);
                if (annotationSource != null) {
                    throw new AnnotationLoaderException("Annotation source for organism " + organism.getName() + " and for software " +
                            software.getName() + " " + software.getVersion() + " already exists.");
                } else {
                    if (annSrc == null) {
                        annSrc = new BioMartAnnotationSource(software, organism);
                    }
                }
            }
            updateBioMartAnnotationSource(properties, annSrc);
            return annSrc;
        } catch (IOException e) {
            throw new AnnotationLoaderException("Cannot read annotation properties", e);
        } finally {
            closeQuietly(input);
        }
    }

    @Override
    protected Class<BioMartAnnotationSource> getClazz() {
        return BioMartAnnotationSource.class;
    }

    private void updateBioMartAnnotationSource(Properties properties, BioMartAnnotationSource annotationSource) throws AnnotationLoaderException {
        updateTypes(properties, annotationSource);

        annotationSource.setUrl(getProperty(URL_PROPNAME, properties));
        annotationSource.setDatabaseName(getProperty(DATABASE_NAME_PROPNAME, properties));
        annotationSource.setDatasetName(getProperty(DATASET_NAME_PROPNAME, properties));
        annotationSource.setMySqlDbName(getProperty(MYSQLDBNAME_PROPNAME, properties));
        annotationSource.setMySqlDbUrl(getProperty(MYSQLDBURL_PROPNAME, properties));

        updateBioMartProperties(properties, annotationSource);

        updateBioMartArrayDesigns(properties, annotationSource);
    }

    @Override
    protected void generateString(BioMartAnnotationSource annSrc, Writer out) throws ConfigurationException {
        PropertiesConfiguration properties = new PropertiesConfiguration();
        properties.addProperty(ORGANISM_PROPNAME, annSrc.getOrganism().getName());
        properties.addProperty(SOFTWARE_NAME_PROPNAME, annSrc.getSoftware().getName());
        properties.addProperty(SOFTWARE_VERSION_PROPNAME, annSrc.getSoftware().getVersion());
        properties.addProperty(URL_PROPNAME, annSrc.getUrl());
        properties.addProperty(DATABASE_NAME_PROPNAME, annSrc.getDatabaseName());
        properties.addProperty(DATASET_NAME_PROPNAME, annSrc.getDatasetName());
        properties.addProperty(MYSQLDBNAME_PROPNAME, annSrc.getMySqlDbName());
        properties.addProperty(MYSQLDBURL_PROPNAME, annSrc.getMySqlDbUrl());

        //Write bioentity types
        StringBuffer types = new StringBuffer();
        int count = 1;
        for (BioEntityType type : annSrc.getTypes()) {
            types.append(type.getName());
            if (count++ < annSrc.getTypes().size()) {
                types.append(",");
            }
        }
        properties.addProperty(TYPES_PROPNAME, types.toString());

        writeBioMartProperties(annSrc, properties);

        writeBioMartArrayDesign(annSrc, properties);
        properties.save(out);
    }

    private void writeBioMartProperties(BioMartAnnotationSource annSrc, PropertiesConfiguration properties) {
        Multimap<String, String> bePropToBmProp = HashMultimap.create();
        for (AnnotatedBioEntityProperty annotatedBioEntityProperty : annSrc.getAnnotatedBioEntityProperties()) {
            bePropToBmProp.put(annotatedBioEntityProperty.getBioEntityProperty().getName(), annotatedBioEntityProperty.getName());
        }

        addCommaSeparatedProperties(BIOMARTPROPERTY_PROPNAME, properties, bePropToBmProp);

    }

    private void writeBioMartArrayDesign(BioMartAnnotationSource annSrc, PropertiesConfiguration properties) {
        Multimap<String, String> bePropToBmProp = HashMultimap.create();

        for (AnnotatedArrayDesign annotatedArrayDesign : annSrc.getAnnotatedArrayDesigns()) {
            bePropToBmProp.put(annotatedArrayDesign.getArrayDesign().getAccession(), annotatedArrayDesign.getName());
        }

        addCommaSeparatedProperties(ARRAYDESIGN_PROPNAME, properties, bePropToBmProp);
    }

    private void updateBioMartArrayDesigns(Properties properties, BioMartAnnotationSource annotationSource) {
        Set<AnnotatedArrayDesign> annotatedArrayDesigns = new HashSet<AnnotatedArrayDesign>();
        for (String propName : properties.stringPropertyNames()) {

            if (propName.startsWith(ARRAYDESIGN_PROPNAME)) {
                ArrayDesign arrayDesign = arrayDesignService.findOrCreateArrayDesignShallow(propName.substring(ARRAYDESIGN_PROPNAME.length() + 1));
                annotatedArrayDesigns.add(new AnnotatedArrayDesign(properties.getProperty(propName).trim(), arrayDesign, annotationSource));
            }
        }

        Set<AnnotatedArrayDesign> removedProperties = new HashSet<AnnotatedArrayDesign>(difference(annotationSource.getAnnotatedArrayDesigns(), annotatedArrayDesigns));
        Set<AnnotatedArrayDesign> addedProperties = new HashSet<AnnotatedArrayDesign>(difference(annotatedArrayDesigns, annotationSource.getAnnotatedArrayDesigns()));

        for (AnnotatedArrayDesign removedProperty : removedProperties) {
            annotationSource.removeBioMartArrayDesign(removedProperty);
        }

        for (AnnotatedArrayDesign addedProperty : addedProperties) {
            annotationSource.addBioMartArrayDesign(addedProperty);
        }
    }

    private void updateBioMartProperties(Properties properties, BioMartAnnotationSource annotationSource) {
        Set<AnnotatedBioEntityProperty> annotatedBioEntityProperties = new HashSet<AnnotatedBioEntityProperty>();
        for (String propName : properties.stringPropertyNames()) {

            if (propName.startsWith(BIOMARTPROPERTY_PROPNAME)) {
                BioEntityProperty beProperty = propertyDAO.findOrCreate(propName.substring(BIOMARTPROPERTY_PROPNAME.length() + 1));
                StringTokenizer tokenizer = new StringTokenizer(properties.getProperty(propName), ",");
                while (tokenizer.hasMoreElements()) {
                    annotatedBioEntityProperties.add(new AnnotatedBioEntityProperty(tokenizer.nextToken().trim(), beProperty, annotationSource));
                }
            }
        }

        Set<AnnotatedBioEntityProperty> removedPropertyAnnotateds = new HashSet<AnnotatedBioEntityProperty>(difference(annotationSource.getAnnotatedBioEntityProperties(), annotatedBioEntityProperties));
        Set<AnnotatedBioEntityProperty> addedPropertyAnnotateds = new HashSet<AnnotatedBioEntityProperty>(difference(annotatedBioEntityProperties, annotationSource.getAnnotatedBioEntityProperties()));

        for (AnnotatedBioEntityProperty removedPropertyAnnotated : removedPropertyAnnotateds) {
            annotationSource.removeBioMartProperty(removedPropertyAnnotated);
        }

        for (AnnotatedBioEntityProperty addedPropertyAnnotated : addedPropertyAnnotateds) {
            annotationSource.addBioMartProperty(addedPropertyAnnotated);
        }
    }

}
