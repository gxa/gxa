package uk.ac.ebi.gxa.dao;

import org.hibernate.SessionFactory;
import uk.ac.ebi.microarray.atlas.model.Organism;

import java.util.List;

public class OrganismDAO extends AbstractDAO<Organism> {

    public OrganismDAO(SessionFactory sessionFactory) {
        super(sessionFactory, Organism.class);
    }

    /**
     * @return lower case matching required in getByName() queries
     */
    @Override
    protected boolean lowerCaseNameMatch() {
        return true;
    }

}
