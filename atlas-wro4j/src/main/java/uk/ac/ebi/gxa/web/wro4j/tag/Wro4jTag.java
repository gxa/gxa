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

import com.google.common.base.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ro.isdc.wro.WroRuntimeException;
import ro.isdc.wro.model.WroModel;
import ro.isdc.wro.model.factory.XmlModelFactory;
import ro.isdc.wro.model.group.Group;
import ro.isdc.wro.model.group.InvalidGroupNameException;
import ro.isdc.wro.model.resource.Resource;

import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspTagException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.tagext.TagSupport;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

import static com.google.common.collect.Collections2.filter;
import static java.util.EnumSet.copyOf;

/**
 * @author Olga Melnichuk
 */
public abstract class Wro4jTag extends TagSupport {
    private static final long serialVersionUID = 201109270813L;

    private static final Logger log = LoggerFactory.getLogger(Wro4jTag.class);
    private static final Wro4jTagProperties properties = new Wro4jTagProperties();

    private final EnumSet<ResourceHtmlTag> tags;

    private String name;

    Wro4jTag(ResourceHtmlTag tag) {
        this(EnumSet.of(tag));
    }

    Wro4jTag(EnumSet<ResourceHtmlTag> tags) {
        this.tags = copyOf(tags);
    }

    public boolean isSupported(Resource resource) {
        if (resource == null)
            return false;
        for (ResourceHtmlTag tag : tags) {
            if (resource.getType() == tag.getType())
                return true;
        }
        return false;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int doStartTag() throws JspException {
        try {
            WroModel model = loadWro4jConfig();
            for (Resource resource : collectResources(model.getGroupByName(name))) {
                out().write(render(resource));
                out().write("\n");
            }
        } catch (IOException e) {
            throw jspTagException(e);
        } catch (Wro4jTagException e) {
            throw jspTagException(e);
        } catch (InvalidGroupNameException e) {
            throw jspTagException(new Wro4jTagException("Group is not in the config file: " + name));
        }
        return SKIP_BODY;
    }

    public int doEndTag() {
        return EVAL_PAGE;
    }

    private String render(Resource resource) {
        return ResourceHtmlTag.forType(resource.getType()).render(ResourcePath.join(getContextPath(), resource.getUri()));
    }

    private Collection<Resource> collectResources(final Group group) throws Wro4jTagException {
        return properties.isDebugOn() ? uncompressedResources(group) : compressedBundle(group);

    }

    private Collection<Resource> compressedBundle(Group group) throws Wro4jTagException {
        List<Resource> list = new ArrayList<Resource>();
        for (ResourceHtmlTag type : tags)
            if (group.hasResourcesOfType(type.getType()))
                list.add(resourceForBundle(group, type));
        return list;
    }

    private Collection<Resource> uncompressedResources(Group group) {
        return filter(group.getResources(), new Predicate<Resource>() {
            @Override
            public boolean apply(@Nullable Resource resource) {
                return isSupported(resource);
            }
        });
    }

    private Resource resourceForBundle(Group group, ResourceHtmlTag tag) throws Wro4jTagException {
        final String template = properties.getNameTemplate().forGroup(group.getName(), tag);
        String path = ResourcePath.normalizeDirectory(properties.getResourcePath(tag.getType()));
        @SuppressWarnings("unchecked")
        Set<String> resources = pageContext.getServletContext().getResourcePaths(path);
        for (String resource : resources) {
            if (resource.substring(path.length()).matches(template)) {
                return Resource.create(resource, tag.getType());
            }
        }
        throw new Wro4jTagException("No file matching the template: '" + template +
                "' found in the path: " + properties.getResourcePath(tag.getType()) +
                " - have you built the compressed versions properly?");
    }

    private JspTagException jspTagException(Exception e) {
        log.error("Wro4jTag error: " + e.getMessage(), e);
        return new JspTagException("Wro4jTag threw an exception; see logs for details");
    }

    private WroModel loadWro4jConfig() throws Wro4jTagException {
        try {
            return getModelFactory().create();
        } catch (WroRuntimeException e) {
            throw new Wro4jTagException("Can't load wro4j config file", e);
        }
    }

    private XmlModelFactory getModelFactory() {
        return new XmlModelFactory() {
            @Override
            protected InputStream getConfigResourceAsStream() throws IOException {
                return pageContext.getServletContext().getResourceAsStream("/WEB-INF/wro.xml");
            }
        };
    }

    private String getContextPath() {
        HttpServletRequest httpServletRequest = (HttpServletRequest) pageContext.getRequest();
        return httpServletRequest.getContextPath();
    }

    private JspWriter out() {
        return pageContext.getOut();
    }

    EnumSet<ResourceHtmlTag> getTags() {
        return tags;
    }
}
