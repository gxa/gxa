package uk.ac.ebi.gxa.dao;

import oracle.jdbc.OracleTypes;
import oracle.sql.ARRAY;
import oracle.sql.ArrayDescriptor;
import oracle.sql.STRUCT;
import oracle.sql.StructDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.*;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.simple.ParameterizedRowMapper;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.jdbc.core.support.AbstractSqlTypeValue;
import uk.ac.ebi.microarray.atlas.model.*;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.text.DecimalFormat;
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
    public static final String EXPERIMENT_LOAD_MONITOR_SELECT =
            "SELECT accession, status, netcdf, similarity, ranking, searchindex, load_type " +
                    "FROM load_monitor " +
                    "WHERE load_type='experiment'";
    public static final String ARRAY_LOAD_MONITOR_SELECT =
            "SELECT accession, status, netcdf, similarity, ranking, searchindex, load_type " +
                    "FROM load_monitor " +
                    "WHERE load_type='arraydesign'";
    public static final String EXPERIMENT_LOAD_MONITOR_BY_ACC_SELECT =
            EXPERIMENT_LOAD_MONITOR_SELECT + " " +
                    "AND accession=?";
    public static final String ARRAY_LOAD_MONITOR_BY_ACC_SELECT =
            ARRAY_LOAD_MONITOR_SELECT + " " +
                    "AND accession=?";
    public static final String EXPERIMENT_LOAD_MONITOR_SORTED_EXPERIMENT_ACCESSIONS =
            "SELECT accession, status, netcdf, similarity, ranking, searchindex, load_type FROM ( " +
                    "SELECT ROWNUM r, accession, status, netcdf, similarity, ranking, searchindex, load_type FROM ( " +
                    "SELECT accession, status, netcdf, similarity, ranking, searchindex, load_type " +
                    "FROM load_monitor " +
                    "WHERE load_type='experiment' " +
                    "ORDER BY accession)) " +
                    "WHERE r BETWEEN ? AND ?";
    // experiment queries
    public static final String EXPERIMENTS_COUNT =
            "SELECT COUNT(*) FROM a2_experiment";

    public static final String EXPERIMENTS_SELECT =
            "SELECT accession, description, performer, lab, experimentid " +
                    "FROM a2_experiment";
    public static final String EXPERIMENTS_PENDING_INDEX_SELECT =
            "SELECT e.accession, e.description, e.performer, e.lab, e.experimentid " +
                    "FROM a2_experiment e, load_monitor lm " +
                    "WHERE e.accession=lm.accession " +
                    "AND (lm.searchindex='pending' OR lm.searchindex='failed') " +
                    "AND lm.load_type='experiment'";
    public static final String EXPERIMENTS_PENDING_NETCDF_SELECT =
            "SELECT e.accession, e.description, e.performer, e.lab, e.experimentid " +
                    "FROM a2_experiment e, load_monitor lm " +
                    "WHERE e.accession=lm.accession " +
                    "AND (lm.netcdf='pending' OR lm.netcdf='failed') " +
                    "AND lm.load_type='experiment'";
    public static final String EXPERIMENTS_PENDING_ANALYTICS_SELECT =
            "SELECT e.accession, e.description, e.performer, e.lab, e.experimentid " +
                    "FROM a2_experiment e, load_monitor lm " +
                    "WHERE e.accession=lm.accession " +
                    "AND (lm.ranking='pending' OR lm.ranking='failed') " + // fixme: similarity?
                    "AND lm.load_type='experiment'";
    public static final String EXPERIMENT_BY_ACC_SELECT =
            EXPERIMENTS_SELECT + " " +
                    "WHERE accession=?";

    // gene queries
    public static final String GENES_COUNT =
            "SELECT COUNT(*) FROM a2_gene";

    public static final String GENES_SELECT =
            "SELECT DISTINCT g.geneid, g.identifier, g.name, s.name AS species " +
                    "FROM a2_gene g, a2_organism s " +
                    "WHERE g.organismid=s.organismid";
    public static final String GENE_BY_IDENTIFIER =
            "SELECT DISTINCT g.geneid, g.identifier, g.name, s.name AS species " +
                    "FROM a2_gene g, a2_organism s " +
                    "WHERE g.identifier=?";
    public static final String DESIGN_ELEMENTS_AND_GENES_SELECT =
            "SELECT de.geneid, de.designelementid " +
                    "FROM a2_designelement de";
    public static final String DESIGN_ELEMENTS_AND_GENES_PENDING_SELECT =
            "SELECT de.geneid, de.designelementid " +
                    "FROM a2_designelement de, a2_gene g, load_monitor lm " +
                    "WHERE de.geneid=g.geneid " +
                    "AND g.identifier=lm.accession " +
                    "AND (lm.searchindex='pending' OR lm.searchindex='failed') " +
                    "AND lm.load_type='gene'";
    public static final String GENES_BY_EXPERIMENT_ACCESSION =
            "SELECT DISTINCT g.geneid, g.identifier, g.name, s.name AS species " +
                    "FROM a2_gene g, a2_organism s, a2_designelement d, a2_assay a, " +
                    "a2_experiment e " +
                    "WHERE g.geneid=d.geneid " +
                    "AND g.organismid = s.organismid " +
                    "AND d.arraydesignid=a.arraydesignid " +
                    "AND a.experimentid=e.experimentid " +
                    "AND e.accession=?";
    public static final String DESIGN_ELEMENTS_AND_GENES_BY_EXPERIMENT_ACCESSION =
            "SELECT de.geneid, de.designelementid " +
                    "FROM a2_designelement de, a2_assay a, a2_experiment e " +
                    "WHERE de.arraydesignid=a.arraydesignid " +
                    "AND a.experimentid=e.experimentid " +
                    "AND e.accession=?";
    public static final String PROPERTIES_BY_RELATED_GENES =
            "SELECT ggpv.geneid, gp.name AS property, gpv.value AS propertyvalue  " +
                    "FROM a2_geneproperty gp, a2_genepropertyvalue gpv, a2_genegpv ggpv " +
                    "WHERE gpv.genepropertyid=gp.genepropertyid and ggpv.genepropertyvalueid = gpv.genepropertyvalueid " +
                    "AND ggpv.geneid IN (:geneids)";
    public static final String GENE_COUNT_SELECT =
            "SELECT COUNT(DISTINCT identifier) FROM a2_gene";

    // assay queries
    public static final String ASSAYS_COUNT =
            "SELECT COUNT(*) FROM a2_assay";

    public static final String ASSAYS_SELECT =
            "SELECT a.accession, e.accession, ad.accession, a.assayid " +
                    "FROM a2_assay a, a2_experiment e, a2_arraydesign ad " +
                    "WHERE e.experimentid=a.experimentid " +
                    "AND a.arraydesignid=ad.arraydesignid";
    public static final String ASSAYS_BY_EXPERIMENT_ACCESSION =
            ASSAYS_SELECT + " " +
                    "AND e.accession=?";
    public static final String ASSAYS_BY_EXPERIMENT_AND_ARRAY_ACCESSION =
            ASSAYS_BY_EXPERIMENT_ACCESSION + " " +
                    "AND ad.accession=?";
    public static final String ASSAYS_BY_RELATED_SAMPLES =
            "SELECT s.sampleid, a.accession " +
                    "FROM a2_assay a, a2_assaysample s " +
                    "WHERE a.assayid=s.assayid " +
                    "AND s.sampleid IN (:sampleids)";
    public static final String PROPERTIES_BY_RELATED_ASSAYS =
            "SELECT apv.assayid, p.name AS property, pv.name AS propertyvalue, apv.isfactorvalue " +
                    "FROM a2_property p, a2_propertyvalue pv, a2_assayPV apv " +
                    "WHERE apv.propertyvalueid=pv.propertyvalueid " +
                    "AND pv.propertyid=p.propertyid " +
                    "AND apv.assayid IN (:assayids)";

    // expression value queries
    public static final String EXPRESSION_VALUES_BY_RELATED_ASSAYS =
            "SELECT ev.assayid, ev.designelementid, ev.value " +
                    "FROM a2_expressionvalue ev, a2_designelement de " +
                    "WHERE ev.designelementid=de.designelementid " +
                    "AND ev.assayid IN (:assayids)";
    public static final String EXPRESSION_VALUES_BY_EXPERIMENT_AND_ARRAY =
            "SELECT ev.assayid, ev.designelementid, ev.value " +
                    "FROM a2_expressionvalue ev " +
                    "JOIN a2_designelement de ON de.designelementid=ev.designelementid " +
                    "JOIN a2_assay a ON a.assayid = ev.assayid " +
                    "WHERE a.experimentid=? AND de.arraydesignid=?";

    // sample queries
    public static final String SAMPLES_BY_ASSAY_ACCESSION =
            "SELECT s.accession, s.species, s.channel, s.sampleid " +
                    "FROM a2_sample s, a2_assay a, a2_assaysample ass " +
                    "WHERE s.sampleid=ass.sampleid " +
                    "AND a.assayid=ass.assayid " +
                    "AND a.accession=?";
    public static final String SAMPLES_BY_EXPERIMENT_ACCESSION =
            "SELECT s.accession, s.species, s.channel, s.sampleid " +
                    "FROM a2_sample s, a2_assay a, a2_assaysample ass, a2_experiment e " +
                    "WHERE s.sampleid=ass.sampleid " +
                    "AND a.assayid=ass.assayid " +
                    "AND a.experimentid=e.experimentid " +
                    "AND e.accession=?";
    public static final String PROPERTIES_BY_RELATED_SAMPLES =
            "SELECT spv.sampleid, p.name AS property, pv.name AS propertyvalue, spv.isfactorvalue " +
                    "FROM a2_property p, a2_propertyvalue pv, a2_samplepv spv " +
                    "WHERE spv.propertyvalueid=pv.propertyvalueid " +
                    "AND pv.propertyid=p.propertyid " +
                    "AND spv.sampleid IN (:sampleids)";

    // query for counts, for statistics
    public static final String PROPERTY_VALUE_COUNT_SELECT =
            "SELECT COUNT(DISTINCT name) FROM a2_propertyvalue";

    // array and design element queries
    public static final String ARRAY_DESIGN_SELECT =
            "SELECT accession, type, name, provider, arraydesignid " +
                    "FROM a2_arraydesign";
    public static final String ARRAY_DESIGN_BY_ACC_SELECT =
            ARRAY_DESIGN_SELECT + " " +
                    "WHERE accession=?";
    public static final String ARRAY_DESIGN_BY_EXPERIMENT_ACCESSION =
            "SELECT " +
                    "DISTINCT d.accession, d.type, d.name, d.provider, d.arraydesignid " +
                    "FROM a2_arraydesign d, a2_assay a, a2_experiment e " +
                    "WHERE e.experimentid=a.experimentid " +
                    "AND a.arraydesignid=d.arraydesignid " +
                    "AND e.accession=?";
    public static final String DESIGN_ELEMENTS_BY_ARRAY_ACCESSION =
            "SELECT de.designelementid, de.accession " +
                    "FROM A2_ARRAYDESIGN ad, A2_DESIGNELEMENT de " +
                    "WHERE de.arraydesignid=ad.arraydesignid " +
                    "AND ad.accession=?";
    public static final String DESIGN_ELEMENT_NAMES_BY_ARRAY_ACCESSION =
            "SELECT de.designelementid, de.name " +
                    "FROM A2_ARRAYDESIGN ad, A2_DESIGNELEMENT de " +
                    "WHERE de.arraydesignid=ad.arraydesignid " +
                    "AND ad.accession=?";
    public static final String DESIGN_ELEMENTS_BY_ARRAY_ID =
            "SELECT de.designelementid, de.accession " +
                    "FROM a2_designelement de " +
                    "WHERE de.arraydesignid=?";
    public static final String DESIGN_ELEMENTS_AND_GENES_BY_RELATED_ARRAY =
            "SELECT de.arraydesignid, de.designelementid, de.accession, de.geneid " +
                    "FROM a2_designelement de " +
                    "WHERE de.arraydesignid IN (:arraydesignids)";
    public static final String DESIGN_ELEMENTS_BY_GENEID =
            "SELECT de.designelementid, de.accession " +
                    "FROM a2_designelement de " +
                    "WHERE de.geneid=?";

    // other useful queries
    public static final String EXPRESSIONANALYTICS_BY_EXPERIMENTID =
            "SELECT ef.name AS ef, efv.name AS efv, a.experimentid, " +
                    "a.designelementid, a.tstat, a.pvaladj, " +
                    "ef.propertyid as efid, efv.propertyvalueid as efvid " +
                    "FROM a2_expressionanalytics a " +
                    "JOIN a2_propertyvalue efv ON efv.propertyvalueid=a.propertyvalueid " +
                    "JOIN a2_property ef ON ef.propertyid=efv.propertyid " +
                    "JOIN a2_designelement de ON de.designelementid=a.designelementID " +
                    "WHERE a.experimentid=?";
    public static final String EXPRESSIONANALYTICS_BY_GENEID =
            "SELECT ef, efv, experimentid, null, tstat, min(pvaladj), efid, efvid FROM " +
                    "(SELECT ef.name AS ef, efv.name AS efv, a.experimentid AS experimentid, " +
                    "first_value(a.tstat) over (partition BY ef.name, efv.name, a.experimentid ORDER BY a.pvaladj ASC) AS tstat, " +
                    "(a.pvaladj ) AS pvaladj," +
                    "ef.propertyid AS efid, efv.propertyvalueid as efvid " +
                    "FROM a2_expressionanalytics a " +
                    "JOIN a2_propertyvalue efv ON efv.propertyvalueid=a.propertyvalueid " +
                    "JOIN a2_property ef ON ef.propertyid=efv.propertyid " +
                    "JOIN a2_designelement de ON de.designelementid=a.designelementid " +
                    "WHERE de.geneid=? AND a.pvaladj < 0.05) GROUP BY  ef, efv, experimentid, tstat, efid, efvid";
    public static final String EXPRESSIONANALYTICS_BY_DESIGNELEMENTID =
            "SELECT ef.name AS ef, efv.name AS efv, a.experimentid, a.designelementid, " +
                    "a.tstat, a.pvaladj, " +
                    "ef.propertyid as efid, efv.propertyvalueid as efvid " +
                    "FROM a2_expressionanalytics a " +
                    "JOIN a2_propertyvalue efv ON efv.propertyvalueid=a.propertyvalueid " +
                    "JOIN a2_property ef ON ef.propertyid=efv.propertyid " +
                    "WHERE a.designelementid=?";
    public static final String ONTOLOGY_MAPPINGS_SELECT =
            "SELECT DISTINCT accession, property, propertyvalue, ontologyterm, " +
                    "issampleproperty, isassayproperty, isfactorvalue, experimentid " +
                    "FROM a2_ontologymapping";
    public static final String ONTOLOGY_MAPPINGS_BY_ONTOLOGY_NAME =
            ONTOLOGY_MAPPINGS_SELECT + " " +
                    "WHERE ontologyname=?";
    public static final String ONTOLOGY_MAPPINGS_BY_EXPERIMENT_ACCESSION =
            ONTOLOGY_MAPPINGS_SELECT + " " +
                    "WHERE accession=?";

    // queries for atlas interface
    public static final String ATLAS_RESULTS_SELECT =
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
    public static final String ATLAS_COUNTS_SELECT =
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
    public static final String ATLAS_COUNTS_BY_EXPERIMENTID =
            ATLAS_COUNTS_SELECT + " " +
                    "WHERE ea.experimentid=? " +
                    "GROUP BY ea.experimentid, p.name, pv.name, " +
                    "CASE WHEN ea.tstat < 0 THEN -1 ELSE 1 END";
    public static final String ATLAS_RESULTS_UP_BY_EXPERIMENTID_GENEID_AND_EFV =
            ATLAS_RESULTS_SELECT + " " +
                    "WHERE ea.experimentid IN (:exptids) " +
                    "AND g.geneid IN (:geneids) " +
                    "AND pv.name IN (:efvs) " +
                    "AND updn='1' " +
                    "AND TOPN<=20 " +
                    "ORDER BY ea.pvaladj " +
                    "GROUP BY ea.experimentid, g.geneid, p.name, pv.name, " +
                    "CASE WHEN ea.tstat < 0 THEN -1 ELSE 1 END";
    public static final String ATLAS_RESULTS_DOWN_BY_EXPERIMENTID_GENEID_AND_EFV =
            ATLAS_RESULTS_SELECT + " " +
                    "WHERE ea.experimentid IN (:exptids) " +
                    "AND g.geneid IN (:geneids) " +
                    "AND pv.name IN (:efvs) " +
                    "AND updn='-1' " +
                    "AND TOPN<=20 " +
                    "ORDER BY ea.pvaladj " +
                    "GROUP BY ea.experimentid, g.geneid, p.name, pv.name, ea.pvaladj, " +
                    "CASE WHEN ea.tstat < 0 THEN -1 ELSE 1 END";
    public static final String ATLAS_RESULTS_UPORDOWN_BY_EXPERIMENTID_GENEID_AND_EFV =
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

    public static final String SPECIES_ALL =
            "SELECT organismid, name FROM A2_organism";

    public static final String PROPERTIES_ALL =
            "SELECT min(p.propertyid), p.name, min(pv.propertyvalueid), pv.name, 1 as isfactorvalue " +
                    "FROM a2_property p, a2_propertyvalue pv " +
                    "WHERE  pv.propertyid=p.propertyid GROUP BY p.name, pv.name";

    public static final String GENEPROPERTY_ALL_NAMES =
            "SELECT name FROM A2_GENEPROPERTY";

    public static final String BEST_DESIGNELEMENTID_FOR_GENE =
            "SELECT topde FROM (SELECT de.designelementid as topde," +
                    "          MIN(a.pvaladj) KEEP(DENSE_RANK FIRST ORDER BY a.pvaladj ASC)" +
                    "     OVER (PARTITION BY a.propertyvalueid) as minp" +
                    " FROM a2_expressionanalytics a, a2_propertyvalue pv, a2_property p, a2_designelement de" +
                    " WHERE pv.propertyid = p.propertyid" +
                    "   AND pv.propertyvalueid = a.propertyvalueid" +
                    "   AND a.designelementid = de.designelementid" +
                    "   AND p.name = ?" +
                    "   AND a.experimentid = ?" +
                    "   AND de.geneid = ?" +
                    "   and rownum=1)";

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

    public List<LoadDetails> getLoadDetailsForExperiments() {
        List results = template.query(EXPERIMENT_LOAD_MONITOR_SELECT,
                                      new LoadDetailsMapper());
        return (List<LoadDetails>) results;
    }

    public LoadDetails getLoadDetailsForExperimentsByAccession(String accession) {
        List results = template.query(EXPERIMENT_LOAD_MONITOR_BY_ACC_SELECT,
                                      new Object[]{accession},
                                      new LoadDetailsMapper());
        return results.size() > 0 ? (LoadDetails) results.get(0) : null;
    }

    public LoadDetails getLoadDetailsForArrayDesignsByAccession(String accession) {
        List results = template.query(ARRAY_LOAD_MONITOR_BY_ACC_SELECT,
                                      new Object[]{accession},
                                      new LoadDetailsMapper());
        return results.size() > 0 ? (LoadDetails) results.get(0) : null;
    }

    public List<LoadDetails> getLoadDetailsForExperimentsByPage(int pageNumber, int experimentsPerPage) {
        int offset = (pageNumber - 1) * experimentsPerPage;
        int rowcount = pageNumber * experimentsPerPage;

        log.debug("Query is {}, from " + offset + " to " + rowcount,
                  EXPERIMENT_LOAD_MONITOR_SORTED_EXPERIMENT_ACCESSIONS);

        List results = template.query(EXPERIMENT_LOAD_MONITOR_SORTED_EXPERIMENT_ACCESSIONS,
                                      new Object[]{offset, rowcount},
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

    /**
     * Same as getAllGenes(), but doesn't do design elements. Sometime we just don't need them.
     *
     * @return list of all genes
     */
    public List<Gene> getAllGenesFast() {
        // do the query to fetch genes without design elements
        return (List<Gene>) template.query(GENES_SELECT,
                                           new GeneMapper());
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

    /**
     * A convenience method that fetches the set of design elements by array design accession.  In this case, design
     * elements are recorded as a map, indexed by design element id and with a value of the design element name.  The
     * design element name corresponds to the arraydesign reporter name or composite reporter name, and is usually the
     * probeset ID. The set of design element ids contains no duplicates, and the results that are returned are the
     * names for design elements.  This takes the accession of the array design as a parameter.
     *
     * @param arrayDesignAccession the accession number of the array design to query for
     * @return the map of design element names indexed by unique design element id integers
     */
    public Map<Integer, String> getDesignElementNamesByArrayAccession(
            String arrayDesignAccession) {
        Object results = template.query(DESIGN_ELEMENT_NAMES_BY_ARRAY_ACCESSION,
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

    public Integer getBestDesignElementForExpressionProfile(int geneId, int experimentId, String ef) {
        try {
            return template.queryForInt(BEST_DESIGNELEMENTID_FOR_GENE, new Object[]{ef, experimentId, geneId});
        }
        catch (EmptyResultDataAccessException e) {
            // no statistically best element found
            return null;
        }
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
                        .declareParameters(new SqlParameter("ACCESSION", Types.VARCHAR))
                        .declareParameters(new SqlParameter("DESCRIPTION", Types.VARCHAR))
                        .declareParameters(new SqlParameter("PERFORMER", Types.VARCHAR))
                        .declareParameters(new SqlParameter("LAB", Types.VARCHAR));

        // map parameters...
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("ACCESSION", experiment.getAccession())
                .addValue("DESCRIPTION", experiment.getDescription())
                .addValue("PERFORMER", experiment.getPerformer())
                .addValue("LAB", experiment.getLab());

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
        List<Property> props = assay.getProperties() == null
                ? new ArrayList<Property>()
                : assay.getProperties();
        Map<String, Float> evs = assay.getExpressionValuesByDesignElementReference() == null
                ? new HashMap<String, Float>()
                : assay.getExpressionValuesByDesignElementReference();
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
        SqlTypeValue expressionValuesParam =
                evs.isEmpty() ? null :
                        convertExpressionValuesToOracleARRAY(evs);

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
                          props.size(), evs.size()});

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
                        .useInParameterNames("ACCESSION")
                        .useInParameterNames("ASSAYS")
                        .useInParameterNames("PROPERTIES")
                        .useInParameterNames("SPECIES")
                        .useInParameterNames("CHANNEL")
                        .declareParameters(
                                new SqlParameter("ACCESSION", Types.VARCHAR))
                        .declareParameters(
                                new SqlParameter("ASSAYS", OracleTypes.ARRAY, "ACCESSIONTABLE"))
                        .declareParameters(
                                new SqlParameter("PROPERTIES", OracleTypes.ARRAY, "PROPERTYTABLE"))
                        .declareParameters(
                                new SqlParameter("SPECIES", Types.VARCHAR))
                        .declareParameters(
                                new SqlParameter("CHANNEL", Types.VARCHAR));

        // map parameters...
        MapSqlParameterSource params = new MapSqlParameterSource();
        SqlTypeValue accessionsParam =
                sample.getAssayAccessions() == null || sample.getAssayAccessions().isEmpty() ? null :
                        convertAssayAccessionsToOracleARRAY(sample.getAssayAccessions());
        SqlTypeValue propertiesParam =
                sample.getProperties() == null || sample.getProperties().isEmpty()
                        ? null
                        : convertPropertiesToOracleARRAY(sample.getProperties());

        params.addValue("ACCESSION", sample.getAccession())
                .addValue("ASSAYS", accessionsParam, OracleTypes.ARRAY, "ACCESSIONTABLE")
                .addValue("PROPERTIES", propertiesParam, OracleTypes.ARRAY, "PROPERTYTABLE")
                .addValue("SPECIES", sample.getSpecies())
                .addValue("CHANNEL", sample.getChannel());

        int assayCount = sample.getAssayAccessions() == null ? 0 : sample.getAssayAccessions().size();
        int propertiesCount = sample.getProperties() == null ? 0 : sample.getProperties().size();
        log.debug("Invoking A2_SAMPLESET with the following parameters..." +
                "\n\tsample accession: {}" +
                "\n\tassays count:     {}" +
                "\n\tproperties count: {}" +
                "\n\tspecies:          {}" +
                "\n\tchannel:          {}",
                  new Object[]{sample.getAccession(), assayCount, propertiesCount, sample.getSpecies(),
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
                                         int[] designElements,
                                         double[] pValues,
                                         double[] tStatistics) {
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
        start = System.currentTimeMillis();
        procedure.execute(params);
        end = System.currentTimeMillis();
        total = new DecimalFormat("#.##").format((end - start) / 1000);
        log.trace("Procedure execution for [experiment: " + experimentAccession + "; " +
                "Property: " + property + "; Property Value: " + propertyValue + "]  complete in " + total + "s.");

        log.debug("Writing analytics for [experiment: " + experimentAccession + "; " +
                "Property: " + property + "; Property Value: " + propertyValue + "] completed!");
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
                                new SqlParameter("ENTRYPRIORITYLIST", Types.VARCHAR))
                        .declareParameters(
                                new SqlParameter("DESIGNELEMENTS", OracleTypes.ARRAY, "DESIGNELEMENTTABLE"));

        SqlTypeValue designElementsParam =
                arrayDesignBundle.getDesignElementNames().isEmpty() ? null :
                        convertDesignElementsToOracleARRAY(arrayDesignBundle);

        StringBuffer sb = new StringBuffer();
        Iterator<String> stringIt = arrayDesignBundle.getGeneIdentifierNames().iterator();
        if (stringIt.hasNext()) {
            sb.append(stringIt.next());
        }
        while (stringIt.hasNext()) {
            sb.append(",").append(stringIt.next());
        }
        String entryPriorityList = sb.toString();

        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("ACCESSION", arrayDesignBundle.getAccession())
                .addValue("TYPE", arrayDesignBundle.getType())
                .addValue("NAME", arrayDesignBundle.getName())  //EQUAL TO ACCESSION ??
                .addValue("PROVIDER", arrayDesignBundle.getProvider())
                .addValue("ENTRYPRIORITYLIST", entryPriorityList)
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

    /**
     * Deletes the array design with the given accession from the database.  If this array design is not present, this
     * does nothing.
     *
     * @param arrayDesignAccession the accession of the array design to remove
     */
    public void deleteArrayDesign(final String arrayDesignAccession) {
        // execute this procedure...
        /*
        PROCEDURE A2_ARRAYDESIGNDELETE(
          Accession varchar2
        )
        */
        SimpleJdbcCall procedure =
                new SimpleJdbcCall(template)
                        .withProcedureName("ATLASLDR.A2_ARRAYDESIGNDELETE")
                        .withoutProcedureColumnMetaDataAccess()
                        .useInParameterNames("ACCESSION")
                        .declareParameters(new SqlParameter("ACCESSION", Types.VARCHAR));

        // map parameters...
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("ACCESSION", arrayDesignAccession);

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

        // map parameters...
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("EXPERIMENTACCESSION", experimentAccession);

        // tracking variables for timings
        long start, end;
        String total;

        log.trace("Executing procedure to delete analytics for experiment: " + experimentAccession);
        start = System.currentTimeMillis();
        procedure.execute(params);
        end = System.currentTimeMillis();
        total = new DecimalFormat("#.##").format((end - start) / 1000);
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

        // map single param
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("EXPERIMENTACCESSION", experimentAccession);

        // and execute
        procedure.execute(params);
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

        // map single param
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("EXPERIMENTACCESSION", experimentAccession);

        // and execute
        procedure.execute(params);
    }

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
            if (array.getGenes() == null) {
                array.setGenes(new HashMap<Integer, List<Integer>>());
            }
        }

        NamedParameterJdbcTemplate namedTemplate = new NamedParameterJdbcTemplate(template);

        // now query for design elements that map to one of these array designs
        ArrayDesignElementMapper arrayDesignElementMapper = new ArrayDesignElementMapper(arrayDesignsByID);
        MapSqlParameterSource arrayParams = new MapSqlParameterSource();
        arrayParams.addValue("arraydesignids", arrayDesignsByID.keySet());
        namedTemplate.query(DESIGN_ELEMENTS_AND_GENES_BY_RELATED_ARRAY, arrayParams, arrayDesignElementMapper);
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
                Object[] propArrayValues;
                if (properties != null && !properties.isEmpty()) {
                    propArrayValues = new Object[properties.size()];

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
                else {
                    // throw an SQLException, as we cannot create a ARRAY with an empty array
                    throw new SQLException("Unable to create an ARRAY from an empty list of properties");
                }
            }
        };
    }

    private SqlTypeValue convertExpressionValuesToOracleARRAY(final Map<String, Float> expressionValues) {
        return new AbstractSqlTypeValue() {
            protected Object createTypeValue(Connection connection, int sqlType, String typeName) throws SQLException {
                // this should be creating an oracle ARRAY of expression values
                // the array of STRUCTS representing each expression value
                Object[] evArrayValues;
                if (expressionValues != null && !expressionValues.isEmpty()) {
                    evArrayValues = new Object[expressionValues.size()];

                    // convert each property to an oracle STRUCT
                    // descriptor for EXPRESSIONVALUE type
                    StructDescriptor structDescriptor =
                            StructDescriptor.createDescriptor("EXPRESSIONVALUE", connection);
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
                else {
                    // throw an SQLException, as we cannot create a ARRAY with an empty array
                    throw new SQLException("Unable to create an ARRAY from an empty list of expression values");
                }
            }
        };
    }

    private SqlTypeValue convertAssayAccessionsToOracleARRAY(final List<String> assayAccessions) {
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
                    ArrayDescriptor arrayDescriptor = ArrayDescriptor.createDescriptor(typeName, connection);
                    return new ARRAY(arrayDescriptor, connection, accessions);
                }
                else {
                    // throw an SQLException, as we cannot create a ARRAY with an empty array
                    throw new SQLException("Unable to create an ARRAY from an empty list of accessions");
                }
            }
        };
    }

    private SqlTypeValue convertExpressionAnalyticsToOracleARRAY(final int[] designElements,
                                                                 final double[] pValues,
                                                                 final double[] tStatistics) {
        if (designElements == null || pValues == null || tStatistics == null ||
                designElements.length != pValues.length || pValues.length != tStatistics.length) {
            throw new RuntimeException(
                    "Cannot store analytics - inconsistent design element counts for pValues and tStatistics");
        }
        else {
            final int deCount = designElements.length;
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
                        for (int i = 0; i < designElements.length; i++) {
                            // array representing the values to go in the STRUCT
                            // Note the floatValue - EXPRESSIONANALYTICS structure assumes floats
                            expressionAnalyticsValues[0] = designElements[i];
                            expressionAnalyticsValues[1] = pValues[i];
                            expressionAnalyticsValues[2] = tStatistics[i];

                            expressionAnalytics[i] =
                                    new STRUCT(structDescriptor, connection, expressionAnalyticsValues);
                        }

                        // created the array of STRUCTs, group into ARRAY
                        ArrayDescriptor arrayDescriptor = ArrayDescriptor.createDescriptor(typeName, connection);
                        return new ARRAY(arrayDescriptor, connection, expressionAnalytics);
                    }
                    else {
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

                for (String designElementName : arrayDesignBundle.getDesignElementNames()) {
                    for (Map.Entry<String, String> databaseEntry :
                            arrayDesignBundle.getDatabaseEntriesForDesignElement(designElementName).entrySet()) {
                        Object[] deStructValues = new Object[3];
                        deStructValues[0] = designElementName;
                        deStructValues[1] = databaseEntry.getKey();
                        deStructValues[2] = databaseEntry.getValue();

                        deArrayValues.add(new STRUCT(structDescriptor, connection, deStructValues));
                    }
                }

                ArrayDescriptor arrayDescriptor = ArrayDescriptor.createDescriptor(typeName, connection);
                return new ARRAY(arrayDescriptor, connection, deArrayValues.toArray());
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
            assay.setArrayDesignAccession(resultSet.getString(3));
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
            Integer geneId = resultSet.getInt(4);

            ArrayDesign ad = arrayByID.get(assayID);
            ad.getDesignElements().put(id, acc);
            ad.getGenes().put(id, Collections.singletonList(geneId)); // TODO: as of today, we have one gene per de

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

    //AZ:to be moved to model

    public static class ExpressionValueMatrix {
        public static class ExpressionValue {
            //can I just leave it here without get/set methods?
            public int assayID;
            public int designElementID;
            public double value;

            public ExpressionValue(int assayID, int designElementID, float value) {
                this.assayID = assayID;
                this.designElementID = designElementID;
                this.value = value;
            }
        }

        public static class DesignElement {
            public int designElementID;
            public int geneID;

            public DesignElement(int designElementID, int geneID) {
                this.designElementID = designElementID;
                this.geneID = geneID;
            }
        }

        public List<ExpressionValue> expressionValues;
        public List<DesignElement> designElements;
        public List<Integer> assays;

        public ExpressionValueMatrix() {
            expressionValues = new ArrayList<ExpressionValue>();
            designElements = new ArrayList<DesignElement>();
            assays = new ArrayList<Integer>();
        }
    }


    public class AssayRowMapper implements ParameterizedRowMapper<Integer> {
        public Integer mapRow(ResultSet rs, int rowNum) throws SQLException {
            return rs.getInt("AssayID");
        }
    }

    public class DesignElementRowMapper implements ParameterizedRowMapper<ExpressionValueMatrix.DesignElement> {
        public ExpressionValueMatrix.DesignElement mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new ExpressionValueMatrix.DesignElement(rs.getInt("DesignElementID")
                    , rs.getInt("GeneID"));
        }
    }

    public class ExpressionValueRowMapper implements ParameterizedRowMapper<ExpressionValueMatrix.ExpressionValue> {
        public ExpressionValueMatrix.ExpressionValue mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new ExpressionValueMatrix.ExpressionValue(rs.getInt("AssayID")
                    , rs.getInt("DesignElementID")
                    , rs.getFloat("Value"));
        }
    }

    public ExpressionValueMatrix getExpressionValueMatrix(int ExperimentID, int ArrayDesignID) {

        ExpressionValueMatrix result = new ExpressionValueMatrix();

        SimpleJdbcCall procedure1 =
                new SimpleJdbcCall(template)
                        .withProcedureName("ATLASAPI.A2_EXPRESSIONVALUEGET")
                        .withoutProcedureColumnMetaDataAccess()
                        .useInParameterNames("EXPERIMENTID")
                        .useInParameterNames("ARRAYDESIGNID")
                        .declareParameters(new SqlParameter("EXPERIMENTID", Types.INTEGER))
                        .declareParameters(new SqlParameter("ARRAYDESIGNID", Types.INTEGER))
                        .returningResultSet("ASSAYS", new AssayRowMapper())
                        .returningResultSet("DESIGNELEMENTS", new DesignElementRowMapper())
                        .returningResultSet("EXPRESSIONVALUES", new ExpressionValueRowMapper());

        // map parameters...
        MapSqlParameterSource params1 = new MapSqlParameterSource()
                .addValue("EXPERIMENTID", ExperimentID)
                .addValue("ARRAYDESIGNID", ArrayDesignID);

        Map<String, Object> proc_result = procedure1.execute(params1);

        result.assays = (List<Integer>) proc_result.get("ASSAYS");
        result.designElements = (List<ExpressionValueMatrix.DesignElement>) proc_result.get("DESIGNELEMENTS");
        result.expressionValues = (List<ExpressionValueMatrix.ExpressionValue>) proc_result.get("EXPRESSIONVALUES");

        return result;
    }

}
