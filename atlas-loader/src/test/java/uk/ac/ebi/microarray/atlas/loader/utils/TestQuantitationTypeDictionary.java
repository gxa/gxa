package uk.ac.ebi.microarray.atlas.loader.utils;

import junit.framework.TestCase;
import uk.ac.ebi.arrayexpress2.magetab.utils.MAGETABUtils;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;

/**
 * Javadocs go here.
 *
 * @author Junit Generation Plugin for Maven, written by Tony Burdett
 * @date 07-10-2009
 */
public class TestQuantitationTypeDictionary extends TestCase {
  private List<String> expected;

  public void setUp() {
    // manually load qtTypes from all property files on the classpath
    expected = new ArrayList<String>();
    try {
      Enumeration<URL> resources =
          this.getClass().getClassLoader().getResources(
              "META-INF/magetab/qttypes.properties");
      while (resources.hasMoreElements()) {
        URL url = resources.nextElement();
        Properties props = new Properties();
        props.load(url.openStream());

        for (Object key : props.keySet()) {
          String qtType = MAGETABUtils.digestHeader(key.toString());

          if (!expected.contains(qtType)) {
            expected.add(qtType);
          }
        }
      }
    }
    catch (IOException e) {
      fail();
    }
  }

  public void tearDown() {
    expected = null;
  }

  public void testListQTTypes() {
    // loads everything in qttypes.propertiee
    String[] qtTypes =
        QuantitationTypeDictionary.getQTDictionary().listQTTypes();
    assertEquals("Unexpected number of qt types", qtTypes.length,
                 expected.size());
    for (String qtType : qtTypes) {
      System.out.println("Next type: " + qtType);
      assertTrue("Wrong qt type", expected.contains(qtType));
    }
  }

  public void testLookupTerm() {
    for (String expectedString : expected) {
      assertTrue("Can't find term",
                 QuantitationTypeDictionary.getQTDictionary().lookupTerm(
                     expectedString));
    }
  }
}
