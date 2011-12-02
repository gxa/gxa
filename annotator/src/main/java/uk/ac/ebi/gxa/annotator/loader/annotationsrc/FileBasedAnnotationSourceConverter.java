package uk.ac.ebi.gxa.annotator.loader.annotationsrc;

import org.apache.commons.configuration.PropertiesConfiguration;
import uk.ac.ebi.gxa.annotator.model.AnnotationSource;
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

        GeneSigAnnotationSource annSrc = fetchAnnSrcById(id);
        //Check if for given software annotation source exists
        if (annSrc == null || !annSrc.getSoftware().equals(software)) {
            final GeneSigAnnotationSource annotationSource = annSrcDAO.findAnnotationSource(software, getClazz());
            if (annotationSource != null) {
                throw new AnnotationLoaderException("Annotation source for software " +
                        software.getName() + " " + software.getVersion() + " already exists.");
            } else {
                if (annSrc == null) {
                    annSrc = new GeneSigAnnotationSource(software);
                } else {
                    annSrc.setSoftware(software);
                }
            }
        }
        return annSrc;
    }

    //ToDo: decide if this method is better written then initAnnotationSource (this method is more readable, but more verbose)
    protected AnnotationSource initAnnotationSource1(String id, Properties properties) throws AnnotationLoaderException {
        Software software = softwareDAO.findOrCreate(getProperty(SOFTWARE_NAME_PROPNAME, properties), getProperty(SOFTWARE_VERSION_PROPNAME, properties));

        AnnotationSource annSrc = null;
        //create new annotation source
        if (id == null) {
            //check if annotation source for a given software exists
            if (!annSrcDAO.isAnnSrcExistForSoftware(software, getClazz())) {
                annSrc = new GeneSigAnnotationSource(software);
            } else {
                throw new AnnotationLoaderException("Cannot create new Annotation source. Annotation source for software " +
                        software.getName() + " " + software.getVersion() + " already exists.");
            }

         // edit existing annotation source
        } else {
            annSrc = fetchAnnSrcById(id);
            // check if software was updated and if AnnotationSource with a new software already exists
            if (!annSrc.getSoftware().equals(software)) {
                if (annSrcDAO.isAnnSrcExistForSoftware(software, getClazz())) {
                    throw new AnnotationLoaderException("Cannot modify Annotation source. Annotation source for software " +
                            software.getName() + " " + software.getVersion() + " already exists.");
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
