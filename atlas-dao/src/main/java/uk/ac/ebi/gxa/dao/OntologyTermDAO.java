package uk.ac.ebi.gxa.dao;

import org.hibernate.SessionFactory;
import uk.ac.ebi.microarray.atlas.model.OntologyTerm;

import java.util.List;

import static com.google.common.collect.Iterables.getFirst;

/**
 * @author Misha Kapushesky
 */
public class OntologyTermDAO extends AbstractDAO<OntologyTerm> {
    OntologyTermDAO(SessionFactory sessionFactory) {
        super(sessionFactory, OntologyTerm.class);
    }

    public OntologyTerm getByAccession(final String accession) {
        @SuppressWarnings("unchecked")
        final List<OntologyTerm> result = template.find("from OntologyTerm where accession = ?", accession);
        return getFirst(result, null);
    }
}
