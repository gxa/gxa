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

package uk.ac.ebi.gxa.annotator.model.connection;

import org.junit.Test;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import static java.util.Arrays.asList;
import static org.junit.Assert.*;

/**
 * User: nsklyar
 * Date: 04/01/2012
 */
public class BioMartConnectionTest{

    private BioMartConnection bmService;

    @Test
    public void testGetDataSetVersion() throws Exception {
        bmService = BioMartConnection.createConnection("http://plants.ensembl.org/biomart/martservice?", "plants", "athaliana_eg_gene");
        String version = bmService.getOnlineSoftwareVersion();
        boolean correctVersion = true;
        try {
            Integer.parseInt(version);
        } catch (NumberFormatException e) {
            correctVersion = false;
        }
        assertTrue("Dataset BioMart version is not correct", correctVersion);
    }

    @Test
    public void testValidateOrganismName() throws Exception {
        bmService = BioMartConnection.createConnection("http://plants.ensembl.org/biomart/martservice?", "plants", "athaliana_eg_gene");
        boolean isValid = bmService.isValidDataSetName();
        assertTrue(isValid);

        bmService = BioMartConnection.createConnection("http://plants.ensembl.org/biomart/martservice?", "plants", "wrong_name");
        boolean isValid2 = bmService.isValidDataSetName();
        assertFalse(isValid2);
    }

    @Test
    public void testValidateAttributeNames() throws Exception {
        bmService = BioMartConnection.createConnection("http://plants.ensembl.org/biomart/martservice?", "plants", "athaliana_eg_gene");
        Set<String> attributes = new HashSet<String>();
        attributes.add("ddd");
        attributes.add("name_1006");

        Collection<String> missing = bmService.validateAttributeNames(attributes);

        assertEquals(1, missing.size());
        assertTrue(missing.contains("ddd"));

    }

    @Test
    public void testFetchInfoFromRegistry() throws Exception {
        bmService = BioMartConnection.createConnection("http://plants.ensembl.org/biomart/martservice?", "plants", "athaliana_eg_gene")  ;
        assertNotNull(bmService.getBioMartName());
        assertNotNull(bmService.getServerVirtualSchema());
    }
}

