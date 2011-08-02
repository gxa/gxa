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

package uk.ac.ebi.gxa.web.wro4j.tag.config;

import org.xml.sax.SAXException;
import uk.ac.ebi.gxa.web.wro4j.tag.WebResource;
import uk.ac.ebi.gxa.web.wro4j.tag.WebResourceType;

import java.io.*;
import java.util.*;

/**
 * @author Olga Melnichuk
 */
public class Wro4jConfig {

    private Wro4jGroups wroGroups = null;

    /**
     * Loads wro group definitions from a config file.
     *
     * @param configPath a path to the config file
     * @throws Wro4jConfigException if any error happened during the config loading
     */
    public void load(File configPath) throws Wro4jConfigException {
        try {
            load(new FileInputStream(configPath));
        } catch (FileNotFoundException e) {
            throw new Wro4jConfigException("Wro4j config not found: " + configPath);
        }
    }

    /**
     * Loads wro group definitions from an input stream.
     *
     * @param in an InputStream to parse
     * @throws Wro4jConfigException if any error happened during the config loading
     */
    public void load(InputStream in) throws Wro4jConfigException {
        try {
            wroGroups = Wro4jConfigParser.parse(in);
        } catch (IOException e) {
            throw new Wro4jConfigException("Wro4j config parse error", e);
        } catch (SAXException e) {
            throw new Wro4jConfigException("Wro4j config parse error", e);
        }
    }

    /**
     * Returns resources of given types for a specified group name.
     *
     * @param groupName     a group name to get resources from
     * @param resourceTypes types of resources to get
     * @return a collection of web resources found
     * @throws Wro4jConfigException if a cyclic path exists; or the group not found
     */
    public Collection<WebResource> getResources(String groupName, Collection<WebResourceType> resourceTypes) throws Wro4jConfigException {
        final List<WebResource> list = new ArrayList<WebResource>();
        traverseGroup(groupName, new TraverseHandler(resourceTypes) {
            public boolean enoughResourcesFound(Collection<WebResource> resources) {
                list.addAll(resources);
                return false;
            }
        }, new Stack<Wro4jGroup>());

        return list;
    }

    /**
     * Checks if the group contains at least one resource of required type.
     *
     * @param groupName    a group name to check for
     * @param resourceType web resource type to check for
     * @return true if at least one resource of given type exists
     * @throws Wro4jConfigException if a cyclic path exists; or the group not found
     */
    public boolean hasResources(String groupName, WebResourceType resourceType) throws Wro4jConfigException {
        final List<WebResource> list = new ArrayList<WebResource>();
        traverseGroup(groupName, new TraverseHandler(Arrays.asList(resourceType)) {
            public boolean enoughResourcesFound(Collection<WebResource> resources) {
                list.addAll(resources);
                return (list.size() > 0);
            }
        }, new Stack<Wro4jGroup>());

        return (list.size() > 0);
    }

    private void traverseGroup(String groupName, TraverseHandler traverseHandler, Stack<Wro4jGroup> stack) throws Wro4jConfigException {
        Wro4jGroup group = addGroup(stack, groupName);
        if (!traverseHandler.enough(group)) {
            for (String groupRef : group.getGroupRefs()) {
                traverseGroup(groupRef, traverseHandler, stack);
            }
        }
        stack.pop();
    }

    private Wro4jGroup addGroup(Stack<Wro4jGroup> stack, String groupName) throws Wro4jConfigException {
        Wro4jGroup group = getGroup(groupName);

        if (stack.contains(group)) {
            throw new Wro4jConfigException("A cyclic path found for wro4j group: '" + groupName + "'");
        }

        stack.push(group);
        return group;
    }

    private Wro4jGroup getGroup(String groupName) throws Wro4jConfigException {
        Wro4jGroup group = wroGroups.findGroup(groupName);
        if (group == null) {
            throw new Wro4jConfigException("Wro4j group not found: '" + groupName + "'");
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
