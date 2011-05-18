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

package uk.ac.ebi.gxa.loader.cache;

import junit.framework.TestCase;
import uk.ac.ebi.microarray.atlas.model.Assay;
import uk.ac.ebi.microarray.atlas.model.Experiment;
import uk.ac.ebi.microarray.atlas.model.Sample;

public class TestAtlasLoadCache extends TestCase {
    private AtlasLoadCache cache;

    public void setUp() {
        cache = new AtlasLoadCache();
    }

    public void tearDown() {
        cache = null;
    }

    public void testAddThenFetchAssay() {
        cache.setExperiment(new Experiment("TEST-EXPERIMENT"));

        String accession = "TEST-ASSAY";

        // create an assay
        Assay a = new Assay(accession);

        // add to cache
        cache.addAssay(a);

        // check cache now contains 1 assay with matching accession
        assertEquals("Cache has wrong number of assays",
                cache.fetchAllAssays().size(), 1);
        for (Assay fetched : cache.fetchAllAssays()) {
            assertEquals("Assay has wrong accession", fetched.getAccession(),
                    accession);
        }

        assertNotNull("Can't fetch assay by accession",
                cache.fetchAssay(accession));
    }

    public void testAddThenFetchSample() {
        String accession = "TEST-SAMPLE";

        // create an sample
        Sample s = new Sample();
        s.setAccession(accession);

        // add to cache
        cache.addSample(s);

        // check cache now contains 1 sample with matching accession
        assertEquals("Cache has wrong number of samples",
                cache.fetchAllSamples().size(), 1);
        for (Sample fetched : cache.fetchAllSamples()) {
            assertEquals("Sample has wrong accession", fetched.getAccession(),
                    accession);
        }

        assertNotNull("Can't fetch sample by accession",
                cache.fetchSample(accession));
    }

    public void testAddThenFetchExperiment() {
        String accession = "TEST-EXPERIMENT";

        // add to cache
        cache.setExperiment(new Experiment(accession));

        // parsing finished, look in our cache...
        assertNotNull("Local cache doesn't contain an experiment", cache.fetchExperiment());
        Experiment fetched = cache.fetchExperiment();
        assertEquals("Experiment has wrong accession", fetched.getAccession(),
                accession);

        assertNotNull("Can't fetch experiment by accession",
                cache.fetchExperiment(accession));
    }

    public void testClear() {
        // add some objects
        cache.setExperiment(new Experiment("TEST-EXPERIMENT"));
        cache.addAssay(new Assay("TEST-ASSAY"));
        cache.addSample(new Sample("TEST-SAMPLE"));

        // now clear
        cache.clear();

        assertEquals("Too many assays", cache.fetchAllAssays().size(), 0);
        assertEquals("Too many samples", cache.fetchAllSamples().size(), 0);
        // parsing finished, look in our cache...
        assertNull("Cache contains an experiment", cache.fetchExperiment());
    }
}
