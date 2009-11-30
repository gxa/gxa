package uk.ac.ebi.microarray.atlas.dao;

import oracle.jdbc.OracleTypes;
import oracle.sql.ARRAY;
import oracle.sql.ArrayDescriptor;
import oracle.sql.STRUCT;
import oracle.sql.StructDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.*;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.jdbc.core.support.AbstractSqlTypeValue;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;
import uk.ac.ebi.microarray.atlas.model.*;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.*;

/**
 * A data access object designed for retrieving common sorts of data from the atlas database.  This DAO should be
 * configured with a spring {@link JdbcTemplate} object which will be used to query the database.
 *
 * @author Tony Burdett
 * @date 21-Sep-2009
 */
public class AtlasDAO {
    // load monitor
    private static final String LOAD_MONITOR_SELECT =
            "SELECT accession, netcdf, similarity, ranking, searchindex, load_type " +
                    "FROM load_monitor";

    // experiment queries
    private static final String EXPERIMENTS_SELECT =
            "SELECT accession, description, performer, lab, experimentid " +
                    "FROM a2_experiment";
    private static final String EXPERIMENTS_PENDING_INDEX_SELECT =
            "SELECT e.accession, e.description, e.performer, e.lab, e.experimentid " +
                    "FROM a2_experiment e, load_monitor lm " +
                    "WHERE e.accession=lm.accession " +
                    "AND (lm.searchindex='pending' OR lm.searchindex='failed') " +
                    "AND lm.load_type='experiment'";
    private static final String EXPERIMENTS_PENDING_NETCDF_SELECT =
            "SELECT e.accession, e.description, e.performer, e.lab, e.experimentid " +
                    "FROM a2_experiment e, load_monitor lm " +
                    "WHERE e.accession=lm.accession " +
                    "AND (lm.netcdf='pending' OR lm.netcdf='failed') " +
                    "AND lm.load_type='experiment'";
    private static final String EXPERIMENTS_PENDING_ANALYTICS_SELECT =
            "SELECT e.accession, e.description, e.performer, e.lab, e.experimentid " +
                    "FROM a2_experiment e, load_monitor lm " +
                    "WHERE e.accession=lm.accession " +
                    "AND (lm.ranking='pending' OR lm.ranking='failed') " + // fixme: similarity?
                    "AND lm.load_type='experiment'";
    private static final String EXPERIMENT_BY_ACC_SELECT =
            EXPERIMENTS_SELECT + " " +
                    "WHERE accession=?";

    // gene queries
    private static final String GENES_SELECT =
            "SELECT DISTINCT g.geneid, g.identifier, g.name, s.name AS species " +
                    "FROM a2_gene g, a2_spec s " +
                    "WHERE g.specid=s.specid";
    private static final String GENES_PENDING_SELECT =
            "SELECT DISTINCT g.geneid, g.identifier, g.name, s.name AS species " +
                    "FROM a2_gene g, a2_spec s, load_monitor lm " +
                    "WHERE g.specid=s.specid " +
                    "AND g.identifier=lm.accession " +
                    "AND (lm.searchindex='pending' OR lm.searchindex='failed') " +
                    "AND lm.load_type='gene'";
    private static final String GENES_BY_EXPERIMENT_ACCESSION =
            "SELECT DISTINCT g.geneid, g.identifier, g.name, s.name AS species " +
                    "FROM a2_gene g, a2_spec s, a2_designelement d, a2_assay a, " +
                    "a2_experiment e " +
                    "WHERE g.geneid=d.geneid " +
                    "AND g.specid = s.specid " +
                    "AND d.arraydesignid=a.arraydesignid " +
                    "AND a.experimentid=e.experimentid " +
                    "AND e.accession=?";
    private static final String PROPERTIES_BY_RELATED_GENES =
            "SELECT gpv.geneid, gp.name AS property, gpv.value AS propertyvalue " +
                    "FROM a2_geneproperty gp, a2_genepropertyvalue gpv " +
                    "WHERE gpv.genepropertyid=gp.genepropertyid " +
                    "AND gpv.geneid IN (:geneids)";
    private static final String GENE_COUNT_SELECT =
            "SELECT COUNT(DISTINCT identifier) FROM a2_gene";

    // assay queries
    private static final String ASSAYS_SELECT =
            "SELECT a.accession, e.accession, ad.accession, a.assayid " +
                    "FROM a2_assay a, a2_experiment e, a2_arraydesign ad " +
                    "WHERE e.experimentid=a.experimentid " +
                    "AND a.arraydesignid=ad.arraydesignid";
    private static final String ASSAYS_BY_EXPERIMENT_ACCESSION =
            ASSAYS_SELECT + " " +
                    "AND e.accession=?";
    private static final String ASSAYS_BY_EXPERIMENT_AND_ARRAY_ACCESSION =
            ASSAYS_BY_EXPERIMENT_ACCESSION + " " +
                    "AND ad.accession=?";
    private static final String ASSAYS_BY_RELATED_SAMPLES =
            "SELECT s.sampleid, a.accession " +
                    "FROM a2_assay a, a2_assaysample s " +
                    "WHERE a.assayid=s.assayid " +
                    "AND s.sampleid IN (:sampleids)";
    private static final String PROPERTIES_BY_RELATED_ASSAYS =
            "SELECT apv.assayid, p.name AS property, pv.name AS propertyvalue, apv.isfactorvalue " +
                    "FROM a2_property p, a2_propertyvalue pv, a2_assaypropertyvalue apv " +
                    "WHERE apv.propertyvalueid=pv.propertyvalueid " +
                    "AND pv.propertyid=p.propertyid " +
                    "AND apv.assayid IN (:assayids)";

    // expression value queries
    private static final String EXPRESSION_VALUES_BY_ASSAY_ID =
            "SELECT ev.designelementid, ev.assayid, de.accession, ev.value " +
                    "FROM a2_expressionvalue ev, a2_designelement de " +
                    "WHERE ev.designelementid=de.designelementid " +
                    "AND ev.assayid=?";
    private static final String EXPRESSION_VALUES_BY_EXPERIMENT_AND_ARRAY =
            "SELECT ev.designelementid, ev.assayid, de.accession, ev.value " +
                    "FROM a2_expressionvalue ev, a2_designelement de " +
                    "WHERE ev.designelementid=de.designelementid " +
                    "AND ev.experimentid=? " +
                    "AND de.arraydesignid=?";

