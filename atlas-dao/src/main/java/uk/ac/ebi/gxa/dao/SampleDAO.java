package uk.ac.ebi.gxa.dao;

import org.hibernate.SessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.microarray.atlas.model.Sample;

import java.util.List;

/**
 * @author Robert Petryszak
 */
public class SampleDAO extends AbstractDAO<Sample> {
    public static final String NAME_COL = "accession";

    public static final Logger log = LoggerFactory.getLogger(SampleDAO.class);

    public SampleDAO(SessionFactory sessionFactory) {
        super(sessionFactory, Sample.class);
    }

    long getTotalCount() {
        return (Long) template.find("select count(a) FROM Sample a").get(0);
    }

    @SuppressWarnings("unchecked")
    public List<Sample> getSamplesByPropertyValue(String propertyValue) {
        return template.find("select s from Experiment e left join e.samples s left join s.properties p where p.propertyValue.value = ? ", propertyValue);
    }

    @Override
    public void save(Sample object) {
        super.save(object);
        template.flush();
    }

    /**
     * @return Name of the column for hibernate to match searched objects against - c.f. super.getByName()
     */
    @Override
    public String getNameColumn() {
        return NAME_COL;
    }
}
