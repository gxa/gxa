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

package uk.ac.ebi.gxa.compute;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import uk.ac.ebi.gxa.R.AtlasRFactory;
import uk.ac.ebi.gxa.R.AtlasRFactoryBuilder;
import uk.ac.ebi.gxa.analytics.compute.AtlasComputeService;
import uk.ac.ebi.gxa.analytics.compute.ComputeTask;
import uk.ac.ebi.rcloud.server.RServices;
import uk.ac.ebi.rcloud.server.RType.RNumeric;

import java.rmi.RemoteException;

import static org.junit.Assert.assertEquals;


public class AtlasComputeServiceTest {
    private AtlasComputeService svc;

    @Before
    public void setUp() throws InstantiationException {
        // build default rFactory - reads R.properties from classpath
        AtlasRFactory rFactory = AtlasRFactoryBuilder.getAtlasRFactoryBuilder().buildAtlasRFactory();
        // build service
        svc = new AtlasComputeService();
        svc.setAtlasRFactory(rFactory);
    }

    @After
    public void tearDown() {
        svc.shutdown();
    }

    @Test
    public void testComputeTask() {
        ComputeTask<RNumeric> task = new ComputeTask<RNumeric>() {
            public RNumeric compute(RServices R) throws RemoteException {
                return (RNumeric) R.getObject("1 + 3");
            }
        };

        RNumeric i = svc.computeTask(task);
        assertEquals(i.getValue()[0], 4);
    }
}
