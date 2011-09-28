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

import com.google.common.base.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ro.isdc.wro.WroRuntimeException;
import ro.isdc.wro.model.factory.XmlModelFactory;
import ro.isdc.wro.model.group.InvalidGroupNameException;

import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspTagException;
import javax.servlet.jsp.tagext.TagSupport;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Set;

import static com.google.common.collect.Collections2.transform;
import static java.util.EnumSet.copyOf;

/**
 * @author Olga Melnichuk
 */
public abstract class Wro4jTag extends TagSupport {
    private static final long serialVersionUID = 201109281357L;
    private static final Logger log = LoggerFactory.getLogger(Wro4jTag.class);
    private final EnumSet<ResourceHtmlTag> tags;
    private String name;

    Wro4jTag(ResourceHtmlTag tag) {
        this(EnumSet.of(tag));
    }

    Wro4jTag(EnumSet<ResourceHtmlTag> tags) {
        this.tags = copyOf(tags);
    }

    Wro4jTagRenderer createRenderer(EnumSet<ResourceHtmlTag> tags) throws Wro4jTagException {
        return new Wro4jTagRenderer(loadWro4jConfig(), new Wro4jTagProperties(), tags, new ContextDirectoryLister());
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int doStartTag() throws JspException {
        if (name == null)
            throw jspTagException("The name parameter is mandatory");

        try {
            final HttpServletRequest request = (HttpServletRequest) pageContext.getRequest();
            createRenderer(tags).render(pageContext.getOut(), name, request.getContextPath());
        } catch (IOException e) {
            throw jspTagException(e);
        } catch (Wro4jTagException e) {
            throw jspTagException(e);
        } catch (InvalidGroupNameException e) {
            throw jspTagException("Group is not in the config file: " + name);
        }
        return SKIP_BODY;
    }

    public int doEndTag() {
        return EVAL_PAGE;
    }

    private JspTagException jspTagException(String message) throws JspTagException {
        log.error("Wro4jTag error: " + message);
        return new JspTagException("Wro4jTag error: " + message);
    }

    private JspTagException jspTagException(Exception e) {
        log.error("Wro4jTag error: " + e.getMessage(), e);
        return new JspTagException("Wro4jTag threw an exception; see logs for details");
    }

    private GroupResolver loadWro4jConfig() throws Wro4jTagException {
        try {
            return new GroupResolver(new XmlFileModelFactory().create());
        } catch (WroRuntimeException e) {
            throw new Wro4jTagException("Can't load wro4j config file", e);
        }
    }

    EnumSet<ResourceHtmlTag> getTags() {
        return tags;
    }

    private class XmlFileModelFactory extends XmlModelFactory {
        @Override
        protected InputStream getConfigResourceAsStream() throws IOException {
            return pageContext.getServletContext().getResourceAsStream("/WEB-INF/wro.xml");
        }
    }

    private class ContextDirectoryLister implements Wro4jTagRenderer.DirectoryLister {
        @Override
        public Collection<String> list(String path) {
            final String dir = ResourcePath.normalizeDirectory(path);
            @SuppressWarnings("unchecked")
            Set<String> resources = pageContext.getServletContext().getResourcePaths(dir);
            return transform(resources, new Function<String, String>() {
                @Override
                public String apply(@Nullable String input) {
                    return input == null ? null : input.substring(dir.length());
                }
            });
        }
    }
}
