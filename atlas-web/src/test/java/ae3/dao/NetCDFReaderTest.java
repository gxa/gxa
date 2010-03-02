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

package ae3.dao;

import org.junit.Test;
import static org.junit.Assert.assertNotNull;
import ae3.model.ExperimentalData;
import ae3.model.Assay;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNull;

/**
 * @author pashky
 */
public class NetCDFReaderTest {
    @Test
    public void testLoadExperiment() throws IOException, URISyntaxException {

        ExperimentalData expData = NetCDFReader.loadExperiment(getTestNCPath(), 645932669);
        assertNotNull(expData);
        assertEquals(1, expData.getArrayDesigns().size());

        assertEquals(0, expData.getExpressionsForGene(123456).size());
        assertEquals(expData.getAssays().size(), expData.getExpressionsForGene(281616865).size());
    }

    @Test
    public void testMultiArrayDesign() throws IOException, URISyntaxException {

        ExperimentalData expData = NetCDFReader.loadExperiment(getTestNCPath(), 824359618);
        assertNotNull(expData);
        assertEquals(2, expData.getArrayDesigns().size());
        
        assertEquals(0, expData.getExpressionsForGene(123456).size());
        assertTrue(expData.getAssays().size() > expData.getExpressionsForGene(169991224).size());
        assertTrue(expData.getAssays().size() > expData.getExpressionsForGene(175824562).size());
    }

    private String getTestNCPath() throws URISyntaxException {
        // won't work for JARs, networks and stuff, but so far so good...
        return new File(getClass().getClassLoader().getResource("dummy.txt").toURI()).getParentFile().getAbsolutePath();
    }
}
