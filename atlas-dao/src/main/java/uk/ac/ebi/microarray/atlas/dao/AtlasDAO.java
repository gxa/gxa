package uk.ac.ebi.microarray.atlas.dao;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowMapper;
import uk.ac.ebi.microarray.atlas.model.*;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A data access object designed for retrieving common sorts of data from the
 * atlas database.  This DAO should be configured with a spring {@link
 * JdbcTemplate} object which will be used to query the database.
 *
 * @author Tony Burdett
 * @date 21-Sep-2009
 */
public class AtlasDAO {
    // experiment queries
    private static final String EXPERIMENTS_SELECT =
            "SELECT accession, description, performer, lab, experimentid " +
                    "FROM a2_experiment";
    private static final String EXPERIMENTS_PENDING_INDEX_SELECT =
            EXPERIMENTS_SELECT + " " +
                    "WHERE something something"; // fixme: load monitor table?
    private static final String EXPERIMENTS_PENDING_NETCDF_SELECT =
            EXPERIMENTS_SELECT + " " +
                    "WHERE something something"; // fixme: load monitor table?
    private static final String EXPERIMENT_BY_ACC_SELECT =
            EXPERIMENTS_SELECT + " " +
                    "WHERE accession=?";

    // gene queries
    private static final String GENES_SELECT =
            "SELECT g.geneid, g.identifier, g.name, " +
                    "s.name AS species, d.designelementid " +
                    "FROM a2_gene g, a2_spec s, a2_designelement d " +
                    "WHERE g.geneid=d.geneid " +
                    "AND g.specid=s.specid";
    private static final String GENES_PENDING_SELECT =
            GENES_SELECT + " " +
                    "AND something something"; // fixme: load monitor table?
    private static final String GENES_BY_EXPERIMENT_ACCESSION =
            "SELECT DISTINCT g.geneid, g.identifier, g.name, " +
                    "s.name AS species, d.designelementid " +
                    "FROM a2_gene g, a2_spec s, a2_designelement d, a2_assay a, " +
                    "a2_experiment e " +
                    "WHERE g.geneid=d.geneid " +
                    "AND g.specid = s.specid " +
                    "AND d.arraydesignid=a.arraydesignid " +
                    "AND a.experimentid=e.experimentid " +
                    "AND e.accession=?";
    private static final String PROPERTIES_BY_GENEID =
            "SELECT gp.name AS property, gpv.value AS propertyvalue " +
                    "FROM a2_geneproperty gp, a2_genepropertyvalue gpv " +
                    "WHERE gpv.genepropertyid=gp.genepropertyid " +
                    "AND gpv.geneid=?";

