package uk.ac.ebi.gxa.dao.bioentity;

import org.hibernate.SessionFactory;
import uk.ac.ebi.gxa.dao.AbstractDAO;
import uk.ac.ebi.microarray.atlas.model.bioentity.BioEntityProperty;

import java.util.Collection;
import java.util.List;

import static com.google.common.collect.Iterables.getFirst;

/**
 * User: nsklyar
 * Date: 24/05/2011
 */
public class BioEntityPropertyDAO extends AbstractDAO<BioEntityProperty> {

    BioEntityPropertyDAO(SessionFactory sessionFactory) {
        super(sessionFactory, BioEntityProperty.class);
    }

    public void save(Collection<BioEntityProperty> properties) {
        template.saveOrUpdate(properties);
    }

    public BioEntityProperty getByName(String name) {
        final List<BioEntityProperty> results = template.find("from BioEntityProperty where name = ?", name.toLowerCase());
        return getFirst(results, null);
    }

     public BioEntityProperty findOrCreate(String propertyName) {
        BioEntityProperty property = getByName(propertyName);
        if (property == null) {
            property = new BioEntityProperty(null, propertyName);
            super.save(property);
            template.flush();
        }
        return property;
    }
}
