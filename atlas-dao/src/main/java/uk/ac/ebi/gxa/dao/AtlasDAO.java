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
import org.springframework.jdbc.core.*;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.jdbc.core.support.AbstractSqlTypeValue;
import uk.ac.ebi.microarray.atlas.model.*;
import uk.ac.ebi.microarray.atlas.services.ExperimentDAO;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.text.DecimalFormat;
import java.util.*;

import static com.google.common.base.Joiner.on;
import static com.google.common.collect.Iterables.partition;

/**
 * A data access object designed for retrieving common sorts of data from the atlas database.  This DAO should be
 * configured with a spring {@link JdbcTemplate} object which will be used to query the database.
 *
 * @author Tony Burdett
 * @author Alexey Filippov
 * @author Nataliya Sklyar
 * @author Misha Kapushesky
 * @author Pavel Kurnosov
 * @author Andrey Zorin
 * @author Robert Petryszak
 * @author Olga Melnichuk
 */
public class AtlasDAO implements ExperimentDAO {
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
                    " WHERE a.arraydesignid=ad.arraydesignid AND ad.accession=?)";
    public static final String EXPERIMENTS_TO_ALL_PROPERTIES_SELECT =
            "SELECT experiment, property, value, ontologyterm from cur_ontologymapping " +
                    "UNION " +
                    "SELECT distinct ap.experiment, ap.property, ap.value, null " +
                    "FROM cur_assayproperty ap where not exists " +
                    "(SELECT 1 from cur_ontologymapping cm " +
                    "WHERE cm.property = ap.property " +
                    "AND cm.value = ap.value " +
                    "AND cm.experiment = ap.experiment)";

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



    public static final String ONTOLOGY_MAPPINGS_BY_ONTOLOGY_NAME =
            "SELECT DISTINCT accession, property, propertyvalue, ontologyterm, experimentid " +
                    "FROM a2_ontologymapping" + " " +
                    "WHERE ontologyname=?";

    public static final String PROPERTIES_ALL =
            "SELECT min(p.propertyid), p.name, min(pv.propertyvalueid), pv.name, 1 as isfactorvalue " +
                    "FROM a2_property p, a2_propertyvalue pv " +
                    "WHERE  pv.propertyid=p.propertyid GROUP BY p.name, pv.name";


    public static final String PROPERTIES_BY_PROPERTY_NAME =
            "SELECT min(p.propertyid), p.name, min(pv.propertyvalueid), pv.name, 1 as isfactorvalue " +
                    "FROM a2_property p, a2_propertyvalue pv " +
                    "WHERE  pv.propertyid=p.propertyid AND p.name=? GROUP BY p.name, pv.name";

    private int maxQueryParams = 10;

    private Logger log = LoggerFactory.getLogger(getClass());

    private ArrayDesignDAOInterface arrayDesignDAO;
    private BioEntityDAOInterface bioEntityDAO;

    protected JdbcTemplate template;

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

    public List<Experiment> getAllExperiments() {
        return getExperiments(EXPERIMENTS_SELECT);
    }

    private List<Experiment> getExperiments(String select) {
        List<Experiment> results = template.query(select, new ExperimentMapper());
        loadExperimentAssets(results);
        return results;
    }

    /**
     * Gets a single experiment from the Atlas Database, queried by the accession of the experiment.
     *
     * @param accession the experiment's accession number (usually in the format E-ABCD-1234)
     * @return an object modelling this experiment
     */
    public Experiment getExperimentByAccession(String accession) {
        List<Experiment> results = template.query(EXPERIMENT_BY_ACC_SELECT,
                new Object[]{accession},
                new ExperimentMapper());

        if (results.size() > 0) {
            loadExperimentAssets(results);
            return results.get(0);
        } else {
            return null;
        }
    }

    /**
     * @param experimentId id of experiment to retrieve
     * @return Experiment (without assets) matching experimentId
     */
    public Experiment getShallowExperimentById(Long experimentId) {
        List<Experiment> results = template.query(EXPERIMENT_BY_ID_SELECT,
                new Object[]{experimentId},
                new ExperimentMapper());

        if (results.size() == 0) {
            return null;
        }
        return results.get(0);
    }

    private void loadExperimentAssets(List<Experiment> results) {
        for (Experiment experiment : results) {
            experiment.addAssets(template.query(EXPERIMENT_BY_ACC_SELECT_ASSETS,
                    new Object[]{experiment.getAccession()},
                    new ExperimentAssetMapper()));
        }
    }

    public List<Experiment> getExperimentByArrayDesign(String accession) {
        List<Experiment> results = template.query(EXPERIMENTS_BY_ARRAYDESIGN_SELECT,
                new Object[]{accession},
                new ExperimentMapper());
        loadExperimentAssets(results);
        return results;
    }


