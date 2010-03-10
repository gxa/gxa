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

package uk.ac.ebi.gxa.loader.utils;

import junit.framework.TestCase;
import uk.ac.ebi.arrayexpress2.magetab.exception.ParseException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * This tests the DataMatrixFileBuffer class.  This does not implement TestCase because junit doesn't like running
 * multithreaded tests and the init() method requires it.
 *
 * @author Tony Burdett
 * @date 03-Sep-2009
 */
public class TestDataMatrixFileBuffer extends TestCase {
    private URL dataMatrixURL;
    private String assayRef;
    private int expressionValueColumn = 266;

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
            float[][] evs = buffer.readExpressionValues(assayRef);
            long endTime = System.currentTimeMillis();

            long readOnceTime = endTime - startTime;

            System.out.println("Reading took: " + readOnceTime + "ms.");

            assertTrue("Read zero expression values", evs.length > 0);

            assertSame("Requested exactly one assays-worth of expression values, got " + evs.length + " results",
                       evs.length, 1);

            try {
                Set<Float> expressionValues = new HashSet<Float>();
                BufferedReader reader = new BufferedReader(new InputStreamReader(dataMatrixURL.openStream()));
                String line;
                int lineNumber = 0;
                while ((line = reader.readLine()) != null) {
                    lineNumber++;
                    if (lineNumber > 2) {
                        expressionValues.add(Float.parseFloat(line.split("\t")[expressionValueColumn]));
                    }
                }

                System.out.println("Expecting float values...");
                for (Float f : expressionValues) {
                    System.out.print("" + f + ", ");
                }
                System.out.println();

                for (float[] de_evs : evs) {
                    assertSame("Got wrong number of design element expression values", de_evs.length, 10);

                    for (float ev : de_evs) {
                        System.out.println("Next ev for " + assayRef + " = " + ev);
                        assertTrue("Expression value " + ev + " was not present in file but is present in the buffer",
                                   expressionValues.contains(ev));
                    }
                }
            }
            catch (IOException e) {
                e.printStackTrace();
                fail();
            }
        }
        catch (ParseException e) {
            System.err.println(e.getErrorItem().getComment());
            e.printStackTrace();
            fail();
        }
    }

    public void testReadDesignElementNames() {
        DataMatrixFileBuffer buffer = DataMatrixFileBuffer.getDataMatrixFileBuffer(dataMatrixURL);

        try {
            Set<String> designElements = new HashSet<String>();
            BufferedReader reader = new BufferedReader(new InputStreamReader(dataMatrixURL.openStream()));
            String line;
            int lineNumber = 0;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (lineNumber > 2) {
                    designElements.add(line.split("\t")[0]);
                }
            }

            for (String deName : buffer.readDesignElements()) {
                assertTrue("Design element " + deName + " was not present in file but is present in the buffer",
                           designElements.contains(deName));
            }
        }
        catch (IOException e) {
            e.printStackTrace();
            fail();
        }
    }

    public void testReadReferenceNames() {
        DataMatrixFileBuffer buffer = DataMatrixFileBuffer.getDataMatrixFileBuffer(dataMatrixURL);

        try {
            Set<String> refNames = new HashSet<String>();
            BufferedReader reader = new BufferedReader(new InputStreamReader(dataMatrixURL.openStream()));
            String line;
            int lineNumber = 0;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (lineNumber == 1) {
                    String[] tokens = line.split("\t");
                    for (int i = 1; i< tokens.length; i++) {
                        refNames.add(tokens[i]);
                    }
                    break;
                }
            }

            for (String refName : buffer.readReferences()) {
                System.out.println("Next refName = " + refName);
                assertTrue("Ref names " + refName + " was not present in file but is present in the buffer",
                           refNames.contains(refName));
            }
        }
        catch (IOException e) {
            e.printStackTrace();
            fail();
        }
    }

    public void testRepeatReads() {
        try {
            DataMatrixFileBuffer buffer =
                    DataMatrixFileBuffer.getDataMatrixFileBuffer(dataMatrixURL);

            // repeat reads
            for (int i = 0; i < 10; i++) {
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
