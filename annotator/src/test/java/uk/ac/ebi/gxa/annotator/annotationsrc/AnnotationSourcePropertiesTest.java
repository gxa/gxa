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

package uk.ac.ebi.gxa.annotator.annotationsrc;

import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.TreeMultimap;
import org.junit.Test;

import java.util.Collection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


/**
 * User: nsklyar
 * Date: 21/03/2012
 */
public class AnnotationSourcePropertiesTest {

    private AnnotationSourceProperties properties;

    @Test
    public void testInitFromText() throws Exception {
        properties = AnnotationSourceProperties.createPropertiesFromText(fullSrc);

        final String result = properties.serializeToString();
        assertEquals(fullSrc.trim(), result.trim());
    }

    @Test
    public void testGetProperty() throws Exception {
        properties = AnnotationSourceProperties.createPropertiesFromText("databaseName = metazoa\n" +
                "datasetName = agambiae_eg_gene\n");
        assertEquals("metazoa", properties.getProperty("databaseName"));
        assertEquals("agambiae_eg_gene", properties.getProperty("datasetName"));
    }

    @Test
    public void testAddProperty() throws Exception {
        properties = new AnnotationSourceProperties();
        properties.addProperty("databaseName", "metazoa");
        assertEquals("databaseName = metazoa", properties.serializeToString());
    }

    @Test
    public void testAddListPropertiesWithPrefix() throws Exception {
        properties = new AnnotationSourceProperties();
        Multimap<String, String> map = TreeMultimap.create();
        map.put("uniprot", "uniprot_sptrembl");
        map.put("uniprot", "uniprot_swissprot_accession");
        map.put("go", "go_accession");

        properties.addListPropertiesWithPrefix("property", map);
        final String actual = properties.serializeToString();
        final String expected = "property.go = go_accession\n" +
                "property.uniprot = uniprot_sptrembl,uniprot_swissprot_accession";
        assertEquals(expected, actual);
    }

    @Test
    public void testAddListProperties() throws Exception {
        properties = new AnnotationSourceProperties();
        properties.addListProperties("types", Lists.newArrayList("enstranscript", "ensgene"));
        assertEquals("types = enstranscript,ensgene", properties.serializeToString());
    }

    @Test
    public void testGetListPropertiesOfType() throws Exception {
        properties = AnnotationSourceProperties.createPropertiesFromText("types = enstranscript,ensgene\n");
        final Collection<String> types = properties.getListPropertiesOfType("types");
        assertEquals(2, types.size());
        assertTrue(types.contains("enstranscript"));
        assertTrue(types.contains("ensgene"));
    }

    @Test
    public void testGetListPropertiesWithPrefix() throws Exception {
        properties = AnnotationSourceProperties.createPropertiesFromText(
                "property.uniprot = uniprot_sptrembl,uniprot_swissprot_accession\n" +
                        "property.go = go_accession\n" +
                        "arrayDesign.A-AFFY-102 = affy_plasmodium_anopheles");
        final Multimap<String, String> propertyValues = properties.getListPropertiesWithPrefix("property");
        assertTrue(propertyValues.keySet().contains("uniprot"));
        assertTrue(propertyValues.keySet().contains("go"));

        assertEquals(2, propertyValues.get("uniprot").size());
        assertEquals(1, propertyValues.get("go").size());
    }

    private static final String fullSrc = "software.name = metazoa\n" +
            "software.version = 12\n" +
            "url = http://metazoa.ensembl.org/biomart/martservice?\n" +
            "organism = anopheles gambiae\n" +
            "databaseName = metazoa\n" +
            "datasetName = agambiae_eg_gene\n" +
            "mySqlDbName = anopheles_gambiae\n" +
            "mySqlDbUrl = mysql-eg-mirror.ebi.ac.uk:4205\n" +
            "types = enstranscript,ensgene\n" +
            "property.symbol = external_gene_id\n" +
            "property.refseq = refseq_peptide\n" +
            "property.unigene = unigene\n" +
            "property.ensgene = ensembl_gene_id\n" +
            "property.interproterm = interpro_short_description\n" +
            "property.interpro = interpro\n" +
            "property.goterm = name_1006\n" +
            "property.description = description\n" +
            "property.ensprotein = ensembl_peptide_id\n" +
            "property.enstranscript = ensembl_transcript_id\n" +
            "property.entrezgene = entrezgene\n" +
            "property.uniprot = uniprot_sptrembl,uniprot_swissprot_accession\n" +
            "property.go = go_accession\n" +
            "arrayDesign.A-AFFY-102 = affy_plasmodium_anopheles";
}
