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

package ae3.anatomogram;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class Editor {
    public static final int CHAR_WIDTH = 4;

    final private Logger log = LoggerFactory.getLogger(getClass());

    private Document document;

    public Editor(Document document) {
        this.document = document;
    }

    public void fill(String id, String color) {
        Element e = document.getElementById(id);
        if (null != e) {
            String s = e.getAttribute("style");
            s = s.replace("fill:#ffffff", "fill:" + color);
            s = s.replace("fill:none", "fill:" + color);
            // Replace red with blue and blue with red
            if ("#0000ff".equals(color)) {
                s = s.replace("fill:#ff0000", "fill:" + color);
            } else if ("#ff0000".equals(color)) {
                s = s.replace("fill:#0000ff", "fill:" + color);
            }
            e.setAttributeNS(null, "style", s);
        }
    }

    public void setText(String id, String text) {
        Element e = document.getElementById(id);
        if (null != e) {
            //setTextContent is abstract
            try {
                e.getFirstChild().getFirstChild().setNodeValue(text);
            }
            catch (Exception ex) {
                log.error("can not set text", ex);
            }
        }
    }

    public void setTextAndAlign(String id, String text) {
        Element e = document.getElementById(id);
        if (null != e) {
            try {
                e.getFirstChild().getFirstChild().setNodeValue(text);
            } catch (Exception ex) {
                log.error("can not set text", ex);
            }


            if (text.length() > 1) {
                Float i = Float.parseFloat(e.getAttribute("x"));

                String new_x = String.format("%1$f", i - (text.length() - 1) * CHAR_WIDTH);

                e.setAttribute("x", new_x);
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
        if (null != e) {
            String s = e.getAttribute("style");
            s = s.replace("stroke:#ffffff", "stroke:" + color);
            e.setAttributeNS(null, "style", s);
        }
    }

    public void setOpacity(String id, String opacity) {
        Element e = document.getElementById(id);
        if (null != e) {
            String s = e.getAttribute("style");
            s = s + ";opacity:" + opacity;
            e.setAttributeNS(null, "style", s);
        }
    }
}
