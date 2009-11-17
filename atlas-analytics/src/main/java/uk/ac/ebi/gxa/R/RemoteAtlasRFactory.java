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
    private final Logger log = LoggerFactory.getLogger(getClass());

    public RemoteAtlasRFactory(String remoteHostName) {
        // do some initialisation - RMI?
        log.warn("Unimplemented R facility - as yet, can't run remote R analytics.  " +
                "Host requested = " + remoteHostName);
    }

    public RServices createRServices() {
        log.warn("Unimplemented R facility - as yet, can't run remote R analytics.");
        return null;
    }

    public void recycleRServices(RServices rServices) throws AtlasRServicesException {
        // do nothing
    }

    public void releaseResources() {
        // no resources ever held
    }
}
