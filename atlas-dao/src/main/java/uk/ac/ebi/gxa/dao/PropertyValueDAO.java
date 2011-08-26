package uk.ac.ebi.gxa.dao;

import org.hibernate.SessionFactory;
import uk.ac.ebi.gxa.dao.exceptions.RecordNotFoundException;
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
     * @throws uk.ac.ebi.gxa.dao.exceptions.RecordNotFoundException if no PropertyValue matching property:value was found
     */
    public PropertyValue find(Property property, String value) throws RecordNotFoundException {
        @SuppressWarnings("unchecked")
        final List<PropertyValue> results = template.find("from PropertyValue where property = ? and value = ?", property, value);
        return getFirst(results, property + ":" + value);
    }

    /**
     * Not implemented
     *
     * @return nothing. Throws {@link UnsupportedOperationException}
     */
    @Override
    public String getNameColumn() {
        throw new UnsupportedOperationException();
    }

    public void delete(PropertyValue propertyValue) {
        template.delete(propertyValue);
    }
}
