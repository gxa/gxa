package ae3.anatomogram;

import ae3.dao.AtlasSolrDAO;
import ae3.model.AtlasGene;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.HashMap;
import java.util.Map;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations={"classpath:testApplicationContext.xml"})
public class AnnotatorTest {

    @Autowired
    private AtlasSolrDAO atlasSolrDAO;

    @Autowired
    private Annotator annotator;

    @Test
    public void testGetHasAnatomogram() throws Exception {

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
                assertEquals(mapEntry.getValue(), !an.isEmpty());
            }
        }
    }
}

   