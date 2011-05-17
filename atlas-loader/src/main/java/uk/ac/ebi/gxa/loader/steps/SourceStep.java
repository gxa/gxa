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

package uk.ac.ebi.gxa.loader.steps;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.MAGETABInvestigation;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.sdrf.node.SourceNode;
import uk.ac.ebi.gxa.loader.AtlasLoaderException;
import uk.ac.ebi.gxa.loader.cache.AtlasLoadCache;
import uk.ac.ebi.gxa.loader.cache.AtlasLoadCacheRegistry;
import uk.ac.ebi.gxa.loader.utils.SDRFWritingUtils;
import uk.ac.ebi.microarray.atlas.model.Sample;

/**
 * Experiment loading step that stores source nodes information from
 * SDRF structures into Atlas internal experiment model.
 * Based on the original handlers code by Tony Burdett.
 *
 * @author Nikolay Pultsin
 */
public class SourceStep implements Step {
    private final static Logger log = LoggerFactory.getLogger(SourceStep.class);
    private final MAGETABInvestigation investigation;

    public SourceStep(MAGETABInvestigation investigation) {
        this.investigation = investigation;
    }

    public String displayName() {
        return "Processing source nodes";
    }

    public void run() throws AtlasLoaderException {
        final AtlasLoadCache cache = AtlasLoadCacheRegistry.getRegistry().retrieveAtlasLoadCache(investigation);

        for (SourceNode node : investigation.SDRF.lookupNodes(SourceNode.class)) {
            log.debug("Writing sample from source node '" + node.getNodeName() + "'");

            Sample sample = cache.fetchSample(node.getNodeName());
            if (sample == null) {
                // create a new sample and add it to the cache
                sample = new Sample();
                sample.setAccession(node.getNodeName());
                cache.addSample(sample);
            }

            // write the characteristic values as properties
            SDRFWritingUtils.writeSampleProperties(sample, node);
        }
    }
}
