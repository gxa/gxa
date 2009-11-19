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
     * Assertains whether the current R environment is valid, and can be used to acquire {@link
     * org.kchine.r.server.RServices} objects.  This will return false if the current R setup cannot be used, for
     * example if a JNI bridge is missing in the case of a local R installation, or if required system properties or
     * environment variables are absent.  If validation checks pass and it appears that the R setup is sound, this will
     * return true.  An exception may be thrown when trying to access the underlying environment; the exception should
     * always be wrapped and rethrown if this is the case.
     *
     * @return true if the R environment is correctly configured, false otherwise
     * @throws AtlasRServicesException if the underlying environment throws an exception on initialization
     */
    boolean validateEnvironment() throws AtlasRServicesException;

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
