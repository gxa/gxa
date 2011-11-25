package uk.ac.ebi.gxa.annotator.loader.annotationsrc;

import org.apache.commons.configuration.PropertiesConfiguration;
import uk.ac.ebi.gxa.annotator.model.genesigdb.GeneSigAnnotationSource;
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

    protected GeneSigAnnotationSource initAnnotationSource(String id, Properties properties) throws AnnotationLoaderException {
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
        return annSrc;
    }

    @Override
    protected void updateExtraProperties(Properties properties, GeneSigAnnotationSource annotationSource) throws AnnotationLoaderException {
    }

    @Override
    protected void writeExtraProperties(GeneSigAnnotationSource annSrc, PropertiesConfiguration properties) {
    }


}
