/*
 * Copyright 2008-2010 Microarray Informatics Team, EMBL-European Bioinformatics Institute
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *
 * For further details of the Gene Expression Atlas project, including source code,
 * downloads and documentation, please see:
 *
 * http://gxa.github.com/gxa
 */

package uk.ac.ebi.gxa.dao;

import oracle.jdbc.OracleTypes;
import oracle.sql.ARRAY;
import oracle.sql.ArrayDescriptor;
import oracle.sql.STRUCT;
import oracle.sql.StructDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.SingleColumnRowMapper;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.SqlTypeValue;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.jdbc.core.support.AbstractSqlTypeValue;
import uk.ac.ebi.gxa.utils.Pair;
import uk.ac.ebi.microarray.atlas.model.ArrayDesign;
import uk.ac.ebi.microarray.atlas.model.ArrayDesignBundle;
import uk.ac.ebi.microarray.atlas.model.Assay;
import uk.ac.ebi.microarray.atlas.model.AtlasStatistics;
import uk.ac.ebi.microarray.atlas.model.Experiment;
import uk.ac.ebi.microarray.atlas.model.ExpressionAnalysis;
import uk.ac.ebi.microarray.atlas.model.LoadDetails;
import uk.ac.ebi.microarray.atlas.model.ObjectWithProperties;
import uk.ac.ebi.microarray.atlas.model.OntologyMapping;
import uk.ac.ebi.microarray.atlas.model.Property;
import uk.ac.ebi.microarray.atlas.model.Sample;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.Joiner.on;
import static java.util.Collections.nCopies;
import static uk.ac.ebi.gxa.utils.CollectionUtil.asChunks;

/**
 * A data access object designed for retrieving common sorts of data from the atlas database.  This DAO should be
 * configured with a spring {@link JdbcTemplate} object which will be used to query the database.
 *
 * @author Tony Burdett
 */
@SuppressWarnings("unchecked")
public class AtlasDAO extends AbstractAtlasDAO {
    // load monitor
    public static final String EXPERIMENT_LOAD_MONITOR_SELECT =
            "SELECT status FROM load_monitor " +
                    "WHERE load_type='experiment'";
    public static final String ARRAY_LOAD_MONITOR_SELECT =
            "SELECT status FROM load_monitor " +
                    "WHERE load_type='arraydesign'";
    public static final String EXPERIMENT_LOAD_MONITOR_BY_ACC_SELECT =
            EXPERIMENT_LOAD_MONITOR_SELECT + " " +
                    "AND accession=?";
    public static final String ARRAY_LOAD_MONITOR_BY_ACC_SELECT =
            ARRAY_LOAD_MONITOR_SELECT + " " +
                    "AND accession=?";

    // experiment queries
    public static final String EXPERIMENTS_COUNT =
            "SELECT COUNT(*) FROM a2_experiment";

    public static final String NEW_EXPERIMENTS_COUNT =
            "SELECT COUNT(*) FROM a2_experiment WHERE loaddate > to_date(?,'MM-YYYY')";

    // The following query does not use NULLS LAST as Hypersonic database used in TestAtlasDAO throws Bad sql grammar exception
    // if 'NULLS LAST' is used in queries
    public static final String EXPERIMENTS_SELECT =
            "SELECT accession, description, performer, lab, experimentid, loaddate, pmid, abstract, releasedate FROM a2_experiment " +
                    "ORDER BY (case when loaddate is null then (select min(loaddate) from a2_experiment) else loaddate end) desc, accession";

    public static final String EXPERIMENTS_PENDING_INDEX_SELECT =
            "SELECT e.accession, e.description, e.performer, e.lab, e.experimentid, e.loaddate, e.pmid, abstract, releasedate " +
                    "FROM a2_experiment e, load_monitor lm " +
                    "WHERE e.accession=lm.accession " +
                    "AND (lm.searchindex='pending' OR lm.searchindex='failed') " +
                    "AND lm.load_type='experiment'";
    public static final String EXPERIMENTS_PENDING_NETCDF_SELECT =
            "SELECT e.accession, e.description, e.performer, e.lab, e.experimentid, e.loaddate, e.pmid, abstract, releasedate " +
                    "FROM a2_experiment e, load_monitor lm " +
                    "WHERE e.accession=lm.accession " +
                    "AND (lm.netcdf='pending' OR lm.netcdf='failed') " +
                    "AND lm.load_type='experiment'";
    public static final String EXPERIMENT_BY_ACC_SELECT =
            "SELECT accession, description, performer, lab, experimentid, loaddate, pmid, abstract, releasedate " +
                    "FROM a2_experiment WHERE accession=?";
    public static final String EXPERIMENT_BY_ID_SELECT =
            "SELECT accession, description, performer, lab, experimentid, loaddate, pmid, abstract, releasedate " +
                    "FROM a2_experiment WHERE experimentid=?";
    public static final String EXPERIMENT_BY_ACC_SELECT_ASSETS = //select all assets (pictures, etc.)
            "SELECT a.name, a.filename, a.description" +
                    " FROM a2_experiment e " +
                    " JOIN a2_experimentasset a ON a.ExperimentID = e.ExperimentID " +
                    " WHERE e.accession=? ORDER BY a.ExperimentAssetID";
    public static final String EXPERIMENTS_BY_ARRAYDESIGN_SELECT =
            "SELECT accession, description, performer, lab, experimentid, loaddate, pmid, abstract, releasedate " +
                    "FROM a2_experiment " +
                    "WHERE experimentid IN " +
                    " (SELECT experimentid FROM a2_assay a, a2_arraydesign ad " +
                    "  WHERE a.arraydesignid=ad.arraydesignid AND ad.accession=?)";
    public static final String EXPERIMENTS_TO_ALL_PROPERTIES_SELECT =
            "SELECT experiment, property, value, ontologyterm from cur_ontologymapping " +
                    "UNION " +
                    "SELECT distinct ap.experiment, ap.property, ap.value, null " +
                    "FROM cur_assayproperty ap where not exists " +
                    "(SELECT 1 from cur_ontologymapping cm " +
                    "WHERE cm.property = ap.property " +
                    "AND cm.value = ap.value " +
                    "AND cm.experiment = ap.experiment)";

    public static final String GENES_COUNT =
            "select count(be.bioentityid) \n" +
                    "from a2_bioentity be \n" +
                    "join a2_bioentitytype bet on bet.bioentitytypeid = be.bioentitytypeid\n" +
                    "where bet.id_for_index = 1";

    // assay queries
    public static final String ASSAYS_COUNT =
            "SELECT COUNT(*) FROM a2_assay";

