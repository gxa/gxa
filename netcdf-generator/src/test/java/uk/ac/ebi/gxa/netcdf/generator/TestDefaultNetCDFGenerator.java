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

package uk.ac.ebi.gxa.netcdf.generator;

import org.dbunit.dataset.ITable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.gxa.dao.AtlasDAOTestCase;
import uk.ac.ebi.gxa.utils.FileUtil;

import java.io.File;

/**
 * Javadocs go here.
 *
 * @author Junit Generation Plugin for Maven, written by Tony Burdett
 * @date 07-10-2009
 */
public class TestDefaultNetCDFGenerator extends AtlasDAOTestCase {
    private DefaultNetCDFGenerator netCDFGenerator;
    private File repoLocation;

    private final Logger log = LoggerFactory.getLogger(getClass());

    public void setUp() throws Exception {
        super.setUp();

        repoLocation = new File(
                System.getProperty("java.io.tmpdir") + File.separator + "test" + File.separator + "netcdfs");

        netCDFGenerator = new DefaultNetCDFGenerator();
        netCDFGenerator.setAtlasDAO(getAtlasDAO());
        netCDFGenerator.setRepositoryLocation(repoLocation);
    }

    public void tearDown() throws Exception {
        super.tearDown();

        netCDFGenerator.shutdown();

        // delete the repo
        if (repoLocation.exists() && !FileUtil.deleteDirectory(repoLocation)) {
            log.warn("Failed to delete " + repoLocation.getAbsolutePath());
        }
        repoLocation = null;
    }

    public void testStartup() {
        try {
            // start the netcdf generator
            netCDFGenerator.startup();

            // check the net cdf directory is created
            assertTrue("NetCDF repository wasn't created", repoLocation.exists());

            // try repeat startups
            netCDFGenerator.startup();
        }
        catch (NetCDFGeneratorException e) {
            e.printStackTrace();
            fail();
        }
    }

    public void testShutdown() {
        try {
            // shutdown the netcdf generator without starting up
            netCDFGenerator.shutdown();

            // startup
            netCDFGenerator.startup();

            // and now shutdown
            netCDFGenerator.shutdown();

            // nothing to test
        }
        catch (NetCDFGeneratorException e) {
            e.printStackTrace();
            fail();
        }
    }

    public void testGenerateNetCDFs() {
        try {
            netCDFGenerator.startup();
            netCDFGenerator.generateNetCDFs();
        }
        catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }

    public void testGenerateNetCDFsForExperiment() {
        try {
            // get the first experiment from our test dataset
            ITable expts = getDataSet().getTable("A2_EXPERIMENT");

            if (expts.getRowCount() > 0) {
                String exptAccession = expts.getValue(0, "accession").toString();

                netCDFGenerator.startup();
                netCDFGenerator.generateNetCDFsForExperiment(exptAccession);
            }
        }
        catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }
}
