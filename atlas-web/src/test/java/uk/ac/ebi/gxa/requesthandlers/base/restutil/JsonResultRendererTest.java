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

import java.util.*;
import java.io.IOException;

import static junit.framework.Assert.assertTrue;

/**
 * @author pashky
 */
public class JsonResultRendererTest {

    @Test
    public void testNoIndentRender() {
        RestResultRenderer r = new JsonRestResultRenderer(true, 4, "atlas");
        r.setErrorWrapper(new RestResultRenderer.ErrorWrapper() {
            public Object wrapError(Throwable e) {
                return "error:" + e.getClass().getSimpleName();
            }
        });
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
            public Map getMap() { Map<String,Object> r =  new HashMap<String,Object>();r.put("b", "bbb");r.put("e", new Iterator() {
                public boolean hasNext() {
                    //return false;
                    throw new IllegalStateException();
                }

                public Object next() {
                    return null;  //To change body of implemented methods use File | Settings | File Templates.
                }

                public void remove() {
                    //To change body of implemented methods use File | Settings | File Templates.
                }
            });return r; }
        };

        StringBuffer sb = new StringBuffer();
        try {
            r.render(o, sb, Object.class);
        } catch (RestResultRenderException e) {
            fail("Unexpected render exception");
        } catch (IOException e) {
            fail("Unexpected render exception");
        } finally {
            System.out.println(sb);
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
