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

import java.util.HashMap;
import java.util.Map;

/**
 * @author Olga Melnichuk
 */
abstract class ResourceHtmlTag {

    private static final Map<ResourceType, ResourceHtmlTag> tags =
            new HashMap<ResourceType, ResourceHtmlTag>() {{
                put(ResourceType.CSS, new ResourceHtmlTag() {
                    @Override
                    public String asString(String src) {
                        return "<link type=\"text/css\" rel=\"stylesheet\" href=\"" + src + "\"/>";
                    }
                });
                put(ResourceType.JS, new ResourceHtmlTag() {
                    @Override
                    public String asString(String src) {
                        return "<script type=\"text/javascript\" src=\"" + src + "\"></script>";
                    }
                });
            }};

    private ResourceHtmlTag() {
    }

    public abstract String asString(String src);

    public static ResourceHtmlTag of(ResourceType type) {
        ResourceHtmlTag tag = tags.get(type);
        if (tag == null) {
            throw new IllegalStateException("Unsupported resource type: " + type);
        }
        return tag;
    }
}
