package uk.ac.ebi.gxa.annotator.loader;

import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import uk.ac.ebi.gxa.annotator.dao.AnnotationDAO;
import uk.ac.ebi.gxa.annotator.loader.biomart.BioMartAnnotator;

/**
 * User: nsklyar
 * Date: 28/07/2011
 */
public class AnnotatorFactory {

    private AnnotationDAO annotationDAO;
    private TransactionTemplate transactionTemplate;

    public AnnotatorFactory(AnnotationDAO annotationDAO, PlatformTransactionManager txManager) {
        this.annotationDAO = annotationDAO;
        this.transactionTemplate = new TransactionTemplate(txManager);
    }

    public BioMartAnnotator getEnsemblAnnotator() {
        return new BioMartAnnotator(annotationDAO, transactionTemplate);
    }

}
