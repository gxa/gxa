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

import org.apache.commons.cli.*;
import org.apache.solr.core.CoreContainer;
import org.slf4j.bridge.SLF4JBridgeHandler;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import uk.ac.ebi.gxa.analytics.generator.AnalyticsGenerator;
import uk.ac.ebi.gxa.analytics.generator.AnalyticsGeneratorException;
import uk.ac.ebi.gxa.analytics.generator.listener.AnalyticsGenerationEvent;
import uk.ac.ebi.gxa.analytics.generator.listener.AnalyticsGeneratorListener;
import uk.ac.ebi.gxa.index.builder.IndexBuilder;
import uk.ac.ebi.gxa.index.builder.UpdateIndexForExperimentCommand;
import uk.ac.ebi.gxa.index.builder.listener.IndexBuilderEvent;
import uk.ac.ebi.gxa.index.builder.listener.IndexBuilderListener;
import uk.ac.ebi.gxa.loader.listener.AtlasLoaderEvent;
import uk.ac.ebi.gxa.loader.listener.AtlasLoaderListener;
import uk.ac.ebi.gxa.impl.ModelImpl;
import uk.ac.ebi.gxa.Model;
import uk.ac.ebi.gxa.Experiment;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.logging.LogManager;

public class LoaderDriver {
    private static String magetab_file_url = "";
    private static String load_type = "";

    private static String accession = "ALL";
    private static boolean do_load = false;
    private static boolean do_delete = false;
    private static boolean do_index = false;
    private static boolean do_netcdf = false;
    private static boolean do_analytics = false;

    public static void main(String[] args) {
        parseArgs(args);

        execute();
    }

    private static void parseArgs(String[] args) {
        // Create a commons-cli parser
        CommandLineParser parser = new BasicParser();
        Options options = new Options();

        options.addOption("h", "help", false, "Print this usage information");

        OptionGroup coreOptions = new OptionGroup();
        Option load = new Option("load", false, "Load a MAGE-TAB format file into the Atlas - requires -f");
        Option delete = new Option("delete", false, "Remove an experiment from the Atlas - requires -a");
        Option index = new Option("index", false, "Run the Atlas index builder - requires one of -a/-all");
        Option netcdf = new Option("netcdf", false, "Run the Atlas NetCDF generator - requires one of -a/-all");
        Option analytics =
                new Option("analytics", false, "Run the Atlas Analytics generator - requires one of -a/-all");

        coreOptions.addOption(load).addOption(delete).addOption(index).addOption(netcdf).addOption(analytics);

        coreOptions.setRequired(true);
        options.addOptionGroup(coreOptions);

        OptionGroup modifierOptions = new OptionGroup();
        Option file = new Option("f", "file", true, "the MAGE-TAB file to load into atlas - use the absolute path");
        file.setArgName("absolute path");
        Option acc = new Option("a", "accession", true, "the accession of an experiment in the Atlas - " +
                "the specified action will be performed on this experiment");
        acc.setArgName("accession");
        Option all = new Option("all", false, "perform the specified action on ALL available experiments");

        modifierOptions.addOption(file).addOption(acc).addOption(all);

        modifierOptions.setRequired(true);
        options.addOptionGroup(modifierOptions);

        Option type = new Option("t", "type", true, "the type of load to perform - use 'experiment' or 'array'");
        type.setArgName("experiment|array");
        type.setOptionalArg(true);
        options.addOption(type);

        // Parse the arguments
        try {
            CommandLine commandLine = parser.parse(options, args);

            if (commandLine.hasOption('h')) {
                printUsage(options);
                System.exit(0);
            }
            if (commandLine.hasOption("load")) {
                do_load = true;
                if (commandLine.hasOption('f') && commandLine.hasOption('t')) {
                    if (commandLine.getOptionValue('f').startsWith("/")) {
                        magetab_file_url = "file://" + commandLine.getOptionValue('f');
                    } else {
                        magetab_file_url = commandLine.getOptionValue('f');
                    }
                    if (commandLine.getOptionValue('t').equals("experiment")) {
                        load_type = "experiment";
                    } else if (commandLine.getOptionValue('t').equals("array")) {
                        load_type = "array";
                    } else if (commandLine.getOptionValue('t').equals("varray")) {
                        load_type = "varray";
                    } else if (commandLine.getOptionValue('t').equals("bioentity")) {
                        load_type = "bioentity";
                    } else if (commandLine.getOptionValue('t').equals("mapping")) {
                        load_type = "mapping";
//                        if (commandLine.hasOption('a')) {
//                            accession = commandLine.getOptionValue('a');
//                        } else {
//                            throw new ParseException("You must specify the array design accession to load mappings");
//                        }
                    } else {
                        throw new ParseException("Valid types to load are 'experiment' or 'array'");
                    }
                } else {
                    throw new ParseException("In order to load, you must provide an absolute path to a MAGE-TAB file " +
                            "and the type of load to carry out");
                }
            }
            if (commandLine.hasOption("delete")) {
                do_delete = true;
                if (commandLine.hasOption('a')) {
                    accession = commandLine.getOptionValue('a');
                } else {
                    throw new ParseException("You must specify the accession to delete");
                }
            }
            if (commandLine.hasOption("netcdf")) {
                do_netcdf = true;
                if (commandLine.hasOption('a')) {
                    accession = commandLine.getOptionValue('a');
                } else if (commandLine.hasOption("all")) {
                    accession = "ALL";
                } else {
                    throw new ParseException("You must specify the accession or 'all' to generate netcdfs");
                }
            }
            if (commandLine.hasOption("index")) {
                do_index = true;
                if (commandLine.hasOption('a')) {
                    accession = commandLine.getOptionValue('a');
                } else {
                    throw new ParseException("You must specify the accession to index");
                }
//                if (!commandLine.hasOption("all")) {
//                    throw new ParseException("You must specify -all to build the index");
//                }
            }
            if (commandLine.hasOption("analytics")) {
                do_analytics = true;
                if (commandLine.hasOption('a')) {
                    accession = commandLine.getOptionValue('a');
                } else if (commandLine.hasOption("all")) {
                    accession = null;
                } else {
                    throw new ParseException("You must specify the accession or 'all' to generate analytics");
                }
            }
        } catch (ParseException e) {
            System.out.println(e.getMessage());
            printUsage(options);
            System.exit(1);
        }
    }

