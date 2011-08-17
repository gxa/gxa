package uk.ac.ebi.gxa.dao;

import org.hibernate.SessionFactory;
import uk.ac.ebi.gxa.dao.hibernate.DAOException;
import uk.ac.ebi.microarray.atlas.model.Experiment;
import uk.ac.ebi.microarray.atlas.model.OntologyTerm;

import java.util.List;

import static com.google.common.collect.Iterables.getFirst;

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