    // sample queries
    private static final String SAMPLES_BY_ASSAY_ACCESSION =
            "SELECT s.accession, s.species, s.channel, s.sampleid " +
                    "FROM a2_sample s, a2_assay a, a2_assaysample ass " +
                    "WHERE s.sampleid=ass.sampleid " +
                    "AND a.assayid=ass.assayid " +
                    "AND a.accession=?";
    private static final String SAMPLES_BY_EXPERIMENT_ACCESSION =
            "SELECT s.accession, s.species, s.channel, s.sampleid " +
                    "FROM a2_sample s, a2_assay a, a2_assaysample ass, a2_experiment e " +
                    "WHERE s.sampleid=ass.sampleid " +
                    "AND a.assayid=ass.assayid " +
                    "AND a.experimentid=e.experimentid " +
                    "AND e.accession=?";
    private static final String PROPERTIES_BY_RELATED_SAMPLES =
            "SELECT spv.sampleid, p.name AS property, pv.name AS propertyvalue, spv.isfactorvalue " +
                    "FROM a2_property p, a2_propertyvalue pv, a2_samplepropertyvalue spv " +
                    "WHERE spv.propertyvalueid=pv.propertyvalueid " +
                    "AND pv.propertyid=p.propertyid " +
                    "AND spv.sampleid IN (:sampleids)";

    // query for counts, for statistics
    private static final String PROPERTY_VALUE_COUNT_SELECT =
            "SELECT COUNT(DISTINCT name) FROM a2_propertyvalue";

    // array and design element queries
    private static final String ARRAY_DESIGN_SELECT =
            "SELECT accession, type, name, provider, arraydesignid " +
                    "FROM a2_arraydesign";
    private static final String ARRAY_DESIGN_BY_ACC_SELECT =
            ARRAY_DESIGN_SELECT + " " +
                    "WHERE accession=?";
    private static final String ARRAY_DESIGN_BY_EXPERIMENT_ACCESSION =
            "SELECT " +
                    "DISTINCT d.accession, d.type, d.name, d.provider, d.arraydesignid " +
                    "FROM a2_arraydesign d, a2_assay a, a2_experiment e " +
                    "WHERE e.experimentid=a.experimentid " +
                    "AND a.arraydesignid=d.arraydesignid " +
                    "AND e.accession=?";
    private static final String DESIGN_ELEMENTS_BY_ARRAY_ACCESSION =
            "SELECT de.designelementid, de.accession " +
                    "FROM A2_ARRAYDESIGN ad, A2_DESIGNELEMENT de " +
                    "WHERE de.arraydesignid=ad.arraydesignid " +
                    "AND ad.accession=?";
    private static final String DESIGN_ELEMENTS_BY_ARRAY_ID =
            "SELECT de.designelementid, de.accession " +
                    "FROM a2_designelement de " +
                    "WHERE de.arraydesignid=?";
    private static final String DESIGN_ELEMENTS_BY_GENEID =
            "SELECT de.designelementid, de.accession " +
                    "FROM a2_designelement de " +
                    "WHERE de.geneid=?";

    // other useful queries
    private static final String EXPRESSIONANALYTICS_BY_EXPERIMENTID =
            "SELECT ef.name AS ef, efv.name AS efv, a.experimentid, " +
                    "a.designelementid, a.tstat, a.pvaladj " +
                    "FROM a2_expressionanalytics a " +
                    "JOIN a2_propertyvalue efv ON efv.propertyvalueid=a.propertyvalueid " +
                    "JOIN a2_property ef ON ef.propertyid=efv.propertyid " +
                    "JOIN a2_designelement de ON de.designelementid=a.designelementID " +
                    "WHERE a.experimentid=?";
    private static final String EXPRESSIONANALYTICS_BY_GENEID =
            "SELECT ef.name AS ef, efv.name AS efv, a.experimentid, a.geneid, " +
                    "a.tstat, a.pvaladj " +
                    "FROM a2_expressionanalytics a " +
                    "JOIN a2_propertyvalue efv ON efv.propertyvalueid=a.propertyvalueid " +
                    "JOIN a2_property ef ON ef.propertyid=efv.propertyid " +
                    "WHERE a.geneid=?";
    private static final String EXPRESSIONANALYTICS_BY_DESIGNELEMENTID =
            "SELECT ef.name AS ef, efv.name AS efv, a.experimentid, a.geneid, " +
                    "a.tstat, a.pvaladj " +
                    "FROM a2_expressionanalytics a " +
                    "JOIN a2_propertyvalue efv ON efv.propertyvalueid=a.propertyvalueid " +
                    "JOIN a2_property ef ON ef.propertyid=efv.propertyid " +
                    "WHERE a.designelementid=?";
    private static final String ONTOLOGY_MAPPINGS_SELECT =
            "SELECT accession, property, propertyvalue, ontologyterm, " +
                    "issampleproperty, isassayproperty, isfactorvalue " +
                    "FROM a2_ontologymapping";
    private static final String ONTOLOGY_MAPPINGS_BY_ONTOLOGY_NAME =
            ONTOLOGY_MAPPINGS_SELECT + " " +
                    "WHERE ontologyname=?";
    private static final String ONTOLOGY_MAPPINGS_BY_EXPERIMENT_ACCESSION =
            ONTOLOGY_MAPPINGS_SELECT + " " +
                    "WHERE accession=?";

