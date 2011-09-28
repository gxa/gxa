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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ro.isdc.wro.model.resource.ResourceType;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import static com.google.common.io.Closeables.closeQuietly;

/**
 * @author Olga Melnichuk
 */
class Wro4jTagProperties {
    private static final Logger log = LoggerFactory.getLogger(Wro4jTagProperties.class);
    private static final String TAG_PROPERTIES = "wro4j-tag.properties";

    private final Properties props = new Properties();

    Wro4jTagProperties() {
        InputStream in = null;
        try {
            in = Wro4jTag.class.getClassLoader().getResourceAsStream(TAG_PROPERTIES);
            if (in == null) {
                log.error(TAG_PROPERTIES + " not found in the classpath");
                throw new IllegalStateException("Cannot load properties");
            }

            props.load(in);
        } catch (IOException e) {
            log.error("Wro4jTag error: " + TAG_PROPERTIES + " not loaded", e);
            throw new IllegalStateException("Cannot load properties");
        } finally {
            closeQuietly(in);
        }
    }

    public String getResourcePath(ResourceType type) {
        return props.getProperty("wro4j.tag.aggregation.path." + type);
    }

    public BundleNameTemplate getNameTemplate() {
        return new BundleNameTemplate(props.getProperty("wro4j.tag.aggregation.name.pattern", null));
    }

    public boolean isDebugOn() {
        return Boolean.parseBoolean(props.getProperty("wro4j.tag.debug", "false"));
    }
}
