package uk.ac.ebi.gxa.dao;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import uk.ac.ebi.gxa.utils.LazyList;
import uk.ac.ebi.microarray.atlas.model.Assay;
import uk.ac.ebi.microarray.atlas.model.Asset;
import uk.ac.ebi.microarray.atlas.model.Experiment;
import uk.ac.ebi.microarray.atlas.model.Sample;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Callable;

public class ExperimentDAO extends AbstractDAO<Experiment> {
    public static final Logger log = LoggerFactory.getLogger(ExperimentDAO.class);
    private AtlasDAO adao;

    public ExperimentDAO(JdbcTemplate template) {
        super(template);
    }

    void setAtlasDAO(AtlasDAO adao) {
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

    public List<Experiment> getExperimentsByArrayDesignAccession(String accession) {
        return template.query(
                "SELECT " + ExperimentDAO.ExperimentMapper.FIELDS + " FROM a2_experiment " +
                        "WHERE experimentid IN " +
                        " (SELECT experimentid FROM a2_assay a" +
                        "    JOIN a2_arraydesign ad ON a.arraydesignid = ad.arraydesignid " +
                        "   WHERE ad.accession=?)",
                new Object[]{accession},
                new ExperimentDAO.ExperimentMapper(adao)
        );
    }

    int getTotalCount() {
        return template.queryForInt(
                "SELECT COUNT(*) FROM a2_experiment"
        );
    }

    @Override
    protected String sequence() {
        return "A2_EXPERIMENT_SEQ";
    }

    @Override
    protected void save(Experiment experiment) {
        final Date loadDate = experiment.getLoadDate();
        final int rowsCount;
        if (experiment.getId() == 0) {
            rowsCount = template.update(
                    "INSERT INTO a2_experiment (" +
                            "accession,description,performer,lab,loaddate,pmid," +
                            "abstract,releasedate,private,curated" +
                            ") VALUES (?,?,?,?,?,?,?,?,?,?)",
                    experiment.getAccession(),
                    experiment.getDescription(),
                    experiment.getPerformer(),
                    experiment.getLab(),
                    loadDate != null ? loadDate : new Date(),
                    experiment.getPubmedId(),
                    experiment.getAbstract(),
                    experiment.getReleaseDate(),
                    experiment.isPrivate(),
                    experiment.isCurated()
            );
        } else {
            rowsCount = template.update(
                    "UPDATE a2_experiment SET" +
                            " description = ?," +
                            " performer = ?," +
                            " lab = ?," +
                            " loaddate = ?," +
                            " pmid = ?," +
                            " abstract = ?," +
                            " releasedate = ?," +
                            " private = ?," +
                            " curated = ?" +
                            " WHERE experimentid = ?",
                    experiment.getDescription(),
                    experiment.getPerformer(),
                    experiment.getLab(),
                    loadDate != null ? loadDate : new Date(),
                    experiment.getPubmedId(),
                    experiment.getAbstract(),
                    experiment.getReleaseDate(),
                    experiment.isPrivate(),
                    experiment.isCurated(),
                    experiment.getId()
            );
        }
        log.info(rowsCount + " rows are updated in A2_EXPERIMENT table");
        final long id = template.queryForLong("SELECT experimentid FROM a2_experiment WHERE accession=?", experiment.getAccession());
        log.info("new experiment id = " + id);
    }

    public Experiment getExperimentByAccession(String accession) {
        try {
            return template.queryForObject(
                    "SELECT " + ExperimentMapper.FIELDS + " FROM a2_experiment WHERE accession=?",
                    new Object[]{accession},
                    new ExperimentDAO.ExperimentMapper(adao));
        } catch (IncorrectResultSizeDataAccessException e) {
            return null;
        }
    }

    public int getCountSince(String lastReleaseDate) {
        return template.queryForInt(
                "SELECT COUNT(*) FROM a2_experiment WHERE loaddate > to_date(?,'MM-YYYY')", lastReleaseDate
        );
    }

    public List<Experiment> getAllExperiments() {
        return template.query(
                "SELECT " + ExperimentMapper.FIELDS + " FROM a2_experiment " +
                        "ORDER BY (" +
                        "    case when loaddate is null " +
                        "        then (select min(loaddate) from a2_experiment) " +
                        "        else loaddate end) desc, " +
                        "    accession", new ExperimentMapper(adao));

    }

    @Deprecated
    public void delete(String experimentAccession) {
        // execute this procedure...
        /*
        PROCEDURE A2_EXPERIMENTDELETE(
          Accession varchar2
        )
        */
        SimpleJdbcCall procedure =
                new SimpleJdbcCall(template)
                        .withProcedureName("ATLASLDR.A2_EXPERIMENTDELETE")
                        .withoutProcedureColumnMetaDataAccess()
                        .useInParameterNames("ACCESSION")
                        .declareParameters(new SqlParameter("ACCESSION", Types.VARCHAR));

        // map parameters...
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("ACCESSION", experimentAccession);

        procedure.execute(params);

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
