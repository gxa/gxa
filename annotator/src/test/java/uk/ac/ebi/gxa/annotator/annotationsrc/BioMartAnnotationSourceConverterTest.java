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

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ebi.gxa.annotator.annotationsrc.arraydesign.ArrayDesignService;
import uk.ac.ebi.gxa.annotator.dao.AnnotationSourceDAO;
import uk.ac.ebi.gxa.annotator.model.BioMartAnnotationSource;
import uk.ac.ebi.gxa.annotator.validation.ValidationReportBuilder;
import uk.ac.ebi.gxa.dao.AtlasDAOTestCase;
import uk.ac.ebi.gxa.dao.OrganismDAO;
import uk.ac.ebi.gxa.dao.SoftwareDAO;
import uk.ac.ebi.gxa.dao.bioentity.BioEntityPropertyDAO;
import uk.ac.ebi.gxa.dao.bioentity.BioEntityTypeDAO;
import uk.ac.ebi.microarray.atlas.model.bioentity.Software;

import static org.junit.Assert.*;

/**
 * User: nsklyar
 * Date: 22/08/2011
 */
@ContextConfiguration
public class BioMartAnnotationSourceConverterTest extends AtlasDAOTestCase {

    @Autowired
    private AnnotationSourceDAO annSrcDAO;
    @Autowired
    private OrganismDAO organismDAO;
    @Autowired
    private SoftwareDAO softwareDAO;
    @Autowired
    private BioEntityTypeDAO typeDAO;
    @Autowired
    private BioEntityPropertyDAO propertyDAO;
    @Autowired
    private ArrayDesignService arrayDesignService;

    private BioMartAnnotationSourceConverter converter;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        converter = new BioMartAnnotationSourceConverter();
        converter.setAnnSrcDAO(annSrcDAO);
        converter.setOrganismDAO(organismDAO);
        converter.setPropertyDAO(propertyDAO);
        converter.setSoftwareDAO(softwareDAO);
        converter.setTypeDAO(typeDAO);
        converter.setArrayDesignService(arrayDesignService);
    }

    @Test
    @Transactional
    public void testEditOrCreateAnnotationSourceCreate() throws Exception {
        ValidationReportBuilder reportBuilder = new ValidationReportBuilder();
        final BioMartAnnotationSource annotationSource = converter.initAnnotationSource(ANN_SRC);
        converter.editAnnotationSource(annotationSource, ANN_SRC);
        assertNotNull(annotationSource);
        assertTrue(reportBuilder.isEmpty());
        assertEquals("gallus gallus", annotationSource.getOrganism().getName());
        assertEquals(new Software("Ensembl", "63"), annotationSource.getSoftware());
        assertEquals(10, annotationSource.getExternalBioEntityProperties().size());
        assertEquals(1, annotationSource.getExternalArrayDesignNames().size());
    }

    @Test
    public void testConvertToString() throws Exception {
        final BioMartAnnotationSource byId = annSrcDAO.getById(1000, BioMartAnnotationSource.class);
        assertNotNull(byId);
        String annSrcAsString = converter.convertToString(byId);
        assertEquals(ANN_SRC_DB, annSrcAsString.trim());
    }

    protected static final String ANN_SRC = "organism = gallus gallus\n" +
            "software.name = Ensembl\n" +
            "software.version = 63\n" +
            "url = http://www.ensembl.org/biomart/martservice?\n" +
            "databaseName = ensembl\n" +
            "datasetName = ggallus_gene_ensembl\n" +
            "types = enstranscript,ensgene\n" +
            "mySqlDbName = mus_musculus\n" +
            "mySqlDbUrl = ensembldb.ensembl.org:5306\n" +
            "property.symbol = external_gene_id\n" +
            "property.ensgene = ensembl_gene_id\n" +
            "property.description_ = description\n" +
            "property.ortholog = human_ensembl_gene,cow_ensembl_gene\n" +
            "property.hgnc_symbol = hgnc_symbol\n" +
            "property.enstranscript = ensembl_transcript_id\n" +
            "property.go_id = go\n" +
            "property.uniprot = uniprot_sptrembl,uniprot_swissprot_accession\n" +
            "arrayDesign.A-AFFY-45 = affy_moe430b";

    protected static final String ANN_SRC_DB =
            "software.name = Ensembl\n" +
                    "software.version = 60\n" +
                    "url = http://www.ensembl.org/biomart/martservice?\n" +
                    "organism = homo sapiens\n" +
                    "databaseName = ensembl\n" +
                    "datasetName = hsapiens_gene_ensembl\n" +
                    "mySqlDbName = homo_sapiens\n" +
                    "mySqlDbUrl = ensembldb.ensembl.org:5306\n" +
                    "types = enstranscript,ensgene\n" +
                    "property.ensgene = ensembl_gene_id\n" +
                    "property.enstranscript = ensembl_transcript_id\n" +
                    "property.goterm = go_cellular_component__dm_name_1006,name_1006\n" +
                    "arrayDesign.A-AFFY-45 = affy_74a";
}
