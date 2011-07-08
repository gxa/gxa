package uk.ac.ebi.gxa.dao;

import org.hibernate.SessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.microarray.atlas.model.Experiment;
import uk.ac.ebi.microarray.atlas.model.Ontology;
import uk.ac.ebi.microarray.atlas.model.OntologyTerm;

import java.util.List;

import static com.google.common.collect.Iterables.getFirst;

/**
 * @author Misha Kapushesky
 */
public class OntologyDAO extends AbstractDAO<Ontology> {
    public static final Logger log = LoggerFactory.getLogger(ExperimentDAO.class);

    OntologyDAO(SessionFactory sessionFactory) {
        super(sessionFactory, Ontology.class);
    }

    public Ontology getByName(final String name) {
        @SuppressWarnings("unchecked")
        final List<Ontology> result = template.find("from Ontology where name = ?", name);
        return getFirst(result, null);
    }
}