    // queries for atlas interface
    private static final String ATLAS_RESULTS_SELECT =
            "SELECT " +
                    "ea.experimentid, " +
                    "g.geneid, " +
                    "p.name AS property, " +
                    "pv.name AS propertyvalue, " +
                    "CASE WHEN ea.tstat < 0 THEN -1 ELSE 1 END AS updn, " +
                    "ea.pvaladj, " +
                    "FROM a2_expressionanalytics ea " +
                    "JOIN a2_propertyvalue pv ON pv.propertyvalueid=ea.propertyvalueid " +
                    "JOIN a2_property p ON p.propertyid=pv.propertyid " +
                    "JOIN a2_designelement de ON de.designelementid=ea.designelementid " +
                    "JOIN a2_gene g ON g.geneid=de.geneid";
    // same as results, but counts geneids instead of returning them
    private static final String ATLAS_COUNTS_SELECT =
            "SELECT " +
                    "ea.experimentid, " +
                    "p.name AS property, " +
                    "pv.name AS propertyvalue, " +
                    "CASE WHEN ea.tstat < 0 THEN -1 ELSE 1 END AS updn, " +
                    "ea.pvaladj, " +
                    "COUNT(DISTINCT(g.geneid)) AS genes " +
                    "FROM a2_expressionanalytics ea " +
                    "JOIN a2_propertyvalue pv ON pv.propertyvalueid=ea.propertyvalueid " +
                    "JOIN a2_property p ON p.propertyid=pv.propertyid " +
                    "JOIN a2_designelement de ON de.designelementid=ea.designelementid " +
                    "JOIN a2_gene g ON g.geneid=de.geneid";
    private static final String ATLAS_COUNTS_BY_EXPERIMENTID =
            ATLAS_COUNTS_SELECT + " " +
                    "WHERE ea.experimentid=? " +
                    "GROUP BY ea.experimentid, g.geneid, p.name, pv.name, ea.pvaladj, " +
                    "CASE WHEN ea.tstat < 0 THEN -1 ELSE 1 END";
    private static final String ATLAS_RESULTS_UP_BY_EXPERIMENTID_GENEID_AND_EFV =
            ATLAS_RESULTS_SELECT + " " +
                    "WHERE ea.experimentid IN (:exptids) " +
                    "AND g.geneid IN (:geneids) " +
                    "AND pv.name IN (:efvs) " +
                    "AND updn='1' " +
                    "AND TOPN<=20 " +
                    "ORDER BY ea.pvaladj " +
                    "GROUP BY ea.experimentid, g.geneid, p.name, pv.name, ea.pvaladj, " +
                    "CASE WHEN ea.tstat < 0 THEN -1 ELSE 1 END";
    private static final String ATLAS_RESULTS_DOWN_BY_EXPERIMENTID_GENEID_AND_EFV =
            ATLAS_RESULTS_SELECT + " " +
                    "WHERE ea.experimentid IN (:exptids) " +
                    "AND g.geneid IN (:geneids) " +
                    "AND pv.name IN (:efvs) " +
                    "AND updn='-1' " +
                    "AND TOPN<=20 " +
                    "ORDER BY ea.pvaladj " +
                    "GROUP BY ea.experimentid, g.geneid, p.name, pv.name, ea.pvaladj, " +
                    "CASE WHEN ea.tstat < 0 THEN -1 ELSE 1 END";
    private static final String ATLAS_RESULTS_UPORDOWN_BY_EXPERIMENTID_GENEID_AND_EFV =
            ATLAS_RESULTS_SELECT + " " +
                    "WHERE ea.experimentid IN (:exptids) " +
                    "AND g.geneid IN (:geneids) " +
                    "AND pv.name IN (:efvs) " +
                    "AND updn<>0 " +
                    "AND TOPN<=20 " +
                    "ORDER BY ea.pvaladj " +
                    "GROUP BY ea.experimentid, g.geneid, p.name, pv.name, ea.pvaladj, " +
                    "CASE WHEN ea.tstat < 0 THEN -1 ELSE 1 END"; // fixme: exclude experiment ids?
    // old atlas queries contained "NOT IN (211794549,215315583,384555530,411493378,411512559)"

    private JdbcTemplate template;
    private TransactionTemplate transactionTemplate;

    private final Logger log = LoggerFactory.getLogger(getClass());

    public JdbcTemplate getJdbcTemplate() {
        return template;
    }

    public void setJdbcTemplate(JdbcTemplate template) {
        this.template = template;
    }

    public TransactionTemplate getTransactionTemplate() {
        return transactionTemplate;
    }

    public void setTransactionTemplate(TransactionTemplate transactionTemplate) {
        this.transactionTemplate = transactionTemplate;
    }

    /*
    DAO read methods
     */

    public List<LoadDetails> getLoadDetails() {
        List results = template.query(LOAD_MONITOR_SELECT,
                                      new LoadDetailsMapper());
        return (List<LoadDetails>) results;
    }

    public List<Experiment> getAllExperiments() {
        List results = template.query(EXPERIMENTS_SELECT,
                                      new ExperimentMapper());
        return (List<Experiment>) results;
    }

    public List<Experiment> getAllExperimentsPendingIndexing() {
        List results = template.query(EXPERIMENTS_PENDING_INDEX_SELECT,
                                      new ExperimentMapper());
        return (List<Experiment>) results;
    }

    public List<Experiment> getAllExperimentsPendingNetCDFs() {
        List results = template.query(EXPERIMENTS_PENDING_NETCDF_SELECT,
                                      new ExperimentMapper());
        return (List<Experiment>) results;
    }

    public List<Experiment> getAllExperimentsPendingAnalytics() {
        List results = template.query(EXPERIMENTS_PENDING_ANALYTICS_SELECT,
                                      new ExperimentMapper());
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

        return results.size() > 0 ? (Experiment) results.get(0) : null;
    }

    /**
     * Fetches all genes in the database.  Note that genes are not automatically prepopulated with property information,
     * to keep query time down.  If you require this data, you can fetch it for the list of genes you want to obtain
     * properties for by calling {@link #getPropertiesForGenes(java.util.List)}.
     *
     * @return the list of all genes in the database.
     */
    public List<Gene> getAllGenes() {
        List results = template.query(GENES_SELECT,
                                      new GeneMapper());

        return (List<Gene>) results;
    }

    public List<Gene> getAllPendingGenes() {
        List results = template.query(GENES_PENDING_SELECT,
                                      new GeneMapper());
        return (List<Gene>) results;
    }

    public List<Gene> getGenesByExperimentAccession(String exptAccession) {
        List results = template.query(GENES_BY_EXPERIMENT_ACCESSION,
                                      new Object[]{exptAccession},
                                      new GeneMapper());

        List<Gene> genes = (List<Gene>) results;

        // populate the other info for these genes
        if (genes.size() > 0) {
            fillOutGenes(genes);
        }

        // and return
        return (List<Gene>) results;
    }

    public void getPropertiesForGenes(List<Gene> genes) {
        // populate the other info for these genes
        if (genes.size() > 0) {
            fillOutGenes(genes);
        }
    }

    public int getGeneCount() {
        Object result = template.query(GENE_COUNT_SELECT, new ResultSetExtractor() {

            public Object extractData(ResultSet resultSet) throws SQLException, DataAccessException {
                if (resultSet.next()) {
                    return resultSet.getInt(1);
                }
                else {
                    return 0;
                }
            }
        });

        return (Integer) result;
    }

