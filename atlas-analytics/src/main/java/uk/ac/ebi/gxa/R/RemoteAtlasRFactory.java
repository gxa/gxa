package uk.ac.ebi.gxa.R;

import org.kchine.r.server.RServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
        if ((System.getenv("R.remote.host") == null || System.getenv("R.remote.host").equals("")) &&
                (System.getProperty("R.remote.host") == null || System.getProperty("R.remote.host").equals(""))) {
            log.error("No R.remote.host property set - this is required to access R instance on the remote host");
            return false;
        }
        else {
            r_host = System.getenv("R.remote.host");
            if (r_host == null || r_host.equals("")) {
                r_host = System.getProperty("R.remote.host");
            }
        }

        // r_home definitely not null or "" now
        if (r_host == null || r_host.equals("")) {
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
