package uk.ac.ebi.gxa.dao.bioentity;

import org.hibernate.SessionFactory;
import uk.ac.ebi.gxa.dao.AbstractDAO;
import uk.ac.ebi.gxa.dao.exceptions.RecordNotFoundException;
import uk.ac.ebi.microarray.atlas.model.bioentity.BioEntityProperty;
import uk.ac.ebi.microarray.atlas.model.bioentity.BioEntityType;


/**
 * User: nsklyar
 * Date: 07/07/2011
 */
public class BioEntityTypeDAO extends AbstractDAO<BioEntityType> {

    private BioEntityPropertyDAO propertyDAO;

    BioEntityTypeDAO(SessionFactory sessionFactory, BioEntityPropertyDAO propertyDAO) {
        super(sessionFactory, BioEntityType.class);
        this.propertyDAO = propertyDAO;
    }

    public BioEntityType findOrCreate(String typeName) {
        try {
            return getByName(typeName);
        } catch (RecordNotFoundException e) {
            BioEntityProperty beProperty = propertyDAO.findOrCreate(typeName);
            BioEntityType type = new BioEntityType(null, typeName, 0, beProperty, beProperty);
            save(type);
            template.flush();
            return type;
        }
    }

    @Override
    protected String getNameColumn() {
        return "name";
    }
}
