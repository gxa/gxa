package ae3.restresult;

import org.junit.Test;
import static org.junit.Assert.fail;

import java.util.List;
import java.util.Arrays;
import java.util.Map;
import java.util.HashMap;
import java.io.IOException;

import static junit.framework.Assert.assertTrue;

/**
 * @author pashky
 */
public class JsonResultRendererTest {

    @Test
    public void testNoIndentRender() {
        RestResultRenderer r = new JsonRestResultRenderer(true, 4);

        Object o = new Object() {
            @RestOut
            public String getString() { return "some text"; }
            @RestOut
            public int getInt() { return 123; }
            @RestOut
            public Double getDouble() { return 1.23464e15; }
            @RestOut(name="list", xmlItemName ="it")
            public List getList() { return Arrays.asList(1, 2, 3, 5); }
            @RestOut(name="mapas", xmlAttr ="id")
            public Map getMap() { Map<String,String> r =  new HashMap<String,String>();r.put("a", "aaa");r.put("b", "bbb");return r; }
        };

        StringBuffer sb = new StringBuffer();
        try {
            r.render(o, sb, Object.class);
            System.out.println(sb);
        } catch (RenderException e) {
            fail("Unexpected render exception");
        } catch (IOException e) {
            fail("Unexpected render exception");
        }
    }

    @Test
    public void testJsonCallback() {
        RestResultRenderer r = new JsonRestResultRenderer(true, 4, "callmeplease");

        StringBuffer sb = new StringBuffer();
        try {
            r.render("test value", sb, Object.class);
            assertTrue(sb.toString().startsWith("callmeplease"));
        } catch (RenderException e) {
            fail("Unexpected render exception");
        } catch (IOException e) {
            fail("Unexpected render exception");
        }
    }

}
