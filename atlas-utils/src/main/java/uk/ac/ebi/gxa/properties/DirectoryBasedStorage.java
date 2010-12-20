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

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Properties;

/**
 * Filesystem based storage implementation.
 * Can be used for large string properties, e.g. for html file snippets.
 * Each property is located in a separate file lying inside the directory specified in the directoryPath field.
 * Property name is namePrefix + fileName.
 * Can set property values, but just for the duration of current session.
 *
 * Method getAvailablePropertyNames doesn't work properly for directories inside the WAR: there is no way to list all files lying in such directory.
 *
 * @author geometer
 */
public class DirectoryBasedStorage implements Storage, AtlasPropertiesListener {
    private Properties props;
    private String directoryPathPrefix;
    private String directoryPath;
    private String namePrefix;
    private boolean external;
	private AtlasProperties atlasProperties;
    private final HashSet<String> missingProperties = new HashSet<String>();

	private String getDirectoryPathPrefix() {
		if (directoryPathPrefix == null) {
			directoryPathPrefix = atlasProperties.getConfigurationDirectoryPath();
		}
		return directoryPathPrefix;
	}

    public void setDirectoryPath(String directoryPath) {
        this.directoryPath = directoryPath;
    }

    public void setNamePrefix(String namePrefix) {
        this.namePrefix = namePrefix;
    }

    public void setExternal(String external) {
        this.external = "true".equalsIgnoreCase(external);
    }

    public void setAtlasProperties(AtlasProperties atlasProperties) {
		this.atlasProperties = atlasProperties;
		atlasProperties.registerListener(this);
	}

	public void onAtlasPropertiesUpdate(AtlasProperties atlasProperties) {
		if (directoryPathPrefix != null &&
			!directoryPathPrefix.equals(atlasProperties.getConfigurationDirectoryPath())) {
			directoryPathPrefix = atlasProperties.getConfigurationDirectoryPath();
			atlasProperties.reload();
		}
	}

    public void reload() {
        this.props = new Properties();
        this.missingProperties.clear();
    }

    public void setProperty(String name, String value) {
        if (props == null)
            reload();
        props.setProperty(name, value);
        if (value != null) {
            missingProperties.remove(name);
        } else {
            missingProperties.add(name);
        }
    }

    public String getProperty(String name) {
        if (!name.startsWith(namePrefix)) {
            return null;
        }
        if (props == null)
            reload();

        if (missingProperties.contains(name)) {
            return null;
        }

        String value = props.getProperty(name);
        if (value == null) {
            value = readValueFromFile(name);
        }

        if (value != null) {
            props.setProperty(name, value);
        } else {
            missingProperties.add(name);
        }
        return value;
    }

    private String readValueFromFile(String name) {
        final String fileName = directoryPath + '/' + name.substring(namePrefix.length());
        try {
            InputStream stream;
            if (external) {
                stream = new FileInputStream(getDirectoryPathPrefix() + '/' + fileName);
            } else {
                stream = getClass().getClassLoader().getResourceAsStream(fileName);
            }
            if (stream != null) {
                InputStreamReader reader = new InputStreamReader(stream);
                StringBuilder valueBuilder = new StringBuilder();
                char[] buf = new char[8192];
                while (true) {
                    int len = reader.read(buf);
                    if (len <= 0) {
                        break;
                    }
                    valueBuilder.append(buf, 0, len);
                }
                reader.close();
                return valueBuilder.toString();
            }
        } catch (IOException e) {
        }
        return null;
    }

    public boolean isWritePersistent() {
        return false;
    }

    /**
     * This method is currenly only used in the property editing UI.
     * The properties from this storage are not editable.
     */
    public Collection<String> getAvailablePropertyNames() {
        return Collections.<String>emptyList();
    }
}
