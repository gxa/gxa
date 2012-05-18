package uk.ac.ebi.gxa.dao;

import org.hibernate.SessionFactory;
import org.springframework.orm.hibernate3.HibernateTemplate;
import uk.ac.ebi.gxa.dao.exceptions.RecordNotFoundException;
import uk.ac.ebi.microarray.atlas.model.bioentity.Software;

import java.util.List;

public class SoftwareDAO {

    protected final HibernateTemplate template;

    SoftwareDAO(SessionFactory sessionFactory) {
        this.template = new HibernateTemplate(sessionFactory);
    }

    public Software getById(long id) {
        return template.get(Software.class, id);
    }

    public Software find(String name, String newVersion) {
        return new Software(name, newVersion);
    }

    public Software findOrCreate(String name, String version) {
        try {
            @SuppressWarnings("unchecked")
            final List<Software> softwares = template.find("from Software where name = ? and version = ?", name, version);
            if (softwares.size() == 1) {
                return softwares.get(0);
            } else {
                throw new RecordNotFoundException(Software.class.getName() + ": " + softwares.size() + " objects returned; expected 1)");
            }
        } catch (RecordNotFoundException e) {
            Software software = new Software(name, version);
            save(software);
            return software;
        }
    }

    public void save(Software object) {
        template.saveOrUpdate(object);
        template.flush();
    }

    @SuppressWarnings("unchecked")
    public List<Software> getActiveSoftwares() {
        return template.find("from Software where isActive = 'T'");
    }

    @SuppressWarnings("unchecked")
    public List<Software> getAllButLegacySoftware() {
        return template.find("from Software where legacy = 'F'");
    }
}