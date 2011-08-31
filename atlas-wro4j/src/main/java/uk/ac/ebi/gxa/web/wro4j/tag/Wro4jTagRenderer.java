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
import ro.isdc.wro.model.group.Group;
import ro.isdc.wro.model.resource.Resource;
import ro.isdc.wro.model.resource.ResourceType;

import javax.annotation.Nonnull;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.PageContext;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author Olga Melnichuk
 */
abstract class Wro4jTagRenderer {

    private final static Logger log = LoggerFactory.getLogger(Wro4jTagRenderer.class);

    private static final Wro4jTagProperties properties = new Wro4jTagProperties();

        static {
            String tagProperties = "wro4j-tag.properties";
            try {
                InputStream in = Wro4jTag.class.getClassLoader().getResourceAsStream(tagProperties);
                if (in == null) {
                    log.error(tagProperties + " not found in the classpath");
                } else {
                    properties.load(in);
                }
            } catch (IOException e) {
                log.error("Wro4jTag error: " + tagProperties + " not loaded", e);
            }
        }

    private final PageContext pageContext;

    private Wro4jTagRenderer(PageContext pageContext) {
        this.pageContext = pageContext;
    }

    public static Wro4jTagRenderer create(PageContext pageContext, Collection<ResourceType> resourceTypes) {
        return create(properties.isDebugOn(), pageContext,  resourceTypes);
    }

    public static Wro4jTagRenderer create(boolean debug, final PageContext pageContext, final Collection<ResourceType> resourceTypes) {
        return debug ?
                new Wro4jTagRenderer(pageContext) {
                    @Override
                    public void render(@Nonnull Group group) throws IOException {
                        List<Resource> list = new ArrayList<Resource>();
                        for (Resource resource : group.getResources()) {
                            if (resourceTypes.contains(resource.getType())) {
                                list.add(resource);
                            }
                        }
                        render(list);
                    }
                } :
                new Wro4jTagRenderer(pageContext) {
                    @Override
                    public void render(@Nonnull Group group) throws IOException, Wro4jTagException {
                        List<Resource> list = new ArrayList<Resource>();
                        for (ResourceType type : resourceTypes) {
                            if (group.hasResourcesOfType(type)) {
                                list.add(Resource.create(groupUri(group, type), type));
                            }
                        }
                        render(list);
                    }

                    private String groupUri(Group group, ResourceType type) throws Wro4jTagException {
                        AggregatedResourcePath aggregationPath = properties.getAggregationPath(type);
                        return aggregationPath
                                .relativeTo(pageContext)
                                .findOne(group.getName());
                    }
                };
    }

    public abstract void render(@Nonnull Group group) throws IOException, Wro4jTagException;

    protected void render(Collection<Resource> resources) throws IOException {
        StringBuilder sb = new StringBuilder();
        for (Resource resource : resources) {
            sb.append(
                    toHtml(resource)
            ).append("\n");
        }

        pageContext.getOut().write(sb.toString());
    }

    private String toHtml(Resource resource) {
        return ResourceHtmlTag.of(resource.getType()).asString(relativeToContextPath(resource.getUri()));
    }

    private String relativeToContextPath(String uri) {
        return ResourcePath.join(((HttpServletRequest) pageContext.getRequest()).getContextPath(), uri);
    }
}
