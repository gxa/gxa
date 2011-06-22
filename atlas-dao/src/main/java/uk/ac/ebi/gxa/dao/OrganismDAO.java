package uk.ac.ebi.gxa.dao;

import org.hibernate.SessionFactory;
import uk.ac.ebi.microarray.atlas.model.Organism;

import java.util.List;

import static com.google.common.collect.Iterables.getFirst;

public class OrganismDAO extends AbstractDAO<Organism> {
    public OrganismDAO(SessionFactory sessionFactory) {
        super(sessionFactory, Organism.class);
    }

    public Organism getByName(String name) {
        final List<Organism> results = template.find("from Organism where name = ?", name.toLowerCase());
        return getFirst(results, null);
    }
}
