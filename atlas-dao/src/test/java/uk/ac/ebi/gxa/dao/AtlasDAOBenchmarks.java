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

import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import uk.ac.ebi.microarray.atlas.model.BioEntity;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import static uk.ac.ebi.gxa.exceptions.LogUtil.logUnexpected;

public class AtlasDAOBenchmarks {
    private PrintWriter reportWriter;
    private AtlasDAO atlasDAO;
    private BioEntityDAO bioEntityDAO;

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
            throw logUnexpected("Cannot create directory: " + directory);
        }
        try {
            reportWriter = new PrintWriter(new BufferedWriter(new FileWriter(report)));
        } catch (IOException e) {
            reportWriter = null;
            throw logUnexpected("Cannot create writer to file: " + report, e);
        }

        // create timer
        timer = new Timer();

        // load properties
        try {
            properties = new Properties();
            properties.load(getClass().getClassLoader().getResourceAsStream("benchmark-ids.properties"));
        } catch (IOException e) {
            properties = null;
            throw logUnexpected("Cannot load properties from benchmark-ids.properties", e);
        }

        // fetch dao
        this.atlasDAO = factory.getBean(AtlasDAO.class);
        this.bioEntityDAO = factory.getBean(BioEntityDAO.class);
    }

    public void runBenchmarking() {
        // just run all benchmarking tests
        System.out.print("Running benchmarks...");
        benchmarkGetAllDesignElementsForGene();
        System.out.print(".");
        benchmarkGetAllArrayDesigns();
        System.out.print(".");
        benchmarkGetAllExperiments();
        System.out.print(".");
        benchmarkGetAllGenes();
        System.out.print(".");
        benchmarkGetArrayDesignByAccession();
        System.out.print(".");
        benchmarkGetAssaysByExperimentAccession();
        System.out.print(".");
        benchmarkGetExperimentByAccession();
        System.out.print(".");
        benchmarkGetGenesByExperimentAccession();
        System.out.print(".");
        benchmarkGetOntologyMappingsByOntology();
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


    public void benchmarkGetAllExperiments() {
        reportBenchmarks("getAllExperiments()", timer.execute(new Runnable() {
            public void run() {
                atlasDAO.getAllExperiments();
            }
        }));
    }

    public void benchmarkGetExperimentByAccession() {
        final String accession = extractParameter("experiment.accession");
        reportBenchmarks("getExperimentByAccession()", timer.execute(new Runnable() {
            public void run() {
                atlasDAO.getExperimentByAccession(accession);
            }
        }));
    }

    public void benchmarkGetAllGenes() {
        reportBenchmarks("getAllGenesFast()", timer.execute(new Runnable() {
            public void run() {
                bioEntityDAO.getAllGenesFast();
            }
        }));
    }

    public void benchmarkGetGenesByExperimentAccession() {
        final String accession = extractParameter("experiment.accession");
        reportBenchmarks("getGenesByExperimentAccession()",
                timer.execute(new Runnable() {
                    public void run() {
                        bioEntityDAO.getGenesByExperimentAccession(accession);
                    }
                }));
    }

    public void benchmarkGetPropertiesForGenes() {
        final String acc = extractParameter("experiment.accession");
        final List<BioEntity> bioEntities = bioEntityDAO.getGenesByExperimentAccession(acc);
        reportBenchmarks("getPropertiesForAssays()",
                timer.execute(new Runnable() {
                    public void run() {
                        bioEntityDAO.getPropertiesForGenes(bioEntities.subList(0, 1));
                    }
                }));
    }

    public void benchmarkGetAssaysByExperimentAccession() {
        final String accession = extractParameter("experiment.accession");
        reportBenchmarks("getAssaysByExperimentAccession()",
                timer.execute(new Runnable() {
                    public void run() {
                        atlasDAO.getAssaysByExperimentAccession(accession);
                    }
                }));
    }

    public void benchmarkGetSamplesByAssayAccession() {
        final String assayAccession = extractParameter("assay.accession");
        reportBenchmarks("getSamplesByAssayAccession()",
                timer.execute(new Runnable() {
                    public void run() {
                        //TODO:
                        atlasDAO.getSamplesByAssayAccession("experimentAccession", assayAccession);
                    }
                }));
    }

    public void benchmarkGetSamplesByExperimentAccession() {
        final String expAcc = extractParameter("experiment.accession");
        reportBenchmarks("getSamplesByExperimentAccession()",
                timer.execute(new Runnable() {
                    public void run() {
                        atlasDAO.getSamplesByExperimentAccession(expAcc);
                    }
                }));
    }

    public void benchmarkGetPropertyValueCount() {
        reportBenchmarks("getPropertyValueCount()", timer.execute(new Runnable() {
            public void run() {
                atlasDAO.getPropertyValueCount();
            }
        }));

    }

    public void benchmarkGetAllArrayDesigns() {
        reportBenchmarks("getAllArrayDesigns()", timer.execute(new Runnable() {
            public void run() {
                atlasDAO.getAllArrayDesigns();
            }
        }));

    }

    public void benchmarkGetAllDesignElementsForGene() {
        reportBenchmarks("getAllDesignElementsForGene()", timer.execute(new Runnable() {
            public void run() {
                bioEntityDAO.getAllDesignElementsForGene();
            }
        }));

    }

    public void benchmarkGetArrayDesignByAccession() {
        final String arrayAcc = extractParameter("array.accession");
        reportBenchmarks("getArrayDesignByAccession()", timer.execute(new Runnable() {
            public void run() {
                atlasDAO.getArrayDesignByAccession(arrayAcc);
            }
        }));
    }

    public void benchmarkGetOntologyMappingsByOntology() {
        final String ontology = "EFO";
        reportBenchmarks("getOntologyMappingsByOntology()",
                timer.execute(new Runnable() {
                    public void run() {
                        atlasDAO.getOntologyMappingsByOntology(ontology);
                    }
                }));
    }

    private String extractParameter(String name) {
        if (properties.containsKey(name)) {
            return properties.getProperty(name);
        } else {
            throw logUnexpected("Property '" + name + "' not found");
        }
    }

    private void reportBenchmarks(String methodName, float time) {
        reportWriter.println("==========");
        reportWriter.println("Benchmark:\t" + methodName);
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
