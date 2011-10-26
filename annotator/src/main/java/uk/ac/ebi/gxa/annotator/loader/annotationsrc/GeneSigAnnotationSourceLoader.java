package uk.ac.ebi.gxa.annotator.loader.annotationsrc;

import uk.ac.ebi.gxa.annotator.model.genesigdb.GeneSigAnnotationSource;

import java.util.Collection;

/**
 * User: nsklyar
 * Date: 25/10/2011
 */
public class GeneSigAnnotationSourceLoader extends AnnotationSourceLoader<GeneSigAnnotationSource>{

    @Override
    public String getAnnSrcAsStringById(String id) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void saveAnnSrc(String text) throws AnnotationLoaderException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Collection<GeneSigAnnotationSource> getCurrentAnnotationSources() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
