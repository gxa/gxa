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

package uk.ac.ebi.gxa.anatomogram.svgutil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import static uk.ac.ebi.gxa.anatomogram.svgutil.CSSUtil.replaceColor;

public class Editor {
    public static final int CHAR_WIDTH = 4;
    private static final String STYLE = "style";

    final private Logger log = LoggerFactory.getLogger(getClass());

    private Document document;

    public Editor(Document document) {
        this.document = document;
    }

    public void fill(String id, String color) {
        Element e = document.getElementById(id);
        if (e == null) {
            log.warn("Cannot set " + id + " to " + color + ":  element was not found");
            return;
        }
        e.setAttributeNS(null, STYLE, replaceColor(e.getAttribute(STYLE), "fill", color));
    }

    public void setText(String id, String text) {
        Element e = document.getElementById(id);
        if (null != e) {
            //setTextContent is abstract
            try {
                e.getFirstChild().getFirstChild().setNodeValue(text);
            } catch (Exception ex) {
                log.error("can not set text", ex);
            }
        }
    }

    public void setTextAndAlign(String id, String text) {
        Element e = document.getElementById(id);
        if (e == null) {
            log.error("Element with id=" + id + " was not found");
            return;
        }

        String prevText = "";
        if (e.getChildNodes().getLength() > 0) {
            Node firstChild = e.getFirstChild();
            if (firstChild.getChildNodes().getLength() > 0) {
                firstChild = firstChild.getFirstChild();
                prevText = firstChild.getNodeValue();
                firstChild.setNodeValue(text);
            }
        }

        int dl = prevText.length() - text.length();
        if (dl != 0) {
            float x = Float.parseFloat(e.getAttribute("x"));
            x = x + dl * CHAR_WIDTH;

            e.setAttribute("x", String.format("%1$f", x));
            if (((Element) e.getFirstChild()).hasAttribute("x")) {
                e.getFirstChild().getAttributes().removeNamedItem("x");
            }
        }
    }

    public void setVisibility(String id, String visibility) {
        Element e = document.getElementById(id);
        if (null != e) {
            e.setAttributeNS(null, "visibility", visibility);
        }
    }

    public void setStroke(String id, String color) {
        Element e = document.getElementById(id);
        if (e == null) {
            log.warn("Cannot set " + id + " to " + color + ":  element was not found");
            return;
        }
        e.setAttributeNS(null, STYLE, replaceColor(e.getAttribute(STYLE), "stroke", color));
    }

    public void setOpacity(String id, String opacity) {
        Element e = document.getElementById(id);
        if (null != e) {
            String s = e.getAttribute(STYLE);
            s = s + ";opacity:" + opacity;
            e.setAttributeNS(null, STYLE, s);
        }
    }
}
