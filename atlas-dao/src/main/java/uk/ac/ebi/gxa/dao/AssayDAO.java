package uk.ac.ebi.gxa.dao;

import oracle.jdbc.OracleTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.SqlTypeValue;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import uk.ac.ebi.gxa.utils.LazyList;
import uk.ac.ebi.microarray.atlas.model.Assay;
import uk.ac.ebi.microarray.atlas.model.Experiment;
import uk.ac.ebi.microarray.atlas.model.Property;
import uk.ac.ebi.microarray.atlas.model.Sample;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;
import java.util.concurrent.Callable;

public class AssayDAO extends AbstractDAO<Assay> {
    public static final Logger log = LoggerFactory.getLogger(AssayDAO.class);

    private final ExperimentDAO edao;
    private final ArrayDesignDAO addao;
    private final SampleDAO sdao;
    private final ObjectPropertyDAO opdao;

    public AssayDAO(JdbcTemplate template, ExperimentDAO edao, ArrayDesignDAO addao, SampleDAO sdao, ObjectPropertyDAO opdao) {
        super(template);
        this.edao = edao;
        this.addao = addao;
        this.sdao = sdao;
        opdao.setOwnerDAO(this);
        this.opdao = opdao;
    }

    @Override
    protected String sequence() {
        return "A2_ASSAY_SEQ";
    }

    @Override
    protected void save(Assay assay) {
        // execute this procedure...
        /*
        PROCEDURE "A2_ASSAYSET" (
           TheAccession varchar2
          ,TheExperimentAccession  varchar2
          ,TheArrayDesignAccession varchar2
          ,TheProperties PropertyTable
        )
        */
        SimpleJdbcCall procedure =
                new SimpleJdbcCall(template)
                        .withProcedureName("ATLASLDR.A2_ASSAYSET")
                        .withoutProcedureColumnMetaDataAccess()
                        .useInParameterNames("ACCESSION")
                        .useInParameterNames("EXPERIMENTACCESSION")
                        .useInParameterNames("ARRAYDESIGNACCESSION")
                        .useInParameterNames("PROPERTIES")
                        .declareParameters(
                                new SqlParameter("ACCESSION", Types.VARCHAR))
                        .declareParameters(
                                new SqlParameter("EXPERIMENTACCESSION", Types.VARCHAR))
                        .declareParameters(
                                new SqlParameter("ARRAYDESIGNACCESSION", Types.VARCHAR))
                        .declareParameters(
                                new SqlParameter("PROPERTIES", OracleTypes.ARRAY, "PROPERTYTABLE"));

        // map parameters...
        List<Property> props = assay.getProperties();
        MapSqlParameterSource params = new MapSqlParameterSource();

        StringBuffer sb = new StringBuffer();
        sb.append("Properties listing for ").append(assay.getAccession()).append(":\n");
        for (Property p : props) {
            sb.append("\t").append(p.getName()).append("\t\t->\t\t").append(p.getValue()).append("\n");
        }
        log.debug(sb.toString());

        SqlTypeValue propertiesParam =
                props.isEmpty() ? null :
                        AtlasDAO.convertPropertiesToOracleARRAY(props);

        params.addValue("ACCESSION", assay.getAccession())
                .addValue("EXPERIMENTACCESSION", assay.getExperiment().getAccession())
                .addValue("ARRAYDESIGNACCESSION", assay.getArrayDesign().getAccession())
                .addValue("PROPERTIES", propertiesParam, OracleTypes.ARRAY, "PROPERTYTABLE");

        log.debug("Invoking A2_ASSAYSET with the following parameters..." +
                "\n\tassay accession:          {}" +
                "\n\texperiment:               {}" +
                "\n\tarray design:             {}" +
                "\n\tproperties count:         {}" +
                "\n\texpression value count:   {}",
                new Object[]{assay.getAccession(), assay.getExperiment().getAccession(), assay.getArrayDesign().getAccession(),
                        props.size(), 0});

        // and execute
        procedure.execute(params);
    }

    @Override
    protected Assay loadById(long id) {
        return template.queryForObject("select " + AssayMapper.FIELDS + " from a2_assay a " +
                "where a.assayid = ?",
                new Object[]{id},
                new AssayMapper());
    }

    public int getTotalCount() {
        return template.queryForInt("SELECT COUNT(*) FROM a2_assay");
    }

    public List<Assay> getAssaysByExperiment(final Experiment experiment) {
        return template.query("SELECT " + AssayMapper.FIELDS + " " +
                "  FROM a2_assay a " +
                "  JOIN a2_experiment e ON e.experimentid = a.experimentid " +
                " WHERE e.accession=?",
                new Object[]{experiment.getAccession()},
                new AssayMapper());
    }


    private class AssayMapper implements RowMapper<Assay> {
        private static final String FIELDS = "a.ASSAYID, a.ACCESSION, a.EXPERIMENTID, a.ARRAYDESIGNID";

        public Assay mapRow(ResultSet rs, int i) throws SQLException {
            final Assay assay = new Assay(rs.getLong(1), rs.getString(2), edao.getById(rs.getLong(3)), addao.getById(rs.getLong(4)));
            registerObject(assay.getId(), assay);
            assay.setSamples(new LazyList<Sample>(new Callable<List<Sample>>() {
                @Override
                public List<Sample> call() throws Exception {
                    return sdao.getByAssay(assay);
                }
            }));
            assay.setProperties(new LazyList<Property>(new Callable<List<Property>>() {
                @Override
                public List<Property> call() throws Exception {
                    return opdao.getByOwner(assay);
                }
            }));
            return assay;
        }
    }
}
