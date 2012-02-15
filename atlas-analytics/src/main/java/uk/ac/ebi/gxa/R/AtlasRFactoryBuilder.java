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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

import static uk.ac.ebi.gxa.R.BiocepPropertiesUtils.biocepPropertiesSetup;

/**
 * A singleton class that is reponsible for build {@link uk.ac.ebi.gxa.R.AtlasRFactory} objects.  The type of factory
 * produced will depend on the parameters specified.
 *
 * @author Tony Burdett
 * @see uk.ac.ebi.gxa.R.AtlasRFactory
 */
public class AtlasRFactoryBuilder {

    private static final Logger log = LoggerFactory.getLogger(AtlasRFactoryBuilder.class);

    private static AtlasRFactoryBuilder factoryBuilder = new AtlasRFactoryBuilder();

    public static AtlasRFactoryBuilder getAtlasRFactoryBuilder() {
        return factoryBuilder;
    }

    private AtlasRFactoryBuilder() {
        // singleton, so private constructor
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
                // R_HOME env variable and -Djava.library.path should be configured on the app start
                return new LocalAtlasRFactory();
            case BIOCEP:
                try {
                    biocepPropertiesSetup(properties);
                } catch (BiocepPropertiesSetupException e) {
                    log.error("Failed setup biocep properties", e);
                    throw new InstantiationException("Failed to setup biocep properties");
                }
                return new BiocepAtlasRFactory();
            default:
                throw new InstantiationException("Unrecognised type: " + rType);
        }
    }
}
