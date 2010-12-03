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

import uk.ac.ebi.rcloud.server.RServices;
import uk.ac.ebi.gxa.properties.AtlasProperties;
import uk.ac.ebi.gxa.properties.AtlasPropertiesListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.DisposableBean;

import java.util.Properties;

/**
 * @author pashky
 */
public class DynamicAtlasRFactory implements AtlasRFactory, AtlasPropertiesListener, InitializingBean, DisposableBean {
    private static Logger log = LoggerFactory.getLogger(DynamicAtlasRFactory.class);

    private volatile AtlasRFactory currentRFactory;
    private AtlasProperties atlasProperties;
    private String currentType = null;

    public void setAtlasProperties(AtlasProperties atlasProperties) {
        this.atlasProperties = atlasProperties;
    }

    private AtlasRFactory getCurrentRFactory() {
        if (currentRFactory == null) {
            currentRFactory = chooseRFactory(currentType = atlasProperties.getRMode().toUpperCase());
        }
        return currentRFactory;
    }

    public void onAtlasPropertiesUpdate(AtlasProperties atlasProperties) {
        String newType = atlasProperties.getRMode().toUpperCase();

        if (currentType != null && currentType.equals(newType))
            return;

        if (currentRFactory != null) {
            currentRFactory.releaseResources();
            currentRFactory = null;
        }
        currentRFactory = chooseRFactory(currentType = newType);
    }

    private static final AtlasRFactory DUMMY_FACTORY = new AtlasRFactory() {
        public boolean validateEnvironment() throws AtlasRServicesException {
            return false;
        }

        public RServices createRServices() throws AtlasRServicesException {
            return null;
        }

        public void recycleRServices(RServices rServices) throws AtlasRServicesException {
        }

        public void releaseResources() {
        }
    };

    private AtlasRFactory chooseRFactory(String typeStr) {
        if (atlasProperties == null || typeStr == null)
            return DUMMY_FACTORY;

        try {
            RType type = RType.valueOf(typeStr);
            Properties props = atlasProperties.getRProperties();
            log.info("Trying to configure R in mode " + type + ", with properties " + props);
            AtlasRFactory factory = AtlasRFactoryBuilder.getAtlasRFactoryBuilder().buildAtlasRFactory(type, props);
            if (factory.validateEnvironment()) {
                log.info("Successfully created R environment for mode " + type.toString());
                return factory;
            } else {
                log.error("Invalid environment for R mode " + typeStr + " , computations will fail");
                return DUMMY_FACTORY;
            }
        } catch (InstantiationException e) {
            log.error("Can't instantiate factory for mode " + typeStr, e);
            return DUMMY_FACTORY;
        } catch (Exception e) {
            log.error("Impossible atlas.R.mode property value: " + typeStr + "; Atlas computations will fail, please configure R through Admin interface");
            return DUMMY_FACTORY;
        }
    }

    public void afterPropertiesSet() throws Exception {
        if (atlasProperties != null)
            atlasProperties.registerListener(this);
    }

    public void destroy() throws Exception {
        if (atlasProperties != null)
            atlasProperties.unregisterListener(this);
    }

    public boolean validateEnvironment() throws AtlasRServicesException {
        return getCurrentRFactory().validateEnvironment();
    }

    public RServices createRServices() throws AtlasRServicesException {
        return getCurrentRFactory().createRServices();
    }

    public void recycleRServices(RServices rServices) throws AtlasRServicesException {
        getCurrentRFactory().recycleRServices(rServices);
    }

    public void releaseResources() {
        if (currentRFactory != null)
            getCurrentRFactory().releaseResources();
    }

}
