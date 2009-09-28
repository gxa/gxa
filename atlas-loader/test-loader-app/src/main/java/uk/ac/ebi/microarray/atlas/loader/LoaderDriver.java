package uk.ac.ebi.microarray.atlas.loader;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import uk.ac.ebi.ae3.indexbuilder.IndexBuilder;
import uk.ac.ebi.ae3.indexbuilder.IndexBuilderException;
import uk.ac.ebi.ae3.indexbuilder.listener.IndexBuilderEvent;
import uk.ac.ebi.ae3.indexbuilder.listener.IndexBuilderListener;

import java.net.MalformedURLException;
import java.net.URL;
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

    // get the url to our file to load
    try {
      URL url = new URL(
          "file:///home/tburdett/Documents/MAGE-TAB/E-GEOD-3790/E-GEOD-3790.idf.txt");

      // run the loader
//      AtlasMAGETABLoader loader = (AtlasMAGETABLoader)factory.getBean("atlasLoader");
//      long start = System.currentTimeMillis();
//      boolean success = loader.load(url);
//      long end = System.currentTimeMillis();

      // run the index builder
      final IndexBuilder builder =
          (IndexBuilder) factory.getBean("indexBuilder");
      long start = System.currentTimeMillis();
      builder.buildIndex(new IndexBuilderListener() {

        public void buildSuccess(IndexBuilderEvent event) {
          System.out.println("Index built successfully!");
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
      long end = System.currentTimeMillis();

      String total = new DecimalFormat("#.##").format((end - start) / 1000);

//      System.out.println("Load ok? " + success + ".  Total load time = " + total + "s.");
      System.out.println("Building index started after " + total + "s.");
    }
    catch (MalformedURLException e) {
      System.err.println("Failed to load- invalid URL");
      e.printStackTrace();
    }
  }
}
