package uk.ac.ebi.microarray.atlas.loader;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;

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
    BeanFactory factory = new ClassPathXmlApplicationContext("loaderContext.xml");

    // get the url to our file to load
    try {
//      URL url = new URL("file:///home/tburdett/Documents/MAGE-TAB/E-GEOD-3790/E-GEOD-3790.idf.txt");
      URL url = new URL("file:///home/tburdett/Documents/MAGE-TAB/E-PFIZ-1/E-PFIZ-1.idf.txt");

      // run the loader
      AtlasMAGETABLoader loader = (AtlasMAGETABLoader)factory.getBean("atlasLoader");

      long start = System.currentTimeMillis();
      boolean success = loader.load(url);
      long end = System.currentTimeMillis();

      String total = new DecimalFormat("#.##").format((end-start)/1000);

      System.out.println("Load ok? " + success + ".  Total load time = " + total + "s.");
    }
    catch (MalformedURLException e) {
      System.err.println("Failed to load- invalid URL");
      e.printStackTrace();
    }
  }
}
