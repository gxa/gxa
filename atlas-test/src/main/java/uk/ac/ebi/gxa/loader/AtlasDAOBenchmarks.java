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

public class AtlasDAOBenchmarks {
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
        File directory = report.getAbsoluteFile().getParentFile();
        if (!directory.exists() && !directory.mkdirs()) {
            throw new RuntimeException("Cannot create directory: " + directory);
        }
        try {
            reportWriter = new PrintWriter(new BufferedWriter(new FileWriter(report)));
        } catch (IOException e) {
            reportWriter = null;
            throw new RuntimeException("Cannot create writer to file: " + report);
        }

        // create timer
        timer = new Timer();

        // load properties
        try {
            properties = new Properties();
            properties.load(getClass().getClassLoader().getResourceAsStream("benchmark-ids.properties"));
        } catch (IOException e) {
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
        benchmarkGetArrayDesignByAccession();
        System.out.print(".");
        benchmarkGetArrayDesignByExperimentAccession();
        System.out.print(".");
        benchmarkGetAssaysByExperimentAccession();
        System.out.print(".");
        benchmarkGetAssaysByExperimentAndArray();
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
                AtlasDAO.EXPERIMENT_LOAD_MONITOR_SELECT,
                timer.execute(new Runnable() {
                    public void run() {
                        atlasDAO.getLoadDetailsForExperiments();
                    }
                }));
    }

    public void benchmarkGetLoadDetailsByAccession() {
        final String accession = this.extractParameter("load.details.accession");
        reportBenchmarks("getLoadDetailsForExperimentsByAccession()", AtlasDAO.EXPERIMENT_LOAD_MONITOR_BY_ACC_SELECT,
                timer.execute(new Runnable() {
                    public void run() {
                        atlasDAO.getLoadDetailsForExperimentsByAccession(accession);
                    }
                }));
    }

    public void benchmarkGetAllExperiments() {
        reportBenchmarks("getAllExperiments()", AtlasDAO.EXPERIMENTS_SELECT, timer.execute(new Runnable() {
            public void run() {
                atlasDAO.getAllExperiments();
            }
        }));
    }

    public void benchmarkGetAllExperimentsPendingIndexing() {
        reportBenchmarks("getAllExperimentsPendingIndexing()", AtlasDAO.EXPERIMENTS_PENDING_INDEX_SELECT,
                timer.execute(new Runnable() {
                    public void run() {
                        atlasDAO.getAllExperimentsPendingIndexing();
                    }
                }));
    }

    public void benchmarkGetAllExperimentsPendingNetCDFs() {
        reportBenchmarks("getAllExperimentsPendingNetCDFs()", AtlasDAO.EXPERIMENTS_PENDING_NETCDF_SELECT,
                timer.execute(new Runnable() {
                    public void run() {
                        atlasDAO.getAllExperimentsPendingNetCDFs();
                    }
                }));
    }

    public void benchmarkGetAllExperimentsPendingAnalytics() {
        reportBenchmarks("getAllExperimentsPendingAnalytics()", AtlasDAO.EXPERIMENTS_PENDING_ANALYTICS_SELECT,
                timer.execute(new Runnable() {
                    public void run() {
                        atlasDAO.getAllExperimentsPendingAnalytics();
                    }
                }));
    }

    public void benchmarkGetExperimentByAccession() {
        final String accession = extractParameter("experiment.accession");
        reportBenchmarks("getExperimentByAccession()", AtlasDAO.EXPERIMENT_BY_ACC_SELECT, timer.execute(new Runnable() {
            public void run() {
                atlasDAO.getExperimentByAccession(accession);
            }
        }));
    }

    public void benchmarkGetAllGenes() {
        reportBenchmarks("getAllGenes()", AtlasDAO.GENES_SELECT, timer.execute(new Runnable() {
            public void run() {
                atlasDAO.getAllGenes();
            }
        }));
    }

    public void benchmarkGetGenesByExperimentAccession() {
        final String accession = extractParameter("experiment.accession");
        reportBenchmarks("getGenesByExperimentAccession()", AtlasDAO.GENES_BY_EXPERIMENT_ACCESSION,
                timer.execute(new Runnable() {
                    public void run() {
                        atlasDAO.getGenesByExperimentAccession(accession);
                    }
                }));
    }

    public void benchmarkGetPropertiesForGenes() {
        final String acc = extractParameter("experiment.accession");
        final List<Gene> genes = atlasDAO.getGenesByExperimentAccession(acc);
        reportBenchmarks("getPropertiesForAssays()", AtlasDAO.PROPERTIES_BY_RELATED_ASSAYS + "(for experiment " + acc + ")",
                timer.execute(new Runnable() {
                    public void run() {
                        atlasDAO.getPropertiesForGenes(genes.subList(0, 1));
                    }
                }));
    }

    public void benchmarkGetGeneCount() {
        reportBenchmarks("getGeneCount()", AtlasDAO.GENE_COUNT_SELECT, timer.execute(new Runnable() {
            public void run() {
                atlasDAO.getGeneCount();
            }
        }));

    }

    public void benchmarkGetAllAssays() {
        reportBenchmarks("getAllAssays()", AtlasDAO.ASSAYS_SELECT, timer.execute(new Runnable() {
            public void run() {
                atlasDAO.getAllAssays();
            }
        }));

    }

    public void benchmarkGetAssaysByExperimentAccession() {
        final String accession = extractParameter("experiment.accession");
        reportBenchmarks("getAssaysByExperimentAccession()", AtlasDAO.ASSAYS_BY_EXPERIMENT_ACCESSION,
                timer.execute(new Runnable() {
                    public void run() {
                        atlasDAO.getAssaysByExperimentAccession(accession);
                    }
                }));
    }

    public void benchmarkGetAssaysByExperimentAndArray() {
        final String accession = extractParameter("experiment.accession");
        final String arrayAccession = extractParameter("array.accession");
        reportBenchmarks("getAssaysByExperimentAndArray()", AtlasDAO.ASSAYS_BY_EXPERIMENT_AND_ARRAY_ACCESSION,
                timer.execute(new Runnable() {
                    public void run() {
                        atlasDAO.getAssaysByExperimentAndArray(accession, arrayAccession);
                    }
                }));
    }

    public void benchmarkGetPropertiesForAssays() {
        final String acc = extractParameter("experiment.accession");
        final List<Assay> assays = atlasDAO.getAssaysByExperimentAccession(acc);
        reportBenchmarks("getPropertiesForAssays()", AtlasDAO.PROPERTIES_BY_RELATED_ASSAYS + "(for experiment " + acc + ")",
                timer.execute(new Runnable() {
                    public void run() {
                        atlasDAO.getPropertiesForAssays(assays);
                    }
                }));
    }

    public void benchmarkGetSamplesByAssayAccession() {
        final String assayAccession = extractParameter("assay.accession");
        reportBenchmarks("getSamplesByAssayAccession()", AtlasDAO.SAMPLES_BY_ASSAY_ACCESSION,
                         timer.execute(new Runnable() {
                             public void run() {
                                 //TODO:
                                 atlasDAO.getSamplesByAssayAccession("experimentAccession",assayAccession);
                             }
                         }));
    }

    public void benchmarkGetSamplesByExperimentAccession() {
        final String expAcc = extractParameter("experiment.accession");
        reportBenchmarks("getSamplesByExperimentAccession()", AtlasDAO.SAMPLES_BY_EXPERIMENT_ACCESSION,
                timer.execute(new Runnable() {
                    public void run() {
                        atlasDAO.getSamplesByExperimentAccession(expAcc);
                    }
                }));
    }

    public void benchmarkGetPropertyValueCount() {
        reportBenchmarks("getPropertyValueCount()", AtlasDAO.PROPERTY_VALUE_COUNT_SELECT, timer.execute(new Runnable() {
            public void run() {
                atlasDAO.getPropertyValueCount();
            }
        }));

    }

    public void benchmarkGetAllArrayDesigns() {
        reportBenchmarks("getAllArrayDesigns()", AtlasDAO.ARRAY_DESIGN_SELECT, timer.execute(new Runnable() {
            public void run() {
                atlasDAO.getAllArrayDesigns();
            }
        }));

    }

    public void benchmarkGetArrayDesignByAccession() {
        final String arrayAcc = extractParameter("array.accession");
        reportBenchmarks("getArrayDesignByAccession()", AtlasDAO.ARRAY_DESIGN_BY_ACC_SELECT, timer.execute(new Runnable() {
            public void run() {
                atlasDAO.getArrayDesignByAccession(arrayAcc);
            }
        }));
    }

    public void benchmarkGetArrayDesignByExperimentAccession() {
        final String expAcc = extractParameter("experiment.accession");
        reportBenchmarks("getArrayDesignByExperimentAccession()", AtlasDAO.ARRAY_DESIGN_BY_EXPERIMENT_ACCESSION,
                timer.execute(new Runnable() {
                    public void run() {
                        atlasDAO.getArrayDesignByExperimentAccession(expAcc);
                    }
                }));

    }

    public void benchmarkGetDesignElementsByArrayAccession() {
        final String arrAcc = extractParameter("array.accession");
        reportBenchmarks("getDesignElementsByArrayAccession()", AtlasDAO.DESIGN_ELEMENTS_BY_ARRAY_ACCESSION,
                timer.execute(new Runnable() {
                    public void run() {
                        atlasDAO.getDesignElementsByArrayAccession(arrAcc);
                    }
                }));

    }

    public void benchmarkGetDesignElementsByArrayID() {
        final long arrID = Long.parseLong(extractParameter("array.id"));
        reportBenchmarks("getDesignElementsByArrayID()", AtlasDAO.DESIGN_ELEMENTS_BY_ARRAY_ID,
                timer.execute(new Runnable() {
                    public void run() {
                        atlasDAO.getDesignElementsByArrayID(arrID);
                    }
                }));

    }

    public void benchmarkGetDesignElementsByGeneID() {
        final long geneID = Long.parseLong(extractParameter("gene.id"));
        reportBenchmarks("getDesignElementsByGeneID()", AtlasDAO.DESIGN_ELEMENT_MAP_BY_GENEID, timer.execute(new Runnable() {
            public void run() {
                atlasDAO.getDesignElementMapByGeneID(geneID);
            }
        }));

    }

    public void benchmarkGetOntologyMappings() {
        reportBenchmarks("getOntologyMappings()", AtlasDAO.ONTOLOGY_MAPPINGS_SELECT, timer.execute(new Runnable() {
            public void run() {
                atlasDAO.getOntologyMappings();
            }
        }));

    }

    public void benchmarkGetOntologyMappingsByOntology() {
        final String ontology = "EFO";
        reportBenchmarks("getOntologyMappingsByOntology()", AtlasDAO.ONTOLOGY_MAPPINGS_BY_ONTOLOGY_NAME,
                timer.execute(new Runnable() {
                    public void run() {
                        atlasDAO.getOntologyMappingsByOntology(ontology);
                    }
                }));
    }

    public void benchmarkGetOntologyMappingsByExperimentAccession() {
        final String accession = extractParameter("experiment.accession");
        reportBenchmarks("getOntologyMappingsByExperimentAccession()", AtlasDAO.ONTOLOGY_MAPPINGS_BY_EXPERIMENT_ACCESSION,
                timer.execute(new Runnable() {
                    public void run() {
                        atlasDAO.getOntologyMappingsByExperimentAccession(accession);
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
        } else {
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

    private static class Timer {
        /**
         * Returns the time, in seconds, that the supplied {@link Runnable} took to execute
         *
         * @param operation the operation to execute
         * @return the time in seconds that this operation took
         */
        public float execute(Runnable operation) {
            // start the timer
            final long indexStart = System.currentTimeMillis();

            // execute the run the operation
            operation.run();

            // end the timer
            final long indexEnd = System.currentTimeMillis();

            // return the runtime
            return ((float) (indexEnd - indexStart)) / 1000;
        }
    }
}
