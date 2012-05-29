package uk.ac.ebi.gxa.dao;

import org.hibernate.SessionFactory;
import uk.ac.ebi.gxa.dao.exceptions.RecordNotFoundException;
import uk.ac.ebi.microarray.atlas.model.Property;
import uk.ac.ebi.microarray.atlas.model.PropertyValue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
        Property property = propertyDAO.getOrCreateProperty(name);
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

    /**
     * @return List of PropertyValue's that are not referenced in any assay/sample
     */
    public Set<PropertyValue> getUnusedPropertyValues() {
        @SuppressWarnings("unchecked")
        Set<PropertyValue> results = new HashSet<PropertyValue>();
        results.addAll(template.find("from PropertyValue pv where not exists (from Assay a left join a.properties p where p.propertyValue.value = pv.value)"));
        results.retainAll(template.find("from PropertyValue pv where not exists (from Sample s left join s.properties p where p.propertyValue.value = pv.value) "));
        return results;
    }

    public boolean isPropertyValueUsed(String propertyName, String propertyValue) {
        if (!template.find("from Assay a left join a.properties p where p.propertyValue.property.name = ? and p.propertyValue.value = ?", propertyName, propertyValue).isEmpty() ||
                !template.find("from Sample s left join s.properties p where p.propertyValue.property.name = ? and p.propertyValue.value = ?", propertyName, propertyValue).isEmpty())
            return true;
        return false;
    }

    /**
     * @return remove PropertyValue's that are not referenced in any assay/sample
     */
    public void removeUnusedPropertyValues() {
        for (PropertyValue propertyValue : getUnusedPropertyValues())
            delete(propertyValue);
    }

    public List<PropertyValue> findValuesForProperty(String propertyName) {
        return template.find("from PropertyValue pv where property.name = ?", propertyName);
    }
}
