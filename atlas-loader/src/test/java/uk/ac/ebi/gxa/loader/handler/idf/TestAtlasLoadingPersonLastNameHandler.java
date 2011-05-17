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

package uk.ac.ebi.gxa.loader.handler.idf;

import junit.framework.TestCase;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.MAGETABInvestigation;
import uk.ac.ebi.arrayexpress2.magetab.handler.HandlerPool;
import uk.ac.ebi.gxa.loader.AtlasLoaderException;
import uk.ac.ebi.gxa.loader.cache.AtlasLoadCache;

import java.net.URL;

public class TestAtlasLoadingPersonLastNameHandler extends TestCase {
    private MAGETABInvestigation investigation;
    private AtlasLoadCache cache;

    private URL parseURL;

    public void setUp() {
        // now, create an investigation
        investigation = new MAGETABInvestigation();
        cache = new AtlasLoadCache();

        parseURL = this.getClass().getClassLoader().getResource("E-GEOD-3790.idf.txt");

        HandlerPool pool = HandlerPool.getInstance();
        pool.useDefaultHandlers();
    }

    public void tearDown() throws Exception {
        investigation = null;
        cache = null;
    }

    public void testWriteValues() throws AtlasLoaderException {
        TestAtlasLoadingAccessionHandler.createParser(cache, investigation, parseURL);

        // get the title of the experiment
        String expected = "Lesley Jones Angela Hodges";
        String actual = cache.fetchExperiment().getPerformer();

        assertEquals("Names don't match", expected, actual);
    }
}
