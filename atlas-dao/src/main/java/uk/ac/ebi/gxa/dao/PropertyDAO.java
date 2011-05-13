package uk.ac.ebi.gxa.dao;

import org.hibernate.SessionFactory;
import uk.ac.ebi.microarray.atlas.model.Property;

public class PropertyDAO extends AbstractDAO<Property> {
    public PropertyDAO(SessionFactory sessionFactory) {
        super(sessionFactory, Property.class);
    }

    public Property getByName(String name) {
        return (Property) template.find("from Property pd where pd.name = ?",
                new Object[]{name}).get(0);
    }
}
