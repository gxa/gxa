package uk.ac.ebi.gxa.dao;

import org.hibernate.SessionFactory;
import uk.ac.ebi.microarray.atlas.model.AssayProperty;

class AssayPropertyDAO extends AbstractDAO<AssayProperty> {
    public AssayPropertyDAO(SessionFactory sessionFactory) {
        super(sessionFactory, AssayProperty.class);
    }
}
