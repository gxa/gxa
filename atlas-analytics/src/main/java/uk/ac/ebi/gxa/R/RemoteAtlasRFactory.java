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
 */
public class RemoteAtlasRFactory implements AtlasRFactory {
    private static final String R_REMOTE_HOST = "R.remote.host";
    private boolean isInitialized = false;

    private final Logger log = LoggerFactory.getLogger(getClass());

    public boolean validateEnvironment() throws AtlasRServicesException {
        // check environment, system properties
        String env = System.getenv(R_REMOTE_HOST);
        String property = System.getProperty(R_REMOTE_HOST);
        if (Strings.isNullOrEmpty(env) && Strings.isNullOrEmpty(property)) {
            log.error("No R.remote.host property set - this is required to access R instance on the remote host");
            return false;
        }

        String r_host = Strings.isNullOrEmpty(env) ? property : env;

        // always set system property
        System.setProperty(R_REMOTE_HOST, r_host);

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
            // TODO: obviously not implemented. Shall we?
            // create connection to remote host
            isInitialized = true;
        }
    }
}
