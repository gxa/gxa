package uk.ac.ebi.gxa.web.wro4j.tag;

import org.junit.Test;
import ro.isdc.wro.model.group.Group;
import ro.isdc.wro.model.resource.Resource;
import ro.isdc.wro.model.resource.ResourceType;

import java.io.IOException;
import java.io.StringWriter;
import java.util.EnumSet;

import static java.util.Arrays.asList;
import static org.easymock.EasyMock.*;

/**
 * @author alf
 */
public class TagRendererTest {
    @Test
    public void testDebug() throws IOException, Wro4jTagException {
        GroupResolver resolver = createMock(GroupResolver.class);
        expect(resolver.getGroup("group"))
                .andReturn(createGroup());

        Wro4jTagRenderer.DirectoryLister lister = createMock(Wro4jTagRenderer.DirectoryLister.class);

        final Wro4jTagProperties properties = prepareMockProperties(true);

        replay(resolver, lister, properties);

        final Wro4jTagRenderer renderer = new Wro4jTagRenderer(resolver,
                properties,
                EnumSet.allOf(ResourceHtmlTag.class),
                lister);
        renderer.render(new StringWriter(), "group", "/ebi");
    }

    @Test
    public void testProduction() throws IOException, Wro4jTagException {
        GroupResolver resolver = createMock(GroupResolver.class);
        expect(resolver.getGroup("group"))
                .andReturn(createGroup());

        Wro4jTagRenderer.DirectoryLister lister = createMock(Wro4jTagRenderer.DirectoryLister.class);
        expect(lister.list("/bundledcss"))
                .andReturn(asList("group-group.ext-css", "test2.js", "test3.css"));
        expect(lister.list("/bundledjs"))
                .andReturn(asList("test2.js", "test3.css", "group-group.ext-js"));

        final Wro4jTagProperties properties = prepareMockProperties(false);
        expect(properties.getResourcePath(ResourceType.CSS))
                .andReturn("/bundledcss")
                .anyTimes();
        expect(properties.getResourcePath(ResourceType.JS))
                .andReturn("/bundledjs")
                .anyTimes();

        replay(resolver, lister, properties);


        final Wro4jTagRenderer renderer = new Wro4jTagRenderer(resolver,
                properties,
                EnumSet.allOf(ResourceHtmlTag.class),
                lister);
        renderer.render(new StringWriter(), "group", "/ebi");
    }

    private Wro4jTagProperties prepareMockProperties(final boolean debug) {
        Wro4jTagProperties properties = createMock(Wro4jTagProperties.class);
        expect(properties.isDebugOn())
                .andReturn(debug);
        expect(properties.getNameTemplate())
                .andReturn(new BundleNameTemplate("group-@groupName@.ext-@extension@"))
                .anyTimes();
        return properties;
    }

    private static Group createGroup() {
        final Group group = new Group();
        group.setName("group");
        group.setResources(asList(
                Resource.create("test.js", ResourceType.JS),
                Resource.create("test2.js", ResourceType.JS),
                Resource.create("test.css", ResourceType.CSS),
                Resource.create("test2.css", ResourceType.CSS)
        ));
        return group;
    }
}
