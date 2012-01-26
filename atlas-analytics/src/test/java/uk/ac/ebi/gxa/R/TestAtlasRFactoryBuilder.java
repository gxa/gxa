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

package uk.ac.ebi.gxa.R;

import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.fail;

/**
 * @author Tony Burdett
 */
public class TestAtlasRFactoryBuilder {

    @Ignore("TODO: R requires LD_LIBRARY_PATH see: http://www.rforge.net/JRI/")
    public void testGetLocalRFactory() throws InstantiationException, AtlasRServicesException {
        AtlasRFactory rFactory = AtlasRFactoryBuilder.getAtlasRFactoryBuilder().buildAtlasRFactory(RType.LOCAL);
        if (rFactory.validateEnvironment()) {
            rFactory.releaseResources();
            return;
        }
        fail("Unable to validate local R environment. See logs for details");
    }

    @Test
    public void testGetBiocepRFactory() throws InstantiationException, AtlasRServicesException {
        AtlasRFactory rFactory = AtlasRFactoryBuilder.getAtlasRFactoryBuilder().buildAtlasRFactory(RType.BIOCEP);
        if (rFactory.validateEnvironment()) {
            rFactory.releaseResources();
            return;
        }
        fail("Unable to validate biocep R environment. See logs for details");
    }

}
