package uk.ac.ebi.gxa.loader;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import uk.ac.ebi.gxa.dao.AtlasDAO;
import uk.ac.ebi.microarray.atlas.model.Assay;
import uk.ac.ebi.microarray.atlas.model.Gene;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Properties;

/**
 * Javadocs go here!
 *
 * @author Tony Burdett
 * @date 02-Dec-2009
 */
public class AtlasDAOBenchmarks {
    /*
    // load monitor
    private static final String LOAD_MONITOR_SELECT =
            "SELECT accession, status, netcdf, similarity, ranking, searchindex, load_type " +
                    "FROM load_monitor";
    private static final String LOAD_MONITOR_BY_ACC_SELECT =
            LOAD_MONITOR_SELECT + " " +
                    "WHERE accession=?";

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
                    "FROM a2_gene g, a2_organism s " +
                    "WHERE g.organismid=s.organismid";
    private static final String GENES_PENDING_SELECT =
            "SELECT DISTINCT g.geneid, g.identifier, g.name, s.name AS species " +
                    "FROM a2_gene g, a2_organism s, load_monitor lm " +
                    "WHERE g.organismid=s.organismid " +
                    "AND g.identifier=lm.accession " +
                    "AND (lm.searchindex='pending' OR lm.searchindex='failed') " +
                    "AND lm.load_type='gene'";
    private static final String GENES_BY_EXPERIMENT_ACCESSION =
            "SELECT DISTINCT g.geneid, g.identifier, g.name, s.name AS species " +
                    "FROM a2_gene g, a2_organism s, a2_designelement d, a2_assay a, " +
                    "a2_experiment e " +
                    "WHERE g.geneid=d.geneid " +
                    "AND g.organismid = s.organismid " +
                    "AND d.arraydesignid=a.arraydesignid " +
                    "AND a.experimentid=e.experimentid " +
                    "AND e.accession=?";
    private static final String PROPERTIES_BY_RELATED_GENES =
            "SELECT ggpv.geneid, gp.name AS property, gpv.value AS propertyvalue " +
                    "FROM a2_geneproperty gp, a2_genepropertyvalue gpv, a2_genegpv ggpv " +
                    "WHERE gpv.genepropertyid=gp.genepropertyid and ggpv.genepropertyvalueid = gpv.genepropertyvalueid " +
                    "AND ggpv.geneid IN (:geneids)";
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
    private static final String EXPRESSION_VALUES_BY_RELATED_ASSAYS =
            "SELECT ev.assayid, ev.designelementid, ev.value " +
                    "FROM a2_expressionvalue ev, a2_designelement de " +
                    "WHERE ev.designelementid=de.designelementid " +
                    "AND ev.assayid IN (:assayids)";
    private static final String EXPRESSION_VALUES_BY_EXPERIMENT_AND_ARRAY =
            "SELECT ev.assayid, ev.designelementid, ev.value " +
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

    */

    private PrintWriter reportWriter;
    private AtlasDAO atlasDAO;

    private Properties properties;
    private Timer timer;

    public static void main(String[] args) {
        new AtlasDAOBenchmarks().runBenchmarking();
    }

    public AtlasDAOBenchmarks() {
        // load spring config
        BeanFactory factory =
                new ClassPathXmlApplicationContext("benchmarksContext.xml");

        // create report file
        File report = new File("dao-benchmarking." + new SimpleDateFormat("ddMMyyyy").format(new Date()) + ".report");
        if (!report.getAbsoluteFile().getParentFile().exists()) {
            report.getAbsoluteFile().getParentFile().mkdirs();
        }
        try {
            reportWriter = new PrintWriter(new BufferedWriter(new FileWriter(report)));
        }
        catch (IOException e) {
            reportWriter = null;
            throw new RuntimeException("Cannot create writer to file: " + report);
        }

        // create timer
        timer = new Timer();

        // load properties
        try {
            properties = new Properties();
            properties.load(getClass().getClassLoader().getResourceAsStream("benchmark-ids.properties"));
        }
        catch (IOException e) {
            properties = null;
            throw new RuntimeException("Cannot load properties from benchmark-ids.properties");
        }

        // fetch dao
        this.atlasDAO = (AtlasDAO) factory.getBean("atlasDAO");
    }

