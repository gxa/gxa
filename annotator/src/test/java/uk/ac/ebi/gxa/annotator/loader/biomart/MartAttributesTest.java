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

package uk.ac.ebi.gxa.annotator.loader.biomart;

import org.junit.Test;

import java.util.Collection;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

/**
 * User: nsklyar
 * Date: 20/01/2012
 */
public class MartAttributesTest {
    @Test
    public void testParseAttributes() throws Exception {
        final Collection<String> result = MartAttributes.parseAttributes(MartAttributesTest.class.getResource("attributes.txt").openStream());
        assertEquals(4, result.size());
        assertTrue(result.contains("ensembl_transcript_id"));
        assertTrue(result.contains("ensembl_gene_id"));
        assertTrue(result.contains("ensembl_peptide_id"));
        assertTrue(result.contains("description"));
    }

    @Test
    public void testParse() throws Exception {
        final Collection<String> result = MartAttributes.parseDataSets(MartAttributesTest.class.getResource("datasets.txt").openStream());
        assertEquals(4, result.size());
        assertTrue(result.contains("oanatinus_gene_ensembl"));
        assertTrue(result.contains("tguttata_gene_ensembl"));
        assertTrue(result.contains("cporcellus_gene_ensembl"));
        assertTrue(result.contains("gaculeatus_gene_ensembl"));
    }
}
