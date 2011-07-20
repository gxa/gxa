package uk.ac.ebi.gxa.loader.bioentity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.support.TransactionTemplate;
import uk.ac.ebi.gxa.dao.AnnotationSourceDAO;
import uk.ac.ebi.gxa.dao.BioEntityDAO;
import uk.ac.ebi.gxa.loader.AtlasLoaderException;
import uk.ac.ebi.gxa.loader.dao.AnnotationDAO;
import uk.ac.ebi.gxa.loader.service.AtlasLoaderServiceListener;
import uk.ac.ebi.microarray.atlas.model.annotation.AnnotationSource;
import uk.ac.ebi.microarray.atlas.model.annotation.BioMartAnnotationSource;

/**
 * User: nsklyar
 * Date: 20/07/2011
 */
public class EnsemblMapper extends ArrayDesignBioentityMapper {

    private AnnotationSourceDAO annSrcDAO;

    private TransactionTemplate transactionTemplate;

    private AtlasLoaderServiceListener listener;

    final private Logger log = LoggerFactory.getLogger(this.getClass());


    public EnsemblMapper(AnnotationSourceDAO annSrcDAO, TransactionTemplate transactionTemplate) {
        this.annSrcDAO = annSrcDAO;
        this.transactionTemplate = transactionTemplate;
    }

    public void process(String annotationSrcId, AtlasLoaderServiceListener listener) throws AtlasLoaderException {
        setListener(listener);

        AnnotationSource annotationSource = annSrcDAO.getById(Long.parseLong(annotationSrcId));
        if (annotationSource == null) {
            throw new AtlasLoaderException("No annotation source with id " + annotationSrcId);
        }

        updateMappings((BioMartAnnotationSource) annotationSource);
    }

    private void updateMappings(BioMartAnnotationSource annotationSource) {
        //To change body of created methods use File | Settings | File Templates.
    }

    public void setListener(AtlasLoaderServiceListener listener) {
        this.listener = listener;
    }
}
