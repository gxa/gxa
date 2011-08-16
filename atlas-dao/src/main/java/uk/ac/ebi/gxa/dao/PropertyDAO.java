package uk.ac.ebi.gxa.dao;

import org.hibernate.SessionFactory;
import uk.ac.ebi.microarray.atlas.model.Property;
import uk.ac.ebi.microarray.atlas.model.PropertyValue;

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

    public void delete(Property property, PropertyValue propertyValue) {
        property.deleteValue(propertyValue);
    }

    @Override
    public void save(Property object) {
        super.save(object);
        template.flush();
    }
}
