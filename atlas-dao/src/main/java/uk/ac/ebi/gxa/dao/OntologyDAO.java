package uk.ac.ebi.gxa.dao;

import org.hibernate.SessionFactory;
import uk.ac.ebi.microarray.atlas.model.Ontology;

public class OntologyDAO extends AbstractDAO<Ontology> {
    public OntologyDAO(SessionFactory sessionFactory) {
        super(sessionFactory, Ontology.class);
    }
}
