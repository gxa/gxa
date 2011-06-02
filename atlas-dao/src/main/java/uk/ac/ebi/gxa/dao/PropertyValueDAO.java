package uk.ac.ebi.gxa.dao;

import org.hibernate.SessionFactory;
import uk.ac.ebi.microarray.atlas.model.Property;
import uk.ac.ebi.microarray.atlas.model.PropertyValue;

import java.util.List;

public class PropertyValueDAO extends AbstractDAO<PropertyValue> {
    public PropertyValueDAO(SessionFactory sessionFactory) {
        super(sessionFactory, PropertyValue.class);
    }

    public PropertyValue find(Property property, String value) {
        final List results = template.find("from PropertyValue where property = ? and value = ?", property, value);
        return results.isEmpty() ? null : (PropertyValue) results.get(0);
    }
}
