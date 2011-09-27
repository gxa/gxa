package uk.ac.ebi.gxa.web.wro4j.tag;

import com.google.common.base.Splitter;
import org.junit.Test;
import ro.isdc.wro.model.resource.Resource;
import ro.isdc.wro.model.resource.ResourceType;

import java.util.EnumSet;

import static org.junit.Assert.*;

public class Wro4jTagTest {
    public static final String LOREM_IPSUM = "Lorem ipsum dolor sit amet, consectetur adipisicing elit, " +
            "sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, " +
            "quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. " +
            "Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat " +
            "nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia " +
            "deserunt mollit anim id est laborum.";

    @Test
    public void testResourceTypes() {
        assertEquals(EnumSet.of(ResourceHtmlTag.JS), jsTag().getTags());
        assertEquals(EnumSet.of(ResourceHtmlTag.CSS), cssTag().getTags());
        assertEquals(EnumSet.allOf(ResourceHtmlTag.class), allTag().getTags());
    }

    @Test
    public void testSupportedResources() {
        Resource js = Resource.create("/uri", ResourceType.JS);
        Resource css = Resource.create("/uri", ResourceType.CSS);

        assertTrue(allTag().isSupported(js));
        assertTrue(allTag().isSupported(css));

        assertTrue(jsTag().isSupported(js));
        assertFalse(jsTag().isSupported(css));

        assertFalse(cssTag().isSupported(js));
        assertTrue(cssTag().isSupported(css));
    }

    @Test
    public void testName() {
        Wro4jTag tag = new Wro4jTag(EnumSet.noneOf(ResourceHtmlTag.class)) {
        };
        for (String name : Splitter.on(" ").split(LOREM_IPSUM)) {
            tag.setName(name);
            assertEquals(name, tag.getName());
        }
    }


    private Wro4jAllTag allTag() {
        return new Wro4jAllTag();
    }

    private Wro4jCssTag cssTag() {
        return new Wro4jCssTag();
    }

    private Wro4jJavaScriptTag jsTag() {
        return new Wro4jJavaScriptTag();
    }
}
