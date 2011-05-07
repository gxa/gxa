package uk.ac.ebi.gxa.dao;

import oracle.jdbc.OracleTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.*;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.jdbc.core.support.AbstractSqlTypeValue;
import uk.ac.ebi.gxa.Experiment;
import uk.ac.ebi.microarray.atlas.model.Assay;
import uk.ac.ebi.microarray.atlas.model.Sample;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.*;

import static com.google.common.base.Joiner.on;
import static com.google.common.collect.Iterables.partition;
import static uk.ac.ebi.gxa.exceptions.LogUtil.createUnexpected;

public class SampleDAO extends AbstractDAO<Sample> {
    private static final Logger log = LoggerFactory.getLogger(SampleDAO.class);

    public static final int MAX_QUERY_PARAMS = 10;
    private final OrganismDAO odao;
    // TODO: 4alf: this is a templorary solution, we should have PropertyDAO instead
    private PropertyValueDAO pvdao;

    public SampleDAO(JdbcTemplate template, OrganismDAO odao, PropertyValueDAO pvdao) {
        super(template);
        this.odao = odao;
        this.pvdao = pvdao;
    }

    static SqlTypeValue convertAssayAccessionsToOracleARRAY(final Set<String> assayAccessions) {
        return new AbstractSqlTypeValue() {
            protected Object createTypeValue(Connection connection, int sqlType, String typeName) throws SQLException {
                Object[] accessions;
                if (assayAccessions != null && !assayAccessions.isEmpty()) {
                    accessions = new Object[assayAccessions.size()];
                    int i = 0;
                    for (String assayAccession : assayAccessions) {
                        accessions[i++] = assayAccession;
                    }

                    // created the array of STRUCTs, group into ARRAY
                    return AtlasDAO.createArray(connection, typeName, accessions);
                } else {
                    // throw an SQLException, as we cannot create a ARRAY with an empty array
                    throw new SQLException("Unable to create an ARRAY from an empty list of accessions");
                }
            }
        };
    }

    @Override
    protected String sequence() {
        return "A2_SAMPLE_SEQ";
    }

    @Override
    protected void save(Sample sample) {
        int rows = template.update("insert into a2_sample (" + SampleMapper.CLEAN_FIELDS + ") " +
                "values (?, ?, ?, ?)", sample.getSampleID(), sample.getAccession(), sample.getOrganism().getId(), sample.getChannel());
        if (rows != 1)
            throw createUnexpected("Cannot overwrite " + sample + " - organisms are supposed to be immutable");
    }

    @Override
    public Sample getById(long id) {
        return template.queryForObject("select " + SampleMapper.FIELDS + " from a2_sample s " +
                "where s.sampleid = ?",
                new Object[]{id},
                new SampleMapper());
    }

    public List<Sample> getByAssay(Assay assay) {
        return template.query("select " + SampleMapper.FIELDS + " from a2_sample s " +
                "join a2_assaysample ass on s.sampleid = ass.sampleid " +
                "where ass.assayid = ?",
                new Object[]{assay.getAssayID()},
                new SampleMapper());
    }

    public List<Sample> getSamplesByExperimentAccession(Experiment experiment) {
        List<Sample> samples = template.query("SELECT " + SampleMapper.FIELDS +
                " FROM a2_sample s, a2_assay a, a2_assaysample ass, a2_experiment e, a2_organism org " +
                "WHERE s.sampleid=ass.sampleid " +
                "AND a.assayid=ass.assayid " +
                "AND a.experimentid=e.experimentid " +
                "AND s.organismid=org.organismid " +
                "AND e.accession=?", new Object[]{experiment.getAccession()}, new SampleMapper());
        // populate the other info for these samples
        if (samples.size() > 0) {
            fillOutSamples(samples);
        }
        return samples;
    }


    private void fillOutSamples(List<Sample> samples) {
        // map samples to sample id
        final Map<Long, Sample> samplesByID = new HashMap<Long, Sample>();
        for (Sample sample : samples) {
            samplesByID.put(sample.getSampleID(), sample);
        }

        // maps properties and assays to relevant sample
        RowCallbackHandler assaySampleMapper = new RowCallbackHandler() {
            public void processRow(ResultSet rs) throws SQLException {
                long sampleID = rs.getLong(1);
                samplesByID.get(sampleID).addAssayAccession(rs.getString(2));
            }
        };
        ObjectPropertyMapper samplePropertyMapper = new ObjectPropertyMapper(samplesByID, pvdao);

        // query template for samples
        NamedParameterJdbcTemplate namedTemplate = new NamedParameterJdbcTemplate(template);

        // if we have more than 'MAX_QUERY_PARAMS' samples, split into smaller queries
        List<Long> sampleIDs = new ArrayList<Long>(samplesByID.keySet());
        for (List<Long> sampleIDsChunk : partition(sampleIDs, MAX_QUERY_PARAMS)) {
            // now query for assays that map to one of these samples
            MapSqlParameterSource assayParams = new MapSqlParameterSource();
            assayParams.addValue("sampleids", sampleIDsChunk);
            namedTemplate.query("SELECT s.sampleid, a.accession " +
                    "FROM a2_assay a, a2_assaysample s " +
                    "WHERE a.assayid=s.assayid " +
                    "AND s.sampleid IN (:sampleids)", assayParams, assaySampleMapper);

            // now query for properties that map to one of these samples
            log.trace("Querying for properties where sample IN (" + on(',').join(sampleIDsChunk) + ")");
            MapSqlParameterSource propertyParams = new MapSqlParameterSource();
            propertyParams.addValue("sampleids", sampleIDsChunk);
            namedTemplate.query("SELECT spv.samplepvid," +
                    "        spv.sampleid,\n" +
                    "        spv.propertyvalueid AS propertyvalue, \n" +
                    "        wm_concat(t.accession) AS efoTerms\n" +
                    "  FROM a2_samplepv spv \n" +
                    "          LEFT JOIN a2_samplepvontology spvo ON spvo.SamplePVID = spv.SAMPLEPVID\n" +
                    "          LEFT JOIN a2_ontologyterm t ON spvo.ontologytermid = t.ontologytermid\n" +
                    " WHERE spv.sampleid IN (:sampleids)" +
                    "  GROUP BY spv.samplepvid, spvo.SamplePVID, spv.SAMPLEID, spv.propertyvalueid ", propertyParams, samplePropertyMapper);
        }
    }

