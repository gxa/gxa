package uk.ac.ebi.gxa.web.wro4j.tag;

import com.google.common.base.Splitter;
import org.junit.Before;
import org.junit.Test;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.UrlResource;
import org.springframework.mock.web.MockPageContext;
import org.springframework.mock.web.MockServletContext;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.Tag;
import java.util.EnumSet;

import static java.util.Arrays.asList;
import static org.junit.Assert.*;

public class Wro4jTagTest {
    public static final String LOREM_IPSUM = "Lorem ipsum dolor sit amet, consectetur adipisicing elit, " +
            "sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, " +
            "quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. " +
            "Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat " +
            "nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia " +
            "deserunt mollit anim id est laborum.";

    private MockPageContext mockPageContext;

    @Before
    public void mockContainer() {
        MockServletContext mockServletContext = new MockServletContext(new ResourceLoader() {
            @Override
            public org.springframework.core.io.Resource getResource(String location) {
                if ("/WEB-INF/wro.xml".equals(location))
                    return new UrlResource(getClassLoader().getResource("config/wro.xml"));
                else
                    fail("Request for unknown resource: " + location);
                return null;
            }

            @Override
            public ClassLoader getClassLoader() {
                return Wro4jTagTest.class.getClassLoader();
            }
        });
        mockPageContext = new MockPageContext(mockServletContext);
    }

    @Test
    public void testResourceTypes() {
        assertEquals(EnumSet.of(ResourceHtmlTag.JS), jsTag().getTags());
        assertEquals(EnumSet.of(ResourceHtmlTag.CSS), cssTag().getTags());
        assertEquals(EnumSet.allOf(ResourceHtmlTag.class), allTag().getTags());
    }

    @Test
    public void testName() {
        Wro4jTag tag = allTag();
        for (String name : Splitter.on(" ").split(LOREM_IPSUM)) {
            tag.setName(name);
            assertEquals(name, tag.getName());
        }
    }

    @Test
    public void testNoName() throws JspException {
        for (Wro4jTag tag : asList(allTag(), cssTag(), jsTag())) {
            try {
                tag.doStartTag();
                fail("The name is mandatory");
            } catch (JspException e) {
                assertTrue(e.getMessage().contains("name"));
                assertTrue(e.getMessage().contains("mandatory"));
            }
        }
    }

    @Test
    public void testBadName() throws JspException {
        final String unknownGroup = "Lorem ipsum dolor sit amet";
        for (Wro4jTag tag : asList(allTag(), cssTag(), jsTag())) {
            try {
                tag.setName(unknownGroup);
                tag.doStartTag();
                fail("The name is somehow known: check the config and/or the code");
            } catch (JspException e) {
                assertTrue(e.getMessage().contains(unknownGroup));
            }
        }
    }

    @Test
    public void testTagBasics() throws JspException {
        for (Wro4jTag tag : asList(allTag(), cssTag(), jsTag())) {
            tag.setName("mixed-resources-1");
            assertEquals(Tag.SKIP_BODY, tag.doStartTag());
            assertEquals(Tag.EVAL_PAGE, tag.doEndTag());
        }
    }

    private Wro4jAllTag allTag() {
        final Wro4jAllTag tag = new Wro4jAllTag();
        tag.setPageContext(mockPageContext);
        return tag;
    }

    private Wro4jCssTag cssTag() {
        final Wro4jCssTag tag = new Wro4jCssTag();
        tag.setPageContext(mockPageContext);
        return tag;
    }

    private Wro4jJavaScriptTag jsTag() {
        final Wro4jJavaScriptTag tag = new Wro4jJavaScriptTag();
        tag.setPageContext(mockPageContext);
        return tag;
    }
}
