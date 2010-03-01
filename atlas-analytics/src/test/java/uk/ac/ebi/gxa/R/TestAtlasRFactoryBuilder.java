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
 * http://ostolop.github.com/gxa/
 */

package uk.ac.ebi.gxa.R;

import junit.framework.TestCase;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Javadocs go here!
 *
 * @author Tony Burdett
 * @date 19-Nov-2009
 */
public class TestAtlasRFactoryBuilder extends TestCase {
    private final Logger log = LoggerFactory.getLogger(getClass());

    @Test
    public void testGetLocalRFactory() {
        try {
            AtlasRFactory rFactory = AtlasRFactoryBuilder.getAtlasRFactoryBuilder().buildAtlasRFactory(RType.LOCAL);
            if (!rFactory.validateEnvironment()) {
                // this is a valid result if no $R_HOME set
                if (System.getenv("R_HOME") == null || System.getenv("R_HOME").equals("")) {
                    log.info("No R_HOME set, so environment is not valid: result is correct");
                }
                else {
                    fail("Unable to validate R remote environment");
                }
            }

            rFactory.releaseResources();
        }
        catch (InstantiationException e) {
            e.printStackTrace();
            fail();
        }
        catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }

    @Test
    public void testGetRemoteRFactory() {
        try {
            AtlasRFactory rFactory = AtlasRFactoryBuilder.getAtlasRFactoryBuilder().buildAtlasRFactory(RType.REMOTE);
            if (!rFactory.validateEnvironment()) {
                // this is a valid result if no $R.remote.host set
                if (System.getenv("R.remote.host") == null || System.getenv("R.remote.host").equals("")) {
                    log.info("No R.remote.host set, so environment is not valid: result is correct");
                }
                else {
                    fail("Unable to validate R remote environment");
                }
            }

            rFactory.releaseResources();
        }
        catch (InstantiationException e) {
            e.printStackTrace();
            fail();
        }
        catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }

    @Test
    public void testGetBiocepRFactory() {
        try {
            AtlasRFactory rFactory = AtlasRFactoryBuilder.getAtlasRFactoryBuilder().buildAtlasRFactory(RType.BIOCEP);
            if (!rFactory.validateEnvironment()) {
                // this is a valid result if no biocep properties set - we're just checking one here
                if (System.getProperty("pools.dbmode.name") == null) {
                    log.info("No biocep properties set, so environment is not valid: result is correct");
                }
                else {
                    fail("Unable to validate R remote environment");
                }
            }

            rFactory.releaseResources();
        }
        catch (InstantiationException e) {
            e.printStackTrace();
            fail();
        }
        catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }

}
