package uk.ac.ebi.gxa.dao;

import org.hibernate.SessionFactory;
import uk.ac.ebi.microarray.atlas.model.Property;

import java.util.List;

public class PropertyDAO extends AbstractDAO<Property> {
    public PropertyDAO(SessionFactory sessionFactory) {
        super(sessionFactory, Property.class);
    }

    public Property getByName(String name) {
        @SuppressWarnings("unchecked")
        final List<Property> results = template.find("from Property where name = ?", name.toLowerCase());
        return results.isEmpty() ? null : results.get(0);
    }
}
