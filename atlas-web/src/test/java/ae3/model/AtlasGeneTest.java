package ae3.model;

import uk.ac.ebi.gxa.index.AbstractOnceIndexTest;
import uk.ac.ebi.gxa.index.ExperimentsTable;
import uk.ac.ebi.gxa.index.Experiment;
import ae3.dao.AtlasDao;
import uk.ac.ebi.gxa.utils.Pair;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertFalse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;

import java.util.*;

import static junit.framework.Assert.assertEquals;

/**
 * @author pashky
 */
public class AtlasGeneTest  extends AbstractOnceIndexTest {
    private AtlasGene gene;

    @Before
    public void initGene() {
        AtlasDao dao = new AtlasDao();
        dao.setSolrServerAtlas(new EmbeddedSolrServer(getContainer(), "atlas"));
        dao.setSolrServerExpt(new EmbeddedSolrServer(getContainer(), "expt"));
        gene = dao.getGeneByIdentifier("ENSG00000066279").getGene();
    }

    @Test
    public void test_getGeneSpecies() {
        assertNotNull(gene.getGeneSpecies());
        assertEquals("Homo sapiens", gene.getGeneSpecies());
    }

    @Test
    public void test_getGeneIds() {
        assertNotNull(gene.getGeneId());
        assertTrue(gene.getGeneId().matches("^[0-9]+$"));
    }

    @Test
    public void test_getGeneName() {
        assertNotNull(gene.getGeneName());
        assertEquals("ASPM", gene.getGeneName());
    }

    @Test
    public void test_getGeneIdentifier() {
        assertNotNull(gene.getGeneIdentifier());
        assertEquals("ENSG00000066279", gene.getGeneIdentifier());
    }

    @Test
    public void test_getGeneEnsembl() {
        assertNotNull(gene.getGeneEnsembl());
        assertEquals("ENSG00000066279", gene.getGeneEnsembl());
    }

    @Test
    public void test_getGoTerm() {
        assertNotNull(gene.getGoTerm());
        assertTrue(gene.getGoTerm().matches(".*\\S+.*"));
    }

    @Test
    public void test_fieldAvailable() {
        assertTrue(gene.fieldAvailable("gene_name"));
        assertFalse(gene.fieldAvailable("gene_chebi"));
    }

    @Test
    public void test_getInterProTerm() {
        assertNotNull(gene.getInterProTerm());
        assertTrue(gene.getInterProTerm().matches(".*\\S+.*"));
    }

    @Test
    public void test_getKeyword() {
        assertNotNull(gene.getKeyword());
        assertTrue(gene.getKeyword().matches(".*\\S+.*"));
    }

    @Test
    public void test_getDisesase() {
        assertNotNull(gene.getDisease());
        assertTrue(gene.getDisease().matches(".*\\S+.*"));
    }

    @Test
    public void test_shortValues() {
        assertTrue(gene.getShortGOTerms().length() <= gene.getGoTerm().length());
        assertTrue(gene.getShortInterProTerms().length() <= gene.getInterProTerm().length());
        assertTrue(gene.getShortDiseases().length() <= gene.getDisease().length());
    }

    @Test
    public void test_getGeneSolrDocument() {
        SolrDocument solrdoc = gene.getGeneSolrDocument();
        assertNotNull(solrdoc);
        assertTrue(solrdoc.getFieldNames().contains("gene_id"));
    }

    @Test
    public void test_getUniprotIds(){
        assertNotNull(gene.getUniprotId());
        assertTrue(gene.getUniprotId().matches("^[A-Z0-9, ]+$"));
    }

    @Test
    public void getSynonyms(){
        assertNotNull(gene.getSynonym());
        assertTrue(gene.getSynonym().contains("ASPM"));
    }

    @Test
    public void test_highlighting() {
        Map<String, List<String>> highlights = new HashMap<String, List<String>>();
        highlights.put("gene_synonym", Arrays.asList("<em>ASPM</em>", "MCPH5", "RP11-32D17.1-002", "hCG_2039667"));
        gene.setGeneHighlights(highlights);
        assertTrue(gene.getHilitSynonym().matches(".*<em>.*"));
        assertNotNull(gene.getGeneHighlightStringForHtml());
        assertTrue(gene.getGeneHighlightStringForHtml().matches(".*<em>.*"));
    }

    @Test
	public void test_getAllFactorValues() {
        Collection<String> efvs = gene.getAllFactorValues("cellline");
        assertNotNull(efvs);
        assertTrue(efvs.size() > 0);
        assertTrue(efvs.contains("BT474"));
	}

    /*
    @Test
	public void test_orthologs() {
        gene.getOrthologs();
        gene.getOrthologsIds();
        public void addOrthoGene(AtlasGene ortho){
        public ArrayList<AtlasGene> getOrthoGenes(){

	}

    @Test
    public void test_getCounts() {

        }
    }
*/
    @Test
    public void test_getExpermientsTable() {
        ExperimentsTable et = gene.getExperimentsTable();
        assertTrue(et.getAll().iterator().hasNext());
    }

    @Test
    public void test_getNumberOfExperiments() {
        assertTrue(gene.getNumberOfExperiments() > 0);
    }

    @Test
    public void test_getAllEfs() {
        Collection<String> efs = gene.getAllEfs();
        assertNotNull(efs);
        assertTrue(efs.size() > 0);
        assertTrue(efs.contains("cellline"));
        assertTrue(efs.contains("organismpart"));
    }

    @Test
    public void test_getHeatMapRows() {
        Collection<ListResultRow> rows = gene.getHeatMapRows();
        assertNotNull(rows);
        assertTrue(rows.size() > 0);
    }

    @Test
    public void test_getTopFVs() {
        Collection<Experiment> efvs = gene.getTopFVs(204778371);
        assertNotNull(efvs);

        double pv = 0;
        for(Experiment t : efvs) {
            assertTrue(pv <= t.getPvalue());
            pv = t.getPvalue();
        }
    }

    @Test
    public void test_getHighestRankEF() {
        Pair<String,Double> hef = gene.getHighestRankEF(204778371);
        assertNotNull(hef);
        assertTrue(hef.getSecond() >= 0);
        assertTrue(hef.getFirst().matches(".*[A-Za-z]+.*"));
    }
}