    public void runBenchmarking() {
        // just run all benchmarking tests
        System.out.print("Running benchmarks...");
        benchmarkGetAllArrayDesigns();
        System.out.print(".");
        benchmarkGetAllAssays();
        System.out.print(".");
        benchmarkGetAllExperiments();
        System.out.print(".");
        benchmarkGetAllExperimentsPendingAnalytics();
        System.out.print(".");
        benchmarkGetAllExperimentsPendingIndexing();
        System.out.print(".");
        benchmarkGetAllExperimentsPendingNetCDFs();
        System.out.print(".");
        benchmarkGetAllGenes();
        System.out.print(".");
        benchmarkGetAllPendingGenes();
        System.out.print(".");
        benchmarkGetArrayDesignByAccession();
        System.out.print(".");
        benchmarkGetArrayDesignByExperimentAccession();
        System.out.print(".");
        benchmarkGetAssaysByExperimentAccession();
        System.out.print(".");
        benchmarkGetAssaysByExperimentAndArray();
        System.out.print(".");
        benchmarkGetAtlasCountsByExperimentID();
        System.out.print(".");
        benchmarkGetAtlasResults();
        System.out.print(".");
        benchmarkGetAtlasStatisticsByDataRelease();
        System.out.print(".");
        benchmarkGetDesignElementsByArrayAccession();
        System.out.print(".");
        benchmarkGetDesignElementsByArrayID();
        System.out.print(".");
        benchmarkGetDesignElementsByGeneID();
        System.out.print(".");
        benchmarkGetExperimentByAccession();
        System.out.print(".");
        benchmarkGetExpressionAnalyticsByDesignElementID();
        System.out.print(".");
        benchmarkGetExpressionAnalyticsByExperimentID();
        System.out.print(".");
        benchmarkGetExpressionAnalyticsByGeneID();
        System.out.print(".");
        benchmarkGetExpressionValuesByExperimentAndArray();
        System.out.print(".");
        benchmarkGetExpressionValuesForAssays();
        System.out.print(".");
        benchmarkGetGeneCount();
        System.out.print(".");
        benchmarkGetGenesByExperimentAccession();
        System.out.print(".");
        benchmarkGetLoadDetails();
        System.out.print(".");
        benchmarkGetLoadDetailsByAccession();
        System.out.print(".");
        benchmarkGetOntologyMappings();
        System.out.print(".");
        benchmarkGetOntologyMappingsByExperimentAccession();
        System.out.print(".");
        benchmarkGetOntologyMappingsByOntology();
        System.out.print(".");
        benchmarkGetPropertiesForAssays();
        System.out.print(".");
        benchmarkGetPropertiesForGenes();
        System.out.print(".");
        benchmarkGetPropertyValueCount();
        System.out.print(".");
        benchmarkGetSamplesByAssayAccession();
        System.out.print(".");
        benchmarkGetSamplesByExperimentAccession();
        System.out.print(".");
        System.out.println("done!");

        reportWriter.close();
    }

    public void benchmarkGetLoadDetails() {
        reportBenchmarks("getLoadDetailsForExperiments()",
                         AtlasDAO.LOAD_MONITOR_SELECT,
                         timer.execute(new TimedOperation() {
                             void doOperation() {
                                 atlasDAO.getLoadDetailsForExperiments();
                             }
                         }));
    }

    public void benchmarkGetLoadDetailsByAccession() {
        final String accession = this.extractParameter("load.details.accession");
        reportBenchmarks("getLoadDetailsForExperimentsByAccession()", AtlasDAO.LOAD_MONITOR_BY_ACC_SELECT,
                         timer.execute(new TimedOperation() {
                             void doOperation() {
                                 atlasDAO.getLoadDetailsForExperimentsByAccession(accession);
                             }
                         }));
    }

    public void benchmarkGetAllExperiments() {
        reportBenchmarks("getAllExperiments()", AtlasDAO.EXPERIMENTS_SELECT, timer.execute(new TimedOperation() {
            void doOperation() {
                atlasDAO.getAllExperiments();
            }
        }));
    }

    public void benchmarkGetAllExperimentsPendingIndexing() {
        reportBenchmarks("getAllExperimentsPendingIndexing()", AtlasDAO.EXPERIMENTS_PENDING_INDEX_SELECT,
                         timer.execute(new TimedOperation() {
                             void doOperation() {
                                 atlasDAO.getAllExperimentsPendingIndexing();
                             }
                         }));
    }

    public void benchmarkGetAllExperimentsPendingNetCDFs() {
        reportBenchmarks("getAllExperimentsPendingNetCDFs()", AtlasDAO.EXPERIMENTS_PENDING_NETCDF_SELECT,
                         timer.execute(new TimedOperation() {
                             void doOperation() {
                                 atlasDAO.getAllExperimentsPendingNetCDFs();
                             }
                         }));
    }

