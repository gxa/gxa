package uk.ac.ebi.gxa.annotator.annotationsrc;

import org.apache.commons.configuration.PropertiesConfiguration;
import uk.ac.ebi.gxa.annotator.model.GeneSigAnnotationSource;
import uk.ac.ebi.microarray.atlas.model.bioentity.Software;

import java.util.Properties;

/**
 * User: nsklyar
 * Date: 10/11/2011
 */
public class FileBasedAnnotationSourceConverter extends AnnotationSourceConverter<GeneSigAnnotationSource> {

    @Override
    protected Class<GeneSigAnnotationSource> getClazz() {
        return GeneSigAnnotationSource.class;
    }
    
    @Override
    protected GeneSigAnnotationSource initAnnotationSource(String id, Properties properties) throws AnnotationLoaderException {
        Software software = softwareDAO.findOrCreate(getProperty(SOFTWARE_NAME_PROPNAME, properties), getProperty(SOFTWARE_VERSION_PROPNAME, properties));

        GeneSigAnnotationSource annSrc = fetchAnnSrcById(id);
        if (annSrc == null) {
            return new GeneSigAnnotationSource(software);
        }

        if (!annSrc.getSoftware().equals(software)) {
            throw new AnnotationLoaderException("Software should not be changed when editing Annotation Source!");
        }
        return annSrc;
    }

    @Override
    protected void updateExtraProperties(Properties properties, GeneSigAnnotationSource annotationSource) throws AnnotationLoaderException {
    }

    @Override
    protected void writeExtraProperties(GeneSigAnnotationSource annSrc, PropertiesConfiguration properties) {
    }


}
