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
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.jdbc.core.*;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.jdbc.core.support.AbstractSqlTypeValue;
import uk.ac.ebi.gxa.Asset;
import uk.ac.ebi.gxa.Experiment;
import uk.ac.ebi.gxa.impl.ModelImpl;
import uk.ac.ebi.microarray.atlas.model.*;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
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
public class AtlasDAO implements ModelImpl.DbAccessor {
    public static final int MAX_QUERY_PARAMS = 10;

    private Logger log = LoggerFactory.getLogger(getClass());

    private ArrayDesignDAOInterface arrayDesignDAO;
    private BioEntityDAOInterface bioEntityDAO;
    private PropertyValueDAO propertyValueDAO;
    private JdbcTemplate template;

    public void setArrayDesignDAO(ArrayDesignDAOInterface arrayDesignDAO) {
        this.arrayDesignDAO = arrayDesignDAO;
    }

    public void setBioEntityDAO(BioEntityDAOInterface bioEntityDAO) {
        this.bioEntityDAO = bioEntityDAO;
    }

    public void setJdbcTemplate(JdbcTemplate template) {
        this.template = template;
    }

    public void setPropertyValueDAO(PropertyValueDAO propertyValueDAO) {
        this.propertyValueDAO = propertyValueDAO;
    }

    public List<Experiment> getAllExperiments(ModelImpl atlasModel) {
        return template.query(
                "SELECT " + ExperimentMapper.FIELDS + " FROM a2_experiment " +
                        "ORDER BY (" +
                        "    case when loaddate is null " +
                        "        then (select min(loaddate) from a2_experiment) " +
                        "        else loaddate end) desc, " +
                        "    accession", new ExperimentMapper(atlasModel)
        );
    }

    /**
     * Gets a single experiment from the Atlas Database, queried by the accession of the experiment.
     *
     * @param accession the experiment's accession number (usually in the format E-ABCD-1234)
     * @return an object modelling this experiment
     */
    public Experiment getExperimentByAccession(ModelImpl atlasModel, String accession) {
        try {
            return template.queryForObject(
                    "SELECT " + ExperimentMapper.FIELDS + " FROM a2_experiment WHERE accession=?",
                    new Object[]{accession},
                    new ExperimentMapper(atlasModel)
            );
        } catch (IncorrectResultSizeDataAccessException e) {
            return null;
        }
    }

    public List<Asset> loadAssetsForExperiment(Experiment experiment) {
        return template.query(
                "SELECT a.name, a.filename, a.description" + " FROM a2_experiment e " +
                        " JOIN a2_experimentasset a ON a.ExperimentID = e.ExperimentID " +
                        " WHERE e.accession=? ORDER BY a.ExperimentAssetID",
                new Object[]{experiment.getAccession()},
                new RowMapper<Asset>() {
                    public Asset mapRow(ResultSet resultSet, int i) throws SQLException {
                        return new Asset(resultSet.getString(1),
                                resultSet.getString(2),
                                resultSet.getString(3));
                    }
                }
        );
    }

    public List<Experiment> getExperimentsByArrayDesignAccession(ModelImpl atlasModel, String accession) {
        return template.query(
                "SELECT " + ExperimentMapper.FIELDS + " FROM a2_experiment " +
                        "WHERE experimentid IN " +
                        " (SELECT experimentid FROM a2_assay a, a2_arraydesign ad " +
                        " WHERE a.arraydesignid=ad.arraydesignid AND ad.accession=?)",
                new Object[]{accession},
                new ExperimentMapper(atlasModel)
        );
    }


