package uk.ac.ebi.gxa.dao.bioentity;

import org.hibernate.SessionFactory;
import uk.ac.ebi.gxa.dao.AbstractDAO;
import uk.ac.ebi.microarray.atlas.model.bioentity.BioEntityProperty;
import uk.ac.ebi.microarray.atlas.model.bioentity.BioEntityType;

import java.util.List;

import static com.google.common.collect.Iterables.getFirst;


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
       BioEntityType type = find(typeName);
        if (type == null) {
            BioEntityProperty beProperty = propertyDAO.getByName(typeName);
            type = new BioEntityType(null, typeName, 0, beProperty, beProperty);
            save(type);
        }

        return type;
    }

    public BioEntityType find(String typeName) {
       final List<BioEntityType> types = template.find("from BioEntityType where name = ?", typeName.toLowerCase());
       return getFirst(types, null);
    }
}