    public static final String ASSAYS_BY_EXPERIMENT_ACCESSION =
            "SELECT a.accession, e.accession, ad.accession, a.assayid " +
                    "FROM a2_assay a, a2_experiment e, a2_arraydesign ad " +
                    "WHERE e.experimentid=a.experimentid " +
                    "AND a.arraydesignid=ad.arraydesignid" + " " +
                    "AND e.accession=?";
    public static final String ASSAYS_BY_RELATED_SAMPLES =
            "SELECT s.sampleid, a.accession " +
                    "FROM a2_assay a, a2_assaysample s " +
                    "WHERE a.assayid=s.assayid " +
                    "AND s.sampleid IN (:sampleids)";
    public static final String PROPERTIES_BY_RELATED_ASSAYS =
            "SELECT apv.assayid,\n" +
                    "        p.name AS property,\n" +
                    "        pv.name AS propertyvalue, apv.isfactorvalue,\n" +
                    "        wm_concat(t.accession) AS efoTerms\n" +
                    "  FROM a2_property p\n" +
                    "          JOIN a2_propertyvalue pv ON pv.propertyid=p.propertyid\n" +
                    "          JOIN a2_assaypv apv ON apv.propertyvalueid=pv.propertyvalueid\n" +
                    "          LEFT JOIN a2_assaypvontology apvo ON apvo.assaypvid = apv.assaypvid\n" +
                    "          LEFT JOIN a2_ontologyterm t ON apvo.ontologytermid = t.ontologytermid\n" +
                    " WHERE apv.assayid IN (:assayids)" +
                    "  GROUP BY apvo.assaypvid, apv.assayid, p.name, pv.name, apv.isfactorvalue";

    // sample queries
    public static final String SAMPLES_BY_ASSAY_ACCESSION =
            "SELECT s.accession, A2_SampleOrganism(s.sampleid) species, s.channel, s.sampleid " +
                    "FROM a2_sample s, a2_assay a, a2_assaysample ass, a2_experiment e " +
                    "WHERE s.sampleid=ass.sampleid " +
                    "AND a.assayid=ass.assayid " +
                    "AND e.experimentid=a.experimentid " +
                    "AND e.accession=? " +
                    "AND a.accession=?";
    public static final String SAMPLES_BY_EXPERIMENT_ACCESSION =
            "SELECT s.accession, A2_SampleOrganism(s.sampleid) species, s.channel, s.sampleid " +
                    "FROM a2_sample s, a2_assay a, a2_assaysample ass, a2_experiment e " +
                    "WHERE s.sampleid=ass.sampleid " +
                    "AND a.assayid=ass.assayid " +
                    "AND a.experimentid=e.experimentid " +
                    "AND e.accession=?";
    public static final String PROPERTIES_BY_RELATED_SAMPLES =
            "SELECT spv.sampleid,\n" +
                    "        p.name AS property,\n" +
                    "        pv.name AS propertyvalue, spv.isfactorvalue,\n" +
                    "        wm_concat(t.accession) AS efoTerms\n" +
                    "  FROM a2_property p\n" +
                    "          JOIN a2_propertyvalue pv ON pv.propertyid=p.propertyid\n" +
                    "          JOIN a2_samplepv spv ON spv.propertyvalueid=pv.propertyvalueid\n" +
                    "          LEFT JOIN a2_samplepvontology spvo ON spvo.SamplePVID = spv.SAMPLEPVID\n" +
                    "          LEFT JOIN a2_ontologyterm t ON spvo.ontologytermid = t.ontologytermid\n" +
                    " WHERE spv.sampleid IN (:sampleids)" +
                    "  GROUP BY spvo.SamplePVID, spv.SAMPLEID, p.name, pv.name, spv.isfactorvalue ";

    // query for counts, for statistics
    public static final String PROPERTY_VALUE_COUNT_SELECT =
            "SELECT COUNT(DISTINCT name) FROM a2_propertyvalue";

    // query for counts, for statistics
    public static final String FACTOR_VALUE_COUNT_SELECT =
            "SELECT COUNT(DISTINCT propertyvalueid) FROM vwexperimentfactors";

  

    public static final String EXPRESSIONANALYTICS_FOR_GENEIDS =
            "SELECT geneid, ef, efv, experimentid, designelementid, tstat, pvaladj, efid, efvid FROM VWEXPRESSIONANALYTICSBYGENE " +
                    "WHERE geneid IN (:geneids)";

    public static final String ONTOLOGY_MAPPINGS_BY_ONTOLOGY_NAME =
            "SELECT DISTINCT accession, property, propertyvalue, ontologyterm, experimentid " +
                    "FROM a2_ontologymapping" + " " +
                    "WHERE ontologyname=?";

    public static final String EXPERIMENT_RELEASEDATE_UPDATE = "Update a2_experiment set releasedate = (select sysdate from dual) where accession = ?";

    public static final String PROPERTIES_ALL =
            "SELECT min(p.propertyid), p.name, min(pv.propertyvalueid), pv.name, 1 as isfactorvalue " +
                    "FROM a2_property p, a2_propertyvalue pv " +
                    "WHERE  pv.propertyid=p.propertyid GROUP BY p.name, pv.name";


    private int maxQueryParams = 500;

    private Logger log = LoggerFactory.getLogger(getClass());

    private ArrayDesignDAO arrayDesignDAO;

    /**
     * Get the maximum allowed number of parameters that can be supplied to a parameterised query.  This is effectively
     * the maximum bound for an "IN" list - i.e. SELECT * FROM foo WHERE foo.bar IN (?,?,?,...,?).  If unset, this
     * defaults to 500.  Typically, the limit for oracle databases is 1000.  If, for any query that takes a list, the
     * size of the list is greater than this value, the query will be split into several smaller subqueries and the
     * results aggregated.  As a user, you should not notice any difference.
     *
     * @return the maximum bound on the query list size
     */
    public int getMaxQueryParams() {
        return maxQueryParams;
    }

    /*
   DAO read methods
    */

    public List<Pair<String, String>> getExperimentFactorsAndValuesByOntologyTerm(String efo) {
        final List<Pair<String, String>> mappedEfs = new ArrayList<Pair<String, String>>();
        template.query("select distinct Property, Value from CUR_ONTOLOGYMAPPING where ONTOLOGYTERM LIKE ?",
                new Object[]{efo}, new RowCallbackHandler() {
                    public void processRow(ResultSet rs) {
                        try {
                            mappedEfs.add(Pair.create(rs.getString("Property"), rs.getString("Value")));
                        } catch (Exception ex) {
                            System.out.println(ex.getMessage());
                        }
                    }
                });
        return mappedEfs;
    }

    public List<LoadDetails> getLoadDetailsForExperiments() {
        List results = template.query(EXPERIMENT_LOAD_MONITOR_SELECT,
                new LoadDetailsMapper());
        return (List<LoadDetails>) results;
    }

    public LoadDetails getLoadDetailsForExperimentsByAccession(String accession) {
        return getLoadDetails(EXPERIMENT_LOAD_MONITOR_BY_ACC_SELECT, accession);
    }

