package uk.ac.ebi.gxa.dao;

import org.hibernate.SessionFactory;
import uk.ac.ebi.microarray.atlas.model.SampleProperty;

public class SamplePropertyDAO extends AbstractDAO<SampleProperty> {
    public SamplePropertyDAO(SessionFactory sessionFactory) {
        super(sessionFactory, SampleProperty.class);
    }
}
