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
import org.junit.Test;

import java.util.Collection;

import static org.junit.Assert.assertEquals;


/**
 * User: nsklyar
 * Date: 19/03/2012
 */
public class AnnotationSourcesExporterTest {
    private static final String SEPARATOR = "\n$$$\n";

    @Test
    public void testJoinAsText() throws Exception {
        final String result = AnnotationSourcesExporter.joinAsText(Lists.newArrayList("first", "second"), "biomart", SEPARATOR);
        assertEquals("Type: biomart\n" +
                "first" +
                SEPARATOR +
                "Type: biomart\n" +
                "second", result);
    }

    @Test
    public void testJoinAll() throws Exception {
        final String s = AnnotationSourcesExporter.joinAll("\n$$$\n"
                , AnnotationSourcesExporter.joinAsText(Lists.newArrayList("first", "second"), "biomart", SEPARATOR)
                , AnnotationSourcesExporter.joinAsText(Lists.newArrayList("first", "second"), "genesig", SEPARATOR));
        assertEquals("Type: biomart\n" +
                "first" +
                SEPARATOR +
                "Type: biomart\n" +
                "second" +
                SEPARATOR +
                "Type: genesig\n" +
                "first" +
                SEPARATOR +
                "Type: genesig\n" +
                "second", s);
    }

    @Test
    public void testGetStringSourcesOfType() throws Exception {
        final Collection<String> genesig = AnnotationSourcesExporter.getStringSourcesOfType("Type: biomart\n" +
                "first" +
                SEPARATOR +
                "Type: biomart\n" +
                "second" +
                SEPARATOR +
                "Type: genesig\n" +
                "first", "genesig", SEPARATOR);
        assertEquals(1, genesig.size());
        assertEquals("first", genesig.iterator().next());
    }

    @Test
    public void testGetStringSourcesOfTypeReal() throws Exception {
        final Collection<String> genesig = AnnotationSourcesExporter.getStringSourcesOfType(real, "BioMartAnnotationSource", "\n$$$\n");
        assertEquals(2, genesig.size());
    }

    String real = "Type: BioMartAnnotationSource\n" +
            "software.name = metazoa\n" +
            "software.version = 13\n" +
            "url = http://metazoa.ensembl.org/biomart/martservice?\n" +
            "organism = anopheles gambiae\n" +
            "databaseName = metazoa\n" +
            "datasetName = agambiae_eg_gene\n" +
            "mySqlDbName = anopheles_gambiae\n" +
            "mySqlDbUrl = mysql-eg-mirror.ebi.ac.uk:4205\n" +
            "types = enstranscript,ensgene\n" +
            "property.description = description\n" +
            "property.ensgene = ensembl_gene_id\n" +
            "property.ensprotein = ensembl_peptide_id\n" +
            "property.enstranscript = ensembl_transcript_id\n" +
            "property.entrezgene = entrezgene\n" +
            "property.go = go_accession\n" +
            "property.goterm = name_1006\n" +
            "property.interpro = interpro\n" +
            "property.interproterm = interpro_short_description\n" +
            "property.refseq = refseq_peptide\n" +
            "property.symbol = external_gene_id\n" +
            "property.unigene = unigene\n" +
            "property.uniprot = uniprot_sptrembl,uniprot_swissprot_accession\n" +
            "arrayDesign.A-AFFY-102 = affy_plasmodium_anopheles\n" +
            "$$$\n" +
            "Type: BioMartAnnotationSource\n" +
            "software.name = plants\n" +
            "software.version = 13\n" +
            "url = http://plants.ensembl.org/biomart/martservice?\n" +
            "organism = arabidopsis thaliana\n" +
            "databaseName = plants\n" +
            "datasetName = athaliana_eg_gene\n" +
            "mySqlDbName = arabidopsis_thaliana\n" +
            "mySqlDbUrl = mysql-eg-mirror.ebi.ac.uk:4205\n" +
            "types = enstranscript,ensgene\n" +
            "property.description = description\n" +
            "property.ensgene = ensembl_gene_id\n" +
            "property.ensprotein = ensembl_peptide_id\n" +
            "property.enstranscript = ensembl_transcript_id\n" +
            "property.entrezgene = entrezgene\n" +
            "property.go = go_accession\n" +
            "property.goterm = name_1006\n" +
            "property.interpro = interpro\n" +
            "property.interproterm = interpro_short_description\n" +
            "property.refseq = refseq_peptide\n" +
            "property.symbol = external_gene_id\n" +
            "property.unigene = unigene\n" +
            "arrayDesign.A-AFFY-2 = affy_ath1_121501";
}
