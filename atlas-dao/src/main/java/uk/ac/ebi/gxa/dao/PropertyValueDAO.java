package uk.ac.ebi.gxa.dao;

import org.hibernate.SessionFactory;
import uk.ac.ebi.gxa.dao.exceptions.RecordNotFoundException;
import uk.ac.ebi.microarray.atlas.model.Property;
import uk.ac.ebi.microarray.atlas.model.PropertyValue;

import java.util.List;

public class PropertyValueDAO extends AbstractDAO<PropertyValue> {
    private PropertyDAO propertyDAO;

    public PropertyValueDAO(SessionFactory sessionFactory, PropertyDAO propertyDAO) {
        super(sessionFactory, PropertyValue.class);
        this.propertyDAO = propertyDAO;
    }

    /**
     * @param property
     * @param value
     * @return PropertyValue matching property:value
     * @throws uk.ac.ebi.gxa.dao.exceptions.RecordNotFoundException
     *          if no PropertyValue matching property:value was found
     */
    public PropertyValue find(Property property, String value) throws RecordNotFoundException {
        @SuppressWarnings("unchecked")
        final List<PropertyValue> results = template.find("from PropertyValue where property = ? and value = ?", property, value);
        return getOnly(results);
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

    public PropertyValue getOrCreatePropertyValue(String name, String value) {
        Property property = propertyDAO.getOrCreateProperty(Property.getSanitizedPropertyAccession(name), name);
        return getOrCreatePropertyValue(property, value);
    }

    public PropertyValue getOrCreatePropertyValue(Property property, String value) {
        try {
            return find(property, value);
        } catch (RecordNotFoundException e) {
            // property value not found - create a new one
            PropertyValue propertyValue = new PropertyValue(null, property, value);
            save(propertyValue);
            return propertyValue;
        }
    }
}