    public void benchmarkGetAllExperimentsPendingAnalytics() {
        reportBenchmarks("getAllExperimentsPendingAnalytics()", AtlasDAO.EXPERIMENTS_PENDING_ANALYTICS_SELECT,
                         timer.execute(new TimedOperation() {
                             void doOperation() {
                                 atlasDAO.getAllExperimentsPendingAnalytics();
                             }
                         }));
    }

    public void benchmarkGetExperimentByAccession() {
        final String accession = extractParameter("experiment.accession");
        reportBenchmarks("getExperimentByAccession()", AtlasDAO.EXPERIMENT_BY_ACC_SELECT, timer.execute(new TimedOperation() {
            void doOperation() {
                atlasDAO.getExperimentByAccession(accession);
            }
        }));
    }

    public void benchmarkGetAllGenes() {
        reportBenchmarks("getAllGenes()", AtlasDAO.GENES_SELECT, timer.execute(new TimedOperation() {
            void doOperation() {
                atlasDAO.getAllGenes();
            }
        }));
    }

    public void benchmarkGetAllPendingGenes() {
        reportBenchmarks("getAllPendingGenes()", AtlasDAO.GENES_PENDING_SELECT, timer.execute(new TimedOperation() {
            void doOperation() {
                atlasDAO.getAllPendingGenes();
            }
        }));
    }

    public void benchmarkGetGenesByExperimentAccession() {
        final String accession = extractParameter("experiment.accession");
        reportBenchmarks("getGenesByExperimentAccession()", AtlasDAO.GENES_BY_EXPERIMENT_ACCESSION,
                         timer.execute(new TimedOperation() {
                             void doOperation() {
                                 atlasDAO.getGenesByExperimentAccession(accession);
                             }
                         }));
    }

    public void benchmarkGetPropertiesForGenes() {
        final String acc = extractParameter("experiment.accession");
        final List<Gene> genes = atlasDAO.getGenesByExperimentAccession(acc);
        reportBenchmarks("getPropertiesForAssays()", AtlasDAO.PROPERTIES_BY_RELATED_ASSAYS + "(for experiment " + acc + ")",
                         timer.execute(new TimedOperation() {
                             void doOperation() {
                                 atlasDAO.getPropertiesForGenes(genes.subList(0, 1));
                             }
                         }));
    }

    public void benchmarkGetGeneCount() {
        reportBenchmarks("getGeneCount()", AtlasDAO.GENE_COUNT_SELECT, timer.execute(new TimedOperation() {
            void doOperation() {
                atlasDAO.getGeneCount();
            }
        }));

    }

    public void benchmarkGetAllAssays() {
        reportBenchmarks("getAllAssays()", AtlasDAO.ASSAYS_SELECT, timer.execute(new TimedOperation() {
            void doOperation() {
                atlasDAO.getAllAssays();
            }
        }));

    }

    public void benchmarkGetAssaysByExperimentAccession() {
        final String accession = extractParameter("experiment.accession");
        reportBenchmarks("getAssaysByExperimentAccession()", AtlasDAO.ASSAYS_BY_EXPERIMENT_ACCESSION,
                         timer.execute(new TimedOperation() {
                             void doOperation() {
                                 atlasDAO.getAssaysByExperimentAccession(accession);
                             }
                         }));
    }

    public void benchmarkGetAssaysByExperimentAndArray() {
        final String accession = extractParameter("experiment.accession");
        final String arrayAccession = extractParameter("array.accession");
        reportBenchmarks("getAssaysByExperimentAndArray()", AtlasDAO.ASSAYS_BY_EXPERIMENT_AND_ARRAY_ACCESSION,
                         timer.execute(new TimedOperation() {
                             void doOperation() {
                                 atlasDAO.getAssaysByExperimentAndArray(accession, arrayAccession);
                             }
                         }));
    }

    public void benchmarkGetPropertiesForAssays() {
        final String acc = extractParameter("experiment.accession");
        final List<Assay> assays = atlasDAO.getAssaysByExperimentAccession(acc);
        reportBenchmarks("getPropertiesForAssays()", AtlasDAO.PROPERTIES_BY_RELATED_ASSAYS + "(for experiment " + acc + ")",
                         timer.execute(new TimedOperation() {
                             void doOperation() {
                                 atlasDAO.getPropertiesForAssays(assays);
                             }
                         }));
    }