    private LoadDetails getLoadDetails(String query, String accession) {
        List results = template.query(query,
                new Object[]{accession},
                new LoadDetailsMapper());
        return results.size() > 0 ? (LoadDetails) results.get(0) : null;
    }

    public LoadDetails getLoadDetailsForArrayDesignsByAccession(String accession) {
        return getLoadDetails(ARRAY_LOAD_MONITOR_BY_ACC_SELECT, accession);
    }

    public List<Experiment> getAllExperiments() {
        return getExperiments(EXPERIMENTS_SELECT);
    }

    public List<Experiment> getAllExperimentsPendingIndexing() {
        return getExperiments(EXPERIMENTS_PENDING_INDEX_SELECT);
    }

    public List<Experiment> getAllExperimentsPendingNetCDFs() {
        return getExperiments(EXPERIMENTS_PENDING_NETCDF_SELECT);
    }

    private List<Experiment> getExperiments(String select) {
        List results = template.query(select, new ExperimentMapper());
        loadExperimentAssets(results);
        return (List<Experiment>) results;
    }

    /**
     * Gets a single experiment from the Atlas Database, queried by the accession of the experiment.
     *
     * @param accession the experiment's accession number (usually in the format E-ABCD-1234)
     * @return an object modelling this experiment
     */
    public Experiment getExperimentByAccession(String accession) {
        List results = template.query(EXPERIMENT_BY_ACC_SELECT,
                new Object[]{accession},
                new ExperimentMapper());

        if (results.size() > 0) {
            loadExperimentAssets(results);
            return (Experiment) results.get(0);
        } else {
            return null;
        }
    }

    /**
     * @param experimentId id of experiment to retrieve
     * @return Experiment (without assets) matching experimentId
     */
    public Experiment getShallowExperimentById(Long experimentId) {
        List results = template.query(EXPERIMENT_BY_ID_SELECT,
                new Object[]{experimentId},
                new ExperimentMapper());

        if (results.size() == 0) {
            return null;
        }
        return (Experiment) results.get(0);
    }

    private void loadExperimentAssets(List results) {
        for (Object experiment : results) {
            ((Experiment) experiment).addAssets(template.query(EXPERIMENT_BY_ACC_SELECT_ASSETS,
                    new Object[]{((Experiment) experiment).getAccession()},
                    new ExperimentAssetMapper()));
        }
    }

    public List<Experiment> getExperimentByArrayDesign(String accession) {
        List results = template.query(EXPERIMENTS_BY_ARRAYDESIGN_SELECT,
                new Object[]{accession},
                new ExperimentMapper());
        loadExperimentAssets(results);
        return results;
    }


    public List<Assay> getAssaysByExperimentAccession(
            String experimentAccession) {
        List results = template.query(ASSAYS_BY_EXPERIMENT_ACCESSION,
                new Object[]{experimentAccession},
                new AssayMapper());

        List<Assay> assays = (List<Assay>) results;

        // populate the other info for these assays
        if (assays.size() > 0) {
            fillOutAssays(assays);
        }

        // and return
        return assays;
    }

    public List<Sample> getSamplesByAssayAccession(String experimentAccession, String assayAccession) {
        return loadSamples(SAMPLES_BY_ASSAY_ACCESSION, experimentAccession, assayAccession);
    }

    public List<Sample> getSamplesByExperimentAccession(String exptAccession) {
        return loadSamples(SAMPLES_BY_EXPERIMENT_ACCESSION, exptAccession);
    }

    private List<Sample> loadSamples(String query, Object... args) {
        List results = template.query(query, args, new SampleMapper());
        List<Sample> samples = (List<Sample>) results;
        // populate the other info for these samples
        if (samples.size() > 0) {
            fillOutSamples(samples);
        }
        return samples;
    }

    public int getPropertyValueCount() {
        return template.queryForInt(PROPERTY_VALUE_COUNT_SELECT);
    }

    public int getFactorValueCount() {
        return template.queryForInt(FACTOR_VALUE_COUNT_SELECT);
    }

    /**
     * Returns all array designs in the underlying datasource.  Note that, to reduce query times, this method does NOT
     * prepopulate ArrayDesigns with their associated design elements (unlike other methods to retrieve array designs
     * more specifically).
     *
     * @return the list of array designs, not prepopulated with design elements.
     */
    public List<ArrayDesign> getAllArrayDesigns() {
        return getArrayDesignDAO().getAllArrayDesigns();
    }

    public ArrayDesign getArrayDesignByAccession(String accession) {
        return getArrayDesignDAO().getArrayDesignByAccession(accession);
    }

    /**
     * @param accession Array design accession
     * @return Array design (with no design element and gene ids filled in) corresponding to accession
     */
    public ArrayDesign getArrayDesignShallowByAccession(String accession) {
        return getArrayDesignDAO().getArrayDesignShallowByAccession(accession);
    }

    public Map<Long, List<ExpressionAnalysis>> getExpressionAnalyticsForGeneIDs(
            final List<Long> geneIDs) {

        final Map<Long, List<ExpressionAnalysis>> result = new HashMap<Long, List<ExpressionAnalysis>>(geneIDs.size());
        NamedParameterJdbcTemplate namedTemplate = new NamedParameterJdbcTemplate(template);

        final int chunksize = getMaxQueryParams();
        for (List<Long> genelist : asChunks(geneIDs, chunksize)) {
            // now query for properties that map to one of these genes
            MapSqlParameterSource propertyParams = new MapSqlParameterSource();
            propertyParams.addValue("geneids", genelist);
            namedTemplate.query(EXPRESSIONANALYTICS_FOR_GENEIDS, propertyParams,
                    new RowMapper() {

                        public Object mapRow(ResultSet resultSet, int i) throws SQLException {
                            Long geneid = resultSet.getLong("geneid");

                            if (!result.containsKey(geneid)) {
                                result.put(geneid, new ArrayList<ExpressionAnalysis>());
                            }

                            ExpressionAnalysis ea = new ExpressionAnalysis();

                            ea.setEfName(resultSet.getString("ef"));
                            ea.setEfvName(resultSet.getString("efv"));
                            ea.setExperimentID(resultSet.getLong("experimentid"));
                            ea.setDesignElementID(resultSet.getLong("designelementid"));
                            ea.setTStatistic(resultSet.getFloat("tstat"));
                            ea.setPValAdjusted(resultSet.getFloat("pvaladj"));
                            ea.setEfId(resultSet.getLong("efid"));
                            ea.setEfvId(resultSet.getLong("efvid"));

                            result.get(geneid).add(ea);

                            return null;
                        }
                    });
        }

        return result;
    }

    public List<OntologyMapping> getOntologyMappingsByOntology(
            String ontologyName) {
        List results = template.query(ONTOLOGY_MAPPINGS_BY_ONTOLOGY_NAME,
                new Object[]{ontologyName},
                new OntologyMappingMapper());
        return (List<OntologyMapping>) results;
    }