    // assay queries
    private static final String ASSAYS_BY_EXPERIMENT_ACCESSION =
            "SELECT a.accession, e.accession, ad.accession, a.assayid " +
                    "FROM a2_assay a, a2_experiment e, a2_arraydesign ad " +
                    "WHERE e.experimentid=a.experimentid " +
                    "AND a.arraydesignid=ad.arraydesignid " +
                    "AND e.accession=?";
    private static final String ASSAYS_BY_EXPERIMENT_AND_ARRAY_ACCESSION =
            ASSAYS_BY_EXPERIMENT_ACCESSION + " " +
                    "AND ad.accession=?";
    private static final String PROPERTIES_BY_ASSAY_ACCESSION =
            "SELECT p.name AS property, pv.name AS propertyvalue, " +
                    "apv.isfactorvalue " +
                    "FROM a2_property p, a2_propertyvalue pv, " +
                    "a2_assaypropertyvalue apv, a2_assay a " +
                    "WHERE apv.propertyvalueid=pv.propertyvalueid " +
                    "AND pv.propertyid=p.propertyid " +
                    "AND apv.assayid=a.assayid " +
                    "AND a.accession=?";

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
    private static final String PROPERTIES_BY_SAMPLE_ACCESSION =
            "SELECT " +
                    "p.name AS property, " +
                    "pv.name AS propertyvalue, " +
                    "spv.isfactorvalue " +
                    "FROM " +
                    "a2_property p, " +
                    "a2_propertyvalue pv, " +
                    "a2_samplepropertyvalue spv, " +
                    "a2_sample s " +
                    "WHERE spv.propertyvalueid=pv.propertyvalueid " +
                    "AND pv.propertyid=p.propertyid " +
                    "AND spv.sampleid=s.sampleid " +
                    "AND s.accession=?";

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
    private static final String ATLAS_COUNTS_BY_EXPERIMENTID =
            "SELECT " +
                    "p.name AS property, " +
                    "pv.name AS propertyvalue, " +
                    "CASE WHEN ea.tstat < 0 THEN -1 ELSE 1 END AS updn, " +
                    "COUNT(DISTINCT(g.GeneID)) AS genes " +
                    "FROM a2_expressionanalytics ea " +
                    "JOIN a2_propertyvalue pv ON pv.propertyvalueid=ea.propertyvalueid " +
                    "JOIN a2_property p ON p.propertyid=pv.propertyid " +
                    "JOIN a2_designelement de ON de.designelementid=ea.designelementid " +
                    "JOIN a2_gene g ON g.geneid=de.geneid " +
                    "WHERE ea.experimentid=? " +
                    "GROUP BY p.name, pv.name, CASE WHEN ea.tstat < 0 THEN -1 ELSE 1 END";
    // fixme: need atlas interface queries in the DAO
    private static final String ATLAS_RESULTS_SELECT =
            "SELECT * FROM (" +
                    "SELECT atlas.experiment_id_key, atlas.gene_id_key, atlas.ef, atlas.efv, atlas.updn, " +
                    "atlas.updn_tstat, atlas.updn_pvaladj, row_number() " +
                    "OVER (" +
                    "PARTITION BY atlas.experiment_id_key, atlas.ef, atlas.efv " +
                    "ORDER BY atlas.updn_pvaladj asc, updn_tstat desc) " +
                    "TOPN FROM aemart.atlas atlas " +
                    "WHERE gene_id_key <> 0 " +
                    "AND atlas.experiment_id_key " +
                    "NOT IN (211794549,215315583,384555530,411493378,411512559) " +
                    "AND atlas.gene_id_key IN (?) " +
                    "AND atlas.experiment_id_key IN (?) " +
                    "AND updn=?" +
                    "AND atlas.efv in (?))" +
                    "WHERE TOPN <= 20 and gene_id_key is not null " +
                    "ORDER by updn_pvaladj asc, updn_tstat desc";
    private static final String ATLAS_RESULTS_NO_UPDN_SELECT =
            "SELECT * FROM (" +
                    "SELECT atlas.experiment_id_key, atlas.gene_id_key, atlas.ef, atlas.efv, atlas.updn, " +
                    "atlas.updn_tstat, atlas.updn_pvaladj, row_number() " +
                    "OVER (" +
                    "PARTITION BY atlas.experiment_id_key, atlas.ef, atlas.efv " +
                    "ORDER BY atlas.updn_pvaladj asc, updn_tstat desc) " +
                    "TOPN FROM aemart.atlas atlas " +
                    "WHERE gene_id_key <> 0 " +
                    "AND atlas.experiment_id_key " +
                    "NOT IN (211794549,215315583,384555530,411493378,411512559) " +
                    "AND atlas.gene_id_key IN (?) " +
                    "AND atlas.experiment_id_key IN (?) " +
                    "AND updn <> ?" +
                    "AND atlas.efv in (?))" +
                    "WHERE TOPN <= 20 and gene_id_key is not null " +
                    "ORDER by updn_pvaladj asc, updn_tstat desc";

    private JdbcTemplate template;

    public JdbcTemplate getJdbcTemplate() {
        return template;
    }

    public void setJdbcTemplate(JdbcTemplate template) {
        this.template = template;
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

    /**
     * Gets a single experiment from the Atlas Database, queried by the accession
     * of the experiment.
     *
     * @param accession the experiment's accession number (usually in the format
     *                  E-ABCD-1234)
     * @return an object modelling this experiment
     */
    public Experiment getExperimentByAccession(String accession) {
        List results = template.query(EXPERIMENT_BY_ACC_SELECT,
                new Object[]{accession},
                new ExperimentMapper());

        return results.size() > 0 ? (Experiment) results.get(0) : null;
    }

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
        return (List<Gene>) results;

    }

    public void getPropertiesForGenes(List<Gene> genes) {
        // also fetch all properties
        for (Gene gene : genes) {
            // fixme: this is inefficient - we'll end up generating lots of queries.  Is it better to handle with a big join?
            List propResults = template.query(PROPERTIES_BY_GENEID,
                    new Object[]{gene.getGeneID()},
                    new GenePropertyMapper());
            // and set on gene
            gene.setProperties(propResults);
        }
    }

    public List<Assay> getAssaysByExperimentAccession(
            String experimentAccession) {
        List results = template.query(ASSAYS_BY_EXPERIMENT_ACCESSION,
                new Object[]{experimentAccession},
                new AssayMapper());

        List<Assay> assays = (List<Assay>) results;

        // also fetch all properties
        for (Assay assay : assays) {
            // fixme: this is inefficient - we'll end up generating lots of queries.  Is it better to handle with a big join?
            List propResults = template.query(PROPERTIES_BY_ASSAY_ACCESSION,
                    new Object[]{assay.getAccession()},
                    new PropertyMapper());
            // and set on assay
            assay.setProperties(propResults);
        }

        return assays;
    }

