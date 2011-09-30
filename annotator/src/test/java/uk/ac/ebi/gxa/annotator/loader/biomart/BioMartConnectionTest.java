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

import java.net.URL;
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
        String version = bmService.getOnlineMartVersion();
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
    public void testGetPropertyForOrganismURL() throws Exception {
        bmService = new BioMartConnection("http://plants.ensembl.org/biomart/martservice?", "plants", "athaliana_eg_gene");

        URL attributesURL = bmService.getAttributesURL(asList("ensembl_gene_id", "ensembl_transcript_id", "external_gene_id"));
        assertEquals("http://plants.ensembl.org/biomart/martservice?query=%3C%3Fxml+version%3D%221.0%22+encoding%3D%22UTF-8%22%3F%3E%3C%21DOCTYPE+Query%3E%3CQuery++virtualSchemaName+%3D+%22plants_mart_11%22+formatter+%3D+%22TSV%22+header+%3D+%220%22+uniqueRows+%3D+%221%22+count+%3D+%22%22+%3E%3CDataset+name+%3D+%22athaliana_eg_gene%22+interface+%3D+%22default%22+%3E%3CAttribute+name+%3D+%22ensembl_gene_id%22+%2F%3E%3CAttribute+name+%3D+%22ensembl_transcript_id%22+%2F%3E%3CAttribute+name+%3D+%22external_gene_id%22+%2F%3E%3C%2FDataset%3E%3C%2FQuery%3E",
                attributesURL.toString());
    }
}
