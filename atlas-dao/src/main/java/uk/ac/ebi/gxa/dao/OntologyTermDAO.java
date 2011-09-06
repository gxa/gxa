package uk.ac.ebi.gxa.dao;

import org.hibernate.SessionFactory;
import uk.ac.ebi.gxa.dao.exceptions.RecordNotFoundException;
import uk.ac.ebi.microarray.atlas.model.Ontology;
import uk.ac.ebi.microarray.atlas.model.OntologyTerm;

/**
 * @author Misha Kapushesky
 */
public class OntologyTermDAO extends AbstractDAO<OntologyTerm> {
    public static final String NAME_COL = "accession";

    OntologyTermDAO(SessionFactory sessionFactory) {
        super(sessionFactory, OntologyTerm.class);
    }

    /**
     * @return Name of the column for hibernate to match searched objects against - c.f. super.getByName()
     */
    @Override
    public String getNameColumn() {
        return NAME_COL;
    }

    public OntologyTerm getOrCreateOntologyTerm(final String accession,
                                                final String term,
                                                final String description,
                                                final Ontology ontology) {
        try {
            return getByName(accession);
        } catch (RecordNotFoundException e) { // ontology term not found - create new one
            OntologyTerm ontologyTerm = new OntologyTerm(null, ontology, term, accession, description);
            save(ontologyTerm);
            return ontologyTerm;
        }
    }
}
