package uk.ac.ebi.gxa.dao;

import org.hibernate.SessionFactory;
import uk.ac.ebi.microarray.atlas.model.bioentity.Software;

/**
 * User: nsklyar
 * Date: 26/05/2011
 */
public class SoftwareDAO extends AbstractDAO<Software>{

    SoftwareDAO(SessionFactory sessionFactory, Class<Software> clazz) {
        super(sessionFactory, clazz);
    }

    public Software find(String name, String version) {
        return null;
    }
}
