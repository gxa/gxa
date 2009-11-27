package uk.ac.ebi.gxa.loader.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.microarray.atlas.dao.AtlasDAO;

import javax.sql.DataSource;

/**
 * An abstract Atlas loader service, containing basic setup that is required across all loader implementations.  This
 * leaves implementing classes free to describe only the logic required to perform loads.
 *
 * @author Tony Burdett
 * @date 27-Nov-2009
 */
public abstract class AtlasLoaderService<T> {
    // fixme: remove requirement on datasource, all interactions should go through DAO
    private DataSource dataSource;
    private AtlasDAO atlasDAO;

    // logging
    private Logger log = LoggerFactory.getLogger(this.getClass());

    protected AtlasLoaderService(DataSource dataSource, AtlasDAO atlasDAO) {
        this.dataSource = dataSource;
        this.atlasDAO = atlasDAO;
    }

    protected DataSource getDataSource() {
        return dataSource;
    }

    protected AtlasDAO getAtlasDAO() {
        return atlasDAO;
    }

    protected Logger getLog() {
        return log;
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