    public List<Sample> getSamplesByAssayAccession(String experimentAccession, String assayAccession) {
        List<Sample> samples = template.query("SELECT " + SampleMapper.FIELDS +
                " FROM a2_sample s, a2_assay a, a2_assaysample ass, a2_experiment e, a2_organism org " +
                "WHERE s.sampleid=ass.sampleid " +
                "AND a.assayid=ass.assayid " +
                "AND e.experimentid=a.experimentid " +
                "AND s.organismid=org.organismid " +
                "AND e.accession=? " +
                "AND a.accession=? ", new Object[]{experimentAccession, assayAccession}, new SampleMapper());
        // populate the other info for these samples
        if (samples.size() > 0) {
            fillOutSamples(samples);
        }
        return samples;
    }

    @Deprecated
    public void save(Sample sample, String experimentAccession) {
        // execute this procedure...
        /*
        PROCEDURE "A2_SAMPLESET" (
            p_Accession varchar2
          , p_Assays AccessionTable
          , p_Properties PropertyTable
          , p_Species varchar2
          , p_Channel varchar2
        )
        */
        SimpleJdbcCall procedure =
                new SimpleJdbcCall(template)
                        .withProcedureName("ATLASLDR.A2_SAMPLESET")
                        .withoutProcedureColumnMetaDataAccess()
                        .useInParameterNames("EXPERIMENTACCESSION")
                        .useInParameterNames("SAMPLEACCESSION")
                        .useInParameterNames("ASSAYS")
                        .useInParameterNames("PROPERTIES")
                        .useInParameterNames("CHANNEL")
                        .declareParameters(
                                new SqlParameter("EXPERIMENTACCESSION", Types.VARCHAR))
                        .declareParameters(
                                new SqlParameter("SAMPLEACCESSION", Types.VARCHAR))
                        .declareParameters(
                                new SqlParameter("ASSAYS", OracleTypes.ARRAY, "ACCESSIONTABLE"))
                        .declareParameters(
                                new SqlParameter("PROPERTIES", OracleTypes.ARRAY, "PROPERTYTABLE"))
                        .declareParameters(
                                new SqlParameter("CHANNEL", Types.VARCHAR));

        // map parameters...
        MapSqlParameterSource params = new MapSqlParameterSource();
        SqlTypeValue accessionsParam = sample.getAssayAccessions().isEmpty() ? null :
                convertAssayAccessionsToOracleARRAY(sample.getAssayAccessions());
        SqlTypeValue propertiesParam = sample.hasNoProperties() ? null
                : AtlasDAO.convertPropertiesToOracleARRAY(sample.getProperties());

        params.addValue("EXPERIMENTACCESSION", experimentAccession)
                .addValue("SAMPLEACCESSION", sample.getAccession())
                .addValue("ASSAYS", accessionsParam, OracleTypes.ARRAY, "ACCESSIONTABLE")
                .addValue("PROPERTIES", propertiesParam, OracleTypes.ARRAY, "PROPERTYTABLE")
                .addValue("CHANNEL", sample.getChannel());

        int assayCount = sample.getAssayAccessions().size();
        int propertiesCount = sample.getPropertiesCount();
        log.debug("Invoking A2_SAMPLESET with the following parameters..." +
                "\n\texperiment accession: {}" +
                "\n\tsample accession:     {}" +
                "\n\tassays count:         {}" +
                "\n\tproperties count:     {}" +
                "\n\tspecies:              {}" +
                "\n\tchannel:              {}",
                new Object[]{experimentAccession, sample.getAccession(), assayCount, propertiesCount,
                        sample.getOrganism().getName(),
                        sample.getChannel()});

        // and execute
        procedure.execute(params);
    }

    private class SampleMapper implements RowMapper<Sample> {
        private static final String CLEAN_FIELDS = "SAMPLEID, ACCESSION, ORGANISMID, CHANNEL";
        private static final String FIELDS = "s.SAMPLEID, s.ACCESSION, s.ORGANISMID, s.CHANNEL";

        public Sample mapRow(ResultSet rs, int i) throws SQLException {
            return new Sample(rs.getLong(1), rs.getString(2), odao.getById(rs.getLong(3)), rs.getString(4));
        }
    }
}