    public List<Assay> getAssaysByExperimentAndArray(String experimentAccession,
                                                     String arrayAccession) {
        List results = template.query(ASSAYS_BY_EXPERIMENT_AND_ARRAY_ACCESSION,
                new Object[]{experimentAccession,
                        arrayAccession},
                new AssayMapper());

        List<Assay> assays = (List<Assay>) results;

        // also fetch all properties
        for (Assay assay : assays) {
            // fixme: this is inefficient - we'll end up generating lots of queries.  Is it better to handle with a big join?
            List propResults = template.query(PROPERTIES_BY_ASSAY_ACCESSION,
                    new Object[]{assay.getAccession()},
                    new PropertyMapper());
            // and set on assay
            assay.setProperties(propResults);
        }

        return assays;
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

        // fixme: this doesn't adequately retrieve all assay accessions for samples
        List<Sample> samples = (List<Sample>) results;

        // also fetch all properties
        for (Sample sample : samples) {
            // fixme: set assay accession, this is broken as it only sets one assay on this sample
            List<String> assays = new ArrayList<String>();
            assays.add(assayAccession);
            sample.setAssayAccessions(assays);

            // fixme: this is inefficient - we'll end up generating lots of queries.  Is it better to handle with a big join?
            List propResults = template.query(PROPERTIES_BY_SAMPLE_ACCESSION,
                    new Object[]{sample.getAccession()},
                    new PropertyMapper());
            // and set on assay
            sample.setProperties(propResults);
        }

        return samples;
    }

    public List<Sample> getSamplesByExperimentAccession(String exptAccession) {
        List results = template.query(SAMPLES_BY_EXPERIMENT_ACCESSION,
                new Object[]{exptAccession},
                new SampleMapper());

        // fixme: this doesn't adequately retrieve all assay accessions for samples
        List<Sample> samples = (List<Sample>) results;

        // also fetch all properties
        for (Sample sample : samples) {
            // fixme: using this query, we don't relate set assay accessions on sample at all

            // fixme: this is inefficient - we'll end up generating lots of queries.  Is it better to handle with a big join?
            List propResults = template.query(PROPERTIES_BY_SAMPLE_ACCESSION,
                    new Object[]{sample.getAccession()},
                    new PropertyMapper());
            // and set on assay
            sample.setProperties(propResults);
        }

        return samples;
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
     * A convenience method that fetches the set of design elements by array
     * design accession.  Design elements are recorded as a map, indexed by design
     * element id and with a value of the design element accession. The set of
     * design element ids contains no duplicates, and the results that are
     * returned are the internal database ids for design elements.  This takes the
     * accession of the array design as a parameter.
     *
     * @param arrayDesignAccession the accession number of the array design to
     *                             query for
     * @return the map of design element accessions indexed by unique design
     *         element id integers
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

    public AtlasResult getAtlasResults(int[] geneIds, int[] exptIds, int upOrDown, String efvFilter) {
        // todo - send the query, really
        return null;
    }

    public AtlasStatistics getAtlasStatistics(int lastExperimentId, String dataRelease) {
        // todo - send the query, really
        return null;
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
            gene.setDesignElementID(resultSet.getInt(5));

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

    private class PropertyMapper implements RowMapper {
        public Object mapRow(ResultSet resultSet, int i) throws SQLException {
            Property property = new Property();

            property.setName(resultSet.getString(1));
            property.setValue(resultSet.getString(2));
            property.setFactorValue(resultSet.getBoolean(3));

            return property;
        }
    }

    private class GenePropertyMapper implements RowMapper {

        public Object mapRow(ResultSet resultSet, int i) throws SQLException {
            Property property = new Property();

            property.setName(resultSet.getString(1));
            property.setValue(resultSet.getString(2));
            property.setFactorValue(false);

            return property;
        }
    }

    private class AtlasCountMapper implements RowMapper {
        public Object mapRow(ResultSet resultSet, int i) throws SQLException {
            AtlasCount atlasCount = new AtlasCount();

            atlasCount.setProperty(resultSet.getString(1));
            atlasCount.setPropertyValue(resultSet.getString(2));
            atlasCount.setUpOrDown(resultSet.getString(3));
            atlasCount.setGeneCount(resultSet.getInt(4));

            return atlasCount;
        }
    }
}
