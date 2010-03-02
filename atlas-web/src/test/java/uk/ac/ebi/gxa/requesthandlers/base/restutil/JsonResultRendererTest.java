/*
 * Copyright 2008-2010 Microarray Informatics Team, EMBL-European Bioinformatics Institute
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *
 * For further details of the Gene Expression Atlas project, including source code,
 * downloads and documentation, please see:
 *
 * http://gxa.github.com/gxa
 */

package uk.ac.ebi.gxa.requesthandlers.base.restutil;

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
        } catch (RestResultRenderException e) {
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
        } catch (RestResultRenderException e) {
            fail("Unexpected render exception");
        } catch (IOException e) {
            fail("Unexpected render exception");
        }
    }

}
