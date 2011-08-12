package uk.ac.ebi.gxa.dao;

import org.hibernate.SessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.gxa.exceptions.LogUtil;
import uk.ac.ebi.microarray.atlas.model.Experiment;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;

import static com.google.common.collect.Iterables.getFirst;

public class ExperimentDAO extends AbstractDAO<Experiment> {
    public static final Logger log = LoggerFactory.getLogger(ExperimentDAO.class);

    public ExperimentDAO(SessionFactory sessionFactory) {
        super(sessionFactory, Experiment.class);
    }

    @SuppressWarnings("unchecked")
    public List<Experiment> getExperimentsByArrayDesignAccession(String accession) {
        return template.find("select e from Experiment e left join e.assays a where a.arrayDesign.accession = ? ", accession);
    }

    long getTotalCount() {
        return (Long) template.find("select count(e) FROM Experiment e").get(0);
    }

    public Experiment getExperimentByAccession(String accession) {
        @SuppressWarnings("unchecked")
        final List<Experiment> result = template.find("from Experiment where accession = ?", accession);
        return getFirst(result, null);
    }

    public long getCountSince(String lastReleaseDate) {
        try {
            return (Long) template.find("select count(id) from Experiment where loadDate > ?",
                    new SimpleDateFormat("MM-yyyy").parse(lastReleaseDate)).get(0);
        } catch (ParseException e) {
            throw LogUtil.createUnexpected("Invalid date: " + lastReleaseDate, e);
        }
    }

    @Deprecated
    public void delete(String experimentAccession) {
        template.delete(getExperimentByAccession(experimentAccession));
    }

    public void delete(Experiment experiment) {
        template.delete(experiment);
    }

    @Override
    public void save(Experiment object) {
        super.save(object);
        template.flush();
    }
}
