package uk.ac.ebi.gxa.web.wro4j.tag;

import org.junit.Test;
import ro.isdc.wro.model.resource.ResourceType;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ResourceHtmlTagTest {
    @Test
    public void allTypesAreFoundAndSenbsible() {
        for (ResourceType type : ResourceType.values()) {
            assertTrue(ResourceHtmlTag.forType(type).getType() == type);
        }
    }

    @Test
    public void htmlTags() {
        assertEquals("<link type=\"text/css\" rel=\"stylesheet\" href=\"/context/stylesheet.css\"/>",
                ResourceHtmlTag.CSS.render("/context/stylesheet.css"));
        assertEquals("<script type=\"text/javascript\" src=\"/js/script.js\"></script>",
                ResourceHtmlTag.JS.render("/js/script.js"));
    }
}
