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

import junit.framework.TestCase;
import uk.ac.ebi.rcloud.server.RServices;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Tony Burdett
 */
public class TestBiocepAtlasRFactory extends TestCase {
    private AtlasRFactory rFactory;
    private List<RServices> rServicesList;

    public void setUp() throws InstantiationException {
        rServicesList = new ArrayList<RServices>();
        rFactory = AtlasRFactoryBuilder.getAtlasRFactoryBuilder().buildAtlasRFactory(RType.BIOCEP);
    }

    public void tearDown() {
        for (RServices rServices : rServicesList) {
            try {
                rFactory.recycleRServices(rServices);
            }
            catch (AtlasRServicesException e) {
                e.printStackTrace();
            }
        }
        rServicesList.clear();
        rFactory.releaseResources();
    }

    public void testMultipleCreateRServices() throws AtlasRServicesException {
        // test 8 iterations
        for (int i = 0; i < 4; i++) {
            rServicesList.add(rFactory.createRServices());
        }
    }
}
