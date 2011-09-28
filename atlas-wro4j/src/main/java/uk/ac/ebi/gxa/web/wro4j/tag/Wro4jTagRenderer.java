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
import ro.isdc.wro.model.WroModel;
import ro.isdc.wro.model.group.Group;
import ro.isdc.wro.model.resource.Resource;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.Writer;
import java.util.*;

import static com.google.common.collect.Collections2.filter;
import static java.util.EnumSet.copyOf;

/**
 * @author alf
 */
public class Wro4jTagRenderer {
    private final WroModel model;
    private final Wro4jTagProperties properties;
    private final EnumSet<ResourceHtmlTag> tags;
    private final DirectoryLister lister;

    public Wro4jTagRenderer(WroModel model, Wro4jTagProperties properties, EnumSet<ResourceHtmlTag> tags, DirectoryLister lister) {
        this.model = model;
        this.properties = properties;
        this.tags = copyOf(tags);
        this.lister = lister;
    }

    public void render(Writer writer, String name, String contextPath) throws IOException, Wro4jTagException {
        for (Resource resource : collectResources(model.getGroupByName(name))) {
            writer.write(render(contextPath, resource));
            writer.write("\n");
        }
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
        String path = properties.getResourcePath(tag.getType());
        for (String filename : lister.list(path)) {
            if (filename.matches(template)) {
                return Resource.create(filename, tag.getType());
            }
        }
        throw new Wro4jTagException("No file matching the template: '" + template +
                "' found in the path: " + properties.getResourcePath(tag.getType()) +
                " - have you built the compressed versions properly?");
    }

    private String render(String contextPath, Resource resource) {
        final String uri = ResourcePath.join(contextPath, resource.getUri());
        return ResourceHtmlTag.forType(resource.getType()).render(uri);
    }

    boolean isSupported(Resource resource) {
        for (ResourceHtmlTag tag : tags) {
            if (resource.getType() == tag.getType())
                return true;
        }
        return false;
    }

    EnumSet<ResourceHtmlTag> getTags() {
        return copyOf(tags);
    }

    static interface DirectoryLister {
        Collection<String> list(String path);
    }
}
