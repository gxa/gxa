package uk.ac.ebi.gxa.dao;

import org.hibernate.SessionFactory;
import uk.ac.ebi.microarray.atlas.model.Property;
import uk.ac.ebi.microarray.atlas.model.PropertyValue;

public class PropertyDAO extends AbstractDAO<Property> {


    public PropertyDAO(SessionFactory sessionFactory) {
        super(sessionFactory, Property.class);
    }


    public void delete(Property property, PropertyValue propertyValue) {
        property.deleteValue(propertyValue);
        save(property);
    }

    @Override
    public void save(Property object) {
        super.save(object);
        template.flush();
    }

    /**
     * @return lower case matching required in getByName() queries
     */
    @Override
    protected boolean lowerCaseNameMatch() {
        return true;
    }
}
