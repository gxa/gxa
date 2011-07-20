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

package uk.ac.ebi.gxa.web.tags.resourcebundle;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * @author Olga Melnichuk
 */
class WebResourceBundleProperties {

    private boolean debug;
    private Map<WebResourceType, String> resourcePaths = Collections.emptyMap();

    WebResourceBundleProperties() {
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
            String path = props.getProperty("resourcebundle.path." + type);
            if (path != null) {
                resourcePaths.put(type, path);
            }
        }

        debug = Boolean.parseBoolean(props.getProperty("resourcebundle.debug", "false"));
    }

    public String getBundlePath(WebResourceType type) {
        String path = resourcePaths.get(type);
        return path == null ? "" : path;
    }

    public boolean isDebugOn() {
        return debug;
    }
}
