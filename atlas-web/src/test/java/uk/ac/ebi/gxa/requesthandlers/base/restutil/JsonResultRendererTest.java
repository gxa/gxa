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

import junit.framework.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.*;

import static junit.framework.Assert.assertTrue;
import static uk.ac.ebi.gxa.utils.CollectionUtil.makeMap;

/**
 * @author pashky
 */
public class JsonResultRendererTest {
    @Test
    public void testNoIndentRender() throws IOException, RestResultRenderException {
        RestResultRenderer r = new JsonRestResultRenderer(true, 4, "atlas");
        r.setErrorWrapper(new RestResultRenderer.ErrorWrapper() {
            public Object wrapError(Throwable e) {
                return "error:" + e.getClass().getSimpleName();
            }
        });
        Object o = new Object() {
            @RestOut
            public String getString() {
                return "some text";
            }

            @RestOut
            public int getInt() {
                return 123;
            }

            @RestOut
            public Double getDouble() {
                return 1.23464e15;
            }

            @RestOut(name = "list", xmlItemName = "it")
            public List getList() {
                return Arrays.asList(1, 2, 3, 5);
            }

            @RestOut(name = "mapas", xmlAttr = "id")
            public Map getMap() {
                Map<String, Object> r = new HashMap<String, Object>();
                r.put("b", "bbb");
                r.put("e", new Iterator() {
                    public boolean hasNext() {
                        //return false;
                        throw new IllegalStateException();
                    }

                    public Object next() {
                        return null;
                    }

                    public void remove() {
                    }
                });
                return r;
            }

            @RestOut
            public int[] getArray() {
                return new int[]{1, 2, 3, 4};
            }
        };

        StringBuffer sb = new StringBuffer();
        r.render(o, sb, Object.class);
        Assert.assertEquals("Wrong format!", "atlas({\n" +
                "    \"int\" : 123,\n" +
                "    \"double\" : 1.2346E15,\n" +
                "    \"array\" : [1, 2, 3, 4],\n" +
                "    \"mapas\" : {\n" +
                "        \"e\" : [\n" +
                "            ]}},\"error:IllegalStateException\")", sb.toString());
    }

    @Test
    public void testJsonCallback() throws IOException, RestResultRenderException {
        RestResultRenderer r = new JsonRestResultRenderer(true, 4, "callmeplease");

        StringBuffer sb = new StringBuffer();
        r.render("test value", sb, Object.class);
        assertTrue(sb.toString().startsWith("callmeplease"));
    }

    @Test
    public void testNullsInHashMap() throws IOException, RestResultRenderException {
        RestResultRenderer r = new JsonRestResultRenderer(true, 4, "hashmap");

        StringBuffer sb = new StringBuffer();
        r.render(makeMap("id", "CCL5:204655_at",
                "median", "118.6915283203125",
                "uq", "151.7723846435547",
                "lq", "86.86836242675781",
                "max", "210.45542907714844",
                "min", "66.39773559570312",
                "up", null,
                "down", null), sb, Object.class);
    }
}
