package uk.ac.ebi.gxa.dao;

import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import uk.ac.ebi.gxa.Experiment;

public class ExperimentDAO extends AbstractDAO<Experiment> {
    private final AtlasDAO adao;

    public ExperimentDAO(JdbcTemplate template, AtlasDAO adao) {
        super(template);
        this.adao = adao;
    }

    public Experiment getById(long id) {
        try {
            return template.queryForObject(
                    "SELECT " + AtlasDAO.ExperimentMapper.FIELDS + " FROM a2_experiment WHERE experimentid = ?",
                    new Object[]{id},
                    new AtlasDAO.ExperimentMapper(adao));
        } catch (IncorrectResultSizeDataAccessException e) {
            return null;
        }
    }

    @Override
    protected String sequence() {
        return "A2_EXPERIMENT_SEQ";
    }

    @Override
    protected void save(Experiment experiment) {
        adao.writeExperimentInternal(experiment);
    }
}
