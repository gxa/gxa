package uk.ac.ebi.gxa.loader;

import org.apache.solr.core.CoreContainer;
import org.slf4j.bridge.SLF4JBridgeHandler;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import uk.ac.ebi.gxa.analytics.generator.AnalyticsGenerator;
import uk.ac.ebi.gxa.analytics.generator.AnalyticsGeneratorException;
import uk.ac.ebi.gxa.index.builder.IndexBuilder;
import uk.ac.ebi.gxa.index.builder.IndexBuilderException;
import uk.ac.ebi.gxa.loader.listener.AtlasLoaderEvent;
import uk.ac.ebi.gxa.loader.listener.AtlasLoaderListener;
import uk.ac.ebi.gxa.netcdf.generator.NetCDFGenerator;
import uk.ac.ebi.gxa.netcdf.generator.NetCDFGeneratorException;
import uk.ac.ebi.gxa.netcdf.generator.listener.NetCDFGenerationEvent;
import uk.ac.ebi.gxa.netcdf.generator.listener.NetCDFGeneratorListener;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.logging.LogManager;

/**
 * Javadocs go here!
 *
 * @author Tony Burdett
 * @date 09-Sep-2009
 */
public class LoaderDriver {
    public static void main(String[] args) {
        // configure logging
        try {
            LogManager.getLogManager()
                    .readConfiguration(LoaderDriver.class.getClassLoader().getResourceAsStream("logging.properties"));
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        SLF4JBridgeHandler.install();

        // load spring config
        BeanFactory factory =
                new ClassPathXmlApplicationContext("loaderContext.xml");

        // loader
        final AtlasLoader loader = (AtlasLoader) factory.getBean("atlasLoader");
        // index
        final IndexBuilder builder = (IndexBuilder) factory.getBean("indexBuilder");
        // netcdfs
        final NetCDFGenerator generator = (NetCDFGenerator) factory.getBean("netcdfGenerator");
        // analytics
        final AnalyticsGenerator analytics = (AnalyticsGenerator) factory.getBean("analyticsGenerator");
        // solrIndex
        final CoreContainer solrContainer = (CoreContainer) factory.getBean("solrContainer");

        // run the loader
        try {
            final URL url = URI.create("file:///home/tburdett/Documents/MAGE-TAB/E-PFIZ-2/E-PFIZ-2.idf.txt").toURL();
            final long indexStart = System.currentTimeMillis();
            loader.loadExperiment(url, new AtlasLoaderListener() {

                public void loadSuccess(AtlasLoaderEvent event) {
                    final long indexEnd = System.currentTimeMillis();

                    String total = new DecimalFormat("#.##").format(
                            (indexEnd - indexStart) / 60000);
                    System.out.println(
                            "Load completed successfully in " + total + " mins.");

                    try {
                        loader.shutdown();
                    }
                    catch (AtlasLoaderException e) {
                        e.printStackTrace();
                    }
                }

                public void loadError(AtlasLoaderEvent event) {
                    System.out.println("Load failed");
                    for (Throwable t : event.getErrors()) {
                        t.printStackTrace();
                    }

                    try {
                        loader.shutdown();
                    }
                    catch (AtlasLoaderException e) {
                        e.printStackTrace();
                    }
                }
            });
        }
        catch (MalformedURLException e) {
            e.printStackTrace();
            System.out.println("Load failed - inaccessible URL");
        }

        // in case we don't run loader
//        try {
//            loader.shutdown();
//        }
//        catch (AtlasLoaderException e) {
//            e.printStackTrace();
//        }

        // run the index builder
//        final long indexStart = System.currentTimeMillis();
//        builder.buildIndex(new IndexBuilderListener() {
//
//            public void buildSuccess(IndexBuilderEvent event) {
//                final long indexEnd = System.currentTimeMillis();
//
//                String total = new DecimalFormat("#.##").format(
//                        (indexEnd - indexStart) / 60000);
//                System.out.println(
//                        "Index built successfully in " + total + " mins.");
//
//                try {
//                    builder.shutdown();
//                    solrContainer.shutdown();
//                }
//                catch (IndexBuilderException e) {
//                    e.printStackTrace();
//                }
//            }
//
//            public void buildError(IndexBuilderEvent event) {
//                System.out.println("Index failed to build");
//                for (Throwable t : event.getErrors()) {
//                    t.printStackTrace();
//                }
//
//
//                try {
//                    builder.shutdown();
//                    solrContainer.shutdown();
//                }
//                catch (IndexBuilderException e) {
//                    e.printStackTrace();
//                }
//            }
//        });

        // in case we don't run index
        try {
            builder.shutdown();
            solrContainer.shutdown();
        }
        catch (IndexBuilderException e) {
            e.printStackTrace();
        }

        // run the NetCDFGenerator
//        final long netStart = System.currentTimeMillis();
//        generator.generateNetCDFs(
//                new NetCDFGeneratorListener() {
//                    public void buildSuccess(NetCDFGenerationEvent event) {
//                        final long netEnd = System.currentTimeMillis();
//
//                        String total = new DecimalFormat("#.##").format(
//                                (netEnd - netStart) / 60000);
//                        System.out.println(
//                                "NetCDFs generated successfully in " + total + " mins.");
//
//                        try {
//                            generator.shutdown();
//                        }
//                        catch (NetCDFGeneratorException e) {
//                            e.printStackTrace();
//                        }
//                    }
//
//                    public void buildError(NetCDFGenerationEvent event) {
//                        System.out.println("NetCDF Generation failed!");
//                        for (Throwable t : event.getErrors()) {
//                            t.printStackTrace();
//                        }
//
//                        try {
//                            generator.shutdown();
//                        }
//                        catch (NetCDFGeneratorException e) {
//                            e.printStackTrace();
//                        }
//                    }
//                });

        // iteratively invoke netcdf generator
//        iterativelyInvokeNetCDFs(generator, 0, 5);

        // in case we don't run netCDF generator
        try {
            generator.shutdown();
        }
        catch (NetCDFGeneratorException e) {
            e.printStackTrace();
        }

        // run the analytics
//        final long netStart = System.currentTimeMillis();
//        analytics.generateAnalytics(
//                new AnalyticsGeneratorListener() {
//                    public void buildSuccess(AnalyticsGenerationEvent event) {
//                        final long netEnd = System.currentTimeMillis();
//
//                        String total = new DecimalFormat("#.##").format(
//                                (netEnd - netStart) / 60000);
//                        System.out.println(
//                                "Analytics generated successfully in " + total + " mins.");
//
//                        try {
//                            analytics.shutdown();
//                        }
//                        catch (AnalyticsGeneratorException e) {
//                            e.printStackTrace();
//                        }
//                    }
//
//                    public void buildError(AnalyticsGenerationEvent event) {
//                        System.out.println("Analytics Generation failed!");
//                        for (Throwable t : event.getErrors()) {
//                            t.printStackTrace();
//                        }
//
//                        try {
//                            analytics.shutdown();
//                        }
//                        catch (AnalyticsGeneratorException e) {
//                            e.printStackTrace();
//                        }
//                    }
//                });

        // in case we don't run analytics
        try {
            analytics.shutdown();
        }
        catch (AnalyticsGeneratorException e) {
            e.printStackTrace();
        }
    }

    private static void iterativelyInvokeNetCDFs(final NetCDFGenerator generator,
                                                 final int iteration,
                                                 final int maxTimes) {
        System.out.println("Invoking generator, iteration " + iteration);

        // run the NetCDFGenerator
        final long netStart = System.currentTimeMillis();
        generator.generateNetCDFsForExperiment(
                "E-TABM-199",
                new NetCDFGeneratorListener() {
                    public void buildSuccess(NetCDFGenerationEvent event) {
                        int it = iteration + 1;
                        final long netEnd = System.currentTimeMillis();

                        String total = new DecimalFormat("#.##").format(
                                (netEnd - netStart) / 60000);
                        System.out.println(
                                "NetCDFs generated successfully in " + total + " mins.");

                        if (it <= maxTimes) {
                            iterativelyInvokeNetCDFs(generator, it, maxTimes);
                        }
                        else {
                            try {
                                generator.shutdown();
                            }
                            catch (NetCDFGeneratorException e) {
                                e.printStackTrace();
                            }
                        }
                    }

                    public void buildError(NetCDFGenerationEvent event) {
                        System.out.println("NetCDF Generation failed!");
                        for (Throwable t : event.getErrors()) {
                            t.printStackTrace();
                            try {
                                generator.shutdown();
                            }
                            catch (NetCDFGeneratorException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                });
    }
}
