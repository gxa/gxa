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

package uk.ac.ebi.gxa.web.tags.resourcebundle.wro4j;

import org.apache.commons.digester.Digester;
import org.apache.commons.digester.annotations.DigesterLoader;
import org.apache.commons.digester.annotations.DigesterLoaderBuilder;
import org.xml.sax.SAXException;
import uk.ac.ebi.gxa.web.tags.resourcebundle.WebResource;
import uk.ac.ebi.gxa.web.tags.resourcebundle.WebResourceBundleConfig;
import uk.ac.ebi.gxa.web.tags.resourcebundle.WebResourceBundleConfigException;
import uk.ac.ebi.gxa.web.tags.resourcebundle.WebResourceType;

import java.io.*;
import java.util.*;

/**
 * @author Olga Melnichuk
 */
public class Wro4jResourceBundleConfig implements WebResourceBundleConfig {

    private Wro4jGroups wroGroups = null;

    @Override
    public void load(File configPath) throws WebResourceBundleConfigException {
        try {
            load(new FileInputStream(configPath));
        } catch (FileNotFoundException e) {
            throw new WebResourceBundleConfigException("Wro4j config not found: " + configPath);
        }
    }

    @Override
    public void load(InputStream in) throws WebResourceBundleConfigException {
        DigesterLoader digesterLoader = new DigesterLoaderBuilder()
                .useDefaultAnnotationRuleProviderFactory()
                .useDefaultDigesterLoaderHandlerFactory();
        Digester digester = digesterLoader.createDigester(Wro4jGroups.class);
        try {
            wroGroups = (Wro4jGroups) digester.parse(in);
        } catch (IOException e) {
            throw new WebResourceBundleConfigException("Wro4j config parse error", e);
        } catch (SAXException e) {
            throw new WebResourceBundleConfigException("Wro4j config parse error", e);
        }
    }

    @Override
    public Collection<WebResource> getResources(String bundleName, Collection<WebResourceType> resourceTypes) throws WebResourceBundleConfigException {
        final List<WebResource> list = new ArrayList<WebResource>();
        traverseGroup(bundleName, new TraverseHandler(resourceTypes) {
            public boolean enoughResourcesFound(Collection<WebResource> resources) {
                list.addAll(resources);
                return false;
            }
        }, new Stack<Wro4jGroup>());

        return list;
    }

    @Override
    public boolean hasResources(String bundleName, WebResourceType resourceType) throws WebResourceBundleConfigException {
        final List<WebResource> list = new ArrayList<WebResource>();
        traverseGroup(bundleName, new TraverseHandler(Arrays.asList(resourceType)) {
            public boolean enoughResourcesFound(Collection<WebResource> resources) {
                list.addAll(resources);
                return (list.size() > 0);
            }
        }, new Stack<Wro4jGroup>());

        return (list.size() > 0);
    }

    private void traverseGroup(String groupName, TraverseHandler traverseHandler, Stack<Wro4jGroup> stack) throws WebResourceBundleConfigException {
        Wro4jGroup group = addGroup(stack, groupName);
        if (!traverseHandler.enough(group)) {
            for (String groupRef : group.getGroupRefs()) {
                traverseGroup(groupRef, traverseHandler, stack);
            }
        }
        stack.pop();
    }

    private Wro4jGroup addGroup(Stack<Wro4jGroup> stack, String groupName) throws WebResourceBundleConfigException {
        Wro4jGroup group = getGroup(groupName);

        if (stack.contains(group)) {
            throw new WebResourceBundleConfigException("Config contains cycles for wro4j group: '" + groupName + "'");
        }

        stack.push(group);
        return group;
    }

    private Wro4jGroup getGroup(String groupName) throws WebResourceBundleConfigException {
        Wro4jGroup group = wroGroups.findGroup(groupName);
        if (group == null) {
            throw new WebResourceBundleConfigException("Wro4j group not found: '" + groupName + "'");
        }
        return group;
    }

    private static abstract class TraverseHandler {
        private final List<WebResourceType> resourceTypes;

        public TraverseHandler(Collection<WebResourceType> resourceTypes) {
            this.resourceTypes = new ArrayList<WebResourceType>(resourceTypes);
        }

        boolean enough(Wro4jGroup group) {
            for (WebResourceType type : resourceTypes) {
                if (enoughResourcesFound(group.getResources(type))) {
                    return true;
                }
            }
            return false;
        }

        public abstract boolean enoughResourcesFound(Collection<WebResource> resources);
    }
}
