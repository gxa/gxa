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

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Pattern;

/**
 * @author Olga Melnichuk
 */
class Wro4jTagProperties {

    private boolean debug;
    private String namePattern;
    private Map<WebResourceType, String> resourcePaths = Collections.emptyMap();

    Wro4jTagProperties() {
    }

    public void load(InputStream in) throws IOException {
        if (in == null) {
            return;
        }

        Properties props = new Properties();
        try {
            props.load(in);
        } finally {
            in.close();
        }
        load(props);
    }

    public void load(Properties props) {
        resourcePaths = new HashMap<WebResourceType, String>();
        for (WebResourceType type : WebResourceType.values()) {
            String path = props.getProperty(aggregationPathPropertyName(type));
            if (path != null) {
                resourcePaths.put(type, path);
            }
        }

        debug = Boolean.parseBoolean(props.getProperty(debugPropertyName(), "false"));
        namePattern = props.getProperty(aggregationNamePatternPropertyName(), "@groupName@\\.@extension@");
    }

    public String getAggregationPath(WebResourceType type) {
        String path = resourcePaths.get(type);
        return path == null ? "" : path;
    }

    public Pattern getAggregationNamePattern(String groupName, WebResourceType resourceType) {
        String p = namePattern.replace("@groupName@", groupName);
        p = p.replace("@extension@", resourceType.ext());
        return Pattern.compile(p);
    }

    public boolean isDebugOn() {
        return debug;
    }

    static String debugPropertyName() {
        return "wro4j.tag.debug";
    }

    static String aggregationPathPropertyName(WebResourceType type) {
        return "wro4j.tag.aggregation.path." + type;
    }

    static String aggregationNamePatternPropertyName() {
        return "wro4j.tag.aggregation.name.pattern";
    }
}
