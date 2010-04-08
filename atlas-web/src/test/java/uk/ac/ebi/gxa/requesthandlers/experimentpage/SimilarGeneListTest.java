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

package uk.ac.ebi.gxa.requesthandlers.experimentpage;

import junit.framework.TestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import uk.ac.ebi.gxa.R.AtlasRFactory;
import uk.ac.ebi.gxa.R.AtlasRFactoryBuilder;
import uk.ac.ebi.gxa.analytics.compute.AtlasComputeService;
import uk.ac.ebi.gxa.analytics.compute.ComputeException;
import uk.ac.ebi.gxa.analytics.compute.ComputeTask;
import uk.ac.ebi.gxa.requesthandlers.experimentpage.result.SimilarityResultSet;
import uk.ac.ebi.rcloud.server.RServices;
import uk.ac.ebi.rcloud.server.RType.RDataFrame;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.rmi.RemoteException;
import java.util.ArrayList;

/**
 * Javadocs go here!
 *
 * @author Tony Burdett
 * @date 17-Nov-2009
 */
public class SimilarGeneListTest extends TestCase {
    private AtlasComputeService svc;

    @Before
    public void setUp() {
        try {
            // build default rFactory - reads R.properties from classpath
            AtlasRFactory rFactory = AtlasRFactoryBuilder.getAtlasRFactoryBuilder().buildAtlasRFactory();
            // build service
            svc = new AtlasComputeService();
            svc.setAtlasRFactory(rFactory);
        }
        catch (Exception e) {
            e.printStackTrace();
            fail("Caught exception whilst setting up");
        }
    }

    @After
    public void tearDown() {
        svc.shutdown();
    }

    @Test
    public void testComputeSimilarityTask() {
        // do a similarity over E-AFMX-5 for an arbitrary design element/array design
        // TODO: fix similarity result test!
        final SimilarityResultSet simRS = new SimilarityResultSet("226010852", "153094131", "153069949", "/ebi/ArrayExpress-files/NetCDFs.ATLAS");
        final String callSim = "sim.nc(" + simRS.getTargetDesignElementId() + ",'" + simRS.getSourceNetCDF() + "')";

        RDataFrame sim = null;
        try {
            sim = svc.computeTask(new ComputeTask<RDataFrame>() {
                public RDataFrame compute(RServices R) throws RemoteException {
                    try {
                        R.sourceFromBuffer(getRCodeFromResource("sim.R"));
                    } catch (IOException e) {
                        fail("Couldn't read sim.R");
                    }

                    return (RDataFrame) R.getObject(callSim);
                }
            });
        }
        catch (ComputeException e) {
            fail("Failed calling: " + callSim + "\n" + e.getMessage());
            e.printStackTrace();
        }

        if (null != sim) {
            simRS.loadResult(sim);
            ArrayList<String> simGeneIds = simRS.getSimGeneIDs();
            assertEquals(simGeneIds.get(0), "153069988");
        }
        else {
            fail("Similarity search returned null");
        }
    }

    private String getRCodeFromResource(String resourcePath) throws IOException {
        // open a stream to the resource
        InputStream in = getClass().getClassLoader().getResourceAsStream(resourcePath);

        // create a reader to read in code
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));

        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line).append("\n");
        }

        in.close();
        return sb.toString();
    }
}
