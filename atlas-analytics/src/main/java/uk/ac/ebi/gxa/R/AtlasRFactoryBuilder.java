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

package uk.ac.ebi.gxa.R;

import uk.ac.ebi.gxa.exceptions.LogUtil;

import java.io.IOException;
import java.util.Properties;

/**
 * A singleton class that is reponsible for build {@link uk.ac.ebi.gxa.R.AtlasRFactory} objects.  The type of factory
 * produced will depend on the parameters specified.
 *
 * @author Tony Burdett
 * @see uk.ac.ebi.gxa.R.AtlasRFactory
 */
public class AtlasRFactoryBuilder {
    private static AtlasRFactoryBuilder factoryBuilder = new AtlasRFactoryBuilder();

    public static AtlasRFactoryBuilder getAtlasRFactoryBuilder() {
        return factoryBuilder;
    }

    private AtlasRFactoryBuilder() {
        // singleton, so private constructor
    }

    /**
     * Creates an AtlasRFactory, which can be used to obtain RServices.  Invoking this method is equivalent to invoking
     * {@link #buildAtlasRFactory(RType, java.util.Properties)} where the RType is attempted to be discovered by reading
     * the file R.properties on the current classpath, and the properties are empty.  If the chosen concrete
     * implementation absolutely requires properties to be set, and these properties can not be discovered either from
     * {@link System}<code>.getProperties()</code> or by reading a dedicated properties file from the classpath, then an
     * {@link UnsupportedOperationException} should be thrown.
     *
     * @return an AtlasRFactory,  configured to generate RServices for the current environment
     * @throws InstantiationException if the type of R installation could not be discovered from properties on the
     *                                current path, or if there is a hard requirement for configuration elements that
     *                                could not be discovered from system properties or classpath properties files.
     */
    public AtlasRFactory buildAtlasRFactory() throws InstantiationException {
        try {
            // no specifed RType enum - read properties file to assess type
            if (getClass().getClassLoader().getResourceAsStream("R.properties") == null) {
                throw new InstantiationException(
                        "R.properties file absent - cannot automatically configure R environment");
            }
            else {
                Properties rProps = new Properties();
                rProps.load(getClass().getClassLoader().getResourceAsStream("R.properties"));

                if (rProps.containsKey("r.env.type")) {
                    String rTypeStr = rProps.getProperty("r.env.type");
                    for (RType rType : RType.values()) {
                        if (rTypeStr.equals(rType.key())) {
                            return buildAtlasRFactory(rType);
                        }
                    }
                    throw new InstantiationException("r.env.type value '" + rTypeStr + "' in R.properties is " +
                            "not recognised as a valid R environment");
                }
                throw new InstantiationException("r.env.type property not found in R.properties");
            }
        }
        catch (IOException e) {
            throw new InstantiationException("Unable to instantiate AtlasRFactoryBuilder - no R.properties file found");
        }
    }

    /**
     * Creates an AtlasRFactory, which can be used to obtain RServices.  Invoking this method is equivalent to invoking
     * {@link #buildAtlasRFactory(RType, java.util.Properties)} with an empty set of properties, where possible.  If the
     * chosen concrete implementation absolutely requires properties to be set, and these properties can not be
     * discovered from {@link System}<code>.getProperties()</code>, then an {@link UnsupportedOperationException} should
     * be thrown.
     *
     * @param rType the type of R installation
     * @return an AtlasRFactory,  configured to generate RServices from the current environment
     * @throws InstantiationException if the type of R installation requested has a hard requirement on configuration
     *                                elements that could not be discovered from system properties or classpath
     *                                properties files
     */
    public AtlasRFactory buildAtlasRFactory(RType rType) throws InstantiationException {
        return buildAtlasRFactory(rType, null);
    }

    /**
     * Creates an AtlasRFactory, which can be used to obtain RServices.  If the chosen concrete implementation
     * absolutely requires properties to be set, and these properties can not be discovered from either the supplied
     * properties object or {@link System}<code>.getProperties()</code>, then an {@link UnsupportedOperationException}
     * should be thrown.
     *
     * @param rType      the type of R installation
     * @param properties a properties object containing required configuration elements for this factory
     * @return an AtlasRFactory,  configured to generate RServices from the current environment
     * @throws InstantiationException if the type of R installation requested has a hard requirement on configuration
     *                                elements that could not be discovered from system properties or classpath
     *                                properties files
     */
    public AtlasRFactory buildAtlasRFactory(RType rType, Properties properties) throws InstantiationException {
        switch (rType) {
            case LOCAL:
                // if we pass R properties here, read them in
                if (properties != null) {
                    setLocalSystemProperties(properties);
                }

                return new LocalAtlasRFactory();
            case BIOCEP:
                if (properties != null) {
                    // set any properties passed in our properties object
                    setBiocepSystemProperties(properties);
                }
                else {
                    try {
                        // try and read from properties file
                        Properties biocepProperties = new Properties();
                        biocepProperties.load(getClass().getClassLoader().getResourceAsStream("biocep.properties"));
                        setBiocepSystemProperties(biocepProperties);
                    }
                    catch (IOException e) {
                        throw new InstantiationException("Cannot initialize AtlasRFactory for " + rType +
                                " with null properties and without a biocep.properties file on the classpath");
                    }
                }

                return new BiocepAtlasRFactory();
            default:
                throw new InstantiationException("Unrecognised type: " + rType);
        }
    }

