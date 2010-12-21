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
import uk.ac.ebi.gxa.loader.datamatrix.DataMatrixFileBuffer;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * This tests the DataMatrixFileBuffer class.  This does not implement TestCase because junit doesn't like running
 * multithreaded tests and the init() method requires it.
 *
 * @author Tony Burdett
 */
public class TestDataMatrixFileBuffer extends TestCase {
    private URL dataMatrixURL;

    protected void setUp() throws Exception {
        dataMatrixURL = this.getClass().getClassLoader().getResource(
                "E-GEOD-3790-processed-data-1627899912.txt");
    }

    protected void tearDown() throws Exception {
        dataMatrixURL = null;
    }


    public void testReadReferenceNames() throws Exception {
        DataMatrixFileBuffer buffer = new DataMatrixFileBuffer(dataMatrixURL, null,
                Arrays.asList("AFFYMETRIX_VALUE,CHPSignal,rma_normalized,gcRMA,signal,value,quantification".toLowerCase().split(","))
        );

        Set<String> refNames = new HashSet<String>();
        BufferedReader reader = new BufferedReader(new InputStreamReader(dataMatrixURL.openStream()));
        String line;
        int lineNumber = 0;
        while ((line = reader.readLine()) != null) {
            lineNumber++;
            if (lineNumber == 1) {
                String[] tokens = line.split("\t");
                refNames.addAll(Arrays.asList(tokens).subList(1, tokens.length));
                break;
            }
        }

        for (String refName : buffer.getReferences()) {
            System.out.println("Next refName = " + refName);
            assertTrue("Ref names " + refName + " was not present in file but is present in the buffer",
                    refNames.contains(refName));
        }
    }
}
