package uk.ac.ebi.gxa.dao;

import org.hibernate.SessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.gxa.dao.exceptions.RecordNotFoundException;
import uk.ac.ebi.gxa.exceptions.LogUtil;
import uk.ac.ebi.microarray.atlas.model.Experiment;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;


public class ExperimentDAO extends AbstractDAO<Experiment> {
    public static final String NAME_COL = "accession";

    public static final Logger log = LoggerFactory.getLogger(ExperimentDAO.class);

    public ExperimentDAO(SessionFactory sessionFactory) {
        super(sessionFactory, Experiment.class);
    }

    @SuppressWarnings("unchecked")
    public List<Experiment> getExperimentsByArrayDesignAccession(String accession) {
        return template.find("select e from Experiment e left join e.assays a where a.arrayDesign.accession = ? ", accession);
    }

    @SuppressWarnings("unchecked")
    public List<Experiment> getExperimentsByAssay(String accession) {
        return template.find("select e from Experiment e left join e.assays a where a.accession = ? ", accession);
    }

    @SuppressWarnings("unchecked")
    public List<Experiment> getExperimentsBySample(String accession) {
        return template.find("select e from Experiment e left join e.samples s where s.accession = ? ", accession);
    }

    @SuppressWarnings("unchecked")
    public List<Experiment> getExperimentsBySamplePropertyOntologyTerm(String ontologyTerm) {
        return template.find("select e from Experiment e left join e.samples s left join s.properties p left join p.terms t where t.accession = ? ", ontologyTerm);
    }

    @SuppressWarnings("unchecked")
    public List<Experiment> getExperimentsBySamplePropertyValue(String propertyName, String propertyValue) {
        return template.find("select e from Experiment e left join e.samples s left join s.properties p where p.propertyValue.property.name = ?  and p.propertyValue.value = ?", propertyName, propertyValue);
    }

    @SuppressWarnings("unchecked")
    public List<Experiment> getExperimentsByAssayPropertyOntologyTerm(String ontologyTerm) {
        return template.find("select e from Experiment e left join e.assays a left join a.properties p left join p.terms t where t.accession = ? ", ontologyTerm);
    }

    @SuppressWarnings("unchecked")
    public List<Experiment> getExperimentsByAssayPropertyValue(String propertyName, String propertyValue) {
        return template.find("select e from Experiment e left join e.assays a left join a.properties p where p.propertyValue.property.name = ?  and p.propertyValue.value = ?", propertyName, propertyValue);
    }

    long getTotalCount() {
        return (Long) template.find("select count(e) FROM Experiment e").get(0);
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
    public void delete(String experimentAccession) throws RecordNotFoundException {
        template.delete(getByName(experimentAccession, NAME_COL));
    }

    public void delete(Experiment experiment) {
        template.delete(experiment);
    }

    @Override
    public void save(Experiment object) {
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
