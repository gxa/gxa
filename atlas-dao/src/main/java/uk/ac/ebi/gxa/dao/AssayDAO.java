package uk.ac.ebi.gxa.dao;

import org.hibernate.SessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.microarray.atlas.model.Assay;

import java.util.List;

public class AssayDAO extends AbstractDAO<Assay> {
    public static final Logger log = LoggerFactory.getLogger(AssayDAO.class);

    public AssayDAO(SessionFactory sessionFactory) {
        super(sessionFactory, Assay.class);
    }

    long getTotalCount() {
        return (Long) template.find("select count(a) FROM Assay a").get(0);
    }

    @SuppressWarnings("unchecked")
    public List<Assay> getAssaysByPropertyValue(String propertyValue) {
        return template.find("select a from Experiment e left join e.assays a left join a.properties p where p.propertyValue.value = ? ", propertyValue);
    }

    @Override
    public void save(Assay object) {
        super.save(object);
        template.flush();
    }
}
