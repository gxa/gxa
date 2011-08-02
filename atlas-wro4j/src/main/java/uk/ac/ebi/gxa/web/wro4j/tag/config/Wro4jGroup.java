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

import org.apache.commons.digester.annotations.rules.CallMethod;
import org.apache.commons.digester.annotations.rules.CallParam;
import org.apache.commons.digester.annotations.rules.FactoryCreate;
import uk.ac.ebi.gxa.web.wro4j.tag.WebResource;
import uk.ac.ebi.gxa.web.wro4j.tag.WebResourceType;

import java.util.*;

/**
 * @author Olga Melnichuk
 */
@FactoryCreate(pattern = "groups/group", factoryClass = Wro4jGroupCreationFactory.class)
public class Wro4jGroup {
    private final Map<WebResourceType, List<WebResource>> map = new HashMap<WebResourceType, List<WebResource>>();
    private final List<String> groupRefs = new ArrayList<String>();
    private String name;

    public Wro4jGroup(String name) {
        this.name = name;
    }

    @CallMethod(pattern = "groups/group/css")
    public void addCssResourcePath(@CallParam(pattern = "groups/group/css") String path) {
        addResource(WebResourceType.CSS, path);
    }

    @CallMethod(pattern = "groups/group/js")
    public void addJsResourcePath(@CallParam(pattern = "groups/group/js") String path) {
        addResource(WebResourceType.JS, path);
    }

    @CallMethod(pattern = "groups/group/group-ref")
    public void addGroupRef(@CallParam(pattern = "groups/group/group-ref") String groupName) {
        groupRefs.add(groupName);
    }

    public String getName() {
        return name;
    }

    private void addResource(WebResourceType type, String path) {
        List<WebResource> list = map.get(type);
        if (list == null) {
            list = new ArrayList<WebResource>();
            map.put(type, list);
        }
        list.add(new WebResource(type, path));
    }

    public Collection<WebResource> getResources(WebResourceType type) {
        List<WebResource> list = map.get(type);
        return list == null ? Collections.<WebResource>emptyList() : Collections.unmodifiableCollection(list);
    }

    public List<String> getGroupRefs() {
        return Collections.unmodifiableList(groupRefs);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Wro4jGroup)) return false;

        Wro4jGroup wroGroup = (Wro4jGroup) o;

        if (name != null ? !name.equals(wroGroup.name) : wroGroup.name != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return name != null ? name.hashCode() : 0;
    }
}
