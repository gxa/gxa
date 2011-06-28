package uk.ac.ebi.gxa.loader.bioentity;

import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import uk.ac.ebi.gxa.loader.AtlasLoaderException;
import uk.ac.ebi.gxa.loader.LoadBioentityCommand;
import uk.ac.ebi.gxa.loader.UpdateAnnotationCommand;
import uk.ac.ebi.gxa.loader.dao.AnnotationDAO;
import uk.ac.ebi.gxa.loader.service.AtlasLoaderServiceListener;

/**
 * User: nsklyar
 * Date: 27/06/2011
 */
public class AnnotationLoader {

    private final AnnotationDAO annotationDAO;
    private TransactionTemplate transactionTemplate;

    public AnnotationLoader(AnnotationDAO annotationDAO, PlatformTransactionManager txManager) {
        this.annotationDAO = annotationDAO;
        this.transactionTemplate = new TransactionTemplate(txManager);
    }

    public void process(LoadBioentityCommand cmd, AtlasLoaderServiceListener listener) throws AtlasLoaderException {
        FileAnnotator annotator = new FileAnnotator(annotationDAO, transactionTemplate);
        annotator.process(cmd.getUrl(), listener);
    }

     public void process(UpdateAnnotationCommand cmd, AtlasLoaderServiceListener listener) throws AtlasLoaderException {
        EnsemblAnnotator annotator = new EnsemblAnnotator(annotationDAO, transactionTemplate);
         annotator.process(cmd.getAccession(), listener);
    }
}
