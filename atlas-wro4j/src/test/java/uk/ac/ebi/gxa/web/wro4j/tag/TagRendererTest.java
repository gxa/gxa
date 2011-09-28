package uk.ac.ebi.gxa.web.wro4j.tag;

import org.junit.Test;
import ro.isdc.wro.model.WroModel;
import ro.isdc.wro.model.resource.Resource;
import ro.isdc.wro.model.resource.ResourceType;

import java.util.Collection;
import java.util.EnumSet;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author alf
 */
public class TagRendererTest {
    @Test
    public void testSomethingSimple() {
        Wro4jTagRenderer renderer = new Wro4jTagRenderer(new WroModel(), new Wro4jTagProperties(), EnumSet.allOf(ResourceHtmlTag.class),
                new Wro4jTagRenderer.DirectoryLister() {
                    @Override
                    public Collection<String> list(String path) {
                        return asList("name1", "name2", "name3");
                    }
                });
    }
//
//    @Test
//    public void testSupportedResources() {
//        Resource js = Resource.create("/uri", ResourceType.JS);
//        Resource css = Resource.create("/uri", ResourceType.CSS);
//
//        assertTrue(allTag().getRenderer().isSupported(js));
//        assertTrue(allTag().getRenderer().isSupported(css));
//
//        assertTrue(jsTag().getRenderer().isSupported(js));
//        assertFalse(jsTag().getRenderer().isSupported(css));
//
//        assertFalse(cssTag().getRenderer().isSupported(js));
//        assertTrue(cssTag().getRenderer().isSupported(css));
//    }
}
