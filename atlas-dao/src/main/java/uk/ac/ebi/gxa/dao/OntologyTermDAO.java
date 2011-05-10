package uk.ac.ebi.gxa.dao;

import org.hibernate.SessionFactory;
import uk.ac.ebi.microarray.atlas.model.OntologyTerm;

public class OntologyTermDAO extends AbstractDAO<OntologyTerm> {
    public OntologyTermDAO(SessionFactory sessionFactory) {
        super(sessionFactory, OntologyTerm.class);
    }
}
