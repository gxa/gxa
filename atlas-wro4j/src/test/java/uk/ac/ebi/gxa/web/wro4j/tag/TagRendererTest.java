package uk.ac.ebi.gxa.web.wro4j.tag;

import org.junit.Test;
import ro.isdc.wro.model.group.Group;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Collection;
import java.util.EnumSet;

import static java.util.Arrays.asList;
import static org.easymock.EasyMock.*;

/**
 * @author alf
 */
public class TagRendererTest {
    @Test
    public void testDebug() throws IOException, Wro4jTagException {
        Wro4jTagProperties properties = createMock(Wro4jTagProperties.class);
        expect(properties.isDebugOn()).andReturn(true);
        replay(properties);

        GroupResolver resolver = createMock(GroupResolver.class);
        expect(resolver.getGroup("group")).andReturn(new Group());
        replay(resolver);

        final Wro4jTagRenderer renderer = new Wro4jTagRenderer(resolver,
                properties,
                EnumSet.allOf(ResourceHtmlTag.class),
                new Wro4jTagRenderer.DirectoryLister() {
                    @Override
                    public Collection<String> list(String path) {
                        return asList("name1", "name2", "name3");
                    }
                });
        renderer.render(new StringWriter(), "group", "/ebi");
    }

    @Test
    public void testProduction() throws IOException, Wro4jTagException {
        Wro4jTagProperties properties = createMock(Wro4jTagProperties.class);
        expect(properties.isDebugOn()).andReturn(false);
        replay(properties);

        GroupResolver resolver = createMock(GroupResolver.class);
        expect(resolver.getGroup("group")).andReturn(new Group());
        replay(resolver);

        final Wro4jTagRenderer renderer = new Wro4jTagRenderer(resolver,
                properties,
                EnumSet.allOf(ResourceHtmlTag.class),
                new Wro4jTagRenderer.DirectoryLister() {
                    @Override
                    public Collection<String> list(String path) {
                        return asList("name1", "name2", "name3");
                    }
                });
        renderer.render(new StringWriter(), "group", "/ebi");
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
