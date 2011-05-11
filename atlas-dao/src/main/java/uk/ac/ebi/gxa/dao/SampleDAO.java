package uk.ac.ebi.gxa.dao;

import org.hibernate.SessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.microarray.atlas.model.Sample;

import java.util.List;

public class SampleDAO extends AbstractDAO<Sample> {
    private static final Logger log = LoggerFactory.getLogger(SampleDAO.class);

    public SampleDAO(SessionFactory sessionFactory) {
        super(sessionFactory, Sample.class);
    }

    @SuppressWarnings("unchecked")
    public List<Sample> getSamplesByAssayAccession(String experimentAccession, String assayAccession) {
        return template.find("from Sample s " +
                " join s.assays a" +
                " where a.accession = ? " +
                " and a.experiment.accession = ?",
                experimentAccession, assayAccession);
    }
}
