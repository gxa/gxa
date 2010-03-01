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
import uk.ac.ebi.arrayexpress2.magetab.exception.ParseException;
import uk.ac.ebi.gxa.loader.utils.DataMatrixFileBuffer;

import java.net.URL;
import java.util.Map;

/**
 * This tests the DataMatrixFileBuffer class.  This does not implement TestCase
 * because junit doesn't like running multithreaded tests and the init() method
 * requires it.
 *
 * @author Tony Burdett
 * @date 03-Sep-2009
 */
public class TestDataMatrixFileBuffer extends TestCase {
  private URL dataMatrixURL;
  private String assayRef;

  protected void setUp() throws Exception {
    dataMatrixURL = this.getClass().getClassLoader().getResource(
        "E-GEOD-3790-processed-data-1627899912.txt");
    assayRef = "HC52 CN B";
  }

  protected void tearDown() throws Exception {
    dataMatrixURL = null;
    assayRef = null;
  }

  public void testReadAssayExpressionValues() {
    try {
      DataMatrixFileBuffer buffer =
          DataMatrixFileBuffer.getDataMatrixFileBuffer(dataMatrixURL);

      long startTime = System.currentTimeMillis();
      Map<String, Map<String, Float>> evs =
          buffer.readExpressionValues(assayRef);
      long endTime = System.currentTimeMillis();

      long readOnceTime = endTime - startTime;

      System.out.println("Reading took: " + readOnceTime + "ms.");

//      System.out.println("First 100 expression values read...");
//      int i = 0;
//      for (ExpressionValue ev : evs) {
//        System.out
//            .println(ev.getDesignElementAccession() + ":" + ev.getValue());
//        i++;
//        if (i > 100) {
//          break;
//        }
//      }

      assertTrue("Read zero expression values", evs.values().size() > 0);
    }
    catch (ParseException e) {
      System.err.println(e.getErrorItem().getComment());
      e.printStackTrace();
      fail();
    }
  }

  public void testRepeatReads() {
    try {
      DataMatrixFileBuffer buffer =
          DataMatrixFileBuffer.getDataMatrixFileBuffer(dataMatrixURL);

      // repeat reads
      for (int i = 0; i < 100; i++) {
        long startTime = System.currentTimeMillis();
        buffer.readExpressionValues(assayRef);
        long endTime = System.currentTimeMillis();

        long repeatTime = endTime - startTime;

        System.out.println(
            "Repeat read number " + i + " took: " + repeatTime + "ms.");
        assertTrue(
            "Repeat read number " + i + " " +
                "took longer than 5ms (" + repeatTime + "ms), " +
                "just to return reference?",
            repeatTime < 5);
      }
    }
    catch (ParseException e) {
      System.err.println(e.getErrorItem().getComment());
      e.printStackTrace();
      fail();
    }
  }

  public void testParseHeaders() {
    // private method, tested implicitly by test reads
  }
}
