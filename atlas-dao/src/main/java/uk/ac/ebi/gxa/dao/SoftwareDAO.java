package uk.ac.ebi.gxa.dao;

import org.hibernate.SessionFactory;
import uk.ac.ebi.gxa.dao.exceptions.RecordNotFoundException;
import uk.ac.ebi.microarray.atlas.model.bioentity.Software;

import java.util.List;

public class SoftwareDAO extends AbstractDAO<Software> {

    SoftwareDAO(SessionFactory sessionFactory) {
        super(sessionFactory, Software.class);
    }

    public Software find(String name, String newVersion) {
        return new Software(name, newVersion);
    }

    public Software findOrCreate(String name, String version) {
        try {
            @SuppressWarnings("unchecked")
            final List<Software> software = template.find("from Software where name = ? and version = ?", name, version);
            return getOnly(software);
        } catch (RecordNotFoundException e) {
            Software software = new Software(name, version);
            save(software);
            return software;
        }
    }

    @Override
    protected String getNameColumn() {
        return "name";
    }

    @Override
    public void save(Software object) {
        super.save(object);
        template.flush();
    }

    @SuppressWarnings("unchecked")
    public List<Software> getActiveSoftwares() {
        return template.find("from Software where isActive = 'T'");
    }
}