    public void benchmarkGetExpressionValuesForAssays() {
        final String acc = extractParameter("experiment.accession");
        final List<Assay> assays = atlasDAO.getAssaysByExperimentAccession(acc);
        reportBenchmarks("getExpressionValuesForAssays()", AtlasDAO.EXPRESSION_VALUES_BY_RELATED_ASSAYS,
                         timer.execute(new TimedOperation() {
                             void doOperation() {
                                 atlasDAO.getExpressionValuesForAssays(assays);
                             }
                         }));
    }

    public void benchmarkGetExpressionValuesByExperimentAndArray() {
        final int expID = Integer.parseInt(extractParameter("experiment.id"));
        final int arrayID = Integer.parseInt(extractParameter("array.id"));
        reportBenchmarks("getExpressionValuesByExperimentAndArray()", AtlasDAO.EXPRESSION_VALUES_BY_EXPERIMENT_AND_ARRAY,
                         timer.execute(new TimedOperation() {
                             void doOperation() {
                                 atlasDAO.getExpressionValuesByExperimentAndArray(expID, arrayID);
                             }
                         }));
    }

    public void benchmarkGetSamplesByAssayAccession() {
        final String assayAccession = extractParameter("assay.accession");
        reportBenchmarks("getSamplesByAssayAccession()", AtlasDAO.SAMPLES_BY_ASSAY_ACCESSION,
                         timer.execute(new TimedOperation() {
                             void doOperation() {
                                 atlasDAO.getSamplesByAssayAccession(assayAccession);
                             }
                         }));
    }

    public void benchmarkGetSamplesByExperimentAccession() {
        final String expAcc = extractParameter("experiment.accession");
        reportBenchmarks("getSamplesByExperimentAccession()", AtlasDAO.SAMPLES_BY_EXPERIMENT_ACCESSION,
                         timer.execute(new TimedOperation() {
                             void doOperation() {
                                 atlasDAO.getSamplesByExperimentAccession(expAcc);
                             }
                         }));
    }

    public void benchmarkGetPropertyValueCount() {
        reportBenchmarks("getPropertyValueCount()", AtlasDAO.PROPERTY_VALUE_COUNT_SELECT, timer.execute(new TimedOperation() {
            void doOperation() {
                atlasDAO.getPropertyValueCount();
            }
        }));

    }

    public void benchmarkGetAllArrayDesigns() {
        reportBenchmarks("getAllArrayDesigns()", AtlasDAO.ARRAY_DESIGN_SELECT, timer.execute(new TimedOperation() {
            void doOperation() {
                atlasDAO.getAllArrayDesigns();
            }
        }));

    }

    public void benchmarkGetArrayDesignByAccession() {
        final String arrayAcc = extractParameter("array.accession");
        reportBenchmarks("getArrayDesignByAccession()", AtlasDAO.ARRAY_DESIGN_BY_ACC_SELECT, timer.execute(new TimedOperation() {
            void doOperation() {
                atlasDAO.getArrayDesignByAccession(arrayAcc);
            }
        }));
    }

    public void benchmarkGetArrayDesignByExperimentAccession() {
        final String expAcc = extractParameter("experiment.accession");
        reportBenchmarks("getArrayDesignByExperimentAccession()", AtlasDAO.ARRAY_DESIGN_BY_EXPERIMENT_ACCESSION,
                         timer.execute(new TimedOperation() {
                             void doOperation() {
                                 atlasDAO.getArrayDesignByExperimentAccession(expAcc);
                             }
                         }));

    }

    public void benchmarkGetDesignElementsByArrayAccession() {
        final String arrAcc = extractParameter("array.accession");
        reportBenchmarks("getDesignElementsByArrayAccession()", AtlasDAO.DESIGN_ELEMENTS_BY_ARRAY_ACCESSION,
                         timer.execute(new TimedOperation() {
                             void doOperation() {
                                 atlasDAO.getDesignElementsByArrayAccession(arrAcc);
                             }
                         }));

    }

    public void benchmarkGetDesignElementsByArrayID() {
        final int arrID = Integer.parseInt(extractParameter("array.id"));
        reportBenchmarks("getDesignElementsByArrayID()", AtlasDAO.DESIGN_ELEMENTS_BY_ARRAY_ID,
                         timer.execute(new TimedOperation() {
                             void doOperation() {
                                 atlasDAO.getDesignElementsByArrayID(arrID);
                             }
                         }));

    }

    public void benchmarkGetDesignElementsByGeneID() {
        final int geneID = Integer.parseInt(extractParameter("gene.id"));
        reportBenchmarks("getDesignElementsByGeneID()", AtlasDAO.DESIGN_ELEMENTS_BY_GENEID, timer.execute(new TimedOperation() {
            void doOperation() {
                atlasDAO.getDesignElementsByGeneID(geneID);
            }
        }));

    }

