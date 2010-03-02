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

import com.jamesmurty.utils.XMLBuilder;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.util.Iterator;

/**
 * XML format REST result renderer.
 * <p/>
 * {@link RestOut} properties used: * name * exposeEmpty * xmlAttr * xmlItemName
 *
 * @author pashky
 */
public class XmlRestResultRenderer implements RestResultRenderer {
    private XMLBuilder xml;

    private boolean indent = false;
    private int indentAmount = 4;
    private Class profile;

    /**
     * Constructor
     *
     * @param indent       set to true if output should be pretty formatted and indented
     * @param indentAmount amount of each indentation step
     */
    public XmlRestResultRenderer(boolean indent, int indentAmount) {
        this.indent = indent;
        this.indentAmount = indentAmount;
    }


    public void render(Object object, Appendable where, final Class profile) throws RestResultRenderException, IOException {
        try {
            xml = XMLBuilder.create("atlasResponse");

            this.profile = profile;
            process(object, null, null);
            
            // and write out
            where.append(xml.asString(indent, indentAmount));
        }
        catch (ParserConfigurationException e) {
            throw new RestResultRenderException(e);
        }
    }

    private void process(Object o, String iname, RestOut outProp) throws IOException, RestResultRenderException {
        outProp = RestResultRendererUtil.mergeAnno(outProp, o.getClass(), getClass(), profile);

        if (o instanceof Number
                || o instanceof Boolean
                || o instanceof String
                || o instanceof Enum
                || (outProp != null && outProp.asString())) {
            xml = xml.t(o.toString());
        }
        else if (o instanceof Iterable || o instanceof Iterator) {
            processArray(o, iname, outProp);
        }
        else {
            processMap(o, iname, outProp);
        }
    }

    private void processMap(Object o, String iname, RestOut outProp) throws IOException, RestResultRenderException {
        outProp = RestResultRendererUtil.mergeAnno(outProp, o.getClass(), getClass(), profile);

        String attrName = null;
        String itemName = null;

        for (RestResultRendererUtil.Prop p : RestResultRendererUtil.iterableProperties(o, profile, this)) {
            if (outProp == null && p.value != null) {
                outProp = RestResultRendererUtil.getAnno(p.value.getClass(), getClass(), profile);
            }

            if (itemName == null) {
                attrName = outProp != null && !"".equals(outProp.xmlAttr()) ? outProp.xmlAttr() : null;
                itemName = getItemName(iname, outProp);
            }

            if (attrName != null) {
                xml = xml.e(itemName).a(attrName, p.name);
            }
            else {
                xml = xml.e(p.name);
            }

            process(p.value, p.name, p.outProp);

            xml = xml.up();
        }

    }

    private void processArray(Object oi, String iname, RestOut outProp) throws RestResultRenderException, IOException {
        outProp = RestResultRendererUtil.mergeAnno(outProp, oi.getClass(), getClass(), profile);

        String attrName = null;
        String itemName = null;

        Iterator i = oi instanceof Iterator ? (Iterator) oi : ((Iterable) oi).iterator();
        int number = 0;
        while (i.hasNext()) {
            Object object = i.next();
            if (outProp == null && object != null) {
                outProp = RestResultRendererUtil.getAnno(object.getClass(), getClass(), profile);
            }
            if (itemName == null) {
                itemName = getItemName(iname, outProp);
                attrName = outProp != null && !"".equals(outProp.xmlAttr()) ? outProp.xmlAttr() : null;
            }
            if (attrName != null) {
                xml = xml.e(itemName).a(attrName, String.valueOf(number++));
            }
            else {
                xml = xml.e(itemName);
            }
            if (object != null) {
                process(object, iname, null);
            }
            xml = xml.up();
        }

    }

    private String getItemName(String iname, RestOut outProp) {
        if (outProp != null && outProp.xmlItemName().length() > 0) {
            return outProp.xmlItemName();
        }
        else if (iname != null && iname.length() > 1 && iname.endsWith("s")) {
            return iname.substring(0, iname.length() - 1);
        }
        else {
            return "item";
        }
    }
}
