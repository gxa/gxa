package uk.ac.ebi.gxa.dao;

import org.hibernate.SessionFactory;
import uk.ac.ebi.gxa.dao.exceptions.RecordNotFoundException;
import uk.ac.ebi.microarray.atlas.model.Property;
import uk.ac.ebi.microarray.atlas.model.PropertyValue;

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

    @Override
    protected String getNameColumn() {
        return "name";
    }

    /**
     * @return lower case matching required in getByName() queries
     */
    @Override
    protected boolean lowerCaseNameMatch() {
        return true;
    }

    public Property getOrCreateProperty(String accession, String displayName) {
        try {
            return getByName(accession);
        } catch (RecordNotFoundException e) {
            // property not found - create a new one
            Property property = Property.createProperty(null, accession, displayName);
            save(property);
            return property;
        }
    }
}
