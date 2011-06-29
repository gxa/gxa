package uk.ac.ebi.gxa.dao;

import org.hibernate.FlushMode;
import org.hibernate.SessionFactory;
import org.springframework.orm.hibernate3.HibernateAccessor;
import org.springframework.orm.hibernate3.SessionFactoryUtils;
import uk.ac.ebi.microarray.atlas.model.bioentity.Software;

import java.util.List;

public class SoftwareDAO extends AbstractDAO<Software> {

    private SessionFactory sessionFactory;

    SoftwareDAO(SessionFactory sessionFactory) {
        super(sessionFactory, Software.class);
        this.sessionFactory = sessionFactory;
    }


    public Software find(String name, String newVersion) {
        Software software = new Software(name, newVersion);
        save(software);
        return software;
    }

    public Software findOrCreate(String name, String newVersion) {
//        template.setFlushMode(HibernateAccessor.FLUSH_COMMIT);
//        FlushMode flushMode = sessionFactory.getCurrentSession().getFlushMode();
//        System.out.println("flushMode = " + flushMode);
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

    public void startSession() {
        SessionFactoryUtils.initDeferredClose(sessionFactory);
    }

    public void finishSession() {
        SessionFactoryUtils.processDeferredClose(sessionFactory);
    }
}