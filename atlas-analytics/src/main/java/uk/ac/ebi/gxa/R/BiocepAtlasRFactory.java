package uk.ac.ebi.gxa.R;

import org.apache.commons.pool.PoolableObjectFactory;
import org.apache.commons.pool.impl.GenericObjectPool;
import org.kchine.r.server.RServices;
import org.kchine.rpf.ServantProvider;
import org.kchine.rpf.ServantProviderFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.rmi.RemoteException;
import java.util.NoSuchElementException;

/**
 * A concrete implementation of {@link uk.ac.ebi.gxa.R.AtlasRFactory} that generates RServices that run on a biocep
 * compute cloud.  This requires a biocep.properties file to be located on the classpath, as information about the
 * biocep environment will be read from this file prior to initialization of any services.
 *
 * @author Misha Kapushesky
 * @author Tony Burdett
 * @date 17-Nov-2009
 */
public class BiocepAtlasRFactory implements AtlasRFactory {
    private boolean isInitialized = false;

    private GenericObjectPool workerPool;
    private final GenericObjectPool.Config workerPoolConfig;

    private final Logger log = LoggerFactory.getLogger(getClass());

    public BiocepAtlasRFactory() {
        // setup worker pool config
        workerPoolConfig = new GenericObjectPool.Config();
        workerPoolConfig.maxActive = 4;
        workerPoolConfig.minIdle = 4;
        workerPoolConfig.maxWait = 1000;
        workerPoolConfig.testOnBorrow = true;
        workerPoolConfig.testOnReturn = true;
    }

    /**
     * Validates that all the system properties required by biocep are set.
     *
     * @return true if the validation succeed, flase if it failed for a reason OTHER than a missing property
     * @throws AtlasRServicesException if any required properties are missing.
     */
    public boolean validateEnvironment() throws AtlasRServicesException {
        if (System.getProperty("pools.dbmode.host") == null) {
            log.warn("pools.dbmode.host not set");
            return false;
        }
        if (System.getProperty("pools.dbmode.port") == null) {
            log.warn("pools.dbmode.port not set");
            return false;
        }
        if (System.getProperty("pools.dbmode.name") == null) {
            log.warn("pools.dbmode.name not set");
            return false;
        }
        if (System.getProperty("pools.dbmode.user") == null) {
            log.warn("biocep.dbmode.user not set");
            return false;
        }
        if (System.getProperty("pools.dbmode.password") == null) {
            log.warn("biocep.db.password not set");
            return false;
        }
        if (System.getProperty("naming.mode") == null) {
            log.warn("biocep.naming.mode not set");
            return false;
        }
        if (System.getProperty("pools.provider.factory") == null) {
            log.warn("biocep.provider.factory not set");
            return false;
        }
        if (System.getProperty("pools.dbmode.type") == null) {
            log.warn("biocep.db.type not set");
            return false;
        }
        if (System.getProperty("pools.dbmode.driver") == null) {
            log.warn("pools.dbmode.driver not set");
            return false;
        }
        if (System.getProperty("pools.dbmode.defaultpoolname") == null) {
            log.warn("pools.dbmode.defaultpoolname not set");
            return false;
        }
        if (System.getProperty("pools.dbmode.killused") == null) {
            log.warn("pools.dbmode.killused not set");
            return false;
        }

        // otherwise, checks passed so return true
        return true;
    }

    public RServices createRServices() throws AtlasRServicesException {
        // lazily initialize servant provider
        initialize();

        try {
            return (RServices) workerPool.borrowObject();
        }
        catch (Exception e) {
            throw new AtlasRServicesException(
                    "Failed to borrow an RServices object from the pool of workers", e);
        }
    }

    public void recycleRServices(RServices rServices) throws UnsupportedOperationException, AtlasRServicesException {
        try {
            workerPool.returnObject(rServices);
        }
        catch (Exception e) {
            throw new AtlasRServicesException(
                    "Failed to release an RServices object back into the pool of workers", e);
        }
    }

    public void releaseResources() {
        if (isInitialized) {
            try {
                if (workerPool.getNumActive() > 0) {
                    log.warn("Shutting down even though there are still some active compute workers");
                }

                workerPool.clear();
                workerPool.close();
            }
            catch (Exception e) {
                e.printStackTrace();
                log.error("Problem shutting down compute service", e.getMessage());
            }
        }
    }

    /**
     * Lazily initializes the worker pool when the first R service is requested
     *
     * @throws AtlasRServicesException if initialization failed
     */
    private void initialize() throws AtlasRServicesException {
        if (!isInitialized) {
            if (validateEnvironment()) {
                // create worked pool
                workerPool = new GenericObjectPool(new RWorkerObjectFactory(), workerPoolConfig);
                isInitialized = true;
            }
            else {
                String msg = "Unable to initialize - R environment is not valid.  See log for details.";
                log.error(msg);
                throw new AtlasRServicesException(msg);
            }
        }
    }

    private class RWorkerObjectFactory implements PoolableObjectFactory {
        private ServantProvider sp;

        public RWorkerObjectFactory() {
            initializeServantProvider();
        }

        private void initializeServantProvider() {
            // and get the servant provider
            sp = ServantProviderFactory.getFactory().getServantProvider();
        }

        public Object makeObject() throws Exception {
            log.info("Borrowing R worker from proxy...");
            RServices R = (RServices) sp.borrowServantProxyNoWait();

            if (null == R) {
                throw new NoSuchElementException();
            }

            log.info("Got worker " + R.getServantName());

            return R;
        }

        public void destroyObject(Object o) throws Exception {
            RServices R = (RServices) o;
            log.info("Returning worker " + R.getServantName() + " proxy");

            sp.returnServantProxy((RServices) o);
        }

        public boolean validateObject(Object o) {
            RServices R = (RServices) o;
            try {
                R.ping();
            }
            catch (RemoteException e) {
                log.info("R worker does not respond to ping correctly ({}). Invalidated.", e.getMessage());
                return false;
            }

            return true;
        }

        public void activateObject(Object o) throws Exception {
        }

        public void passivateObject(Object o) throws Exception {
        }
    }
}
