package uk.ac.ebi.gxa.dao;

import org.hibernate.SessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.microarray.atlas.model.Assay;

public class AssayDAO extends AbstractDAO<Assay> {
    public static final Logger log = LoggerFactory.getLogger(AssayDAO.class);

    public AssayDAO(SessionFactory sessionFactory) {
        super(sessionFactory, Assay.class);
    }

    long getTotalCount() {
        return (Long) template.find("select count(a) FROM Assay a").get(0);
    }
}
