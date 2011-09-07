package uk.ac.ebi.gxa.dao;

import org.hibernate.SessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.gxa.dao.exceptions.RecordNotFoundException;
import uk.ac.ebi.microarray.atlas.model.Ontology;

/**
 * @author Misha Kapushesky
 */
public class OntologyDAO extends AbstractDAO<Ontology> {
    public static final Logger log = LoggerFactory.getLogger(ExperimentDAO.class);

    OntologyDAO(SessionFactory sessionFactory) {
        super(sessionFactory, Ontology.class);
    }

    @Override
    protected String getNameColumn() {
        return "name";
    }

    public Ontology getOrCreateOntology(
            final String ontologyName,
            final String ontologyDescription,
            final String ontologySourceUri,
            final String ontologyVersion) {
        try {
            return getByName(ontologyName);
        } catch (RecordNotFoundException e) {
            // ontology not found - create a new one
            Ontology ontology = new Ontology(null, ontologyName, ontologySourceUri, ontologyDescription,
                    ontologyVersion);
            save(ontology);
            return ontology;
        }
    }
}
