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

package uk.ac.ebi.gxa.analytics.generator;

import junit.framework.TestCase;
import uk.ac.ebi.gxa.netcdf.NetCDFProxy;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class TestExperimentAnalyticsGeneratorService extends TestCase {
    public void testDoublePipeEscape() {
        String uefv = "diseasestate||normal 9";
        String[] values = uefv.split(NetCDFProxy.NCDF_PROP_VAL_SEP_REGEX);
        String ef = values[0];
        if (values.length > 1) {
            String efv = values[1];

            assertEquals("ef is wrong!", "diseasestate", ef);
            assertEquals("efv is wrong!", "normal 9", efv);
        }
    }

    public void testGetRCodeFromResource() throws IOException {
        // open a stream to the resource
        InputStream in = getClass().getClassLoader().getResourceAsStream("R/analytics.R");

        // create a reader to read in code
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));

        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line);
        }
    }
}
