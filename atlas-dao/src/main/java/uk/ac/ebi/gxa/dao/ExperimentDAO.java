package uk.ac.ebi.gxa.dao;

import org.hibernate.SessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import uk.ac.ebi.gxa.exceptions.LogUtil;
import uk.ac.ebi.microarray.atlas.model.Experiment;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;

public class ExperimentDAO extends AbstractDAO<Experiment> {
    public static final Logger log = LoggerFactory.getLogger(ExperimentDAO.class);

    public ExperimentDAO(SessionFactory sessionFactory) {
        super(sessionFactory, Experiment.class);
    }

    @SuppressWarnings("unchecked")
    public List<Experiment> getExperimentsByArrayDesignAccession(String accession) {
        return template.find(
                "from Experiment e " +
                        "left join e.assays a " +
                        "where a.arrayDesign.accession = ? ",
                new Object[]{accession});
    }

    int getTotalCount() {
        return (Integer) template.find("select count(e) FROM Experiment e").get(0);
    }

    public Experiment getExperimentByAccession(String accession) {
        try {
            return (Experiment) template.find("from Experiment e " + "where e.accession = ?",
                    new Object[]{accession}).get(0);
        } catch (IncorrectResultSizeDataAccessException e) {
            return null;
        }
    }

    public int getCountSince(String lastReleaseDate) {
        try {
            return (Integer) template.find(
                    "select count(e) from Experiment e where e.loadDate > ?",
                    new SimpleDateFormat("MM-YYYY").parse(lastReleaseDate)).get(0);
        } catch (ParseException e) {
            throw LogUtil.createUnexpected("Invalid date: " + lastReleaseDate, e);
        }
    }

    @SuppressWarnings("unchecked")
    public List<Experiment> getAllExperiments() {
        return template.find("from Experiment");

    }

    @Deprecated
    public void delete(String experimentAccession) {
        template.delete(getExperimentByAccession(experimentAccession));
    }
}
