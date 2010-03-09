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
package ae3.util;

import uk.ac.ebi.gxa.properties.AtlasProperties;

import javax.servlet.jsp.tagext.TagSupport;
import javax.servlet.jsp.JspException;
import java.io.IOException;

import org.apache.commons.lang.StringEscapeUtils;


/**
 * @author pashky
 */
public class CuratedNameTag extends TagSupport {
    private String ef;
    private String geneProp;
    private String escape;

    public void setEf(String ef) {
        this.ef = ef;
    }

    public void setGeneProp(String geneProp) {
        this.geneProp = geneProp;
    }

    public void setEscape(String escape) {
        this.escape = escape;
    }

    @Override
    public int doStartTag() throws JspException {
        try {
            AtlasProperties atlasProperties = (AtlasProperties)pageContext.getServletContext().getAttribute("atlasProperties");
            String text =
                    ef != null ? atlasProperties.getCuratedEf(ef) :
                    geneProp != null ? atlasProperties.getCuratedGeneProperty(geneProp) : null;
            if("js".equals(escape))
                StringEscapeUtils.escapeJavaScript(pageContext.getOut(), text);
            else if("xml".equals(escape))
                StringEscapeUtils.escapeHtml(pageContext.getOut(), text);
            else
                pageContext.getOut().print(text);
        } catch (IOException ioe) {
            throw new JspException("I/O Error : " + ioe.getMessage());
        }
        return SKIP_BODY;
    }
}
