package uk.ac.ebi.gxa.dao;

import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import uk.ac.ebi.gxa.Asset;
import uk.ac.ebi.gxa.Experiment;
import uk.ac.ebi.gxa.utils.LazyList;
import uk.ac.ebi.microarray.atlas.model.Assay;
import uk.ac.ebi.microarray.atlas.model.Sample;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.Callable;

public class ExperimentDAO extends AbstractDAO<Experiment> {
    private final AtlasDAO adao;

    public ExperimentDAO(JdbcTemplate template, AtlasDAO adao) {
        super(template);
        this.adao = adao;
    }

    public Experiment getById(long id) {
        try {
            return template.queryForObject(
                    "SELECT " + ExperimentMapper.FIELDS + " FROM a2_experiment WHERE experimentid = ?",
                    new Object[]{id},
                    new ExperimentMapper(adao));
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

    static class ExperimentMapper implements RowMapper<Experiment> {
        static final String FIELDS = " experimentid, accession, description, performer, lab, " +
                " loaddate, pmid, abstract, releasedate, private, curated ";
        private AtlasDAO atlasDAO;

        public ExperimentMapper(final AtlasDAO atlasDAO) {
            this.atlasDAO = atlasDAO;
        }

        public Experiment mapRow(ResultSet resultSet, int i) throws SQLException {
            final Experiment experiment = atlasDAO.model.createExperiment(
                    resultSet.getLong(1), resultSet.getString(2)
            );

            experiment.setDescription(resultSet.getString(3));
            experiment.setPerformer(resultSet.getString(4));
            experiment.setLab(resultSet.getString(5));
            experiment.setLoadDate(resultSet.getDate(6));
            experiment.setPubmedIdString(resultSet.getString(7));
            experiment.setAbstract(resultSet.getString(8));
            experiment.setReleaseDate(resultSet.getDate(9));
            experiment.setPrivate(resultSet.getBoolean(10));
            experiment.setCurated(resultSet.getBoolean(11));

            experiment.setAssets(new LazyList<Asset>(new Callable<List<Asset>>() {
                @Override
                public List<Asset> call() throws Exception {
                    return atlasDAO.loadAssetsForExperiment(experiment);
                }
            }));
            experiment.setAssays(new LazyList<Assay>(new Callable<List<Assay>>() {
                @Override
                public List<Assay> call() throws Exception {
                    return atlasDAO.getAssaysByExperimentAccession(experiment);
                }
            }));
            experiment.setSamples(new LazyList<Sample>(new Callable<List<Sample>>() {
                @Override
                public List<Sample> call() throws Exception {
                    return atlasDAO.getSamplesByExperimentAccession(experiment);
                }
            }));

            return experiment;
        }
    }
}
