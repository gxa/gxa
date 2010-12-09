/*
 * Copyright 2008-2010 Microarray Informatics Team, EMBL-European Bioinformatics Institute
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
package uk.ac.ebi.gxa.properties;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * Resource .properties file storage implementation. Can set property values, but just for the duration of current session
 *
 * @author pashky
 */
public class ResourceFileStorage implements Storage {
    private Properties props;
    private String resourcePath;

    public void setResourcePath(String resourcePath) {
        this.resourcePath = resourcePath;
    }

    public void reload() {
        this.props = new Properties();
        InputStream stream = null;
        try {
            stream = getClass().getClassLoader().getResourceAsStream(resourcePath);
            if (stream != null) {
                this.props.load(stream);
            }
        } catch (IOException e) {
            throw new RuntimeException("Can't load properties file " + resourcePath);
        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException ignored) {
                }
            }
        }
    }

    public void setProperty(String name, String value) {
        if (props == null)
            reload();
        props.setProperty(name, value);
    }

    public String getProperty(String name) {
        if (props == null)
            reload();
        return props.getProperty(name);
    }

    public boolean isWritePersistent() {
        return false;
    }

    public Collection<String> getAvailablePropertyNames() {
        if (props == null)
            reload();

        List<String> result = new ArrayList<String>();
        for (Enumeration keyi = props.keys(); keyi.hasMoreElements();)
            result.add(keyi.nextElement().toString());
        return result;
    }
}
