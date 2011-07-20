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
import uk.ac.ebi.gxa.web.tags.resourcebundle.*;

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
        return getResources(bundleName, resourceTypes, new Stack<Wro4jGroup>());
    }

    @Override
    public void assertConfigured(String bundleName) throws WebResourceBundleConfigException {
        getGroup(bundleName);
    }

    private Collection<WebResource> getResources(String bundleName, Collection<WebResourceType> resourceTypes, Stack<Wro4jGroup> stack) throws WebResourceBundleConfigException {
        Wro4jGroup group = addGroup(stack, bundleName);
        List<WebResource> list = new ArrayList<WebResource>();
        for (WebResourceType type : resourceTypes) {
            list.addAll(group.getResources(type));
        }
        for (String groupRef : group.getGroupRefs()) {
            list.addAll(getResources(groupRef, resourceTypes, stack));
        }

        stack.pop();
        return list;

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
}