    private static void execute() {
        // configure logging
        try {
            LogManager.getLogManager()
                    .readConfiguration(LoaderDriver.class.getClassLoader().getResourceAsStream("logging.properties"));
        } catch (Exception e) {
            e.printStackTrace();
        }
        SLF4JBridgeHandler.install();

        // load spring config
        BeanFactory factory = new ClassPathXmlApplicationContext("loaderContext.xml");
        final Model atlasModel = factory.getBean(ModelImpl.class);
        final AtlasLoader loader = factory.getBean(AtlasLoader.class);
        final IndexBuilder builder = factory.getBean(IndexBuilder.class);
        final AnalyticsGenerator analytics = factory.getBean(AnalyticsGenerator.class);
        final CoreContainer solrContainer = factory.getBean(CoreContainer.class);
        // net

        // run the loader
        if (do_load) {
            try {
                final URL url = URI.create(magetab_file_url).toURL();
                final long indexStart = System.currentTimeMillis();
                AtlasLoaderListener listener = new AtlasLoaderListener() {

                    public void loadProgress(String progress) {
                        System.out.println(progress);
                    }

                    public void loadWarning(String message) {
                        System.out.println(message);
                    }

                    public void loadSuccess(AtlasLoaderEvent event) {
                        final long indexEnd = System.currentTimeMillis();

                        String total = new DecimalFormat("#.##").format(
                                (indexEnd - indexStart) / 60000);
                        System.out.println(
                                "Load completed successfully in " + total + " mins.");
                    }

                    public void loadError(AtlasLoaderEvent event) {
                        System.out.println("Load failed");
                        for (Throwable t : event.getErrors()) {
                            t.printStackTrace();
                        }
                    }
                };

                if (load_type.equals("experiment")) {
                    loader.doCommand(new LoadExperimentCommand(url), listener);
                } else if (load_type.equals("array")) {
                    loader.doCommand(new LoadArrayDesignCommand(url), listener);
                } else if (load_type.equals("varray")) {
                    loader.doCommand(new LoadVirtualArrayDesignCommand(url), listener);
                } else if (load_type.equals("bioentity")) {
                    loader.doCommand(new LoadBioentityCommand(url), listener);
                } else if (load_type.equals("mapping")) {
                    loader.doCommand(new LoadArrayDesignMappingCommand(url), listener);
                }

            } catch (MalformedURLException e) {
                e.printStackTrace();
                System.out.println("Load failed - inaccessible URL");
            }
        }

        if (do_netcdf) {
            /*
            if (accession.equals("ALL"))
                netcdf.generateNetCDFForAllExperiments(false);
            else
                netcdf.generateNetCDFForExperiment(accession, false);
            */
        }

        if (do_delete) {
            // in case we want to delete an experiment
            System.out.println("Deleting experiment...");
            atlasModel.createExperiment(accession).deleteFromStorage();
            System.out.println("Experiment deleted!");
        }

        // run the index builder
        if (do_index) {
            final long indexStart = System.currentTimeMillis();
            IndexBuilderListener listener = new IndexBuilderListener() {

                public void buildSuccess() {
                    final long indexEnd = System.currentTimeMillis();

                    String total = new DecimalFormat("#.##").format(
                            (indexEnd - indexStart) / 60000);
                    System.out.println(
                            "Index built successfully in " + total + " mins.");

                    solrContainer.shutdown();
                }

                public void buildError(IndexBuilderEvent event) {
                    System.out.println("Index failed to build");
                    for (Throwable t : event.getErrors()) {
                        t.printStackTrace();
                    }

                    solrContainer.shutdown();
                }

                public void buildProgress(String progressStatus) {
                    System.out.println("Index progress now: " + progressStatus);
                }
            };

            builder.doCommand(new UpdateIndexForExperimentCommand(accession), listener);
        } else {
            solrContainer.shutdown();
        }

        // run the analytics
        if (do_analytics) {
            final long netStart = System.currentTimeMillis();
            AnalyticsGeneratorListener listener = new AnalyticsGeneratorListener() {
                public void buildSuccess() {
                    final long netEnd = System.currentTimeMillis();

                    String total = new DecimalFormat("#.##").format(
                            (netEnd - netStart) / 60000);
                    System.out.println(
                            "Analytics generated successfully in " + total + " mins.");

                    try {
                        analytics.shutdown();
                    } catch (AnalyticsGeneratorException e) {
                        e.printStackTrace();
                    }
                }

                public void buildError(AnalyticsGenerationEvent event) {
                    System.out.println("Analytics Generation failed!");
                    for (Throwable t : event.getErrors()) {
                        t.printStackTrace();
                    }

                    try {
                        analytics.shutdown();
                    } catch (AnalyticsGeneratorException e) {
                        e.printStackTrace();
                    }
                }

                public void buildProgress(String progressStatus) {
                    System.out.println("Analytics progress now: " + progressStatus);
                }

                public void buildWarning(String message) {
                    System.out.println("Warning: " + message);
                }


            };
            if (accession.equals("ALL")) {
                analytics.generateAnalytics(listener);
            } else {
                analytics.generateAnalyticsForExperiment(accession, listener);
            }
        } else {
            // in case we don't run analytics
            try {
                analytics.shutdown();
            } catch (AnalyticsGeneratorException e) {
                e.printStackTrace();
            }
        }
    }

    private static void printUsage(Options options) {
        String usage = "atlas";

        String header = "Atlas Test Workbench";
        StringBuilder footer = new StringBuilder();
        footer.append("\n");
        footer.append("This is an application for interacting with various aspects of the Atlas internal ");
        footer.append("functionality without the overhead of deploying as a full web application.  ");
        footer.append("You can use it as an Atlas 'Workbench'.");

        HelpFormatter helpFormatter = new HelpFormatter();
        helpFormatter.setWidth(80);
        helpFormatter.printHelp(usage, header, options, footer.toString(), true);
    }
}
