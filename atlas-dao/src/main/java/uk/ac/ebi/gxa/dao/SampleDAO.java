package uk.ac.ebi.gxa.dao;

import org.hibernate.SessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.microarray.atlas.model.Sample;

/**
 *
 * @author Robert Petryszak
 */
public class SampleDAO extends AbstractDAO<Sample> {
        public static final Logger log = LoggerFactory.getLogger(SampleDAO.class);

    public SampleDAO(SessionFactory sessionFactory) {
        super(sessionFactory, Sample.class);
    }

    long getTotalCount() {
        return (Long) template.find("select count(a) FROM Sample a").get(0);
    }
}
