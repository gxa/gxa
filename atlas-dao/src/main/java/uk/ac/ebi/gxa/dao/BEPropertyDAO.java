package uk.ac.ebi.gxa.dao;

import org.hibernate.SessionFactory;
import uk.ac.ebi.microarray.atlas.model.bioentity.BioEntityProperty;

import java.util.Collection;

/**
 * User: nsklyar
 * Date: 24/05/2011
 */
public class BEPropertyDAO extends AbstractDAO<BioEntityProperty>{

    BEPropertyDAO(SessionFactory sessionFactory) {
        super(sessionFactory, BioEntityProperty.class);
    }

    public void save(Collection<BioEntityProperty> properties) {
        template.saveOrUpdate(properties);
    }
}