    /**
     * @param experimentAccession the accession of experiment to retrieve assays for
     * @return list of assays
     * @deprecated Use id instead of accession
     */
    @Deprecated
    public List<Assay> getAssaysByExperimentAccession(String experimentAccession) {
        List<Assay> assays = template.query(
                "SELECT a.accession, e.accession, ad.accession, a.assayid " +
                        "FROM a2_assay a, a2_experiment e, a2_arraydesign ad " +
                        "WHERE e.experimentid=a.experimentid " +
                        "AND a.arraydesignid=ad.arraydesignid" + " " +
                        "AND e.accession=?",
                new Object[]{experimentAccession},
                new RowMapper<Assay>() {
                    public Assay mapRow(ResultSet resultSet, int i) throws SQLException {
                        final Assay assay = new Assay(resultSet.getString(1));

                        assay.setExperimentAccession(resultSet.getString(2));
                        assay.setArrayDesignAccession(resultSet.getString(3));
                        assay.setAssayID(resultSet.getLong(4));

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

    /**
     * @param experimentAccession the accession of experiment to retrieve samples for
     * @param assayAccession      the accession of the assay to retrieve samples for
     * @return list of samples
     * @deprecated Use ids instead of accessions
     */
    @Deprecated
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

    public List<Sample> getSamplesByExperimentAccession(String exptAccession) {
        List<Sample> samples = template.query("SELECT " + SampleMapper.FIELDS +
                " FROM a2_sample s, a2_assay a, a2_assaysample ass, a2_experiment e, a2_organism org " +
                "WHERE s.sampleid=ass.sampleid " +
                "AND a.assayid=ass.assayid " +
                "AND a.experimentid=e.experimentid " +
                "AND s.organismid=org.organismid " +
                "AND e.accession=?", new Object[]{exptAccession}, new SampleMapper());
        // populate the other info for these samples
        if (samples.size() > 0) {
            fillOutSamples(samples);
        }
        return samples;
    }

    /**
     * Returns all array designs in the underlying datasource.  Note that, to reduce query times, this method does NOT
     * prepopulate ArrayDesigns with their associated design elements (unlike other methods to retrieve array designs
     * more specifically).
     *
     * @return the list of array designs, not prepopulated with design elements.
     */
    public List<ArrayDesign> getAllArrayDesigns() {
        return arrayDesignDAO.getAllArrayDesigns();
    }

    public ArrayDesign getArrayDesignByAccession(String accession) {
        return arrayDesignDAO.getArrayDesignByAccession(accession);
    }

    /**
     * @param accession Array design accession
     * @return Array design (with no design element and gene ids filled in) corresponding to accession
     */
    public ArrayDesign getArrayDesignShallowByAccession(String accession) {
        return arrayDesignDAO.getArrayDesignShallowByAccession(accession);
    }

    public List<OntologyMapping> getOntologyMappingsByOntology(
            String ontologyName) {
        return template.query("SELECT DISTINCT accession, property, propertyvalue, ontologyterm, experimentid " +
                "FROM a2_ontologymapping" + " " +
                "WHERE ontologyname=?",
                new Object[]{ontologyName},
                new ExperimentPropertyMapper() {
                    public OntologyMapping mapRow(ResultSet resultSet, int i) throws SQLException {
                        OntologyMapping mapping = super.mapRow(resultSet, i);
                        mapping.setExperimentId(resultSet.getLong(5));
                        return mapping;
                    }
                });
    }

    public List<OntologyMapping> getExperimentsToAllProperties() {
        return template.query("SELECT experiment, property, value, ontologyterm from cur_ontologymapping " +
                "UNION " +
                "SELECT distinct ap.experiment, ap.property, ap.value, null " +
                "FROM cur_assayproperty ap where not exists " +
                "(SELECT 1 from cur_ontologymapping cm " +
                "WHERE cm.property = ap.property " +
                "AND cm.value = ap.value " +
                "AND cm.experiment = ap.experiment)",
                new ExperimentPropertyMapper());
    }


    public AtlasStatistics getAtlasStatistics(final String dataRelease, final String lastReleaseDate) {
        // manually count all experiments/genes/assays
        AtlasStatistics stats = new AtlasStatistics();

        stats.setDataRelease(dataRelease);
        stats.setExperimentCount(template.queryForInt(
                "SELECT COUNT(*) FROM a2_experiment"
        ));
        stats.setAssayCount(template.queryForInt(
                "SELECT COUNT(*) FROM a2_assay"
        ));
        stats.setGeneCount(bioEntityDAO.getGeneCount());
        stats.setNewExperimentCount(template.queryForInt(
                "SELECT COUNT(*) FROM a2_experiment WHERE loaddate > to_date(?,'MM-YYYY')", lastReleaseDate
        ));
        stats.setFactorValueCount(template.queryForInt(
                "SELECT COUNT(DISTINCT propertyvalueid) FROM a2_assayPV"
        ));

        return stats;
    }

    /**
     * Writes the given experiment to the database, using the default transaction strategy configured for the
     * datasource.
     *
     * @param experiment the experiment to write
     */
    public void writeExperimentInternal(Experiment experiment) {
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
        final long id = template.queryForLong("SELECT experimentid FROM a2_experiment WHERE accession=?", new Object[]{experiment.getAccession()});
        log.info("new experiment id = " + id);
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

    /**
     * Writes array designs and associated data back to the database.
     *
     * @param arrayDesignBundle an object encapsulating the array design data that must be written to the database
     * @deprecated
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
    public void deleteExperimentFromDatabase(final String experimentAccession) {
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


    private void fillOutAssays(List<Assay> assays) {
        // map assays to assay id
        Map<Long, Assay> assaysByID = new HashMap<Long, Assay>();
        for (Assay assay : assays) {
            // index this assay
            assaysByID.put(assay.getAssayID(), assay);
        }

        // maps properties to assays
        ObjectPropertyMapper assayPropertyMapper = new ObjectPropertyMapper(assaysByID);

        // query template for assays
        NamedParameterJdbcTemplate namedTemplate = new NamedParameterJdbcTemplate(template);

        // if we have more than 'MAX_QUERY_PARAMS' assays, split into smaller queries
        final ArrayList<Long> assayIds = new ArrayList<Long>(assaysByID.keySet());
        for (List<Long> assayIDsChunk : partition(assayIds, MAX_QUERY_PARAMS)) {
            // now query for properties that map to one of the samples in the sublist
            MapSqlParameterSource propertyParams = new MapSqlParameterSource();
            propertyParams.addValue("assayids", assayIDsChunk);
            namedTemplate.query("SELECT apv.assayid,\n" +
                    "        apv.propertyvalueid AS propertyvalue,\n" +
                    "        wm_concat(t.accession) AS efoTerms\n" +
                    "  FROM a2_assaypv apv \n" +
                    "          LEFT JOIN a2_assaypvontology apvo ON apvo.assaypvid = apv.assaypvid\n" +
                    "          LEFT JOIN a2_ontologyterm t ON apvo.ontologytermid = t.ontologytermid\n" +
                    " WHERE apv.assayid IN (:assayids)" +
                    "  GROUP BY apvo.assaypvid, apv.assayid, apv.propertyvalueid", propertyParams, assayPropertyMapper);
        }
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
        ObjectPropertyMapper samplePropertyMapper = new ObjectPropertyMapper(samplesByID);

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
            namedTemplate.query("SELECT spv.sampleid,\n" +
                    "        spv.propertyvalueid AS propertyvalue, \n" +
                    "        wm_concat(t.accession) AS efoTerms\n" +
                    "  FROM a2_samplepv spv \n" +
                    "          LEFT JOIN a2_samplepvontology spvo ON spvo.SamplePVID = spv.SAMPLEPVID\n" +
                    "          LEFT JOIN a2_ontologyterm t ON spvo.ontologytermid = t.ontologytermid\n" +
                    " WHERE spv.sampleid IN (:sampleids)" +
                    "  GROUP BY spvo.SamplePVID, spv.SAMPLEID, spv.propertyvalueid ", propertyParams, samplePropertyMapper);
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
                    Object[] propStructValues = new Object[4];
                    for (Property property : properties) {
                        // array representing the values to go in the STRUCT
                        propStructValues[0] = "";
                        propStructValues[1] = property.getName();
                        propStructValues[2] = property.getValue();
                        propStructValues[3] = property.getEfoTerms();

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
        return bioEntityDAO.getSpeciesForExperiment(experimentId);
    }

    private static class ExperimentMapper implements RowMapper<Experiment> {
        private static final String FIELDS = " accession, description, performer, lab, " +
                " experimentid, loaddate, pmid, abstract, releasedate, private, curated ";

        private final ModelImpl atlasModel;

        ExperimentMapper(ModelImpl atlasModel) {
            this.atlasModel = atlasModel;
        }

        public Experiment mapRow(ResultSet resultSet, int i) throws SQLException {
            Experiment experiment = atlasModel.createExperiment(
                    resultSet.getString(1),
                    resultSet.getLong(5)
            );

            experiment.setDescription(resultSet.getString(2));
            experiment.setPerformer(resultSet.getString(3));
            experiment.setLab(resultSet.getString(4));
            experiment.setLoadDate(resultSet.getDate(6));
            experiment.setPubmedIdString(resultSet.getString(7));
            experiment.setAbstract(resultSet.getString(8));
            experiment.setReleaseDate(resultSet.getDate(9));
            experiment.setPrivate(resultSet.getBoolean(10));
            experiment.setCurated(resultSet.getBoolean(11));

            return experiment;
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

    private class ObjectPropertyMapper implements RowCallbackHandler {
        private Map<Long, ? extends ObjectWithProperties> objectsById;

        public ObjectPropertyMapper(Map<Long, ? extends ObjectWithProperties> objectsById) {
            this.objectsById = objectsById;
        }

        public void processRow(ResultSet rs) throws SQLException {


            long objectId = rs.getLong(1);
            PropertyValue pv = propertyValueDAO.getById(rs.getLong(2));
            Property property = new Property(pv, rs.getString(3));

            objectsById.get(objectId).addProperty(property);
        }
    }

    private static class SampleMapper implements RowMapper<Sample> {
        private static final String FIELDS = "s.accession, org.name species, s.channel, s.sampleid ";

        public Sample mapRow(ResultSet resultSet, int i) throws SQLException {
            Sample sample = new Sample();

            sample.setAccession(resultSet.getString(1));
            sample.setSpecies(resultSet.getString(2));
            sample.setChannel(resultSet.getString(3));
            sample.setSampleID(resultSet.getLong(4));

            return sample;
        }
    }
}
