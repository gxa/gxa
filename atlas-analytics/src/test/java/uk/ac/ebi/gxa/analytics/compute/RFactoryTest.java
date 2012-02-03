/*
 * Copyright 2008-2012 Microarray Informatics Team, EMBL-European Bioinformatics Institute
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

package uk.ac.ebi.gxa.analytics.compute;

import org.junit.*;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.gxa.R.AtlasRFactory;
import uk.ac.ebi.gxa.R.AtlasRFactoryBuilder;
import uk.ac.ebi.gxa.R.AtlasRServicesException;
import uk.ac.ebi.gxa.R.RType;
import uk.ac.ebi.rcloud.server.RServices;
import uk.ac.ebi.rcloud.server.RType.RNumeric;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Olga Melnichuk
 */
@RunWith(Parameterized.class)
public class RFactoryTest {

    private static final Logger log = LoggerFactory.getLogger(RFactoryTest.class);
    
    private static AtlasRFactory rFactory;
    private final RType rType;

    public RFactoryTest(RType rType) {
        this.rType = rType;
    }

    @Parameterized.Parameters
    public static List<RType[]> parameters() {
        return asList(new RType[] {RType.LOCAL},  new RType[]{RType.BIOCEP});
    }

    @Before
    public void createRFactory() throws InstantiationException {
        AtlasRFactory rf = AtlasRFactoryBuilder.getAtlasRFactoryBuilder().buildAtlasRFactory(rType); 
        assertTrue(rf.validateEnvironment());
        rFactory = rf;
    }

    @After
    public void clearRFactory() {
        if (rFactory != null) {
            rFactory.releaseResources();
            rFactory = null;
        }
    }
    
    @Test
    public void testMultipleRServices() {
        final int n = 4;
        List<RServices> services = new ArrayList<RServices>();
        for (int i = 0; i < n; i++) {
            try {
                services.add(rFactory.createRServices());
            } catch (AtlasRServicesException e) {
                log.error("Can not create RServices", e);
            }
        }
        assertEquals(n, services.size());
        recycleRServices(services);
    }

    @Test
    public void testComputeTask() {
        final AtlasComputeService computeService = new AtlasComputeService();
        computeService.setAtlasRFactory(rFactory);

        ComputeTask<RNumeric> task = new ComputeTask<RNumeric>() {
            public RNumeric compute(RServices R) throws RemoteException {
                return (RNumeric) R.getObject("1 + 3");
            }
        };

        RNumeric i = computeService.computeTask(task);
        assertEquals(i.getValue()[0], 4.0f, 1e-10f);
    }

    private void recycleRServices(List<RServices> services) {
        for (RServices rServices : services) {
            try {
                rFactory.recycleRServices(rServices);
            } catch (AtlasRServicesException e) {
                e.printStackTrace();
            }
        }
        services.clear();
    }
}
