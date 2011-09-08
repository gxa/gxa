package uk.ac.ebi.gxa.dao;

import org.hibernate.SessionFactory;
import uk.ac.ebi.gxa.dao.exceptions.RecordNotFoundException;
import uk.ac.ebi.microarray.atlas.model.Organism;

public class OrganismDAO extends AbstractDAO<Organism> {

    public OrganismDAO(SessionFactory sessionFactory) {
        super(sessionFactory, Organism.class);
    }

    @Override
    protected String getNameColumn() {
        return "name";
    }

    /**
     * @return lower case matching required in getByName() queries
     */
    @Override
    protected boolean lowerCaseNameMatch() {
        return true;
    }

    public Organism getOrCreateOrganism(String name) {
        try {
            return getByName(name);
        } catch (RecordNotFoundException e) {
            // organism not found - create a new one
            Organism organism = new Organism(null, name);
            save(organism);
            template.flush();
            return organism;
        }
    }
}
