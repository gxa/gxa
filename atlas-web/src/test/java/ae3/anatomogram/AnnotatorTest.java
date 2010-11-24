package ae3.anatomogram;

import ae3.dao.AtlasSolrDAO;
import ae3.model.AtlasGene;
import org.junit.Before;
import org.junit.Test;
import uk.ac.ebi.gxa.AbstractIndexNetCDFTestCase;
import uk.ac.ebi.gxa.requesthandlers.genepage.AnatomogramRequestHandler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static junit.framework.Assert.assertEquals;

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
                //test getHasAnatomogram
                assertEquals((boolean) mapEntry.getValue(), (boolean) annotator.getHasAnatomogram(atlasGene, type));

                if (null == atlasGene)
                    continue;

                String organism = atlasGene.getGeneSpecies();
                //test getKnownEfo
                List<String> knownEfo = annotator.getKnownEfo(type, organism);
                //assertNotNull((Object)knownEfo);

                List<AnatomogramRequestHandler.Annotation> annotations = new ArrayList<AnatomogramRequestHandler.Annotation>();
                for (String efo : knownEfo) {
                    annotations.add(AnatomogramRequestHandler.newAnnotation(efo, efo, 1, 1));
                }
                //test processAnatomogram(); -->null
                annotator.process(organism, annotations, Annotator.Encoding.Png, null, type);
            }
        }

    }

    protected String getModuleName() {
        return "atlas-web";
    }
}

   