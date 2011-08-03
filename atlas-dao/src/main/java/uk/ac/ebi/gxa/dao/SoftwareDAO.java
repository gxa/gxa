package uk.ac.ebi.gxa.dao;

import org.hibernate.SessionFactory;
import uk.ac.ebi.microarray.atlas.model.bioentity.Software;

import java.util.List;

public class SoftwareDAO extends AbstractDAO<Software> {

    SoftwareDAO(SessionFactory sessionFactory) {
        super(sessionFactory, Software.class);
    }


    public Software find(String name, String newVersion) {
        Software software = new Software(name, newVersion);
        return software;
    }

    public Software findOrCreate(String name, String newVersion) {
//        template.setFlushMode(HibernateAccessor.FLUSH_COMMIT);
//        FlushMode flushMode = sessionFactory.getCurrentSession().getFlushMode();
//        sessionFactory.getCurrentSession().setFlushMode(FlushMode.COMMIT);
        List<Software> softwareList = template.find("from Software where name = ? and version = ?", name, newVersion);
        if (softwareList.size() == 1) {
            return softwareList.get(0);
        } else {
            Software software = new Software(name, newVersion);
            save(software);
            return software;
        }
    }

    @Override
    public void save(Software object) {
        super.save(object);
        template.flush();
    }

    public List<Software> getActiveSoftwares() {
        return template.find("from Software where isActive = 1");
    }

}