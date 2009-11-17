package uk.ac.ebi.gxa.R;

import org.kchine.r.server.RServices;

/**
 * A factory for generating {@link org.kchine.r.server.RServices}.
 *
 * @author Tony Burdett
 * @date 17-Nov-2009
 */
public interface AtlasRFactory {
    /**
     * Generates an {@link org.kchine.r.server.RServices} object that can be used to perform R calculations.
     *
     * @return an RServices object, to which calculations can be submitted.
     * @throws AtlasRServicesException if this factory cannot create RServices using default properties
     */
    RServices createRServices() throws AtlasRServicesException;

    /**
     * If the RServices object obtained needs to be shutdown, returned to a pool, or somehow destroyed any other way,
     * this method handles that collection of resources.
     *
     * @param rServices the rServices object that has been finished with
     * @throws AtlasRServicesException if this factory failed to recycle the RService for any reason
     */
    void recycleRServices(RServices rServices) throws AtlasRServicesException;

    /**
     * Release any resources that may be held by this AtlasRFactory (for example, a pool of worker threads).
     */
    void releaseResources();
}
