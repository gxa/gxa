package uk.ac.ebi.gxa.annotator.loader.annotationsrc;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import uk.ac.ebi.gxa.annotator.model.genesigdb.GeneSigAnnotationSource;
import uk.ac.ebi.microarray.atlas.model.bioentity.BioEntityProperty;
import uk.ac.ebi.microarray.atlas.model.bioentity.BioEntityType;
import uk.ac.ebi.microarray.atlas.model.bioentity.Software;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
import java.util.Properties;

import static com.google.common.io.Closeables.closeQuietly;

/**
 * User: nsklyar
 * Date: 10/11/2011
 */
public class FileBasedAnnotationSourceConverter extends AnnotationSourceConverter<GeneSigAnnotationSource> {

    private static final String BIOENTITYPROPERTY_PROPNAME = "property";

    @Override
    public GeneSigAnnotationSource editOrCreateAnnotationSource(String id, String text) throws AnnotationLoaderException {
        Reader input = new StringReader(text);
        Properties properties = new Properties();
        try {
            properties.load(input);
            //Fetch organism and software
            Software software = softwareDAO.findOrCreate(getProperty(SOFTWARE_NAME_PROPNAME, properties), getProperty(SOFTWARE_VERSION_PROPNAME, properties));

            GeneSigAnnotationSource annSrc = fetchAnnSrc(id);
            //Check if for given organism and software annotation source exists
            if (annSrc == null || !annSrc.getSoftware().equals(software)) {
                final GeneSigAnnotationSource annotationSource = annSrcDAO.findAnnotationSource(software, getClazz());
                if (annotationSource != null) {
                    throw new AnnotationLoaderException("Annotation source for software " +
                            software.getName() + " " + software.getVersion() + " already exists.");
                } else {
                    if (annSrc == null) {
                        annSrc = new GeneSigAnnotationSource(software);
                    }
                }
            }
            updateAnnotationSource(properties, annSrc);
            return annSrc;
        } catch (IOException e) {
            throw new AnnotationLoaderException("Cannot read annotation properties", e);
        } finally {
            closeQuietly(input);
        }
    }

    @Override
    protected Class<GeneSigAnnotationSource> getClazz() {
        return GeneSigAnnotationSource.class;
    }

    @Override
    protected void generateString(GeneSigAnnotationSource annSrc, Writer out) throws ConfigurationException {
        PropertiesConfiguration properties = new PropertiesConfiguration();
        properties.addProperty(SOFTWARE_NAME_PROPNAME, annSrc.getSoftware().getName());
        properties.addProperty(SOFTWARE_VERSION_PROPNAME, annSrc.getSoftware().getVersion());
        properties.addProperty(URL_PROPNAME, annSrc.getUrl());

        //ToDo: update property

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

        properties.save(out);
    }

    private void updateAnnotationSource(Properties properties, GeneSigAnnotationSource annotationSource) throws AnnotationLoaderException {
        updateTypes(properties, annotationSource);

        annotationSource.setUrl(getProperty(URL_PROPNAME, properties));
        for (String propName : properties.stringPropertyNames()) {

            if (propName.startsWith(BIOENTITYPROPERTY_PROPNAME)) {
                BioEntityProperty beProperty = propertyDAO.findOrCreate(propName.substring(BIOENTITYPROPERTY_PROPNAME.length() + 1));
                annotationSource.setBioEntityProperty(beProperty);
            }
        }

    }

}
