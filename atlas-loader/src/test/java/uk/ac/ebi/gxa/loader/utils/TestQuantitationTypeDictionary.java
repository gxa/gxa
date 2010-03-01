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
 * http://ostolop.github.com/gxa/
 */

package uk.ac.ebi.gxa.loader.utils;

import junit.framework.TestCase;
import uk.ac.ebi.arrayexpress2.magetab.utils.MAGETABUtils;
import uk.ac.ebi.gxa.loader.utils.QuantitationTypeDictionary;

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
