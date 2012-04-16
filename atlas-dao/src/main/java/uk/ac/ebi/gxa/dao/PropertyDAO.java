package uk.ac.ebi.gxa.dao;

import org.hibernate.SessionFactory;
import uk.ac.ebi.gxa.dao.exceptions.RecordNotFoundException;
import uk.ac.ebi.microarray.atlas.model.Property;
import uk.ac.ebi.microarray.atlas.model.PropertyValue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PropertyDAO extends AbstractDAO<Property> {


    public PropertyDAO(SessionFactory sessionFactory) {
        super(sessionFactory, Property.class);
    }


    public void delete(Property property, PropertyValue propertyValue) {
        property.deleteValue(propertyValue);
        save(property);
        // Clear hibernate session cache to force a re-load of assays/samples from the database. When a propertyValue is removed via hibernate,
        // foreign key constraints' 'ON DELETE CASCADES' in Oracle remove the corresponding Assay/SampleProperties. Since this is invisible to
        // hibernate, it does not refresh these objects in its session (L1) cache - hence the need to explicitly clear the cache.
        template.getSessionFactory().getCurrentSession().clear();
    }

    @Override
    public void save(Property object) {
        super.save(object);
        template.flush();
    }

    public void delete(Property object) {
        template.delete(object);
        template.flush();
    }

    @Override
    protected String getNameColumn() {
        return "name";
    }

    /**
     * @return lower case matching required in getByName() queries
     */
    @Override
    protected boolean lowerCaseNameMatch() {
        return false;
    }

    public Property getOrCreateProperty(String displayName) {
        final String accession = Property.getSanitizedPropertyAccession(displayName);
        try {
            return getByName(accession);
        } catch (RecordNotFoundException e) {
            // property not found - create a new one
            Property property = Property.createProperty(null, accession, displayName);
            save(property);
            return property;
        }
    }
    /**
     * @return List of Properties that are not referenced in any assay/sample
     */
    public Set<Property> getUnusedProperties() {
        @SuppressWarnings("unchecked")
        Set<Property> results = new HashSet<Property>();
        results.addAll(template.find("from Property pr where not exists (from Assay a left join a.properties p where p.propertyValue.property.name = pr.name)"));
        results.retainAll(template.find("from Property pr where not exists (from Sample s left join s.properties p where p.propertyValue.property.name = pr.name)"));
        return results;
    }

    /**
     * Remove all properties that are not referenced in any assay/sample
     */
    public void removeUnusedProperties() {
        for (Property property : getUnusedProperties())
            delete(property);
    }

    /**
     *
     * @param property
     * @return all values for property
     */
    public List<PropertyValue> getValues(Property property) {
        return property.getValues();
    }
}
