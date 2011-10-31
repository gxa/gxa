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

package uk.ac.ebi.gxa.annotator.loader.annotationsrc;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ebi.gxa.annotator.dao.AnnotationSourceDAO;
import uk.ac.ebi.gxa.annotator.loader.arraydesign.ArrayDesignService;
import uk.ac.ebi.gxa.annotator.loader.biomart.BioMartConnection;
import uk.ac.ebi.gxa.annotator.model.biomart.BioMartAnnotationSource;
import uk.ac.ebi.gxa.dao.AtlasDAOTestCase;
import uk.ac.ebi.gxa.dao.OrganismDAO;
import uk.ac.ebi.gxa.dao.SoftwareDAO;
import uk.ac.ebi.gxa.dao.bioentity.BioEntityPropertyDAO;
import uk.ac.ebi.gxa.dao.bioentity.BioEntityTypeDAO;
import uk.ac.ebi.microarray.atlas.model.bioentity.Software;

import java.io.Reader;
import java.io.StringReader;
import java.util.Collection;

/**
 * User: nsklyar
 * Date: 22/08/2011
 */
@ContextConfiguration
public class BioMartAnnotationSourceLoaderTest extends AtlasDAOTestCase {

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

    private BioMartAnnotationSourceLoader loader;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        loader = new BioMartAnnotationSourceLoader();
        loader.setAnnSrcDAO(annSrcDAO);
        loader.setOrganismDAO(organismDAO);
        loader.setPropertyDAO(propertyDAO);
        loader.setSoftwareDAO(softwareDAO);
        loader.setTypeDAO(typeDAO);
        loader.setArrayDesignService(arrayDesignService);
    }

    @Test
    @Transactional
    public void testReadSource() throws Exception {
        Reader reader = new StringReader(ANN_SRC);
        BioMartAnnotationSource annotationSource = loader.readSource(reader);
        assertNotNull(annotationSource);
        assertEquals("gallus gallus", annotationSource.getOrganism().getName());
        assertEquals(new Software("Ensembl", "63"), annotationSource.getSoftware());
        assertEquals(10, annotationSource.getBioMartProperties().size());
        assertEquals(1, annotationSource.getBioMartArrayDesignNames().size());
    }

    @Test
    @Transactional
    public void testGetCurrentAnnotationSources() throws Exception {
        Collection<BioMartAnnotationSource> annotationSources = annSrcDAO.getAnnotationSourcesOfType(BioMartAnnotationSource.class);
        assertEquals(1, annotationSources.size());
        for (BioMartAnnotationSource annSrc : annotationSources) {
            BioMartConnection connection = new BioMartConnection(annSrc.getUrl(), annSrc.getDatabaseName(), annSrc.getDatasetName());
            assertFalse("Test version should be different from current one", connection.getOnlineMartVersion().equals(annSrc.getSoftware().getVersion()));
        }

        Collection<BioMartAnnotationSource> currentAnnotationSources = loader.getCurrentAnnotationSources();
        assertEquals(1, currentAnnotationSources.size());
        for (BioMartAnnotationSource annSrc : currentAnnotationSources) {
            BioMartConnection connection = new BioMartConnection(annSrc.getUrl(), annSrc.getDatabaseName(), annSrc.getDatasetName());
            assertEquals(connection.getOnlineMartVersion(), annSrc.getSoftware().getVersion());
        }
    }

    @Test
    public void testGetAnnSrcAsStringById() throws Exception {
        String annSrcAsString = loader.getAnnSrcAsStringById("1000");
        assertEquals(ANN_SRC_DB, annSrcAsString.trim());
    }

    private static final String ANN_SRC = "organism = gallus gallus\n" +
            "software.name = Ensembl\n" +
            "software.version = 63\n" +
            "url = http://www.ensembl.org/biomart/martservice?\n" +
            "databaseName = ensembl\n" +
            "datasetName = ggallus_gene_ensembl\n" +
            "types = enstranscript,ensgene\n" +
            "mySqlDbName = mus_musculus\n" +
            "mySqlDbUrl = ensembldb.ensembl.org:5306\n" +
            "biomartProperty.symbol = external_gene_id\n" +
            "biomartProperty.ensgene = ensembl_gene_id\n" +
            "biomartProperty.description_ = description\n" +
            "biomartProperty.ortholog = human_ensembl_gene,cow_ensembl_gene\n" +
            "biomartProperty.hgnc_symbol = hgnc_symbol\n" +
            "biomartProperty.enstranscript = ensembl_transcript_id\n" +
            "biomartProperty.uniprot = uniprot_sptrembl,uniprot_swissprot_accession\n" +
            "biomartProperty.go_id = go\n" +
            "arrayDesign.A-AFFY-45 = affy_moe430b";

    private static final String ANN_SRC_DB = "organism = homo sapiens\n" +
            "software.name = Ensembl\n" +
            "software.version = 60\n" +
            "url = http://www.ensembl.org/biomart/martservice?\n" +
            "databaseName = ensembl\n" +
            "datasetName = hsapiens_gene_ensembl\n" +
            "mySqlDbName = homo_sapiens\n" +
            "mySqlDbUrl = ensembldb.ensembl.org:5306\n" +
            "types = enstranscript,ensgene\n" +
            "biomartProperty.ensgene = ensembl_gene_id\n" +
            "biomartProperty.goterm = go_cellular_component__dm_name_1006,name_1006\n" +
            "biomartProperty.enstranscript = ensembl_transcript_id\n" +
            "arrayDesign.A-AFFY-45 = affy_74a";
}
