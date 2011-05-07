package uk.ac.ebi.gxa.dao;

import oracle.jdbc.OracleTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.SqlTypeValue;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import uk.ac.ebi.gxa.utils.LazyList;
import uk.ac.ebi.microarray.atlas.model.*;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import static com.google.common.collect.Iterables.partition;

public class AssayDAO extends AbstractDAO<Assay> {
    public static final Logger log = LoggerFactory.getLogger(AssayDAO.class);
    public static final int MAX_QUERY_PARAMS = 10;

    private final ExperimentDAO edao;
    private final ArrayDesignDAO addao;
    private final SampleDAO sdao;
    private PropertyValueDAO pvdao;

    public AssayDAO(JdbcTemplate template, ExperimentDAO edao, ArrayDesignDAO addao, SampleDAO sdao, PropertyValueDAO pvdao) {
        super(template);
        this.edao = edao;
        this.addao = addao;
        this.sdao = sdao;
        this.pvdao = pvdao;
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

//
//        int rows = template.update("insert into a2_assay (" + AssayMapper.FIELDS + ") " +
//                "values (?, ?, ?, ?)", o.getAssayID(), o.getAccession(),
//                o.getExperiment().getId(), o.getArrayDesign().getArrayDesignID());
//        if (rows != 1)
//            throw createUnexpected("Cannot overwrite " + o + " - assays are supposed to be immutable");
    }

    @Override
    public Assay getById(long id) {
        return template.queryForObject("select " + AssayMapper.FIELDS + " from a2_assay " +
                "where assayid = ?",
                new Object[]{id},
                new AssayMapper());
    }

    public int getTotalCount() {
        return template.queryForInt("SELECT COUNT(*) FROM a2_assay");
    }

    public List<Assay> getAssaysByExperiment(final Experiment experiment) {
        List<Assay> assays = template.query(
                "SELECT a.accession, ad.accession, a.assayid " +
                        "FROM a2_assay a, a2_experiment e, a2_arraydesign ad " +
                        "WHERE e.experimentid=a.experimentid " +
                        "AND a.arraydesignid=ad.arraydesignid" + " " +
                        "AND e.accession=?",
                new Object[]{experiment.getAccession()},
                new RowMapper<Assay>() {
                    public Assay mapRow(ResultSet resultSet, int i) throws SQLException {
                        final Assay assay = new Assay(resultSet.getString(1));
                        assay.setExperiment(edao.getExperimentByAccession(experiment.getAccession()));
                        assay.setArrayDesign(new ArrayDesign(resultSet.getString(2)));
                        assay.setAssayID(resultSet.getLong(3));

                        return assay;
                    }
                }
        );

        // populate the other info for these assays
        if (!assays.isEmpty()) {
            fillOutAssays(assays);
        }

        // and return
        return assays;
    }


    private void fillOutAssays(List<Assay> assays) {
        // map assays to assay id
        Map<Long, Assay> assaysByID = new HashMap<Long, Assay>();
        for (Assay assay : assays) {
            // TODO: 4alf: this is a quick hack to stop the app from dying. Remove it asap.
            assay.setSamples(new ArrayList<Sample>());
            // index this assay
            assaysByID.put(assay.getAssayID(), assay);
        }

        // maps properties to assays
        ObjectPropertyMapper assayPropertyMapper = new ObjectPropertyMapper(assaysByID, pvdao);

        // query template for assays
        NamedParameterJdbcTemplate namedTemplate = new NamedParameterJdbcTemplate(template);

        // if we have more than 'MAX_QUERY_PARAMS' assays, split into smaller queries
        final ArrayList<Long> assayIds = new ArrayList<Long>(assaysByID.keySet());
        for (List<Long> assayIDsChunk : partition(assayIds, MAX_QUERY_PARAMS)) {
            // now query for properties that map to one of the samples in the sublist
            MapSqlParameterSource propertyParams = new MapSqlParameterSource();
            propertyParams.addValue("assayids", assayIDsChunk);
            namedTemplate.query("SELECT apv.assaypvid, " +
                    "        apv.assayid,\n" +
                    "        apv.propertyvalueid AS propertyvalue,\n" +
                    "        wm_concat(t.accession) AS efoTerms\n" +
                    "  FROM a2_assaypv apv \n" +
                    "          LEFT JOIN a2_assaypvontology apvo ON apvo.assaypvid = apv.assaypvid\n" +
                    "          LEFT JOIN a2_ontologyterm t ON apvo.ontologytermid = t.ontologytermid\n" +
                    " WHERE apv.assayid IN (:assayids)" +
                    "  GROUP BY apv.assaypvid, apvo.assaypvid, apv.assayid, apv.propertyvalueid", propertyParams, assayPropertyMapper);
        }
    }


    private class AssayMapper implements RowMapper<Assay> {
        private static final String FIELDS = "ASSAYID, ACCESSION, EXPERIMENTID, ARRAYDESIGNID";

        public Assay mapRow(ResultSet rs, int i) throws SQLException {
            final Assay assay = new Assay(rs.getLong(1), rs.getString(2), edao.getById(rs.getLong(3)), addao.getById(rs.getLong(4)));
            assay.setSamples(new LazyList<Sample>(new Callable<List<Sample>>() {
                @Override
                public List<Sample> call() throws Exception {
                    return sdao.getByAssay(assay);
                }
            }));
            return assay;
        }
    }
}
