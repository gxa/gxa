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

import com.google.common.base.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.rcloud.server.RServices;

/**
 * A concrete implementation of {@link uk.ac.ebi.gxa.R.AtlasRFactory} that generates RServices that run on a remote
 * machine.
 *
 * @author Tony Burdett
 * @date 17-Nov-2009
 */
public class RemoteAtlasRFactory implements AtlasRFactory {
    private boolean isInitialized = false;

    private String remoteHost;

    private final Logger log = LoggerFactory.getLogger(getClass());

    public boolean validateEnvironment() throws AtlasRServicesException {
        // check environment, system properties
        String r_host = null;
        if ((Strings.isNullOrEmpty(System.getenv("R.remote.host"))) &&
                (Strings.isNullOrEmpty(System.getProperty("R.remote.host")))) {
            log.error("No R.remote.host property set - this is required to access R instance on the remote host");
            return false;
        }
        else {
            r_host = System.getenv("R.remote.host");
            if (Strings.isNullOrEmpty(r_host)) {
                r_host = System.getProperty("R.remote.host");
            }
        }

        // r_home definitely not null or "" now
        if (Strings.isNullOrEmpty(r_host)) {
            return false;
        }

        // always set system property
        System.setProperty("R.remote.host", r_host);

        // checks passed so return true
        return true;
    }

    public RServices createRServices() throws AtlasRServicesException {
        initialize();
        log.warn("Unimplemented R facility - as yet, can't run remote R analytics.");
        return null;
    }

    public void recycleRServices(RServices rServices) throws AtlasRServicesException {
        // do nothing
    }

    public void releaseResources() {
        // no resources ever held
    }

    private void initialize() throws AtlasRServicesException {
        if (!isInitialized) {
            // read system property R.remote.host into String
            remoteHost = System.getProperty("R.remote.host");

            // create connection to remote host

            isInitialized = true;
        }
    }
}
