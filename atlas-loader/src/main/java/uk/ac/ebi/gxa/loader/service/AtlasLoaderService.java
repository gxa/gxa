package uk.ac.ebi.gxa.loader.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.gxa.dao.AtlasDAO;

/**
 * An abstract Atlas loader service, containing basic setup that is required across all loader implementations.  This
 * leaves implementing classes free to describe only the logic required to perform loads.
 *
 * @author Tony Burdett
 * @date 27-Nov-2009
 */
public abstract class AtlasLoaderService<T> {
    private AtlasDAO atlasDAO;
    private boolean allowReloading = false;

    // logging
    private Logger log = LoggerFactory.getLogger(this.getClass());

    protected AtlasLoaderService(AtlasDAO atlasDAO) {
        this.atlasDAO = atlasDAO;
    }

    protected Logger getLog() {
        return log;
    }

    protected AtlasDAO getAtlasDAO() {
        return atlasDAO;
    }

    protected boolean allowReloading() {
        return allowReloading;
    }

    /**
     * Sets whether or not reloads should be suppressed by this load service.  If this is set to true, attempting to
     * reload an existing experiment will cause an exception.  If false, reloads will procede like any other load
     * (although a warning should be issued to the log stream by implementations of this class).
     *
     * @param allowReloading whether or not to automatically allow reloads
     */
    public void setAllowReloading(boolean allowReloading) {
        this.allowReloading = allowReloading;
    }

    /**
     * Perform a load on the given loader resource.  Normally, experiment and array design loaders will be separate
     * implementations of this class so there is not a requirement to separate out the load methods.
     *
     * @param loaderResource the resource to load
     * @return true if this load succeeds, false otherwise
     */
    public abstract boolean load(T loaderResource);
}
