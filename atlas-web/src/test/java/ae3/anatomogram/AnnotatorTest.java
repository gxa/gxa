package ae3.anatomogram;

import ae3.dao.AtlasSolrDAO;
import ae3.model.AtlasGene;
import org.junit.Before;
import org.junit.Test;
import uk.ac.ebi.gxa.AbstractIndexNetCDFTestCase;

import java.util.HashMap;
import java.util.Map;

public class AnnotatorTest extends AbstractIndexNetCDFTestCase {
    private AtlasSolrDAO atlasSolrDAO;
    private Annotator annotator;

    @Before
    public void init() {
        atlasSolrDAO = new AtlasSolrDAO();
        atlasSolrDAO.setSolrServerAtlas(getSolrServerAtlas());
        atlasSolrDAO.setSolrServerExpt(getSolrServerExpt());
        annotator = new Annotator();
        annotator.load();
    }

    @Test
    public void testGetHasAnatomogram() throws Exception {
        init();

        Map<String, Boolean> geneIds = new HashMap<String, Boolean>() {{
            put("ABCDEF", false);
            put("ENSMUSG00000020275", true);
            put(".-_", false);
        }};
        for (Map.Entry<String, Boolean> mapEntry : geneIds.entrySet()) {
            //atlasGene may be null
            AtlasGene atlasGene = atlasSolrDAO.getGeneByIdentifier(mapEntry.getKey()).getGene();

            for (Annotator.AnatomogramType type : new Annotator.AnatomogramType[]{Annotator.AnatomogramType.Web, Annotator.AnatomogramType.Das}) {

                if (null == atlasGene)
                    continue;

                Anatomogram an = annotator.getAnatomogram(type, atlasGene);
                assertEquals((boolean) mapEntry.getValue(), !an.isEmpty());
            }
        }

    }
}

   