    /**
     * Gets all assays in the database.  Note that, unlike other queries for assays, this query does not prepopulate all
     * property information.  This is done to keep the query time don to a minimum.  If you need this information, you
     * should populate it by calling {@link #getPropertiesForAssays(java.util.List)} on the list (or sublist) of assays
     * you wish to fetch properties for.  Bear in mind that doing this for a very large list of assays will result in a
     * slow query.
     *
     * @return the list of all assays in the database
     */
    public List<Assay> getAllAssays() {
        List results = template.query(ASSAYS_SELECT,
                                      new AssayMapper());

        // and return
        return (List<Assay>) results;
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

    public List<Assay> getAssaysByExperimentAndArray(String experimentAccession,
                                                     String arrayAccession) {
        List results = template.query(ASSAYS_BY_EXPERIMENT_AND_ARRAY_ACCESSION,
                                      new Object[]{experimentAccession,
                                                   arrayAccession},
                                      new AssayMapper());

        List<Assay> assays = (List<Assay>) results;

        // populate the other info for these assays
        if (assays.size() > 0) {
            fillOutAssays(assays);
        }

        // and return
        return assays;
    }

    public void getPropertiesForAssays(List<Assay> assays) {
        // populate the other info for these assays
        if (assays.size() > 0) {
            fillOutAssays(assays);
        }
    }

    public void getExpressionValuesForAssays(List<Assay> assays) {
        // fetch all expression values
        for (Assay assay : assays) {
            // fixme: this is inefficient - we'll end up generating lots of queries.  Is it better to handle with a big join?
            Object results = template.query(EXPRESSION_VALUES_BY_ASSAY_ID,
                                            new Object[]{assay.getAssayID()},
                                            new ExpressionValueMapper());

            // cast the result to the map, and extract the map for this assay
            Map<Integer, Map<Integer, Float>> map =
                    (Map<Integer, Map<Integer, Float>>) results;

            // extract and set the values map on this assay
            assay.setExpressionValues(map.get(assay.getAssayID()));
        }
    }

    public Map<Integer, Map<Integer, Float>> getExpressionValuesByExperimentAndArray(
            int experimentID, int arrayDesignID) {
        Object results = template.query(EXPRESSION_VALUES_BY_EXPERIMENT_AND_ARRAY,
                                        new Object[]{experimentID, arrayDesignID},
                                        new ExpressionValueMapper());


        return (Map<Integer, Map<Integer, Float>>) results;
    }

    public List<Sample> getSamplesByAssayAccession(String assayAccession) {
        List results = template.query(SAMPLES_BY_ASSAY_ACCESSION,
                                      new Object[]{assayAccession},
                                      new SampleMapper());
        List<Sample> samples = (List<Sample>) results;

        // populate the other info for these samples
        if (samples.size() > 0) {
            fillOutSamples(samples);
        }

        // and return
        return samples;
    }

    public List<Sample> getSamplesByExperimentAccession(String exptAccession) {
        List results = template.query(SAMPLES_BY_EXPERIMENT_ACCESSION,
                                      new Object[]{exptAccession},
                                      new SampleMapper());
        List<Sample> samples = (List<Sample>) results;


        // populate the other info for these samples
        if (samples.size() > 0) {
            fillOutSamples(samples);
        }

        // and return
        return samples;
    }

    public int getPropertyValueCount() {
        Object result = template.query(PROPERTY_VALUE_COUNT_SELECT, new ResultSetExtractor() {

            public Object extractData(ResultSet resultSet) throws SQLException, DataAccessException {
                if (resultSet.next()) {
                    return resultSet.getInt(1);
                }
                else {
                    return 0;
                }
            }
        });

        return (Integer) result;
    }

    public List<ArrayDesign> getAllArrayDesigns() {
        List results = template.query(ARRAY_DESIGN_SELECT,
                                      new ArrayDesignMapper());

        // cast to correct type
        List<ArrayDesign> arrayDesigns = (List<ArrayDesign>) results;

        // and populate design elements for each
        for (ArrayDesign arrayDesign : arrayDesigns) {
            arrayDesign.setDesignElements(
                    getDesignElementsByArrayID(arrayDesign.getArrayDesignID()));
        }

        return arrayDesigns;
    }

    public ArrayDesign getArrayDesignByAccession(String accession) {
        List results = template.query(ARRAY_DESIGN_BY_ACC_SELECT,
                                      new Object[]{accession},
                                      new ArrayDesignMapper());

        // get first result only
        ArrayDesign arrayDesign =
                results.size() > 0 ? (ArrayDesign) results.get(0) : null;

        if (arrayDesign != null) {
            arrayDesign.setDesignElements(
                    getDesignElementsByArrayID(arrayDesign.getArrayDesignID()));
        }

        return arrayDesign;
    }

    public List<ArrayDesign> getArrayDesignByExperimentAccession(
            String exptAccession) {
        List results = template.query(ARRAY_DESIGN_BY_EXPERIMENT_ACCESSION,
                                      new Object[]{exptAccession},
                                      new ArrayDesignMapper());

        // cast to correct type
        List<ArrayDesign> arrayDesigns = (List<ArrayDesign>) results;

        // and populate design elements for each
        for (ArrayDesign arrayDesign : arrayDesigns) {
            arrayDesign.setDesignElements(
                    getDesignElementsByArrayID(arrayDesign.getArrayDesignID()));
        }

        return arrayDesigns;
    }

    /**
     * A convenience method that fetches the set of design elements by array design accession.  Design elements are
     * recorded as a map, indexed by design element id and with a value of the design element accession. The set of
     * design element ids contains no duplicates, and the results that are returned are the internal database ids for
     * design elements.  This takes the accession of the array design as a parameter.
     *
     * @param arrayDesignAccession the accession number of the array design to query for
     * @return the map of design element accessions indexed by unique design element id integers
     */
    public Map<Integer, String> getDesignElementsByArrayAccession(
            String arrayDesignAccession) {
        Object results = template.query(DESIGN_ELEMENTS_BY_ARRAY_ACCESSION,
                                        new Object[]{arrayDesignAccession},
                                        new DesignElementMapper());
        return (Map<Integer, String>) results;
    }

    public Map<Integer, String> getDesignElementsByArrayID(
            int arrayDesignID) {
        Object results = template.query(DESIGN_ELEMENTS_BY_ARRAY_ID,
                                        new Object[]{arrayDesignID},
                                        new DesignElementMapper());
        return (Map<Integer, String>) results;
    }

    public Map<Integer, String> getDesignElementsByGeneID(int geneID) {
        Object results = template.query(DESIGN_ELEMENTS_BY_GENEID,
                                        new Object[]{geneID},
                                        new DesignElementMapper());
        return (Map<Integer, String>) results;
    }

    public List<ExpressionAnalysis> getExpressionAnalyticsByGeneID(
            int geneID) {
        List results = template.query(EXPRESSIONANALYTICS_BY_GENEID,
                                      new Object[]{geneID},
                                      new ExpressionAnalyticsMapper());
        return (List<ExpressionAnalysis>) results;
    }

    public List<ExpressionAnalysis> getExpressionAnalyticsByDesignElementID(
            int designElementID) {
        List results = template.query(EXPRESSIONANALYTICS_BY_DESIGNELEMENTID,
                                      new Object[]{designElementID},
                                      new ExpressionAnalyticsMapper());
        return (List<ExpressionAnalysis>) results;
    }

    public List<ExpressionAnalysis> getExpressionAnalyticsByExperimentID(
            int experimentID) {
        List results = template.query(EXPRESSIONANALYTICS_BY_EXPERIMENTID,
                                      new Object[]{experimentID},
                                      new ExpressionAnalyticsMapper());
        return (List<ExpressionAnalysis>) results;
    }

    public List<OntologyMapping> getOntologyMappings() {
        List results = template.query(ONTOLOGY_MAPPINGS_SELECT,
                                      new OntologyMappingMapper());
        return (List<OntologyMapping>) results;
    }

    public List<OntologyMapping> getOntologyMappingsByOntology(
            String ontologyName) {
        List results = template.query(ONTOLOGY_MAPPINGS_BY_ONTOLOGY_NAME,
                                      new Object[]{ontologyName},
                                      new OntologyMappingMapper());
        return (List<OntologyMapping>) results;
    }

    public List<OntologyMapping> getOntologyMappingsByExperimentAccession(
            String experimentAccession) {
        List results = template.query(ONTOLOGY_MAPPINGS_BY_EXPERIMENT_ACCESSION,
                                      new Object[]{experimentAccession},
                                      new OntologyMappingMapper());
        return (List<OntologyMapping>) results;
    }

    public List<AtlasCount> getAtlasCountsByExperimentID(int experimentID) {
        List results = template.query(ATLAS_COUNTS_BY_EXPERIMENTID,
                                      new Object[]{experimentID},
                                      new AtlasCountMapper());
        return (List<AtlasCount>) results;
    }

    public List<AtlasTableResult> getAtlasResults(int[] geneIds, int[] exptIds, int upOrDown, String[] efvs) {
        NamedParameterJdbcTemplate namedTemplate = new NamedParameterJdbcTemplate(template);

        MapSqlParameterSource parameters = new MapSqlParameterSource();
        parameters.addValue("geneids", geneIds);
        parameters.addValue("exptids", exptIds);
        parameters.addValue("efvs", efvs);

        List results;
        if (upOrDown == 1) {
            results = namedTemplate.query(ATLAS_RESULTS_UP_BY_EXPERIMENTID_GENEID_AND_EFV,
                                          parameters,
                                          new AtlasResultMapper());
        }
        else if (upOrDown == -1) {
            results = namedTemplate.query(ATLAS_RESULTS_DOWN_BY_EXPERIMENTID_GENEID_AND_EFV,
                                          parameters,
                                          new AtlasResultMapper());
        }
        else {
            results = namedTemplate.query(ATLAS_RESULTS_UPORDOWN_BY_EXPERIMENTID_GENEID_AND_EFV,
                                          parameters,
                                          new AtlasResultMapper());

        }

        return (List<AtlasTableResult>) results;
    }

    public AtlasStatistics getAtlasStatisticsByDataRelease(String dataRelease) {
        // manually count all experiments/genes/assays
        AtlasStatistics stats = new AtlasStatistics();

        stats.setDataRelease(dataRelease);
        stats.setExperimentCount(getAllExperiments().size());
        stats.setAssayCount(getAllAssays().size());
        stats.setGeneCount(getAllGenes().size());
        stats.setNewExperimentCount(0);
        stats.setPropertyValueCount(getPropertyValueCount());

        return stats;
    }

    /*
    DAO write methods
     */

    public void writeLoadDetails(final String experimentAccession,
                                 final LoadStage loadStage,
                                 final LoadStatus loadStatus) {
        // load monitor updates in it's own transaction
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            protected void doInTransactionWithoutResult(TransactionStatus transactionStatus) {
                try {
                    // create stored procedure call
                    SimpleJdbcCall procedure = new SimpleJdbcCall(template).withProcedureName("LOAD_PROGRESS");

                    // execute this procedure...
                    /*
                    create or replace procedure load_progress(
                      experiment_accession varchar
                      ,stage varchar --load, netcdf, similarity, ranking, searchindex
                      ,status varchar --done, pending
                    )
                    */

                    // map parameters...
                    MapSqlParameterSource params = new MapSqlParameterSource()
                            .addValue("experiment_accession", experimentAccession)
                            .addValue("stage", loadStage.toString().toLowerCase())
                            .addValue("status", loadStatus.toString().toLowerCase());

                    procedure.execute(params);
                }
                catch (Exception e) {
                    log.error("load_progress transaction update failed! " + e.getMessage());
                    e.printStackTrace();
                    transactionStatus.setRollbackOnly();
                    throw new RuntimeException(e);
                }
            }
        });
    }

    /**
     * Writes a fully comprehensive "bundle" of data related to one or many experiments.  The parameters passed should
     * be related experiments plus all the associated assays and samples.  All the related data will be written, or none
     * at all - if anything fails, all changes should be rolled back.
     *
     * @param experiments the collection of experiments to write to the datasource
     * @param assays      all assays associated with the collection of experiments being written
     * @param samples     all samples associated with the collection of experiments being written
     */
    public void writeExperimentsBundle(final Collection<Experiment> experiments,
                                       final Collection<Assay> assays,
                                       final Collection<Sample> samples) {
        // this operation runs in an isolated transaction - if one update fails, everything should be rolled back
        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            protected void doInTransactionWithoutResult(TransactionStatus transactionStatus) {
                try {
                    // write experiments first
                    for (Experiment expt : experiments) {
                        writeExperiment(expt);
                    }
                    // then write assays
                    for (Assay assay : assays) {
                        writeAssay(assay);
                    }
                    // finally write samples
                    for (Sample sample : samples) {
                        writeSample(sample);
                    }
                }
                catch (Exception e) {
                    log.error("Experiments bundle transaction update failed! " + e.getMessage());
                    e.printStackTrace();
                    transactionStatus.setRollbackOnly();
                    throw new RuntimeException(e);
                }
            }
        });
    }

    /**
     * Writes the given experiment to the database, using the default transaction strategy configured for the
     * datasource.
     *
     * @param experiment the experiment to write
     */
    public void writeExperiment(final Experiment experiment) {
        // execute stored procedure a2_ExperimentSet - standard param types
        SimpleJdbcCall procedure = new SimpleJdbcCall(template).withProcedureName("A2_EXPERIMENTSET");

        // execute this procedure...
        /*
        create or replace PROCEDURE "A2_EXPERIMENTSET" (
          TheAccession varchar2
          ,TheDescription varchar2
          ,ThePerformer varchar2
          ,TheLab varchar2
        )
        */

        // map parameters...
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("THEACCESSION", experiment.getAccession())
                .addValue("THEDESCRIPTION", experiment.getDescription())
                .addValue("THEPERFORMER", experiment.getPerformer())
                .addValue("THELAB", experiment.getLab());

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
        create or replace PROCEDURE "A2_ASSAYSET" (
           TheAccession varchar2
          ,TheExperimentAccession  varchar2
          ,TheArrayDesignAccession varchar2
          ,TheProperties PropertyTable
          ,TheExpressionValues ExpressionValueTable
        )
        */
        SimpleJdbcCall procedure =
                new SimpleJdbcCall(template)
                        .withProcedureName("A2_ASSAYSET")
                        .withoutProcedureColumnMetaDataAccess()
                        .useInParameterNames("THEACCESSION")
                        .useInParameterNames("THEEXPERIMENTACCESSION")
                        .useInParameterNames("THEARRAYDESIGNACCESSION")
                        .useInParameterNames("THEPROPERTIES")
                        .useInParameterNames("THEEXPRESSIONVALUES")
                        .declareParameters(
                                new SqlParameter("THEACCESSION", Types.VARCHAR))
                        .declareParameters(
                                new SqlParameter("THEEXPERIMENTACCESSION", Types.VARCHAR))
                        .declareParameters(
                                new SqlParameter("THEARRAYDESIGNACCESSION", Types.VARCHAR))
                        .declareParameters(
                                new SqlParameter("THEPROPERTIES", OracleTypes.ARRAY, "PROPERTYTABLE"))
                        .declareParameters(
                                new SqlParameter("THEEXPRESSIONVALUES", OracleTypes.ARRAY, "EXPRESSIONVALUETABLE"));

        // map parameters...
        List<Property> props = assay.getProperties() == null
                ? new ArrayList<Property>()
                : assay.getProperties();
        Map<String, Float> evs = assay.getExpressionValuesByAccession() == null
                ? new HashMap<String, Float>()
                : assay.getExpressionValuesByAccession();
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("THEACCESSION", assay.getAccession())
                .addValue("THEEXPERIMENTACCESSION", assay.getExperimentAccession())
                .addValue("THEARRAYDESIGNACCESSION", assay.getArrayDesignAccession())
                .addValue("THEPROPERTIES",
                          convertPropertiesToOracleARRAY(props),
                          OracleTypes.ARRAY,
                          "PROPERTYTABLE")
                .addValue("THEEXPRESSIONVALUES",
                          convertExpressionValuesToOracleARRAY(evs),
                          OracleTypes.ARRAY,
                          "EXPRESSIONVALUETABLE");

        // and execute
        procedure.execute(params);
    }

    /**
     * Writes the given sample to the database, using the default transaction strategy configured for the datasource.
     *
     * @param sample the sample to write
     */
    public void writeSample(final Sample sample) {
        // execute this procedure...
        /*
        create or replace PROCEDURE "A2_SAMPLESET" (
            p_Accession varchar2
          , p_Assays AccessionTable
          , p_Properties PropertyTable
          , p_Species varchar2
          , p_Channel varchar2
        )
        */
        SimpleJdbcCall procedure =
                new SimpleJdbcCall(template)
                        .withProcedureName("A2_SAMPLESET")
                        .withoutProcedureColumnMetaDataAccess()
                        .useInParameterNames("P_ACCESSION")
                        .useInParameterNames("P_ASSAYS")
                        .useInParameterNames("P_PROPERTIES")
                        .useInParameterNames("P_SPECIES")
                        .useInParameterNames("P_CHANNEL")
                        .declareParameters(
                                new SqlParameter("P_ACCESSION", Types.VARCHAR))
                        .declareParameters(
                                new SqlParameter("P_ASSAYS", OracleTypes.ARRAY, "ACCESSIONTABLE"))
                        .declareParameters(
                                new SqlParameter("P_PROPERTIES", OracleTypes.ARRAY, "PROPERTYTABLE"))
                        .declareParameters(
                                new SqlParameter("P_SPECIES", Types.VARCHAR))
                        .declareParameters(
                                new SqlParameter("P_CHANNEL", Types.VARCHAR));

        // map parameters...
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("P_ACCESSION", sample.getAccession())
                .addValue("P_ASSAYS",
                          convertAssayAccessionsToOracleARRAY(sample.getAssayAccessions()),
                          OracleTypes.ARRAY,
                          "ACCESSIONTABLE")
                .addValue("P_PROPERTIES",
                          convertPropertiesToOracleARRAY(sample.getProperties()),
                          OracleTypes.ARRAY,
                          "PROPERTYTABLE")
                .addValue("P_SPECIES", sample.getSpecies())
                .addValue("P_CHANNEL", sample.getChannel());

        // and execute
        procedure.execute(params);
    }

    /*
    utils methods for doing standard stuff
     */

    private void fillOutGenes(List<Gene> genes) {
        // map genes to gene id
        Map<Integer, Gene> genesByID = new HashMap<Integer, Gene>();
        for (Gene gene : genes) {
            // index this assay
            genesByID.put(gene.getGeneID(), gene);

            // also, initialize properties if null - once this method is called, you should never get an NPE
            if (gene.getProperties() == null) {
                gene.setProperties(new ArrayList<Property>());
            }
        }

        NamedParameterJdbcTemplate namedTemplate = new NamedParameterJdbcTemplate(template);

        // now query for properties that map to one of these genes
        GenePropertyMapper genePropertyMapper = new GenePropertyMapper(genesByID);
        MapSqlParameterSource propertyParams = new MapSqlParameterSource();
        propertyParams.addValue("geneids", genesByID.keySet());
        namedTemplate.query(PROPERTIES_BY_RELATED_GENES, propertyParams, genePropertyMapper);

        for (Gene gene : genes) {
            gene.setDesignElementIDs(getDesignElementsByGeneID(gene.getGeneID()).keySet());
        }
    }

    private void fillOutAssays(List<Assay> assays) {
        // map assays to assay id
        Map<Integer, Assay> assaysByID = new HashMap<Integer, Assay>();
        for (Assay assay : assays) {
            // index this assay
            assaysByID.put(assay.getAssayID(), assay);

            // also, initialize properties if null - once this method is called, you should never get an NPE
            if (assay.getProperties() == null) {
                assay.setProperties(new ArrayList<Property>());
            }
        }

        NamedParameterJdbcTemplate namedTemplate = new NamedParameterJdbcTemplate(template);

        // now query for properties that map to one of these samples
        AssayPropertyMapper assayPropertyMapper = new AssayPropertyMapper(assaysByID);
        MapSqlParameterSource propertyParams = new MapSqlParameterSource();
        propertyParams.addValue("assayids", assaysByID.keySet());
        namedTemplate.query(PROPERTIES_BY_RELATED_ASSAYS, propertyParams, assayPropertyMapper);
    }

    private void fillOutSamples(List<Sample> samples) {
        // map samples to sample id
        Map<Integer, Sample> samplesByID = new HashMap<Integer, Sample>();
        for (Sample sample : samples) {
            samplesByID.put(sample.getSampleID(), sample);

            // also, initialize properties/assays if null - once this method is called, you should never get an NPE
            if (sample.getProperties() == null) {
                sample.setProperties(new ArrayList<Property>());
            }
            if (sample.getAssayAccessions() == null) {
                sample.setAssayAccessions(new ArrayList<String>());
            }
        }

        NamedParameterJdbcTemplate namedTemplate = new NamedParameterJdbcTemplate(template);

        // now query for assays that map to one of these samples
        AssaySampleMapper assaySampleMapper = new AssaySampleMapper(samplesByID);
        MapSqlParameterSource assayParams = new MapSqlParameterSource();
        assayParams.addValue("sampleids", samplesByID.keySet());
        namedTemplate.query(ASSAYS_BY_RELATED_SAMPLES, assayParams, assaySampleMapper);

        // now query for properties that map to one of these samples
        SamplePropertyMapper samplePropertyMapper = new SamplePropertyMapper(samplesByID);
        MapSqlParameterSource propertyParams = new MapSqlParameterSource();
        propertyParams.addValue("sampleids", samplesByID.keySet());
        namedTemplate.query(PROPERTIES_BY_RELATED_SAMPLES, propertyParams, samplePropertyMapper);
    }

    private SqlTypeValue convertPropertiesToOracleARRAY(final List<Property> properties) {
        return new AbstractSqlTypeValue() {
            protected Object createTypeValue(Connection connection, int sqlType, String typeName) throws SQLException {
                // fixme: might need to extract the OracleConnection here if ClassCastExceptions occur - but hopefully spring manages this

                // this should be creating an oracle ARRAY of properties
                // the array of STRUCTS representing each property
                Object[] propArrayValues = new Object[properties.size()];

                // convert each property to an oracle STRUCT
                int i = 0;
                Object[] propStructValues = new Object[4];
                for (Property property : properties) {
                    // array representing the values to go in the STRUCT
                    propStructValues[0] = property.getAccession();
                    propStructValues[1] = property.getName();
                    propStructValues[2] = property.getValue();
                    propStructValues[3] = property.isFactorValue();

                    // descriptor for PROPERTY type
                    StructDescriptor structDescriptor = StructDescriptor.createDescriptor("PROPERTY", connection);
                    // each array value is a new STRUCT
                    propArrayValues[i++] = new STRUCT(structDescriptor, connection, propStructValues);
                }

                // created the array of STRUCTs, group into ARRAY
                ArrayDescriptor arrayDescriptor = ArrayDescriptor.createDescriptor(typeName, connection);
                return new ARRAY(arrayDescriptor, connection, propArrayValues);
            }
        };
    }

    private SqlTypeValue convertExpressionValuesToOracleARRAY(final Map<String, Float> expressionValues) {
        return new AbstractSqlTypeValue() {
            protected Object createTypeValue(Connection connection, int sqlType, String typeName) throws SQLException {
                // fixme: might need to extract the OracleConnection here if ClassCastExceptions occur - but hopefully spring manages this

                // this should be creating an oracle ARRAY of expression values
                // the array of STRUCTS representing each expression value
                Object[] evArrayValues = new Object[expressionValues.size()];

                // convert each property to an oracle STRUCT
                // descriptor for EXPRESSIONVALUE type
                StructDescriptor structDescriptor = StructDescriptor.createDescriptor("EXPRESSIONVALUE", connection);
                int i = 0;
                Object[] evStructValues = new Object[2];
                for (Map.Entry<String, Float> expressionValue : expressionValues.entrySet()) {
                    // array representing the values to go in the STRUCT
                    evStructValues[0] = expressionValue.getKey();
                    evStructValues[1] = expressionValue.getValue();

                    evArrayValues[i++] = new STRUCT(structDescriptor, connection, evStructValues);
                }

                // created the array of STRUCTs, group into ARRAY
                ArrayDescriptor arrayDescriptor = ArrayDescriptor.createDescriptor(typeName, connection);
                return new ARRAY(arrayDescriptor, connection, evArrayValues);
            }
        };
    }

    private SqlTypeValue convertAssayAccessionsToOracleARRAY(final List<String> assayAccessions) {
        return new AbstractSqlTypeValue() {
            protected Object createTypeValue(Connection connection, int sqlType, String typeName) throws SQLException {
                // fixme: might need to extract the OracleConnection here if ClassCastExceptions occur - but hopefully spring manages this

                Object[] accessions = new Object[assayAccessions.size()];
                int i = 0;
                for (String assayAccession : assayAccessions) {
                    accessions[i++] = assayAccession;
                }

                // created the array of STRUCTs, group into ARRAY
                ArrayDescriptor arrayDescriptor = ArrayDescriptor.createDescriptor(typeName, connection);
                return new ARRAY(arrayDescriptor, connection, accessions);
            }
        };
    }

    private class LoadDetailsMapper implements RowMapper {

        public Object mapRow(ResultSet resultSet, int i) throws SQLException {
            LoadDetails details = new LoadDetails();

            // accession, netcdf, similarity, ranking, searchindex
            details.setAccession(resultSet.getString(1));
            details.setNetCDF(resultSet.getString(2));
            details.setSimilarity(resultSet.getString(3));
            details.setRanking(resultSet.getString(4));
            details.setSearchIndex(resultSet.getString(5));
            details.setLoadType(resultSet.getString(6));

            return details;
        }
    }

    private class ExperimentMapper implements RowMapper {
        public Object mapRow(ResultSet resultSet, int i) throws SQLException {
            Experiment experiment = new Experiment();

            experiment.setAccession(resultSet.getString(1));
            experiment.setDescription(resultSet.getString(2));
            experiment.setPerformer(resultSet.getString(3));
            experiment.setLab(resultSet.getString(4));
            experiment.setExperimentID(resultSet.getInt(5));

            return experiment;
        }
    }

    private class GeneMapper implements RowMapper {
        public Gene mapRow(ResultSet resultSet, int i) throws SQLException {
            Gene gene = new Gene();

            gene.setGeneID(resultSet.getInt(1));
            gene.setIdentifier(resultSet.getString(2));
            gene.setName(resultSet.getString(3));
            gene.setSpecies(resultSet.getString(4));

            return gene;
        }
    }

    private class AssayMapper implements RowMapper {
        public Object mapRow(ResultSet resultSet, int i) throws SQLException {
            Assay assay = new Assay();

            assay.setAccession(resultSet.getString(1));
            assay.setExperimentAccession(resultSet.getString(2));
            assay.setArrayDesignAcession(resultSet.getString(3));
            assay.setAssayID(resultSet.getInt(4));

            return assay;
        }
    }

    private class ExpressionValueMapper implements ResultSetExtractor {
        public Object extractData(ResultSet resultSet)
                throws SQLException, DataAccessException {
            // maps assay ID (int) to a map of expression values - which is...
            // a map of design element IDs (int) to expression value (float)
            Map<Integer, Map<Integer, Float>> assayToEVs =
                    new HashMap<Integer, Map<Integer, Float>>();

            while (resultSet.next()) {
                // get assay ID key
                int assayID = resultSet.getInt(2);
                // get design element id key
                int designElementID = resultSet.getInt(1);
                // get expression value
                float value = resultSet.getFloat(4);
                // check assay key - can we add new expression value to existing map?
                if (!assayToEVs.containsKey(assayID)) {
                    // if not, create a new expression values maps
                    assayToEVs.put(assayID, new HashMap<Integer, Float>());
                }
                // insert the expression value map into the assay-linked map
                assayToEVs.get(assayID).put(designElementID, value);
            }

            return assayToEVs;
        }
    }

    private class SampleMapper implements RowMapper {
        public Object mapRow(ResultSet resultSet, int i) throws SQLException {
            Sample sample = new Sample();

            sample.setAccession(resultSet.getString(1));
            sample.setSpecies(resultSet.getString(2));
            sample.setChannel(resultSet.getString(3));
            sample.setSampleID(resultSet.getInt(4));

            return sample;
        }
    }

    private class AssaySampleMapper implements RowMapper {
        Map<Integer, Sample> samplesMap;

        public AssaySampleMapper(Map<Integer, Sample> samplesMap) {
            this.samplesMap = samplesMap;
        }

        public Object mapRow(ResultSet resultSet, int i) throws SQLException {
            int sampleID = resultSet.getInt(1);
            samplesMap.get(sampleID).addAssayAccession(resultSet.getString(2));
            return null;
        }
    }

    private class ArrayDesignMapper implements RowMapper {
        public Object mapRow(ResultSet resultSet, int i)
                throws SQLException {
            ArrayDesign array = new ArrayDesign();

            array.setAccession(resultSet.getString(1));
            array.setType(resultSet.getString(2));
            array.setName(resultSet.getString(3));
            array.setProvider(resultSet.getString(4));
            array.setArrayDesignID(resultSet.getInt(5));

            return array;
        }
    }

    private class DesignElementMapper implements ResultSetExtractor {
        public Object extractData(ResultSet resultSet)
                throws SQLException, DataAccessException {
            Map<Integer, String> designElements = new HashMap<Integer, String>();

            while (resultSet.next()) {
                designElements.put(resultSet.getInt(1), resultSet.getString(2));
            }

            return designElements;
        }
    }

    private class ExpressionAnalyticsMapper implements RowMapper {
        public Object mapRow(ResultSet resultSet, int i) throws SQLException {
            ExpressionAnalysis ea = new ExpressionAnalysis();

            ea.setEfName(resultSet.getString(1));
            ea.setEfvName(resultSet.getString(2));
            ea.setExperimentID(resultSet.getInt(3));
            ea.setDesignElementID(resultSet.getInt(4));
            ea.setTStatistic(resultSet.getDouble(5));
            ea.setPValAdjusted(resultSet.getDouble(6));

            return ea;
        }
    }

    private class OntologyMappingMapper implements RowMapper {
        public Object mapRow(ResultSet resultSet, int i) throws SQLException {
            OntologyMapping mapping = new OntologyMapping();

            mapping.setExperimentAccession(resultSet.getString(1));
            mapping.setProperty(resultSet.getString(2));
            mapping.setPropertyValue(resultSet.getString(3));
            mapping.setOntologyTerm(resultSet.getString(4));
            mapping.setSampleProperty(resultSet.getBoolean(5));
            mapping.setAssayProperty(resultSet.getBoolean(6));
            mapping.setFactorValue(resultSet.getBoolean(7));

            return mapping;
        }
    }

    private class AssayPropertyMapper implements RowMapper {
        private Map<Integer, Assay> assaysByID;

        public AssayPropertyMapper(Map<Integer, Assay> assaysByID) {
            this.assaysByID = assaysByID;
        }

        public Object mapRow(ResultSet resultSet, int i) throws SQLException {
            Property property = new Property();

            int assayID = resultSet.getInt(1);

            property.setName(resultSet.getString(2));
            property.setValue(resultSet.getString(3));
            property.setFactorValue(resultSet.getBoolean(4));

            assaysByID.get(assayID).addProperty(property);

            return property;
        }
    }

    private class SamplePropertyMapper implements RowMapper {
        private Map<Integer, Sample> samplesByID;

        public SamplePropertyMapper(Map<Integer, Sample> samplesByID) {
            this.samplesByID = samplesByID;
        }

        public Object mapRow(ResultSet resultSet, int i) throws SQLException {
            Property property = new Property();

            int sampleID = resultSet.getInt(1);

            property.setName(resultSet.getString(2));
            property.setValue(resultSet.getString(3));
            property.setFactorValue(resultSet.getBoolean(4));

            samplesByID.get(sampleID).addProperty(property);

            return property;
        }
    }

    private class GenePropertyMapper implements RowMapper {
        private Map<Integer, Gene> genesByID;

        public GenePropertyMapper(Map<Integer, Gene> genesByID) {
            this.genesByID = genesByID;
        }

        public Object mapRow(ResultSet resultSet, int i) throws SQLException {
            Property property = new Property();

            int geneID = resultSet.getInt(1);

            property.setName(resultSet.getString(2));
            property.setValue(resultSet.getString(3));
            property.setFactorValue(false);

            genesByID.get(geneID).addProperty(property);

            return property;
        }
    }

    // todo - AtlasCount and AtlasResult can probably be consolidated to link a collection of genes to atlas results
    private class AtlasCountMapper implements RowMapper {
        public Object mapRow(ResultSet resultSet, int i) throws SQLException {
            AtlasCount atlasCount = new AtlasCount();

            atlasCount.setProperty(resultSet.getString(2));
            atlasCount.setPropertyValue(resultSet.getString(3));
            atlasCount.setUpOrDown(resultSet.getString(4));
            atlasCount.setGeneCount(resultSet.getInt(6));

            return atlasCount;
        }
    }

    private class AtlasResultMapper implements RowMapper {
        public Object mapRow(ResultSet resultSet, int i) throws SQLException {
            AtlasTableResult atlasTableResult = new AtlasTableResult();

            atlasTableResult.setExperimentID(resultSet.getInt(1));
            atlasTableResult.setGeneID(resultSet.getInt(2));
            atlasTableResult.setProperty(resultSet.getString(3));
            atlasTableResult.setPropertyValue(resultSet.getString(4));
            atlasTableResult.setUpOrDown(resultSet.getString(5));
            atlasTableResult.setPValAdj(resultSet.getDouble(6));

            return atlasTableResult;
        }
    }
}
