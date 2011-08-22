package uk.ac.ebi.gxa.annotator.loader.annotationsrc;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ebi.gxa.annotator.dao.AnnotationSourceDAO;
import uk.ac.ebi.gxa.annotator.model.biomart.BioMartAnnotationSource;
import uk.ac.ebi.gxa.dao.AtlasDAOTestCase;

import java.io.Reader;
import java.io.StringReader;

/**
 * User: nsklyar
 * Date: 22/08/2011
 */
@ContextConfiguration
public class BioMartAnnotationSourceLoaderTest extends AtlasDAOTestCase {

    @Autowired
    private AnnotationSourceDAO annSrcDAO;

    private BioMartAnnotationSourceLoader loader;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        loader = new BioMartAnnotationSourceLoader();
        loader.setAnnSrcDAO(annSrcDAO);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void testReadSource() throws Exception {
        Reader reader = new StringReader(ANN_SRC);
        BioMartAnnotationSource annotationSource = loader.readSource(reader);
        assertNotNull(annotationSource);

    }

    @Test
    public void testGetCurrentAnnotationSources() throws Exception {

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
//            "biomartProperty.ensfamily = family\n" +
//            "biomartProperty.symbol = external_gene_id\n" +
//            "biomartProperty.ensfamily_description = family_description\n" +
//            "biomartProperty.refseq = refseq_dna,refseq_peptide\n" +
//            "biomartProperty.mirbase_accession = mirbase_accession\n" +
//            "biomartProperty.embl = embl\n" +
//            "biomartProperty.unigene = unigene\n" +
//            "biomartProperty.ensgene = ensembl_gene_id\n" +
//            "biomartProperty.interproterm = interpro_short_description\n" +
//            "biomartProperty.interpro = interpro\n" +
//            "biomartProperty.goterm = name_1006\n" +
//            "biomartProperty.description = description\n" +
//            "biomartProperty.ortholog = human_ensembl_gene,cow_ensembl_gene,drosophila_ensembl_gene,rat_ensembl_gene,yeast_ensembl_gene,zebrafish_ensembl_gene,mouse_ensembl_gene,ciona_intestinalis_ensembl_gene,xenopus_ensembl_gene\n" +
//            "biomartProperty.ensprotein = ensembl_peptide_id\n" +
//            "biomartProperty.hgnc_symbol = hgnc_symbol\n" +
//            "biomartProperty.mirbase_id = mirbase_id\n" +
//            "biomartProperty.enstranscript = ensembl_transcript_id\n" +
//            "biomartProperty.entrezgene = entrezgene\n" +
//            "biomartProperty.uniprot = uniprot_sptrembl,uniprot_swissprot_accession\n" +
            "biomartProperty.go = go_id";
}