    public void benchmarkGetExpressionAnalyticsByGeneID() {
        final int geneID = Integer.parseInt(extractParameter("gene.id"));
        reportBenchmarks("getExpressionAnalyticsByGeneID()", AtlasDAO.EXPRESSIONANALYTICS_BY_GENEID,
                         timer.execute(new TimedOperation() {
                             void doOperation() {
                                 atlasDAO.getExpressionAnalyticsByGeneID(geneID);
                             }
                         }));

    }

    public void benchmarkGetExpressionAnalyticsByDesignElementID() {
        final int deID = Integer.parseInt(extractParameter("design.element.id"));
        reportBenchmarks("getExpressionAnalyticsByDesignElementID()", AtlasDAO.EXPRESSIONANALYTICS_BY_DESIGNELEMENTID,
                         timer.execute(new TimedOperation() {
                             void doOperation() {
                                 atlasDAO.getExpressionAnalyticsByDesignElementID(deID);
                             }
                         }));
    }

    public void benchmarkGetExpressionAnalyticsByExperimentID() {
        final int expID = Integer.parseInt(extractParameter("experiment.id"));
        reportBenchmarks("getExpressionAnalyticsByExperimentID()", AtlasDAO.EXPRESSIONANALYTICS_BY_EXPERIMENTID,
                         timer.execute(new TimedOperation() {
                             void doOperation() {
                                 atlasDAO.getExpressionAnalyticsByExperimentID(expID);
                             }
                         }));
    }

    public void benchmarkGetOntologyMappings() {
        reportBenchmarks("getOntologyMappings()", AtlasDAO.ONTOLOGY_MAPPINGS_SELECT, timer.execute(new TimedOperation() {
            void doOperation() {
                atlasDAO.getOntologyMappings();
            }
        }));

    }

    public void benchmarkGetOntologyMappingsByOntology() {
        final String ontology = "EFO";
        reportBenchmarks("getOntologyMappingsByOntology()", AtlasDAO.ONTOLOGY_MAPPINGS_BY_ONTOLOGY_NAME,
                         timer.execute(new TimedOperation() {
                             void doOperation() {
                                 atlasDAO.getOntologyMappingsByOntology(ontology);
                             }
                         }));
    }

    public void benchmarkGetOntologyMappingsByExperimentAccession() {
        final String accession = extractParameter("experiment.accession");
        reportBenchmarks("getOntologyMappingsByExperimentAccession()", AtlasDAO.ONTOLOGY_MAPPINGS_BY_EXPERIMENT_ACCESSION,
                         timer.execute(new TimedOperation() {
                             void doOperation() {
                                 atlasDAO.getOntologyMappingsByExperimentAccession(accession);
                             }
                         }));

    }

    public void benchmarkGetAtlasCountsByExperimentID() {
        final int expID = Integer.parseInt(extractParameter("experiment.id"));
        reportBenchmarks("getAtlasCountsByExperimentID()", AtlasDAO.ATLAS_COUNTS_BY_EXPERIMENTID,
                         timer.execute(new TimedOperation() {
                             void doOperation() {
                                 atlasDAO.getAtlasCountsByExperimentID(expID);
                             }
                         }));
    }

    public void benchmarkGetAtlasResults() {

    }

    public void benchmarkGetAtlasStatisticsByDataRelease() {

    }

    private String extractParameter(String name) {
        if (properties.containsKey(name)) {
            return properties.getProperty(name);
        }
        else {
            throw new RuntimeException("Property '" + name + "' not found");
        }
    }

    private void reportBenchmarks(String methodName, String sql, float time) {
        reportWriter.println("==========");
        reportWriter.println("Benchmark:\t" + methodName);
        reportWriter.println("SQL:\t\t" + sql);
        reportWriter.println("Time:\t\t" + time + "s.");
        reportWriter.println("==========");
        reportWriter.println();
        reportWriter.flush();
    }

    private class Timer {
        /**
         * Returns the time, in seconds, that the supplied TimedOperation took to execute
         *
         * @param operation the operation to execute
         * @return the time in seconds that this operation took
         */
        public float execute(TimedOperation operation) {
            // start the timer
            final long indexStart = System.currentTimeMillis();

            // execute the run the operation
            operation.doOperation();

            // end the timer
            final long indexEnd = System.currentTimeMillis();

            // return the runtime
            return ((float) (indexEnd - indexStart)) / 1000;
        }
    }

    private abstract class TimedOperation {
        abstract void doOperation();
    }
}
