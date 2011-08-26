package uk.ac.ebi.gxa.dao;

import org.hibernate.SessionFactory;
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
}
