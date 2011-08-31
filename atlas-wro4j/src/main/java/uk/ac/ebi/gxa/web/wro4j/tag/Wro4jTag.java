/*
 * Copyright 2008-2011 Microarray Informatics Team, EMBL-European Bioinformatics Institute
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

package uk.ac.ebi.gxa.web.wro4j.tag;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ro.isdc.wro.WroRuntimeException;
import ro.isdc.wro.model.WroModel;
import ro.isdc.wro.model.group.InvalidGroupNameException;
import ro.isdc.wro.model.resource.ResourceType;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspTagException;
import javax.servlet.jsp.tagext.TagSupport;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author Olga Melnichuk
 */
public class Wro4jTag extends TagSupport {

    private static final long serialVersionUID = 6184065535320355872L;

    private final static Logger log = LoggerFactory.getLogger(Wro4jTag.class);

    private String name;

    private final List<ResourceType> resourceTypes = new ArrayList<ResourceType>();

    public Wro4jTag() {
        this(ResourceType.values());
    }

    Wro4jTag(ResourceType... types) {
        resourceTypes.addAll(Arrays.asList(types));
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int doStartTag() throws JspException {
        try {
            writeHtml();
        } catch (IOException e) {
            throw jspTagException(e);
        } catch (Wro4jTagException e) {
            throw jspTagException(e);
        }
        return SKIP_BODY;
    }

    public int doEndTag() {
        return EVAL_PAGE;
    }

    private JspTagException jspTagException(Exception e) {
        log.error("Wro4jTag error: " + e.getMessage(), e);
        return new JspTagException("Wro4jTag threw an exception; see logs for details");
    }

    private void writeHtml() throws Wro4jTagException, IOException {
        WroModel wroModel = loadWro4jConfig();
        Wro4jTagRenderer renderer = Wro4jTagRenderer.create(pageContext, resourceTypes);
        try {
            renderer.render(wroModel.getGroupByName(name));
        } catch (InvalidGroupNameException e) {
            throw new Wro4jTagException("Group is not in the config file: " + name);
        }
    }

    private WroModel loadWro4jConfig() throws Wro4jTagException {
        File webInf = new File(pageContext.getServletContext().getRealPath("/"), "WEB-INF");
        File wro4jConfigPath = new File(webInf, "wro.xml");
        try {
            return new Wro4jXmlModelFactory(wro4jConfigPath).create();
        } catch (WroRuntimeException e) {
            throw new Wro4jTagException("Can't load wro4j config file", e);
        }
    }
}
