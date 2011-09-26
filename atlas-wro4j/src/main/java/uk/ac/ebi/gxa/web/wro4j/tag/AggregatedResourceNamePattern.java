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

import java.util.EnumMap;

/**
 * @author Olga Melnichuk
 */
class AggregatedResourceNamePattern {
    private static final String DEFAULT_PATTERN = "@groupName@\\.@extension@";
    private static final EnumMap<ResourceType, String> fileExtensions =
            new EnumMap<ResourceType, String>(ResourceType.class) {{
                put(ResourceType.CSS, "css");
                put(ResourceType.JS, "js");
            }};

    private final String namePattern;

    public AggregatedResourceNamePattern(String namePattern, ResourceType type) {
        namePattern = namePattern == null ? DEFAULT_PATTERN : namePattern.replaceAll("\\.", "\\\\\\.");
        String ext = fileExtensions.get(type);
        if (ext == null) {
            throw new IllegalArgumentException("Unrecognized resource type: " + type);
        }
        this.namePattern = namePattern.replace("@extension@", ext);
    }

    public String pattern(String groupName) {
        return namePattern.replace("@groupName@", groupName);
    }
}
