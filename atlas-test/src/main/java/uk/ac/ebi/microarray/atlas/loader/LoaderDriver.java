package uk.ac.ebi.microarray.atlas.loader;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import uk.ac.ebi.microarray.atlas.netcdf.NetCDFGenerator;
import uk.ac.ebi.microarray.atlas.netcdf.NetCDFGeneratorException;
import uk.ac.ebi.microarray.atlas.netcdf.listener.NetCDFGenerationEvent;
import uk.ac.ebi.microarray.atlas.netcdf.listener.NetCDFGeneratorListener;

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

    long start, end;
    String total;

    // run the loader
//    URL url = new URL(
//        "file:///home/tburdett/Documents/MAGE-TAB/E-GEOD-3790/E-GEOD-3790.idf.txt");
//    AtlasMAGETABLoader loader = (AtlasMAGETABLoader)factory.getBean("atlasLoader");
//    start = System.currentTimeMillis();
//    boolean success = loader.load(url);
//    end = System.currentTimeMillis();
//    total = new DecimalFormat("#.##").format((end - start) / 1000);
//    System.out.println("Load ok? " + success + ".  Total load time = " + total + "s.");

    // run the index builder
//    final IndexBuilder builder =
//        (IndexBuilder) factory.getBean("indexBuilder");
//    start = System.currentTimeMillis();
//    builder.buildIndex(new IndexBuilderListener() {
//
//      public void buildSuccess(IndexBuilderEvent event) {
//        System.out.println("Index built successfully!");
//        try {
//          builder.shutdown();
//        }
//        catch (IndexBuilderException e) {
//          e.printStackTrace();
//        }
//      }
//
//      public void buildError(IndexBuilderEvent event) {
//        System.out.println("Index failed to build");
//        for (Throwable t : event.getErrors()) {
//          t.printStackTrace();
//          try {
//            builder.shutdown();
//          }
//          catch (IndexBuilderException e) {
//            e.printStackTrace();
//          }
//        }
//      }
//    });
//    end = System.currentTimeMillis();
//
//    total = new DecimalFormat("#.##").format((end - start) / 1000);
//    System.out.println("Building index started after " + total + "s.");

    // run the NetCDFGenerator
    final NetCDFGenerator generator =
        (NetCDFGenerator) factory.getBean("netcdfGenerator");
    start = System.currentTimeMillis();
    generator.generateNetCDFsForExperiment("E-MEXP-405", new NetCDFGeneratorListener() {
//    generator.generateNetCDFs(new NetCDFGeneratorListener() {

      public void buildSuccess(NetCDFGenerationEvent event) {
        System.out.println("NetCDF generation completed successfully!");
        try {
          generator.shutdown();
        }
        catch (NetCDFGeneratorException e) {
          e.printStackTrace();
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
    end = System.currentTimeMillis();

    total = new DecimalFormat("#.##").format((end - start) / 1000);
    System.out.println("Building NetCDFs started after " + total + "s.");
  }
}
