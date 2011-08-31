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

import ro.isdc.wro.model.resource.ResourceType;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * @author Olga Melnichuk
 */
class AggregatedResourceNamePattern {
    private static final String DEFAULT_PATTERN = "@groupName@\\.@extension@";
    private static final Map<ResourceType, String> fileExtensions = Collections.unmodifiableMap(new HashMap<ResourceType, String>() {{
        put(ResourceType.CSS, "css");
        put(ResourceType.JS, "js");
    }});

    private final String namePattern;

    public AggregatedResourceNamePattern(String namePattern, ResourceType type) {
        if (namePattern == null) {
            namePattern = DEFAULT_PATTERN;
        }
        String ext = fileExtensions.get(type);
        if (ext == null) {
            throw new IllegalStateException("Unrecognized resource type: " + type);
        }
        this.namePattern = namePattern.replace("@extension@", ext);
    }

    public Pattern compile(String groupName) {
        String s = namePattern.replace("@groupName@", groupName);
        return Pattern.compile(s);
    }
}
