package uk.ac.ebi.gxa.dao;

import org.hibernate.SessionFactory;
import uk.ac.ebi.microarray.atlas.model.bioentity.BioEntityType;

import java.util.List;


/**
 * User: nsklyar
 * Date: 07/07/2011
 */
public class BioEntityTypeDAO extends AbstractDAO<BioEntityType> {

    BioEntityTypeDAO(SessionFactory sessionFactory) {
        super(sessionFactory, BioEntityType.class);
    }

    public BioEntityType findOrCreate(String typeName) {
       final List<BioEntityType> types = template.find("from BioEntityType where name = ?", typeName.toLowerCase());
       if (types.size() == 1) {
           return types.get(0);
       } else {
           BioEntityType type = new BioEntityType(null, typeName, 0);
           save(type);
           return type;
       }
    }
}
