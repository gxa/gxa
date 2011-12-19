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

package uk.ac.ebi.gxa.annotator.loader.biomart;

import junit.framework.TestCase;
import org.junit.Test;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import static java.util.Arrays.asList;

/**
 * User: nsklyar
 * Date: 05/05/2011
 */
public class BioMartConnectionTest extends TestCase {

    private BioMartConnection bmService;

    @Test
    public void testGetDataSetVersion() throws Exception {
        bmService = new BioMartConnection("http://plants.ensembl.org/biomart/martservice?", "plants", "athaliana_eg_gene");
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
        bmService = new BioMartConnection("http://plants.ensembl.org/biomart/martservice?", "plants", "athaliana_eg_gene");
        boolean isValid = bmService.isValidDataSetName();
        assertTrue(isValid);

        bmService = new BioMartConnection("http://plants.ensembl.org/biomart/martservice?", "plants", "wrong_name");
        boolean isValid2 = bmService.isValidDataSetName();
        assertFalse(isValid2);
    }

    @Test
    public void testValidateAttributeNames() throws Exception {
        bmService = new BioMartConnection("http://plants.ensembl.org/biomart/martservice?", "plants", "athaliana_eg_gene");
        Set<String> attributes = new HashSet<String>();
        attributes.add("ddd");
        attributes.add("name_1006");

        Collection<String> missing = bmService.validateAttributeNames(attributes);

        assertEquals(1, missing.size());
        assertTrue(missing.contains("ddd"));

    }

    @Test
    public void testFetchInfoFromRegistry() throws Exception {
        bmService = new BioMartConnection("http://plants.ensembl.org/biomart/martservice?", "plants", "athaliana_eg_gene");
        bmService.connect();
        assertNotNull(bmService.getBioMartName());
        assertNotNull(bmService.getServerVirtualSchema());
    }

    @Test
    public void testPrepareAttributesString() throws Exception {
        bmService = new BioMartConnection("http://plants.ensembl.org/biomart/martservice?", "plants", "athaliana_eg_gene");
        final String attStr = bmService.prepareAttributesString(asList("ensembl_gene_id", "ensembl_transcript_id", "external_gene_id"));
        assertEquals("<Attribute name = \"ensembl_gene_id\" /><Attribute name = \"ensembl_transcript_id\" /><Attribute name = \"external_gene_id\" />", attStr);
    }

    @Test
    public void testPrepareURLString() throws Exception {
        bmService = new BioMartConnection("http://plants.ensembl.org/biomart/martservice?", "plants", "athaliana_eg_gene");
        final String attStr = bmService.prepareAttributesString(asList("gene", "transcript"));
        assertEquals("<Attribute name = \"gene\" /><Attribute name = \"transcript\" />", attStr);
        final String urlStr = bmService.prepareURLString(attStr, "ens_plants", "athaliana_eg_gene");
        assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?><!DOCTYPE Query><Query  virtualSchemaName = \"ens_plants\" formatter = \"TSV\" header = \"0\" uniqueRows = \"1\" count = \"\" ><Dataset name = \"athaliana_eg_gene\" interface = \"default\" ><Attribute name = \"gene\" /><Attribute name = \"transcript\" /></Dataset></Query>", urlStr);
    }

}
