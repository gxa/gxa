package uk.ac.ebi.gxa.dao;

import org.hibernate.SessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.microarray.atlas.model.Sample;

/**
 * Created by IntelliJ IDEA.
 * User: rpetry
 * Date: 7/13/11
 * Time: 3:57 PM
 * To change this template use File | Settings | File Templates.
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
