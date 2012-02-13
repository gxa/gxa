/*
 * Copyright 2008-2012 Microarray Informatics Team, EMBL-European Bioinformatics Institute
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

package uk.ac.ebi.gxa.R;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import static com.google.common.io.Closeables.closeQuietly;
import static java.lang.System.setProperty;

/**
 * @author Olga Melnichuk
 */
public class BiocepPropertiesUtils {

    private enum BiocepProperty {
        NamingMode("biocep.naming.mode", "naming.mode"),
        ProviderFactory("biocep.provider.factory", "pools.provider.factory"),
        DbType("biocep.db.type", "pools.dbmode.type"),
        DbDriver("biocep.db.driver", "pools.dbmode.driver"),
        DbUser("biocep.db.user", "pools.dbmode.user"),
        DbPassword("biocep.db.password", "pools.dbmode.password"),
        DefaultPool("biocep.defaultpool", "pools.dbmode.defaultpool"),
        KillUsed("biocep.killused", "pools.dbmode.killused"),
        DbUrl("biocep.db.url") {
            @Override
            public void setup(String value) throws BiocepPropertiesSetupException {
                if (!value.contains("@")) {
                    throw new BiocepPropertiesSetupException("Invalid database connection string: " + value);
                }

                String[] tokens = value.split(":");

                int hostIndex = 0;
                String host = null;
                for (String token : tokens) {
                    if (token.startsWith("@")) {
                        host = token.replaceFirst("@", "");
                        break;
                    }
                    hostIndex++;
                }

                if (host == null) {
                    throw new BiocepPropertiesSetupException("Invalid database connection string: " + value + ". No host specified.");
                }

                String port = "1521";
                if (tokens.length > hostIndex + 1) {
                    port = tokens[hostIndex + 1];
                }

                String name = tokens[tokens.length - 1];
                setProperty("pools.dbmode.host", host);
                setProperty("pools.dbmode.port", port);
                setProperty("pools.dbmode.name", name);
            }
        };

        private final String name;
        private final String systemName;

        private BiocepProperty(String name, String systemName) {
            this.name = name;
            this.systemName = systemName;
        }

        private BiocepProperty(String name) {
            this(name, null);
        }

        public Properties set(String value, Properties properties) {
            properties.setProperty(name, value);
            return properties;
        }

        void setup(Properties properties) throws BiocepPropertiesSetupException {
            String value = properties.getProperty(name);
            if (value == null) {
                throw new BiocepPropertiesSetupException("Required property '" + name + "' was not found");
            }
            setup(value);
        }

        void setup(String value) throws BiocepPropertiesSetupException {
            if (systemName != null) {
                setProperty(systemName, value);
            }
        }

        public static void setupAll(Properties properties) throws BiocepPropertiesSetupException {
            for (BiocepProperty p : values()) {
                p.setup(properties);
            }
        }
    }

    /**
     * Creates a single "killUsed" property {@link Properties} object.
     *
     * @param value a boolean value to assign to the property
     * @return a {@link Properties} object with a single property: "killUsed"
     */
    public static Properties killUsed(boolean value) {
        return killUsed(value, new Properties());
    }

    public static Properties killUsed(boolean value, @Nonnull Properties properties) {
        return BiocepProperty.KillUsed.set(Boolean.toString(value), properties);
    }

    /**
     * Installs the user defined properties to the system using <code>System.setProperty()</code>
     *
     * @param userDefined properties to be setup; these properties override the default values if any exist
     * @throws BiocepPropertiesSetupException - if a required property is not found in user properties and defaults;
     *                                        - if a value format is not valid (e.g. database url)
     *                                        - if any error happened during reading default properties
     */
    public static void biocepPropertiesSetup(Properties userDefined) throws BiocepPropertiesSetupException {
        Properties defaults;
        try {
            defaults = defaults();
        } catch (IOException e) {
            throw new BiocepPropertiesSetupException("Failed to load default biocep.properties", e);
        }

        Properties properties = new Properties(defaults);
        if (userDefined != null) {
            properties.putAll(userDefined);
        }
        BiocepProperty.setupAll(properties);
    }

    private static Properties defaults() throws IOException {
        Properties properties = new Properties();
        InputStream in = BiocepPropertiesUtils.class.getClassLoader().getResourceAsStream("biocep.properties");
        if (in != null) {
            try {
                properties.load(in);
            } finally {
                closeQuietly(in);
            }
        }
        return properties;
    }

}
