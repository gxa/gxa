/*
 * Copyright 2008-2011 Microarray Informatics Team, EMBL-European Bioinformatics Institute
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

package uk.ac.ebi.gxa.annotator.annotationsrc.arraydesign;

import junit.framework.TestCase;
import org.junit.Test;
import uk.ac.ebi.gxa.annotator.annotationsrc.arraydesign.ArrayExpressConnection;

/**
 * User: nsklyar
 * Date: 02/09/2011
 */
public class ArrayExpressConnectionTest extends TestCase {

    @Test
    public void testFetchArrayDesignData() throws Exception {
        ArrayExpressConnection connection = new ArrayExpressConnection("A-AFFY-2");

        assertEquals("Affymetrix GeneChip Arabidopsis Genome [ATH1-121501]", connection.getName());
        assertEquals("Affymetrix, Inc. (support@affymetrix.com)", connection.getProvider());
        assertEquals("in_situ_oligo_features", connection.getType());

    }

    @Test
    public void testFetchArrayDesignDataFail() throws Exception {
        ArrayExpressConnection connection = new ArrayExpressConnection("A-AFFY-299999");
        assertEquals(ArrayExpressConnection.NONAE_AD_NAME + "A-AFFY-299999", connection.getName());
        assertEquals("", connection.getType());
        assertEquals("", connection.getProvider());
    }

    //ToDo: is it possible to simulate a failed connection?
}