    public List<Property> getAllProperties() {
        List results = template.query(PROPERTIES_ALL, new PropertyMapper());
        return (List<Property>) results;
    }

    public List<OntologyMapping> getExperimentsToAllProperties() {
        List results = template.query(EXPERIMENTS_TO_ALL_PROPERTIES_SELECT,
                new ExperimentPropertyMapper());
        return (List<OntologyMapping>) results;
    }


    public AtlasStatistics getAtlasStatistics(final String dataRelease, final String lastReleaseDate) {
        // manually count all experiments/genes/assays
        AtlasStatistics stats = new AtlasStatistics();

        stats.setDataRelease(dataRelease);
        stats.setExperimentCount(template.queryForInt(EXPERIMENTS_COUNT));
        stats.setAssayCount(template.queryForInt(ASSAYS_COUNT));
        stats.setGeneCount(template.queryForInt(GENES_COUNT));
        stats.setNewExperimentCount(template.queryForInt(NEW_EXPERIMENTS_COUNT, lastReleaseDate));
        stats.setPropertyValueCount(getPropertyValueCount());
        stats.setFactorValueCount(getFactorValueCount());

        return stats;
    }

    /*
    DAO write methods
     */

    public void writeLoadDetails(final String accession,
                                 final LoadStage loadStage,
                                 final LoadStatus loadStatus) {
        writeLoadDetails(accession, loadStage, loadStatus, LoadType.EXPERIMENT);
    }

    public void writeLoadDetails(final String accession,
                                 final LoadStage loadStage,
                                 final LoadStatus loadStatus,
                                 final LoadType loadType) {
        // execute this procedure...
        /*
        create or replace procedure load_progress(
          experiment_accession varchar
          ,stage varchar --load, netcdf, similarity, ranking, searchindex
          ,status varchar --done, pending
        )
        */
        SimpleJdbcCall procedure =
                new SimpleJdbcCall(template)
                        .withProcedureName("ATLASLDR.LOAD_PROGRESS")
                        .withoutProcedureColumnMetaDataAccess()
                        .useInParameterNames("EXPERIMENT_ACCESSION")
                        .useInParameterNames("STAGE")
                        .useInParameterNames("STATUS")
                        .useInParameterNames("LOAD_TYPE")
                        .declareParameters(new SqlParameter("EXPERIMENT_ACCESSION", Types.VARCHAR))
                        .declareParameters(new SqlParameter("STAGE", Types.VARCHAR))
                        .declareParameters(new SqlParameter("STATUS", Types.VARCHAR))
                        .declareParameters(new SqlParameter("LOAD_TYPE", Types.VARCHAR));

        // map parameters...
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("EXPERIMENT_ACCESSION", accession)
                .addValue("STAGE", loadStage.toString().toLowerCase())
                .addValue("STATUS", loadStatus.toString().toLowerCase())
                .addValue("LOAD_TYPE", loadType.toString().toLowerCase());

        log.debug("Invoking load_progress stored procedure with parameters (" + accession + ", " + loadStage + ", " +
                loadStatus + ", " + loadType + ")");
        procedure.execute(params);
        log.debug("load_progress stored procedure completed");
    }

    /**
     * Writes the given experiment to the database, using the default transaction strategy configured for the
     * datasource.
     *
     * @param experiment the experiment to write
     */
    public void writeExperiment(final Experiment experiment) {
        // execute this procedure...
        /*
        PROCEDURE "A2_EXPERIMENTSET" (
          TheAccession varchar2
          ,TheDescription varchar2
          ,ThePerformer varchar2
          ,TheLab varchar2
        )
        */
        SimpleJdbcCall procedure =
                new SimpleJdbcCall(template)
                        .withProcedureName("ATLASLDR.A2_EXPERIMENTSET")
                        .withoutProcedureColumnMetaDataAccess()
                        .useInParameterNames("ACCESSION")
                        .useInParameterNames("DESCRIPTION")
                        .useInParameterNames("PERFORMER")
                        .useInParameterNames("LAB")
                        .useInParameterNames("PMID")
                        .useInParameterNames("ABSTRACT")
                        .declareParameters(new SqlParameter("ACCESSION", Types.VARCHAR))
                        .declareParameters(new SqlParameter("DESCRIPTION", Types.VARCHAR))
                        .declareParameters(new SqlParameter("PERFORMER", Types.VARCHAR))
                        .declareParameters(new SqlParameter("LAB", Types.VARCHAR))
                        .declareParameters(new SqlParameter("PMID", Types.VARCHAR))
                        .declareParameters(new SqlParameter("ABSTRACT", Types.VARCHAR));

        // map parameters...
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("ACCESSION", experiment.getAccession())
                .addValue("DESCRIPTION", experiment.getDescription())
                .addValue("PERFORMER", experiment.getPerformer())
                .addValue("LAB", experiment.getLab())
                .addValue("PMID", experiment.getPubmedID())
                .addValue("ABSTRACT", experiment.getArticleAbstract());

        procedure.execute(params);
    }

