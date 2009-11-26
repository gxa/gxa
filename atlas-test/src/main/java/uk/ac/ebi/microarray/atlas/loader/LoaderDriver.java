package uk.ac.ebi.microarray.atlas.loader;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import uk.ac.ebi.gxa.index.builder.IndexBuilder;
import uk.ac.ebi.gxa.index.builder.IndexBuilderException;
import uk.ac.ebi.gxa.index.builder.listener.IndexBuilderEvent;
import uk.ac.ebi.gxa.index.builder.listener.IndexBuilderListener;
import uk.ac.ebi.gxa.loader.AtlasMAGETABLoader;
import uk.ac.ebi.gxa.netcdf.generator.NetCDFGenerator;
import uk.ac.ebi.gxa.netcdf.generator.NetCDFGeneratorException;

import java.text.DecimalFormat;

/**
 * Javadocs go here!
 *
 * @author Tony Burdett
 * @date 09-Sep-2009
 */
public class LoaderDriver {
    public static void main(String[] args) {
        // load spring config
        BeanFactory factory =
                new ClassPathXmlApplicationContext("loaderContext.xml");

        // loader
        final AtlasMAGETABLoader loader = (AtlasMAGETABLoader) factory.getBean("atlasLoader");
        // index
        final IndexBuilder builder =
                (IndexBuilder) factory.getBean("indexBuilder");
        // including genes?
        System.out.println("Include genes: " + builder.getIncludeGenes());
        // netcdfs
        final NetCDFGenerator generator =
                (NetCDFGenerator) factory.getBean("netcdfGenerator");


//        // run the loader
//        try {
//            URL url = new URL("file:///home/tburdett/Documents/MAGE-TAB/E-GEOD-3790/E-GEOD-3790.idf.txt");
//            final long loadStart = System.currentTimeMillis();
//            boolean success = loader.load(url);
//            final long loadEnd = System.currentTimeMillis();
//            String total = new DecimalFormat("#.##").format((loadEnd - loadStart) / 1000);
//            System.out.println("Load ok? " + success + ".  Total load time = " + total + "s.");
//        }
//        catch (MalformedURLException e) {
//            e.printStackTrace();
//            System.out.println("Load failed - inaccessible URL");
//        }

        // run the index builder
        final long indexStart = System.currentTimeMillis();
        builder.updateIndex(new IndexBuilderListener() {

            public void buildSuccess(IndexBuilderEvent event) {
                final long indexEnd = System.currentTimeMillis();

                String total = new DecimalFormat("#.##").format(
                        (indexEnd - indexStart) / 60000);
                System.out.println(
                        "Index built successfully in " + total + " mins.");

                try {
                    builder.shutdown();
                }
                catch (IndexBuilderException e) {
                    e.printStackTrace();
                }
            }

            public void buildError(IndexBuilderEvent event) {
                System.out.println("Index failed to build");
                for (Throwable t : event.getErrors()) {
                    t.printStackTrace();
                    try {
                        builder.shutdown();
                    }
                    catch (IndexBuilderException e) {
                        e.printStackTrace();
                    }
                }
            }
        });

//        // in case we don't run indexbuilder
//        try {
//            builder.shutdown();
//        }
//        catch (IndexBuilderException e) {
//            e.printStackTrace();
//        }
//
//        // run the NetCDFGenerator
//        final long netStart = System.currentTimeMillis();
//        generator.generateNetCDFsForExperiment(
//                "E-GEOD-1725",
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
//                            try {
//                                generator.shutdown();
//                            }
//                            catch (NetCDFGeneratorException e) {
//                                e.printStackTrace();
//                            }
//                        }
//                    }
//                });

        // in case we don't run netCDF generator
        try {
            generator.shutdown();
        }
        catch (NetCDFGeneratorException e) {
            e.printStackTrace();
        }
    }
}
