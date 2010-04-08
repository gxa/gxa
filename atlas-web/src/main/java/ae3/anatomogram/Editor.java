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

import org.apache.batik.dom.svg.SVGOMTSpanElement;
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

    public void fill(String Id, String Color) {
        Element e = document.getElementById(Id);
        if (null != e) {
            String s = e.getAttribute("style");
            s = s.replace("fill:#ffffff", "fill:" + Color);
            s = s.replace("fill:none", "fill:" + Color);
            s = s.replace("fill:#0000ff", "fill:" + Color);
            e.setAttributeNS(null, "style", s);
        }
    }

    public void setText(String Id, String Text) {
        Element e = document.getElementById(Id);
        if (null != e) {
            //setTextContent is abstract
            try {
                ((SVGOMTSpanElement) e.getFirstChild()).getFirstChild().setNodeValue(Text);
            }
            catch (Exception ex) {
                log.error("can not set text", ex);
            }
        }
    }

    public void setTextAndAlign(String Id, String Text) {
        Element e = document.getElementById(Id);
        if (null != e) {
            try {
                ((SVGOMTSpanElement) e.getFirstChild()).getFirstChild().setNodeValue(Text);
            } catch (Exception ex) {
                log.error("can not set text", ex);
            }


            if (Text.length() > 1) {
                Float i = Float.parseFloat(e.getAttribute("x"));

                String new_x = String.format("%1$f", i - (Text.length() - 1) * CHAR_WIDTH);

                e.setAttribute("x", new_x);
                e.getFirstChild().getAttributes().removeNamedItem("x");
            }
        }
    }

    public void setVisibility(String Id, String Visibility) {
        Element e = document.getElementById(Id);
        if (null != e) {
            e.setAttributeNS(null, "visibility", Visibility);
        }
    }

    public void setStroke(String Id, String Color) {
        Element e = document.getElementById(Id);
        if (null != e) {
            String s = e.getAttribute("style");
            s = s.replace("stroke:#ffffff", "stroke:" + Color);
            e.setAttributeNS(null, "style", s);
        }
    }
};