    private void setLocalSystemProperties(Properties properties) throws InstantiationException {
        if (properties.getProperty("R_HOME") == null) {
            throw new InstantiationException("Supplied properties don't contain required property 'R_HOME'");
        }
        System.setProperty("R_HOME", properties.getProperty("R_HOME"));
    }

    private void setRemoteSystemProperties(Properties properties) throws InstantiationException {
        if (properties.getProperty("R.remote.host") == null) {
            throw new InstantiationException("Supplied properties don't contain required property 'R.remote.host'");
        }
        System.setProperty("R.remote.host", properties.getProperty("R.remote.host"));
    }

    private void setBiocepSystemProperties(Properties biocepProps) throws InstantiationException {
        // databaseURL should be something like "jdbc:oracle:thin:@www.myhost.com:1521:MYDATABASE"
        if (biocepProps.getProperty("biocep.db.url") == null) {
            throw new InstantiationException("Supplied properties don't contain required property 'biocep.db.url'");
        }
        String databaseURL = biocepProps.getProperty("biocep.db.url");

        if (!databaseURL.contains("@")) {
            throw LogUtil.createUnexpected("No '@' found in the database URL - database connection string " +
                    "isn't using JDBC oracle-thin driver?");
        }

        // split the url up on ":" char
        String[] tokens = databaseURL.split(":");

        // host is the token that begins with '@'
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
            throw new InstantiationException("Could not read host from the database URL");
        }

        // port is the bit immediately after the host (if present - and if not, use 1521)
        String port;
        // last token is database name - so if there are more tokens between host and name, this is the port
        if (tokens.length > hostIndex + 1) {
            port = tokens[hostIndex + 1];
        }
        else {
            port = "1521";
        }

        // name is the database name - this is the bit at the end
        String name = tokens[tokens.length - 1];

        // customized DB location, parsed from URL string
        System.setProperty(
                "pools.dbmode.host",
                host);
        System.setProperty(
                "pools.dbmode.port",
                port);
        System.setProperty(
                "pools.dbmode.name",
                name);

        // username and password properties, which has to be duplicated in context.xml and in biocep.properties
        if (biocepProps.getProperty("biocep.db.user") == null) {
            throw new InstantiationException("Supplied properties don't contain required property 'biocep.db.user'");
        }
        System.setProperty(
                "pools.dbmode.user",
                biocepProps.getProperty("biocep.db.user"));
        if (biocepProps.getProperty("biocep.db.password") == null) {
            throw new InstantiationException(
                    "Supplied properties don't contain required property 'biocep.db.password'");
        }
        System.setProperty(
                "pools.dbmode.password",
                biocepProps.getProperty("biocep.db.password"));

        // standard config, probably won't normally change
        if (biocepProps.getProperty("biocep.naming.mode") == null) {
            throw new InstantiationException(
                    "Supplied properties don't contain required property 'biocep.naming.mode'");
        }
        System.setProperty(
                "naming.mode",
                biocepProps.getProperty("biocep.naming.mode"));
        if (biocepProps.getProperty("biocep.provider.factory") == null) {
            throw new InstantiationException(
                    "Supplied properties don't contain required property 'biocep.provider.factory'");
        }
        System.setProperty(
                "pools.provider.factory",
                biocepProps.getProperty("biocep.provider.factory"));
        if (biocepProps.getProperty("biocep.db.type") == null) {
            throw new InstantiationException("Supplied properties don't contain required property 'biocep.db.type'");
        }
        System.setProperty(
                "pools.dbmode.type",
                biocepProps.getProperty("biocep.db.type"));
        if (biocepProps.getProperty("biocep.db.driver") == null) {
            throw new InstantiationException("Supplied properties don't contain required property 'biocep.db.driver'");
        }
        System.setProperty(
                "pools.dbmode.driver",
                biocepProps.getProperty("biocep.db.driver"));
        if (biocepProps.getProperty("biocep.defaultpool") == null) {
            throw new InstantiationException(
                    "Supplied properties don't contain required property 'biocep.defaultpool'");
        }
        System.setProperty(
                "pools.dbmode.defaultpool",
                biocepProps.getProperty("biocep.defaultpool"));
        if (biocepProps.getProperty("biocep.killused") == null) {
            throw new InstantiationException("Supplied properties don't contain required property 'biocep.killused'");
        }
        System.setProperty(
                "pools.dbmode.killused",
                biocepProps.getProperty("biocep.killused"));
    }
}
