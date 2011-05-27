package uk.ac.ebi.gxa.dao;

import org.hibernate.SessionFactory;
import uk.ac.ebi.microarray.atlas.model.bioentity.BEProperty;

import java.util.Collection;

/**
 * User: nsklyar
 * Date: 24/05/2011
 */
public class BEPropertyDAO extends AbstractDAO<BEProperty>{

    BEPropertyDAO(SessionFactory sessionFactory, Class<BEProperty> clazz) {
        super(sessionFactory, clazz);
    }

    public void save(Collection<BEProperty> properties) {
        template.saveOrUpdate(properties);
    }
}