    /**
     * Writes the given assay to the database, using the default transaction strategy configured for the datasource.
     *
     * @param assay the assay to write
     */
    public void writeAssay(final Assay assay) {
        // execute this procedure...
        /*
        PROCEDURE "A2_ASSAYSET" (
           TheAccession varchar2
          ,TheExperimentAccession  varchar2
          ,TheArrayDesignAccession varchar2
          ,TheProperties PropertyTable
          ,TheExpressionValues ExpressionValueTable
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
                        .useInParameterNames("EXPRESSIONVALUES")
                        .declareParameters(
                                new SqlParameter("ACCESSION", Types.VARCHAR))
                        .declareParameters(
                                new SqlParameter("EXPERIMENTACCESSION", Types.VARCHAR))
                        .declareParameters(
                                new SqlParameter("ARRAYDESIGNACCESSION", Types.VARCHAR))
                        .declareParameters(
                                new SqlParameter("PROPERTIES", OracleTypes.ARRAY, "PROPERTYTABLE"))
                        .declareParameters(
                                new SqlParameter("EXPRESSIONVALUES", OracleTypes.ARRAY, "EXPRESSIONVALUETABLE"));

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
                        convertPropertiesToOracleARRAY(props);
        SqlTypeValue expressionValuesParam = null;

        params.addValue("ACCESSION", assay.getAccession())
                .addValue("EXPERIMENTACCESSION", assay.getExperimentAccession())
                .addValue("ARRAYDESIGNACCESSION", assay.getArrayDesignAccession())
                .addValue("PROPERTIES", propertiesParam, OracleTypes.ARRAY, "PROPERTYTABLE")
                .addValue("EXPRESSIONVALUES", expressionValuesParam, OracleTypes.ARRAY, "EXPRESSIONVALUETABLE");

        log.debug("Invoking A2_ASSAYSET with the following parameters..." +
                "\n\tassay accession:          {}" +
                "\n\texperiment:               {}" +
                "\n\tarray design:             {}" +
                "\n\tproperties count:         {}" +
                "\n\texpression value count:   {}",
                new Object[]{assay.getAccession(), assay.getExperimentAccession(), assay.getArrayDesignAccession(),
                        props.size(), 0});

        // and execute
        procedure.execute(params);
    }

    /**
     * Writes the given sample to the database, using the default transaction strategy configured for the datasource.
     *
     * @param sample              the sample to write
     * @param experimentAccession experiment
     */
    public void writeSample(final Sample sample, final String experimentAccession) {
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
                : convertPropertiesToOracleARRAY(sample.getProperties());

        params.addValue("EXPERIMENTACCESSION", experimentAccession)
                .addValue("SAMPLEACCESSION", sample.getAccession())
                .addValue("ASSAYS", accessionsParam, OracleTypes.ARRAY, "ACCESSIONTABLE")
                .addValue("PROPERTIES", propertiesParam, OracleTypes.ARRAY, "PROPERTYTABLE")
                .addValue("CHANNEL", sample.getChannel());

        int assayCount = sample.getAssayAccessions().size();
        int propertiesCount = sample.getProperties().size();
        log.debug("Invoking A2_SAMPLESET with the following parameters..." +
                "\n\texperiment accession: {}" +
                "\n\tsample accession:     {}" +
                "\n\tassays count:         {}" +
                "\n\tproperties count:     {}" +
                "\n\tspecies:              {}" +
                "\n\tchannel:              {}",
                new Object[]{experimentAccession, sample.getAccession(), assayCount, propertiesCount,
                        sample.getSpecies(),
                        sample.getChannel()});

        // and execute
        procedure.execute(params);
    }

    /**
     * Writes expression analytics data back to the database, after post-processing by an external analytics process.
     * Expression analytics consist of p-values and t-statistics for each design element, annotated with a property and
     * property value.  As such, for each annotated design element there should be unique analytics data for each
     * annotation.
     *
     * @param experimentAccession the accession of the experiment these analytics values belong to
     * @param property            the name of the property for this set of analytics
     * @param propertyValue       the property value for this set of analytics
     * @param designElements      an array of ints, representing the design element ids for this analytics 'bundle'
     * @param pValues             an array of doubles, indexed in the same order as design elements, capturing the
     *                            pValues in the context of this property/property value pair
     * @param tStatistics         an array of doubles, indexed in the same order as design elements, capturing the
     *                            tStatistic (a double) in the context of this property/property value pair
     */
    public void writeExpressionAnalytics(String experimentAccession,
                                         String property,
                                         String propertyValue,
                                         long[] designElements,
                                         float[] pValues,
                                         float[] tStatistics) {
        // execute this procedure...
        /*
        PROCEDURE A2_AnalyticsSet(
          ExperimentAccession      IN   varchar2
          ,Property                 IN   varchar2
          ,PropertyValue            IN   varchar2
          ,ExpressionAnalytics ExpressionAnalyticsTable
        )
        */
        log.debug("Starting writing analytics for [experiment: " + experimentAccession + "; " +
                "Property: " + property + "; Property Value: " + propertyValue + "]");
        SimpleJdbcCall procedure =
                new SimpleJdbcCall(template)
                        .withProcedureName("ATLASLDR.A2_ANALYTICSSET")
                        .withoutProcedureColumnMetaDataAccess()
                        .useInParameterNames("EXPERIMENTACCESSION")
                        .useInParameterNames("PROPERTY")
                        .useInParameterNames("PROPERTYVALUE")
                        .useInParameterNames("EXPRESSIONANALYTICS")
                        .declareParameters(
                                new SqlParameter("EXPERIMENTACCESSION", Types.VARCHAR))
                        .declareParameters(
                                new SqlParameter("PROPERTY", Types.VARCHAR))
                        .declareParameters(
                                new SqlParameter("PROPERTYVALUE", Types.VARCHAR))
                        .declareParameters(
                                new SqlParameter("EXPRESSIONANALYTICS", OracleTypes.ARRAY, "EXPRESSIONANALYTICSTABLE"));

        // tracking variables for timings
        long start, end;
        String total;

        // map parameters...
        log.trace("Mapping parameters to oracle structures for [experiment: " + experimentAccession + "; " +
                "Property: " + property + "; Property Value: " + propertyValue + "]");
        start = System.currentTimeMillis();
        MapSqlParameterSource params = new MapSqlParameterSource();
        SqlTypeValue analyticsParam =
                designElements == null || designElements.length == 0
                        ? null
                        : convertExpressionAnalyticsToOracleARRAY(designElements, pValues, tStatistics);

        params.addValue("EXPERIMENTACCESSION", experimentAccession)
                .addValue("PROPERTY", property)
                .addValue("PROPERTYVALUE", propertyValue)
                .addValue("EXPRESSIONANALYTICS", analyticsParam);
        end = System.currentTimeMillis();
        total = new DecimalFormat("#.##").format((end - start) / 1000);
        log.trace("Parameter mapping for [experiment: " + experimentAccession + "; " +
                "Property: " + property + "; Property Value: " + propertyValue + "] complete in " + total + "s.");

        log.trace("Executing procedure for [experiment: " + experimentAccession + "; " +
                "Property: " + property + "; Property Value: " + propertyValue + "]");
        total = measureProcedureTime(procedure, params);
        log.trace("Procedure execution for [experiment: " + experimentAccession + "; " +
                "Property: " + property + "; Property Value: " + propertyValue + "]  complete in " + total + "s.");

        log.debug("Writing analytics for [experiment: " + experimentAccession + "; " +
                "Property: " + property + "; Property Value: " + propertyValue + "] completed!");
    }

    private String measureProcedureTime(SimpleJdbcCall procedure, MapSqlParameterSource params) {
        long t = System.currentTimeMillis();
        procedure.execute(params);
        return new DecimalFormat("#.##").format((System.currentTimeMillis() - t) / 1000);
    }

    /**
     * Writes array designs and associated data back to the database.
     *
     * @param arrayDesignBundle an object encapsulating the array design data that must be written to the database
     */
    public void writeArrayDesignBundle(ArrayDesignBundle arrayDesignBundle) {
        // execute this procedure...
        /*
        PROCEDURE A2_ARRAYDESIGNSET(
          Accession varchar2
          ,Type varchar2
          ,Name varchar2
          ,Provider varchar2
          ,DesignElements DesignElementTable
        );
         */
        SimpleJdbcCall procedure =
                new SimpleJdbcCall(template)
                        .withProcedureName("ATLASLDR.A2_ARRAYDESIGNSET")
                        .withoutProcedureColumnMetaDataAccess()
                        .useInParameterNames("ACCESSION")
                        .useInParameterNames("TYPE")
                        .useInParameterNames("NAME")
                        .useInParameterNames("PROVIDER")
                        .useInParameterNames("ENTRYPRIORITYLIST")
                        .useInParameterNames("DESIGNELEMENTS")
                        .declareParameters(
                                new SqlParameter("ACCESSION", Types.VARCHAR))
                        .declareParameters(
                                new SqlParameter("TYPE", Types.VARCHAR))
                        .declareParameters(
                                new SqlParameter("NAME", Types.VARCHAR))
                        .declareParameters(
                                new SqlParameter("PROVIDER", Types.VARCHAR))
                        .declareParameters(
                                new SqlParameter("ENTRYPRIORITYLIST", OracleTypes.ARRAY, "IDVALUETABLE"))
                        .declareParameters(
                                new SqlParameter("DESIGNELEMENTS", OracleTypes.ARRAY, "DESIGNELEMENTTABLE"));

        SqlTypeValue designElementsParam =
                arrayDesignBundle.getDesignElementNames().isEmpty() ? null :
                        convertDesignElementsToOracleARRAY(arrayDesignBundle);

        SqlTypeValue geneIdentifierPriorityParam = convertToOracleARRAYofIDVALUE(
                arrayDesignBundle.getGeneIdentifierNames());

        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("ACCESSION", arrayDesignBundle.getAccession())
                .addValue("TYPE", arrayDesignBundle.getType())
                .addValue("NAME", arrayDesignBundle.getName())
                .addValue("PROVIDER", arrayDesignBundle.getProvider())
                .addValue("ENTRYPRIORITYLIST", geneIdentifierPriorityParam, OracleTypes.ARRAY, "IDVALUETABLE")
                .addValue("DESIGNELEMENTS", designElementsParam, OracleTypes.ARRAY, "DESIGNELEMENTTABLE");

        procedure.execute(params);
    }

    /*
    DAO delete methods
     */

    /**
     * Deletes the experiment with the given accession from the database.  If this experiment is not present, this does
     * nothing.
     *
     * @param experimentAccession the accession of the experiment to remove
     */
    public void deleteExperiment(final String experimentAccession) {
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

    public void deleteExpressionAnalytics(String experimentAccession) {
        // execute this procedure...
        /*
        PROCEDURE A2_ANALYTICSDELETE(
          ExperimentAccession varchar2
        )
        */
        SimpleJdbcCall procedure =
                new SimpleJdbcCall(template)
                        .withProcedureName("ATLASLDR.A2_ANALYTICSDELETE")
                        .withoutProcedureColumnMetaDataAccess()
                        .useInParameterNames("EXPERIMENTACCESSION")
                        .declareParameters(new SqlParameter("EXPERIMENTACCESSION", Types.VARCHAR));

        // tracking variables for timings
        log.trace("Executing procedure to delete analytics for experiment: " + experimentAccession);
        String total = measureProcedureTime(procedure, singletonParam(experimentAccession));
        log.trace("Procedure execution to delete analytics for experiment: " + experimentAccession +
                " complete in " + total + "s.");
    }

    /*
    utils methods for doing standard stuff
     */

    public void startExpressionAnalytics(String experimentAccession) {
        // execute the startup analytics procedure...
        /*
        PROCEDURE A2_AnalyticsSetBegin(
           ExperimentAccession      IN   varchar2
        )
        */
        SimpleJdbcCall procedure =
                new SimpleJdbcCall(template)
                        .withProcedureName("ATLASLDR.A2_ANALYTICSSETBEGIN")
                        .withoutProcedureColumnMetaDataAccess()
                        .useInParameterNames("EXPERIMENTACCESSION")
                        .declareParameters(new SqlParameter("EXPERIMENTACCESSION", Types.VARCHAR));

        // and execute
        procedure.execute(singletonParam(experimentAccession));
    }

    public void finaliseExpressionAnalytics(String experimentAccession) {
        // execute the ending analytics procedure...
        /*
        PROCEDURE A2_AnalyticsSetEnd(
           ExperimentAccession      IN   varchar2
        )
        */
        SimpleJdbcCall procedure =
                new SimpleJdbcCall(template)
                        .withProcedureName("ATLASLDR.A2_ANALYTICSSETEND")
                        .withoutProcedureColumnMetaDataAccess()
                        .useInParameterNames("EXPERIMENTACCESSION")
                        .declareParameters(new SqlParameter("EXPERIMENTACCESSION", Types.VARCHAR));

        // and execute
        procedure.execute(singletonParam(experimentAccession));
    }

    private MapSqlParameterSource singletonParam(String experimentAccession) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("EXPERIMENTACCESSION", experimentAccession);
        return params;
    }


    private void fillOutAssays(List<Assay> assays) {
        // map assays to assay id
        Map<Long, Assay> assaysByID = new HashMap<Long, Assay>();
        for (Assay assay : assays) {
            // index this assay
            assaysByID.put(assay.getAssayID(), assay);
        }

        // maps properties to assays
        ObjectPropertyMappper assayPropertyMapper = new ObjectPropertyMappper(assaysByID);

        // query template for assays
        NamedParameterJdbcTemplate namedTemplate = new NamedParameterJdbcTemplate(template);

        // if we have more than 'maxQueryParams' assays, split into smaller queries
        final ArrayList<Long> assayIds = new ArrayList<Long>(assaysByID.keySet());
        for (List<Long> assayIDsChunk : asChunks(assayIds, maxQueryParams)) {
            // now query for properties that map to one of the samples in the sublist
            MapSqlParameterSource propertyParams = new MapSqlParameterSource();
            propertyParams.addValue("assayids", assayIDsChunk);
            namedTemplate.query(PROPERTIES_BY_RELATED_ASSAYS, propertyParams, assayPropertyMapper);
        }
    }

    private void fillOutSamples(List<Sample> samples) {
        // map samples to sample id
        Map<Long, Sample> samplesByID = new HashMap<Long, Sample>();
        for (Sample sample : samples) {
            samplesByID.put(sample.getSampleID(), sample);
        }

        // maps properties and assays to relevant sample
        AssaySampleMapper assaySampleMapper = new AssaySampleMapper(samplesByID);
        ObjectPropertyMappper samplePropertyMapper = new ObjectPropertyMappper(samplesByID);

        // query template for samples
        NamedParameterJdbcTemplate namedTemplate = new NamedParameterJdbcTemplate(template);

        // if we have more than 'maxQueryParams' samples, split into smaller queries
        List<Long> sampleIDs = new ArrayList<Long>(samplesByID.keySet());
        for (List<Long> sampleIDsChunk : asChunks(sampleIDs, maxQueryParams)) {
            // now query for assays that map to one of these samples
            MapSqlParameterSource assayParams = new MapSqlParameterSource();
            assayParams.addValue("sampleids", sampleIDsChunk);
            namedTemplate.query(ASSAYS_BY_RELATED_SAMPLES, assayParams, assaySampleMapper);

            // now query for properties that map to one of these samples
            log.trace("Querying for properties where sample IN (" + on(',').join(sampleIDsChunk) + ")");
            MapSqlParameterSource propertyParams = new MapSqlParameterSource();
            propertyParams.addValue("sampleids", sampleIDsChunk);
            namedTemplate.query(PROPERTIES_BY_RELATED_SAMPLES, propertyParams, samplePropertyMapper);
        }
    }

    @Deprecated
    private SqlTypeValue convertPropertiesToOracleARRAY(final List<Property> properties) {
        return new AbstractSqlTypeValue() {
            protected Object createTypeValue(Connection connection, int sqlType, String typeName) throws SQLException {
                // this should be creating an oracle ARRAY of properties
                // the array of STRUCTS representing each property
                Object[] propArrayValues;
                if (properties != null && !properties.isEmpty()) {
                    propArrayValues = new Object[properties.size()];

                    // convert each property to an oracle STRUCT
                    int i = 0;
                    Object[] propStructValues = new Object[5];
                    for (Property property : properties) {
                        // array representing the values to go in the STRUCT
                        propStructValues[0] = property.getAccession();
                        propStructValues[1] = property.getName();
                        propStructValues[2] = property.getValue();
                        propStructValues[3] = property.isFactorValue();
                        propStructValues[4] = property.getEfoTerms();

                        // descriptor for PROPERTY type
                        StructDescriptor structDescriptor = StructDescriptor.createDescriptor("PROPERTY", connection);
                        // each array value is a new STRUCT
                        propArrayValues[i++] = new STRUCT(structDescriptor, connection, propStructValues);
                    }
                    // created the array of STRUCTs, group into ARRAY
                    return createArray(connection, typeName, propArrayValues);
                } else {
                    // throw an SQLException, as we cannot create a ARRAY with an empty array
                    throw new SQLException("Unable to create an ARRAY from an empty list of properties");
                }
            }
        };
    }

    private Object createArray(Connection connection, String typeName, Object... propArrayValues) throws SQLException {
        ArrayDescriptor arrayDescriptor = ArrayDescriptor.createDescriptor(typeName, connection);
        return new ARRAY(arrayDescriptor, connection, propArrayValues);
    }

    private <T> SqlTypeValue convertToOracleARRAYofIDVALUE(final Collection<T> list) {
        return new AbstractSqlTypeValue() {
            protected Object createTypeValue(Connection connection, int sqlType, String typeName) throws SQLException {
                // this should be creating an oracle ARRAY of properties
                // the array of STRUCTS representing each property
                Object[] strArrayValues;
                if (list != null && !list.isEmpty()) {
                    strArrayValues = new Object[list.size()];

                    // convert each property to an oracle STRUCT
                    int i = 0;
                    Object[] propStructValues = new Object[2];
                    for (T elt : list) {
                        // array representing the values to go in the STRUCT
                        propStructValues[0] = i;
                        propStructValues[1] = elt;

                        // descriptor for PROPERTY type
                        StructDescriptor structDescriptor = StructDescriptor.createDescriptor("IDVALUE", connection);
                        // each array value is a new STRUCT
                        strArrayValues[i++] = new STRUCT(structDescriptor, connection, propStructValues);
                    }
                    // created the array of STRUCTs, group into ARRAY
                    return createArray(connection, typeName, strArrayValues);
                } else {
                    // throw an SQLException, as we cannot create a ARRAY with an empty array
                    throw new SQLException("Unable to create an ARRAY from an empty list");
                }
            }
        };
    }

    private SqlTypeValue convertAssayAccessionsToOracleARRAY(final Set<String> assayAccessions) {
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
                    return createArray(connection, typeName, accessions);
                } else {
                    // throw an SQLException, as we cannot create a ARRAY with an empty array
                    throw new SQLException("Unable to create an ARRAY from an empty list of accessions");
                }
            }
        };
    }

    private SqlTypeValue convertExpressionAnalyticsToOracleARRAY(final long[] designElements,
                                                                 final float[] pValues,
                                                                 final float[] tStatistics) {
        if (designElements == null || pValues == null || tStatistics == null ||
                designElements.length != pValues.length || pValues.length != tStatistics.length) {
            throw new IllegalArgumentException("Inconsistent design element counts for pValues and tStatistics");
        } else {
            int realDECount = 0;
            for (long de : designElements) {
                realDECount += de != 0 ? 1 : 0;
            }
            final int deCount = realDECount;
            return new AbstractSqlTypeValue() {
                protected Object createTypeValue(Connection connection, int sqlType, String typeName)
                        throws SQLException {
                    // this should be creating an oracle ARRAY of 'expressionAnalytics'
                    // the array of STRUCTS representing each property
                    Object[] expressionAnalytics;
                    if (deCount != 0) {
                        expressionAnalytics = new Object[deCount];

                        // convert each expression analytic pair into an oracle STRUCT
                        // descriptor for EXPRESSIONANALYTICS type
                        StructDescriptor structDescriptor =
                                StructDescriptor.createDescriptor("EXPRESSIONANALYTICS", connection);
                        Object[] expressionAnalyticsValues = new Object[3];
                        int j = 0;
                        for (int i = 0; i < designElements.length; i++) {
                            if (0 != designElements[i]) {
                                // array representing the values to go in the STRUCT
                                // Note the floatValue - EXPRESSIONANALYTICS structure assumes floats
                                expressionAnalyticsValues[0] = designElements[i];
                                expressionAnalyticsValues[1] = pValues[i];
                                expressionAnalyticsValues[2] = tStatistics[i];

                                expressionAnalytics[j] =
                                        new STRUCT(structDescriptor, connection, expressionAnalyticsValues);
                                j++;
                            }
                        }

                        if (0 == j)
                            throw new SQLException("Cannot write empty expression analytics!");

                        // created the array of STRUCTs, group into ARRAY
                        return createArray(connection, typeName, expressionAnalytics);
                    } else {
                        // throw an SQLException, as we cannot create a ARRAY with an empty array
                        throw new SQLException("Unable to create an ARRAY from empty lists of expression analytics");
                    }
                }
            };
        }
    }

    private SqlTypeValue convertDesignElementsToOracleARRAY(final ArrayDesignBundle arrayDesignBundle) {
        return new AbstractSqlTypeValue() {
            protected Object createTypeValue(Connection connection, int sqlType, String typeName) throws SQLException {
                List<Object> deArrayValues = new ArrayList<Object>();

                StructDescriptor structDescriptor =
                        StructDescriptor.createDescriptor("DESIGNELEMENT2", connection);

                // loop over all design element names
                for (String designElementName : arrayDesignBundle.getDesignElementNames()) {
                    // loop over the mappings of database entry 'type' to the set of values
                    Map<String, List<String>> dbeMappings =
                            arrayDesignBundle.getDatabaseEntriesForDesignElement(designElementName);
                    for (Map.Entry<String, List<String>> entry : dbeMappings.entrySet()) {
                        // loop over the enumeration of database entry values
                        List<String> databaseEntryValues = entry.getValue();
                        for (String databaseEntryValue : databaseEntryValues) {
                            // create a new row in the table for each combination
                            Object[] deStructValues = new Object[3];
                            deStructValues[0] = designElementName;
                            deStructValues[1] = entry.getKey();
                            deStructValues[2] = databaseEntryValue;

                            deArrayValues.add(new STRUCT(structDescriptor, connection, deStructValues));
                        }
                    }
                }

                return createArray(connection, typeName, deArrayValues.toArray());
            }
        };
    }


    public int getCountAssaysForExperimentID(long experimentID) {
        return template.queryForInt(
                "SELECT COUNT(DISTINCT ASSAYID) FROM VWEXPERIMENTASSAY WHERE EXPERIMENTID=?",
                experimentID);
    }

    public List<String> getSpeciesForExperiment(long experimentId) {
        List<Long> designIds = template.query("select distinct a.arraydesignid from A2_ASSAY a\n" +
                "         where a.EXPERIMENTID = ?\n",
                new Object[]{experimentId},
                new SingleColumnRowMapper<Long>());
        return template.query("select distinct o.name \n" +
                "from A2_ORGANISM o\n" +
                "join a2_bioentity be on be.organismid = o.organismid\n" +
                "join a2_designeltbioentity debe on debe.bioentityid = be.bioentityid\n" +
                "join a2_designelement de on de.designelementid = debe.designelementid\n" +
                "join a2_arraydesign ad on ad.arraydesignid = de.arraydesignid\n" +
                "where debe.softwareid = ad.mappingswid\n" +
                "and de.arraydesignid in (" + on(",").join(nCopies(designIds.size(), "?")) + ")",
                designIds.toArray(),
                new SingleColumnRowMapper<String>());
    }

    private static class LoadDetailsMapper implements RowMapper {
        public Object mapRow(ResultSet resultSet, int i) throws SQLException {
            LoadDetails details = new LoadDetails();
            details.setStatus(resultSet.getString(1));
            return details;
        }
    }

    private static class ExperimentMapper implements RowMapper {
        public Object mapRow(ResultSet resultSet, int i) throws SQLException {
            Experiment experiment = new Experiment();

            experiment.setAccession(resultSet.getString(1));
            experiment.setDescription(resultSet.getString(2));
            experiment.setPerformer(resultSet.getString(3));
            experiment.setLab(resultSet.getString(4));
            experiment.setExperimentID(resultSet.getLong(5));
            experiment.setLoadDate(resultSet.getDate(6));
            experiment.setPubmedID(resultSet.getString(7));
            experiment.setArticleAbstract(resultSet.getString(8));
            experiment.setReleaseDate(resultSet.getDate(9));

            return experiment;
        }
    }

    private static class ExperimentAssetMapper implements RowMapper {
        public Object mapRow(ResultSet resultSet, int i) throws SQLException {
            return new Experiment.Asset(resultSet.getString(1),
                    resultSet.getString(2),
                    resultSet.getString(3));
        }
    }


    private static class AssayMapper implements RowMapper {
        public Object mapRow(ResultSet resultSet, int i) throws SQLException {
            Assay assay = new Assay();

            assay.setAccession(resultSet.getString(1));
            assay.setExperimentAccession(resultSet.getString(2));
            assay.setArrayDesignAccession(resultSet.getString(3));
            assay.setAssayID(resultSet.getLong(4));

            return assay;
        }
    }

    private static class SampleMapper implements RowMapper {
        public Object mapRow(ResultSet resultSet, int i) throws SQLException {
            Sample sample = new Sample();

            sample.setAccession(resultSet.getString(1));
            sample.setSpecies(resultSet.getString(2));
            sample.setChannel(resultSet.getString(3));
            sample.setSampleID(resultSet.getLong(4));

            return sample;
        }
    }

    private static class AssaySampleMapper implements RowMapper {
        Map<Long, Sample> samplesMap;

        public AssaySampleMapper(Map<Long, Sample> samplesMap) {
            this.samplesMap = samplesMap;
        }

        public Object mapRow(ResultSet resultSet, int i) throws SQLException {
            long sampleID = resultSet.getLong(1);
            samplesMap.get(sampleID).addAssayAccession(resultSet.getString(2));
            return null;
        }
    }

    private static class OntologyMappingMapper extends ExperimentPropertyMapper {
        public Object mapRow(ResultSet resultSet, int i) throws SQLException {
            OntologyMapping mapping = (OntologyMapping) super.mapRow(resultSet, i);
            mapping.setExperimentId(resultSet.getLong(5));
            return mapping;
        }
    }

    private static class ExperimentPropertyMapper implements RowMapper {
        public Object mapRow(ResultSet resultSet, int i) throws SQLException {
            OntologyMapping mapping = new OntologyMapping();
            mapping.setExperimentAccession(resultSet.getString(1));
            mapping.setProperty(resultSet.getString(2));
            mapping.setPropertyValue(resultSet.getString(3));
            mapping.setOntologyTerm(resultSet.getString(4));
            return mapping;
        }
    }

    static class ObjectPropertyMappper implements RowMapper {
        private Map<Long, ? extends ObjectWithProperties> objectsById;

        public ObjectPropertyMappper(Map<Long, ? extends ObjectWithProperties> objectsById) {
            this.objectsById = objectsById;
        }

        public Object mapRow(ResultSet resultSet, int i) throws SQLException {
            Property property = new Property();

            long assayID = resultSet.getLong(1);

            property.setName(resultSet.getString(2));
            property.setValue(resultSet.getString(3));
            property.setFactorValue(resultSet.getBoolean(4));

            objectsById.get(assayID).addProperty(property);

            return property;
        }
    }

    private static class PropertyMapper implements RowMapper {
        public Object mapRow(ResultSet resultSet, int i) throws SQLException {
            Property property = new Property();
            property.setPropertyId(resultSet.getLong(1));
            property.setAccession(resultSet.getString(2));
            property.setName(resultSet.getString(2));
            property.setPropertyValueId(resultSet.getLong(3));
            property.setValue(resultSet.getString(4));
            property.setFactorValue(resultSet.getInt(5) > 0);
            return property;
        }
    }

    public void setExperimentReleaseDate(String accession) {
        template.update(EXPERIMENT_RELEASEDATE_UPDATE, accession);
    }

    //ToDo: it's probably better to inject this DAO
    private ArrayDesignDAO getArrayDesignDAO() {
        if (arrayDesignDAO == null) {
            arrayDesignDAO = new ArrayDesignDAO();
            arrayDesignDAO.setJdbcTemplate(template);
        }

        return arrayDesignDAO;
    }
}
