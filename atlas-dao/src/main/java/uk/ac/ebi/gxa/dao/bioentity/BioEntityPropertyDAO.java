package uk.ac.ebi.gxa.dao.bioentity;

import org.hibernate.SessionFactory;
import uk.ac.ebi.gxa.dao.AbstractDAO;
import uk.ac.ebi.gxa.dao.exceptions.RecordNotFoundException;
import uk.ac.ebi.microarray.atlas.model.bioentity.BioEntityProperty;

/**
 * User: nsklyar
 * Date: 24/05/2011
 */
public class BioEntityPropertyDAO extends AbstractDAO<BioEntityProperty> {

    BioEntityPropertyDAO(SessionFactory sessionFactory) {
        super(sessionFactory, BioEntityProperty.class);
    }

    @Override
    protected String getNameColumn() {
        return "name";
    }

    public BioEntityProperty findOrCreate(String propertyName) {
        try {
            return getByName(propertyName);
        } catch (RecordNotFoundException e) {
            BioEntityProperty property = new BioEntityProperty(null, propertyName);
            super.save(property);
            template.flush();
            return property;
        }
    }

}
