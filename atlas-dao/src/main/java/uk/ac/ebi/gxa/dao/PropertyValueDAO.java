package uk.ac.ebi.gxa.dao;

import org.hibernate.SessionFactory;
import uk.ac.ebi.microarray.atlas.model.Property;
import uk.ac.ebi.microarray.atlas.model.PropertyValue;

import java.util.Collection;

public class PropertyValueDAO extends AbstractDAO<PropertyValue> {
    public PropertyValueDAO(SessionFactory sessionFactory) {
        super(sessionFactory, PropertyValue.class);
    }

    @SuppressWarnings("unchecked")
    public Collection<PropertyValue> getAllPropertyValues() {
        return template.find("from PropertyValue join fetch property");
    }

    @SuppressWarnings("unchecked")
    public Collection<PropertyValue> getAllPropertyValues(Property pd) {
        return template.find("from PropertyValue pv " +
                "where pv.property = ?",
                new Object[]{pd});
    }
}
