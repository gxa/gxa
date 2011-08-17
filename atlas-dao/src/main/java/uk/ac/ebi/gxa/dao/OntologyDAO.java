package uk.ac.ebi.gxa.dao;

import org.hibernate.SessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.microarray.atlas.model.Ontology;

/**
 * @author Misha Kapushesky
 */
public class OntologyDAO extends AbstractDAO<Ontology> {
    public static final Logger log = LoggerFactory.getLogger(ExperimentDAO.class);

    OntologyDAO(SessionFactory sessionFactory) {
        super(sessionFactory, Ontology.class);
    }
}