    public List<Assay> getAssaysByExperimentAccession(
            String experimentAccession) {
        List<Assay> assays = template.query(ASSAYS_BY_EXPERIMENT_ACCESSION,
                new Object[]{experimentAccession},
                new AssayMapper());

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
        List<Sample> samples = template.query(query, args, new SampleMapper());
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

    public List<OntologyMapping> getOntologyMappingsByOntology(
            String ontologyName) {
        return template.query(ONTOLOGY_MAPPINGS_BY_ONTOLOGY_NAME,
                new Object[]{ontologyName},
                new OntologyMappingMapper());
    }

    public List<Property> getAllProperties() {
        return template.query(PROPERTIES_ALL, new PropertyMapper());
    }

    public List<Property> getPropertiesByPropertyName(String propertyName) {
        return template.query(PROPERTIES_BY_PROPERTY_NAME, new Object[]{propertyName}, new PropertyMapper());
    }

    public List<OntologyMapping> getExperimentsToAllProperties() {
        return template.query(EXPERIMENTS_TO_ALL_PROPERTIES_SELECT,
                new ExperimentPropertyMapper());
    }


    public AtlasStatistics getAtlasStatistics(final String dataRelease, final String lastReleaseDate) {
        // manually count all experiments/genes/assays
        AtlasStatistics stats = new AtlasStatistics();

        stats.setDataRelease(dataRelease);
        stats.setExperimentCount(template.queryForInt(EXPERIMENTS_COUNT));
        stats.setAssayCount(template.queryForInt(ASSAYS_COUNT));
        stats.setGeneCount(getBioEntityDAO().getGeneCount());
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
                        convertPropertiesToOracleARRAY(props);

        params.addValue("ACCESSION", assay.getAccession())
                .addValue("EXPERIMENTACCESSION", assay.getExperimentAccession())
                .addValue("ARRAYDESIGNACCESSION", assay.getArrayDesignAccession())
                .addValue("PROPERTIES", propertiesParam, OracleTypes.ARRAY, "PROPERTYTABLE");

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
        int propertiesCount = sample.getPropertiesCount();
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
        for (List<Long> assayIDsChunk : partition(assayIds, maxQueryParams)) {
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
        for (List<Long> sampleIDsChunk : partition(sampleIDs, maxQueryParams)) {
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
        return getBioEntityDAO().getSpeciesForExperiment(experimentId);
    }

    public void setJdbcTemplate(JdbcTemplate template) {
        this.template = template;
    }

    private static class ExperimentMapper implements RowMapper<Experiment> {
        public Experiment mapRow(ResultSet resultSet, int i) throws SQLException {
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

    private static class ExperimentAssetMapper implements RowMapper<Experiment.Asset> {
        public Experiment.Asset mapRow(ResultSet resultSet, int i) throws SQLException {
            return new Experiment.Asset(resultSet.getString(1),
                    resultSet.getString(2),
                    resultSet.getString(3));
        }
    }

    private static class AssayMapper implements RowMapper<Assay> {
        public Assay mapRow(ResultSet resultSet, int i) throws SQLException {
            Assay assay = new Assay();

            assay.setAccession(resultSet.getString(1));
            assay.setExperimentAccession(resultSet.getString(2));
            assay.setArrayDesignAccession(resultSet.getString(3));
            assay.setAssayID(resultSet.getLong(4));

            return assay;
        }
    }

    private static class SampleMapper implements RowMapper<Sample> {
        public Sample mapRow(ResultSet resultSet, int i) throws SQLException {
            Sample sample = new Sample();

            sample.setAccession(resultSet.getString(1));
            sample.setSpecies(resultSet.getString(2));
            sample.setChannel(resultSet.getString(3));
            sample.setSampleID(resultSet.getLong(4));

            return sample;
        }
    }

    private static class AssaySampleMapper implements RowCallbackHandler {
        Map<Long, Sample> samplesMap;

        public AssaySampleMapper(Map<Long, Sample> samplesMap) {
            this.samplesMap = samplesMap;
        }

        public void processRow(ResultSet rs) throws SQLException {
            long sampleID = rs.getLong(1);
            samplesMap.get(sampleID).addAssayAccession(rs.getString(2));
        }
    }

    private static class OntologyMappingMapper extends ExperimentPropertyMapper {
        public OntologyMapping mapRow(ResultSet resultSet, int i) throws SQLException {
            OntologyMapping mapping = super.mapRow(resultSet, i);
            mapping.setExperimentId(resultSet.getLong(5));
            return mapping;
        }
    }

    private static class ExperimentPropertyMapper implements RowMapper<OntologyMapping> {
        public OntologyMapping mapRow(ResultSet resultSet, int i) throws SQLException {
            OntologyMapping mapping = new OntologyMapping();
            mapping.setExperimentAccession(resultSet.getString(1));
            mapping.setProperty(resultSet.getString(2));
            mapping.setPropertyValue(resultSet.getString(3));
            mapping.setOntologyTerm(resultSet.getString(4));
            return mapping;
        }
    }

    static class ObjectPropertyMappper implements RowMapper<Property> {
        private Map<Long, ? extends ObjectWithProperties> objectsById;

        public ObjectPropertyMappper(Map<Long, ? extends ObjectWithProperties> objectsById) {
            this.objectsById = objectsById;
        }

        public Property mapRow(ResultSet resultSet, int i) throws SQLException {
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
        template.update("Update a2_experiment set releasedate = (select sysdate from dual) where accession = ?", accession);
    }

    //ToDo: it's probably better to inject this DAO
    private ArrayDesignDAOInterface getArrayDesignDAO() {
        if (arrayDesignDAO == null) {
            arrayDesignDAO = new OldArrayDesignDAO();
            arrayDesignDAO.setJdbcTemplate(template);
        }

        return arrayDesignDAO;
    }

     private BioEntityDAOInterface getBioEntityDAO() {
        if (bioEntityDAO == null) {
            bioEntityDAO = new OldGeneDAO();
            bioEntityDAO.setJdbcTemplate(template);
        }

        return bioEntityDAO;
    }

    public void setArrayDesignDAO(ArrayDesignDAOInterface arrayDesignDAO) {
        this.arrayDesignDAO = arrayDesignDAO;
    }

    public void setBioEntityDAO(BioEntityDAOInterface bioEntityDAO) {
        this.bioEntityDAO = bioEntityDAO;
    }
}
