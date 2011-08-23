package uk.ac.ebi.gxa.dao;

import org.hibernate.SessionFactory;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;
import uk.ac.ebi.gxa.dao.hibernate.DAOException;
import uk.ac.ebi.microarray.atlas.model.Property;
import uk.ac.ebi.microarray.atlas.model.PropertyValue;

import java.util.List;

public class PropertyValueDAO extends AbstractDAO<PropertyValue> {
    public PropertyValueDAO(SessionFactory sessionFactory) {
        super(sessionFactory, PropertyValue.class);
    }

    /**
     * @param property
     * @param value
     * @return PropertyValue matching property:value
     * @throws DAOException if no PropertyValue matching property:value was found
     */
    public PropertyValue find(Property property, String value) throws DAOException {
        @SuppressWarnings("unchecked")
        final List<PropertyValue> results = template.find("from PropertyValue where property = ? and value = ?", property, value);
        return getFirst(results, property + ":" + value);
    }

    @Override
    public String getNameColumn() {
        throw new NotImplementedException();
    }

    public void delete(PropertyValue propertyValue) {
        template.delete(propertyValue);
    }
}
