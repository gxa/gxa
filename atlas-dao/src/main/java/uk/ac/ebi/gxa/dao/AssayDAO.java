package uk.ac.ebi.gxa.dao;

import org.hibernate.SessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.microarray.atlas.model.Assay;
import uk.ac.ebi.microarray.atlas.model.Experiment;

import java.util.List;

public class AssayDAO extends AbstractDAO<Assay> {
    public static final Logger log = LoggerFactory.getLogger(AssayDAO.class);

    public AssayDAO(SessionFactory sessionFactory) {
        super(sessionFactory, Assay.class);
    }

    @SuppressWarnings("unchecked")
    public List<Assay> getByExperiment(final Experiment experiment) {
        return template.find("from Assay a where a.experiment.accession = ? ",
                new Object[]{experiment.getAccession()});
    }

    long getTotalCount() {
         return (Long) template.find("select count(a) FROM Assay a").get(0);
     }
}
