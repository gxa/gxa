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
            "SELECT accession, status, netcdf, similarity, ranking, searchindex, load_type " +
                    "FROM load_monitor";
    private static final String LOAD_MONITOR_BY_ACC_SELECT =
            LOAD_MONITOR_SELECT + " " +
                    "WHERE accession=?";

    // experiment queries
    private static final String EXPERIMENTS_COUNT =
            "SELECT COUNT(*) FROM a2_experiment";

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
    private static final String GENES_COUNT =
            "SELECT COUNT(*) FROM a2_gene";

    private static final String GENES_SELECT =
            "SELECT DISTINCT g.geneid, g.identifier, g.name, s.name AS species " +
                    "FROM a2_gene g, a2_spec s " +
                    "WHERE g.specid=s.specid";
    private static final String GENE_BY_IDENTIFIER =
            "SELECT DISTINCT g.geneid, g.identifier, g.name, s.name AS species " +
                    "FROM a2_gene g, a2_spec s " +
                    "WHERE g.identifier=?";
    private static final String DESIGN_ELEMENTS_AND_GENES_SELECT =
            "SELECT de.geneid, de.designelementid " +
                    "FROM a2_designelement de";
    private static final String GENES_PENDING_SELECT =
            "SELECT DISTINCT g.geneid, g.identifier, g.name, s.name AS species " +
                    "FROM a2_gene g, a2_spec s, load_monitor lm " +
                    "WHERE g.specid=s.specid " +
                    "AND g.identifier=lm.accession " +
                    "AND (lm.searchindex='pending' OR lm.searchindex='failed') " +
                    "AND lm.load_type='gene'";
    private static final String DESIGN_ELEMENTS_AND_GENES_PENDING_SELECT =
            "SELECT de.geneid, de.designelementid " +
                    "FROM a2_designelement de, a2_gene g, load_monitor lm " +
                    "WHERE de.geneid=g.geneid " +
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
    private static final String DESIGN_ELEMENTS_AND_GENES_BY_EXPERIMENT_ACCESSION =
            "SELECT de.geneid, de.designelementid " +
                    "FROM a2_designelement de, a2_assay a, a2_experiment e " +
                    "WHERE de.arraydesignid=a.arraydesignid " +
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
    private static final String ASSAYS_COUNT =
            "SELECT COUNT(*) FROM a2_assay";

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
    private static final String EXPRESSION_VALUES_BY_RELATED_ASSAYS =
            "SELECT ev.assayid, ev.designelementid, ev.value " +
                    "FROM a2_expressionvalue ev, a2_designelement de " +
                    "WHERE ev.designelementid=de.designelementid " +
                    "AND ev.assayid IN (:assayids)";
    private static final String EXPRESSION_VALUES_BY_EXPERIMENT_AND_ARRAY =
            "SELECT ev.assayid, ev.designelementid, ev.value " +
                    "FROM a2_expressionvalue ev " +
                    "JOIN a2_designelement de ON de.designelementid=ev.designelementid " +
                    "JOIN a2_assay a ON a.assayid = ev.assayid " +
                    "WHERE a.experimentid=? AND de.arraydesignid=?";

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
    private static final String DESIGN_ELEMENTS_BY_RELATED_ARRAY =
            "SELECT de.arraydesignid, de.designelementid, de.accession " +
                    "FROM a2_designelement de " +
                    "WHERE de.arraydesignid IN (:arraydesignids)";
    private static final String DESIGN_ELEMENTS_BY_GENEID =
            "SELECT de.designelementid, de.accession " +
                    "FROM a2_designelement de " +
                    "WHERE de.geneid=?";

    // other useful queries
    private static final String EXPRESSIONANALYTICS_BY_EXPERIMENTID =
            "SELECT ef.name AS ef, efv.name AS efv, a.experimentid, " +
                    "a.designelementid, a.tstat, a.pvaladj, " +
                    "ef.propertyid as efid, efv.propertyvalueid as efvid " +
                    "FROM a2_expressionanalytics a " +
                    "JOIN a2_propertyvalue efv ON efv.propertyvalueid=a.propertyvalueid " +
                    "JOIN a2_property ef ON ef.propertyid=efv.propertyid " +
                    "JOIN a2_designelement de ON de.designelementid=a.designelementID " +
                    "WHERE a.experimentid=?";
    private static final String EXPRESSIONANALYTICS_BY_GENEID =
            "SELECT ef.name AS ef, efv.name AS efv, a.experimentid, a.geneid, " +
                    "a.tstat, a.pvaladj, " +
                    "ef.propertyid as efid, efv.propertyvalueid as efvid " +
                    "FROM a2_expressionanalytics a " +
                    "JOIN a2_propertyvalue efv ON efv.propertyvalueid=a.propertyvalueid " +
                    "JOIN a2_property ef ON ef.propertyid=efv.propertyid " +
                    "WHERE a.geneid=?";
    private static final String EXPRESSIONANALYTICS_BY_DESIGNELEMENTID =
            "SELECT ef.name AS ef, efv.name AS efv, a.experimentid, a.geneid, " +
                    "a.tstat, a.pvaladj, " +
                    "ef.propertyid as efid, efv.propertyvalueid as efvid " +
                    "FROM a2_expressionanalytics a " +
                    "JOIN a2_propertyvalue efv ON efv.propertyvalueid=a.propertyvalueid " +
                    "JOIN a2_property ef ON ef.propertyid=efv.propertyid " +
                    "WHERE a.designelementid=?";
    private static final String ONTOLOGY_MAPPINGS_SELECT =
            "SELECT DISTINCT accession, property, propertyvalue, ontologyterm, " +
                    "issampleproperty, isassayproperty, isfactorvalue, experimentid " +
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
                    "min(ea.pvaladj), " +
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
                    "min(ea.pvaladj), " +
                    "COUNT(DISTINCT(g.geneid)) AS genes, " +
                    "min(p.propertyid) AS propertyid, " +
                    "min(pv.propertyvalueid)  AS propertyvalueid " +
                    "FROM a2_expressionanalytics ea " +
                    "JOIN a2_propertyvalue pv ON pv.propertyvalueid=ea.propertyvalueid " +
                    "JOIN a2_property p ON p.propertyid=pv.propertyid " +
                    "JOIN a2_designelement de ON de.designelementid=ea.designelementid " +
                    "JOIN a2_gene g ON g.geneid=de.geneid";
    private static final String ATLAS_COUNTS_BY_EXPERIMENTID =
            ATLAS_COUNTS_SELECT + " " +
                    "WHERE ea.experimentid=? " +
                    "GROUP BY ea.experimentid, p.name, pv.name, " +
                    "CASE WHEN ea.tstat < 0 THEN -1 ELSE 1 END";
    private static final String ATLAS_RESULTS_UP_BY_EXPERIMENTID_GENEID_AND_EFV =
            ATLAS_RESULTS_SELECT + " " +
                    "WHERE ea.experimentid IN (:exptids) " +
                    "AND g.geneid IN (:geneids) " +
                    "AND pv.name IN (:efvs) " +
                    "AND updn='1' " +
                    "AND TOPN<=20 " +
                    "ORDER BY ea.pvaladj " +
                    "GROUP BY ea.experimentid, g.geneid, p.name, pv.name, " +
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

    private static final String SPECIES_ALL =
            "SELECT specid, name FROM A2_SPEC";

    private static final String PROPERTIES_ALL =
            "SELECT min(p.propertyid), p.name, min(pv.propertyvalueid), pv.name, 1 as isfactorvalue " +
                    "FROM a2_property p, a2_propertyvalue pv " +
                    "WHERE  pv.propertyid=p.propertyid GROUP BY p.name, pv.name";

    private static final String GENEPROPERTY_ALL_NAMES =
            "SELECT name FROM A2_GENEPROPERTY";

    private JdbcTemplate template;
    private int maxQueryParams = 500;

    private Logger log = LoggerFactory.getLogger(getClass());

    public JdbcTemplate getJdbcTemplate() {
        return template;
    }

    public void setJdbcTemplate(JdbcTemplate template) {
        this.template = template;
    }

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

    /**
     * Set the maximum allowed number of parameters that can be supplied to a parameterised query.  This is effectively
     * the maximum bound for an "IN" list - i.e. SELECT * FROM foo WHERE foo.bar IN (?,?,?,...,?).  If unset, this
     * defaults to 500.  Typically, the limit for oracle databases is 1000.  If, for any query that takes a list, the
     * size of the list is greater than this value, the query will be split into several smaller subqueries and the
     * results aggregated.
     *
     * @param maxQueryParams the maximum bound on the query list size - this should never be greater than that allowed
     *                       by the database, but can be smaller
     */
    public void setMaxQueryParams(int maxQueryParams) {
        this.maxQueryParams = maxQueryParams;
    }

    /*
   DAO read methods
    */

    public List<LoadDetails> getLoadDetails() {
        List results = template.query(LOAD_MONITOR_SELECT,
                                      new LoadDetailsMapper());
        return (List<LoadDetails>) results;
    }

    public LoadDetails getLoadDetailsByAccession(String accession) {
        List results = template.query(LOAD_MONITOR_BY_ACC_SELECT,
                                      new Object[]{accession},
                                      new LoadDetailsMapper());
        return results.size() > 0 ? (LoadDetails) results.get(0) : null;
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
     * properties for by calling {@link #getPropertiesForGenes(java.util.List)}.  Genes <b>are</b> prepopulated with
     * design element information, however.
     *
     * @return the list of all genes in the database
     */
    public List<Gene> getAllGenes() {
        // do the first query to fetch genes without design elements
        List results = template.query(GENES_SELECT,
                                      new GeneMapper());

        // do the second query to obtain design elements
        List<Gene> genes = (List<Gene>) results;

        // map genes to gene id
        Map<Integer, Gene> genesByID = new HashMap<Integer, Gene>();
        for (Gene gene : genes) {
            // index this assay
            genesByID.put(gene.getGeneID(), gene);

            // also, initialize properties if null - once this method is called, you should never get an NPE
            if (gene.getDesignElementIDs() == null) {
                gene.setDesignElementIDs(new HashSet<Integer>());
            }
        }

        // map of genes and their design elements
        GeneDesignElementMapper geneDesignElementMapper = new GeneDesignElementMapper(genesByID);

        // now query for design elements, and genes, by the experiment accession, and map them together
        template.query(DESIGN_ELEMENTS_AND_GENES_SELECT,
                       geneDesignElementMapper);
        // and return
        return genes;
    }

    public Gene getGeneByIdentifier(String identifier) {
        // do the query to fetch gene without design elements
        List results = template.query(GENE_BY_IDENTIFIER,
                                      new Object[]{identifier},
                                      new GeneMapper());

        if (results.size() > 0) {
            Gene gene = (Gene) results.get(0);
            gene.setDesignElementIDs(getDesignElementsByGeneID(gene.getGeneID()).keySet());
            return gene;
        }
        return null;
    }


    /**
     * Fetches all genes in the database that are pending indexing.  Note that genes are not automatically prepopulated
     * with property information, to keep query time down.  If you require this data, you can fetch it for the list of
     * genes you want to obtain properties for by calling {@link #getPropertiesForGenes(java.util.List)}.  Genes
     * <b>are</b> prepopulated with design element information, however.
     *
     * @return the list of all genes in the database that are pending indexing
     */
    public List<Gene> getAllPendingGenes() {
        // do the first query to fetch genes without design elements
        List results = template.query(GENES_PENDING_SELECT,
                                      new GeneMapper());

        // do the second query to obtain design elements
        List<Gene> genes = (List<Gene>) results;

        // map genes to gene id
        Map<Integer, Gene> genesByID = new HashMap<Integer, Gene>();
        for (Gene gene : genes) {
            // index this assay
            genesByID.put(gene.getGeneID(), gene);

            // also, initialize properties if null - once this method is called, you should never get an NPE
            if (gene.getDesignElementIDs() == null) {
                gene.setDesignElementIDs(new HashSet<Integer>());
            }
        }

        // map of genes and their design elements
        GeneDesignElementMapper geneDesignElementMapper = new GeneDesignElementMapper(genesByID);

        // now query for design elements, and genes, by the experiment accession, and map them together
        template.query(DESIGN_ELEMENTS_AND_GENES_PENDING_SELECT,
                       geneDesignElementMapper);
        // and return
        return genes;
    }

    /**
     * Fetches all genes for the given experiment accession.  Note that genes are not automatically prepopulated with
     * property information, to keep query time down.  If you require this data, you can fetch it for the list of genes
     * you want to obtain properties for by calling {@link #getPropertiesForGenes(java.util.List)}.  Genes <b>are</b>
     * prepopulated with design element information, however.
     *
     * @param exptAccession the accession number of the experiment to query for
     * @return the list of all genes in the database for this experiment accession
     */
    public List<Gene> getGenesByExperimentAccession(String exptAccession) {
        // do the first query to fetch genes without design elements
        log.debug("Querying for genes by experiment " + exptAccession);
        List results = template.query(GENES_BY_EXPERIMENT_ACCESSION,
                                      new Object[]{exptAccession},
                                      new GeneMapper());
        log.debug("Genes for " + exptAccession + " acquired");

        // do the second query to obtain design elements
        List<Gene> genes = (List<Gene>) results;

        // map genes to gene id
        Map<Integer, Gene> genesByID = new HashMap<Integer, Gene>();
        for (Gene gene : genes) {
            // index this assay
            genesByID.put(gene.getGeneID(), gene);

            // also, initialize properties if null - once this method is called, you should never get an NPE
            if (gene.getDesignElementIDs() == null) {
                gene.setDesignElementIDs(new HashSet<Integer>());
            }
        }

        // map of genes and their design elements
        GeneDesignElementMapper geneDesignElementMapper = new GeneDesignElementMapper(genesByID);

        // now query for design elements, and genes, by the experiment accession, and map them together
        log.debug("Querying for design elements mapped to genes of " + exptAccession);
        template.query(DESIGN_ELEMENTS_AND_GENES_BY_EXPERIMENT_ACCESSION,
                       new Object[]{exptAccession},
                       geneDesignElementMapper);
        log.debug("Design elements for genes of " + exptAccession + " acquired");

        // and return
        return genes;
    }

    public void getPropertiesForGenes(List<Gene> genes) {
        // populate the other info for these genes
        if (genes.size() > 0) {
            fillOutGeneProperties(genes);
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
        // map assays to assay id
        Map<Integer, Assay> assaysByID = new HashMap<Integer, Assay>();
        for (Assay assay : assays) {
            // index this assay
            assaysByID.put(assay.getAssayID(), assay);

            // also, initialize properties if null - once this method is called, you should never get an NPE
            if (assay.getExpressionValues() == null) {
                assay.setExpressionValues(new HashMap<Integer, Float>());
            }
        }

        // maps properties to assays
        AssayExpressionValueMapper assayExpressionValueMapper = new AssayExpressionValueMapper(assaysByID);

        // query template for assays
        NamedParameterJdbcTemplate namedTemplate = new NamedParameterJdbcTemplate(template);

        // if we have more than 'maxQueryParams' assays, split into smaller queries
        List<Integer> assayIDs = new ArrayList<Integer>(assaysByID.keySet());
        boolean done = false;
        int startpos = 0;
        int endpos = maxQueryParams;

        while (!done) {
            List<Integer> assayIDsChunk;
            if (endpos > assayIDs.size()) {
                // we've reached the last segment, so query all of these
                assayIDsChunk = assayIDs.subList(startpos, assayIDs.size());
                done = true;
            }
            else {
                // still more left - take next sublist and increment counts
                assayIDsChunk = assayIDs.subList(startpos, endpos);
                startpos = endpos;
                endpos += maxQueryParams;
            }

            // now query for properties that map to one of the samples in the sublist
            MapSqlParameterSource propertyParams = new MapSqlParameterSource();
            propertyParams.addValue("assayids", assayIDsChunk);
            namedTemplate.query(EXPRESSION_VALUES_BY_RELATED_ASSAYS, propertyParams, assayExpressionValueMapper);
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

    /**
     * Returns all array designs in the underlying datasource.  Note that, to reduce query times, this method does NOT
     * prepopulate ArrayDesigns with their associated design elements (unlike other methods to retrieve array designs
     * more specifically).  For this reason, you should always ensure that after calling this method you use the {@link
     * #getDesignElementsForArrayDesigns(java.util.List)} method on the resulting list.
     *
     * @return the list of array designs, not prepopulated with design elements.
     */
    public List<ArrayDesign> getAllArrayDesigns() {
        List results = template.query(ARRAY_DESIGN_SELECT,
                                      new ArrayDesignMapper());

        return (List<ArrayDesign>) results;
    }

    public ArrayDesign getArrayDesignByAccession(String accession) {
        List results = template.query(ARRAY_DESIGN_BY_ACC_SELECT,
                                      new Object[]{accession},
                                      new ArrayDesignMapper());

        // get first result only
        ArrayDesign arrayDesign =
                results.size() > 0 ? (ArrayDesign) results.get(0) : null;

        if (arrayDesign != null) {
            fillOutArrayDesigns(Collections.singletonList(arrayDesign));
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
        if (arrayDesigns.size() > 0) {
            fillOutArrayDesigns(arrayDesigns);
        }

        return arrayDesigns;
    }

    public void getDesignElementsForArrayDesigns(List<ArrayDesign> arrayDesigns) {
        // populate the other info for these assays
        if (arrayDesigns.size() > 0) {
            fillOutArrayDesigns(arrayDesigns);
        }
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

    public List<Species> getAllSpecies() {
        List results = template.query(SPECIES_ALL, new SpeciesMapper());
        return (List<Species>) results;
    }

    public List<Property> getAllProperties() {
        List results = template.query(PROPERTIES_ALL, new PropertyMapper());
        return (List<Property>) results;
    }

    public Set<String> getAllGenePropertyNames() {
        List results = template.query(GENEPROPERTY_ALL_NAMES, new RowMapper() {
            public Object mapRow(ResultSet resultSet, int i) throws SQLException {
                return resultSet.getString(1);
            }
        });
        return new HashSet<String>(results);
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
        stats.setExperimentCount(template.queryForInt(EXPERIMENTS_COUNT));
        stats.setAssayCount(template.queryForInt(ASSAYS_COUNT));
        stats.setGeneCount(template.queryForInt(GENES_COUNT));
        stats.setNewExperimentCount(0);
        stats.setPropertyValueCount(getPropertyValueCount());

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
        create or replace PROCEDURE "A2_EXPERIMENTSET" (
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
                        .useInParameterNames("THEACCESSION")
                        .useInParameterNames("THEDESCRIPTION")
                        .useInParameterNames("THEPERFORMER")
                        .useInParameterNames("THELAB")
                        .declareParameters(new SqlParameter("THEACCESSION", Types.VARCHAR))
                        .declareParameters(new SqlParameter("THEDESCRIPTION", Types.VARCHAR))
                        .declareParameters(new SqlParameter("THEPERFORMER", Types.VARCHAR))
                        .declareParameters(new SqlParameter("THELAB", Types.VARCHAR));

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
                        .withProcedureName("ATLASLDR.A2_ASSAYSET")
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
                        .withProcedureName("ATLASLDR.A2_SAMPLESET")
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

    private void fillOutArrayDesigns(List<ArrayDesign> arrayDesigns) {
        // map array designs to array design id
        Map<Integer, ArrayDesign> arrayDesignsByID = new HashMap<Integer, ArrayDesign>();
        for (ArrayDesign array : arrayDesigns) {
            // index this array
            arrayDesignsByID.put(array.getArrayDesignID(), array);

            // also initialize design elements is null - once this method is called, you should never get an NPE
            if (array.getDesignElements() == null) {
                array.setDesignElements(new HashMap<Integer, String>());
            }
        }

        NamedParameterJdbcTemplate namedTemplate = new NamedParameterJdbcTemplate(template);

        // now query for design elements that map to one of these array designs
        ArrayDesignElementMapper arrayDesignElementMapper = new ArrayDesignElementMapper(arrayDesignsByID);
        MapSqlParameterSource arrayParams = new MapSqlParameterSource();
        arrayParams.addValue("arraydesignids", arrayDesignsByID.keySet());
        namedTemplate.query(DESIGN_ELEMENTS_BY_RELATED_ARRAY, arrayParams, arrayDesignElementMapper);
    }

    private void fillOutGeneProperties(List<Gene> genes) {
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

        // map of genes and their properties
        GenePropertyMapper genePropertyMapper = new GenePropertyMapper(genesByID);

        // query template for genes
        NamedParameterJdbcTemplate namedTemplate = new NamedParameterJdbcTemplate(template);

        // if we have more than 'maxQueryParams' genes, split into smaller queries
        List<Integer> geneIDs = new ArrayList<Integer>(genesByID.keySet());
        boolean done = false;
        int startpos = 0;
        int endpos = maxQueryParams;

        while (!done) {
            List<Integer> geneIDsChunk;
            if (endpos > geneIDs.size()) {
                // we've reached the last segment, so query all of these
                geneIDsChunk = geneIDs.subList(startpos, geneIDs.size());
                done = true;
            }
            else {
                // still more left - take next sublist and increment counts
                geneIDsChunk = geneIDs.subList(startpos, endpos);
                startpos = endpos;
                endpos += maxQueryParams;
            }

            // now query for properties that map to one of these genes
            MapSqlParameterSource propertyParams = new MapSqlParameterSource();
            propertyParams.addValue("geneids", geneIDsChunk);
            namedTemplate.query(PROPERTIES_BY_RELATED_GENES, propertyParams, genePropertyMapper);
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

        // maps properties to assays
        AssayPropertyMapper assayPropertyMapper = new AssayPropertyMapper(assaysByID);

        // query template for assays
        NamedParameterJdbcTemplate namedTemplate = new NamedParameterJdbcTemplate(template);

        // if we have more than 'maxQueryParams' assays, split into smaller queries
        List<Integer> assayIDs = new ArrayList<Integer>(assaysByID.keySet());
        boolean done = false;
        int startpos = 0;
        int endpos = maxQueryParams;

        while (!done) {
            List<Integer> assayIDsChunk;
            if (endpos > assayIDs.size()) {
                // we've reached the last segment, so query all of these
                assayIDsChunk = assayIDs.subList(startpos, assayIDs.size());
                done = true;
            }
            else {
                // still more left - take next sublist and increment counts
                assayIDsChunk = assayIDs.subList(startpos, endpos);
                startpos = endpos;
                endpos += maxQueryParams;
            }

            // now query for properties that map to one of the samples in the sublist
            MapSqlParameterSource propertyParams = new MapSqlParameterSource();
            propertyParams.addValue("assayids", assayIDsChunk);
            namedTemplate.query(PROPERTIES_BY_RELATED_ASSAYS, propertyParams, assayPropertyMapper);
        }
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

        // maps properties and assays to relevant sample
        AssaySampleMapper assaySampleMapper = new AssaySampleMapper(samplesByID);
        SamplePropertyMapper samplePropertyMapper = new SamplePropertyMapper(samplesByID);

        // query template for samples
        NamedParameterJdbcTemplate namedTemplate = new NamedParameterJdbcTemplate(template);

        // if we have more than 'maxQueryParams' samples, split into smaller queries
        List<Integer> sampleIDs = new ArrayList<Integer>(samplesByID.keySet());
        boolean done = false;
        int startpos = 0;
        int endpos = maxQueryParams;

        while (!done) {
            List<Integer> sampleIDsChunk;
            if (endpos > sampleIDs.size()) {
                sampleIDsChunk = sampleIDs.subList(startpos, sampleIDs.size());
                done = true;
            }
            else {
                sampleIDsChunk = sampleIDs.subList(startpos, endpos);
                startpos = endpos;
                endpos += maxQueryParams;
            }

            // now query for assays that map to one of these samples
            MapSqlParameterSource assayParams = new MapSqlParameterSource();
            assayParams.addValue("sampleids", sampleIDsChunk);
            namedTemplate.query(ASSAYS_BY_RELATED_SAMPLES, assayParams, assaySampleMapper);

            // now query for properties that map to one of these samples
            MapSqlParameterSource propertyParams = new MapSqlParameterSource();
            propertyParams.addValue("sampleids", sampleIDsChunk);
            namedTemplate.query(PROPERTIES_BY_RELATED_SAMPLES, propertyParams, samplePropertyMapper);
        }
    }

    private SqlTypeValue convertPropertiesToOracleARRAY(final List<Property> properties) {
        return new AbstractSqlTypeValue() {
            protected Object createTypeValue(Connection connection, int sqlType, String typeName) throws SQLException {
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
            details.setStatus(resultSet.getString(2));
            details.setNetCDF(resultSet.getString(3));
            details.setSimilarity(resultSet.getString(4));
            details.setRanking(resultSet.getString(5));
            details.setSearchIndex(resultSet.getString(6));
            details.setLoadType(resultSet.getString(7));

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
                int assayID = resultSet.getInt(1);
                // get design element id key
                int designElementID = resultSet.getInt(2);
                // get expression value
                float value = resultSet.getFloat(3);
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

    private class AssayExpressionValueMapper implements RowMapper {
        private Map<Integer, Assay> assaysByID;

        public AssayExpressionValueMapper(Map<Integer, Assay> assaysByID) {
            this.assaysByID = assaysByID;
        }

        public Object mapRow(ResultSet resultSet, int i) throws SQLException {
            // get assay ID key
            int assayID = resultSet.getInt(1);
            // get design element id key
            int designElementID = resultSet.getInt(2);
            // get expression value
            float value = resultSet.getFloat(3);

            assaysByID.get(assayID).getExpressionValues().put(designElementID, value);

            return null;
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

            ea.setEfId(resultSet.getInt(7));
            ea.setEfvId(resultSet.getInt(8));

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
            mapping.setExperimentId(resultSet.getLong(8));

            return mapping;
        }
    }

    private class ArrayDesignElementMapper implements RowMapper {
        private Map<Integer, ArrayDesign> arrayByID;

        public ArrayDesignElementMapper(Map<Integer, ArrayDesign> arraysByID) {
            this.arrayByID = arraysByID;
        }

        public Object mapRow(ResultSet resultSet, int i) throws SQLException {
            int assayID = resultSet.getInt(1);

            Integer id = resultSet.getInt(2);
            String acc = resultSet.getString(3);

            arrayByID.get(assayID).getDesignElements().put(id, acc);

            return null;
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

    private class GeneDesignElementMapper implements RowMapper {
        private Map<Integer, Gene> genesByID;

        public GeneDesignElementMapper(Map<Integer, Gene> genesByID) {
            this.genesByID = genesByID;
        }

        public Object mapRow(ResultSet resultSet, int i) throws SQLException {
            int geneID = resultSet.getInt(1);
            int designElementID = resultSet.getInt(2);

            genesByID.get(geneID).getDesignElementIDs().add(designElementID);

            return designElementID;
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

            atlasCount.setPropertyId(resultSet.getInt(7));
            atlasCount.setPropertyValueId(resultSet.getInt(8));

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

    private static class SpeciesMapper implements RowMapper {
        public Object mapRow(ResultSet resultSet, int i) throws SQLException {
            return new Species(resultSet.getInt(1), resultSet.getString(2));
        }
    }

    private static class PropertyMapper implements RowMapper {
        public Object mapRow(ResultSet resultSet, int i) throws SQLException {
            Property property = new Property();
            property.setPropertyId(resultSet.getInt(1));
            property.setAccession(resultSet.getString(2));
            property.setName(resultSet.getString(2));
            property.setPropertyValueId(resultSet.getInt(3));
            property.setValue(resultSet.getString(4));
            property.setFactorValue(resultSet.getInt(5) > 0);
            return property;
        }
    }
}
