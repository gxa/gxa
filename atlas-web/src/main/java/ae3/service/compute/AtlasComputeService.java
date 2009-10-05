package ae3.service.compute;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.kchine.rpf.ServantProvider;
import org.kchine.rpf.ServantProviderFactory;
import org.kchine.r.server.RServices;
import org.apache.commons.pool.impl.GenericObjectPool;
import org.apache.commons.pool.PoolableObjectFactory;

import java.io.IOException;
import java.io.Reader;
import java.io.InputStreamReader;
import java.util.NoSuchElementException;
import java.rmi.RemoteException;

import ae3.util.AtlasProperties;

/**
 * Provides access to computational infrastructure by maintaining a pool of computational workers.
 * To use, pass a {@link ae3.service.compute.ComputeTask} to the method {@link #computeTask(ComputeTask)},
 * the return type is determined by the type parameter to {@code ComputeTask}.
 * <p>
 * For example:
 * <code>
 * RNumeric i = computeService.computeTask(new ComputeTask<RNumeric> () {
 *   public compute(RServices R) throws RemoteException {
 *     return (RNumeric) R.getObject("1 + 3");
 *   }
 * );
 * </code>
 * <p>
 * If the workers in the pool need any kind of special initialization, a {@link ae3.service.compute.AtlasComputeService.RWorkerInitializer}
 * can be passed to the constructor. The {@link ae3.service.compute.AtlasComputeService.RWorkerInitializer#initializeWorker(org.kchine.r.server.RServices)}
 * method will be called on each worker, when it's borrowed.
 *
 * @author ostolop
 */
public class AtlasComputeService implements Compute {
    final private Logger log = LoggerFactory.getLogger(getClass());
    final private GenericObjectPool workerPool;
    final private static GenericObjectPool.Config workerPoolConfig;

    static {
        workerPoolConfig = new GenericObjectPool.Config();
        workerPoolConfig.maxActive = 4;
        workerPoolConfig.minIdle = 4;
        workerPoolConfig.maxWait = 1000;
        workerPoolConfig.testOnBorrow = true;
        workerPoolConfig.testOnReturn = true;
    }

    /**
     *
     */
    public interface RWorkerInitializer {
        public void initializeWorker(RServices R) throws IOException;
    }

    public AtlasComputeService () {
        RWorkerInitializer defaultInitializer = new RWorkerInitializer() {

            public void initializeWorker(RServices R) throws IOException {
                String simSrc = getRCodeFromResource("/sim.R");
                R.sourceFromBuffer(simSrc);
            }

            private String getRCodeFromResource(final String res) throws IOException {
                final char[] buffer = new char[0x10000];

                StringBuilder out = new StringBuilder();
                Reader in = new InputStreamReader(AtlasComputeService.class.getResourceAsStream(res), "UTF-8");

                int read;
                do {
                    read = in.read(buffer, 0, buffer.length);
                    if (read > 0) {
                        out.append(buffer, 0, read);
                    }
                } while (read >= 0);

                return out.toString();
            }
        };

        workerPool = new GenericObjectPool(new RWorkerObjectFactory(defaultInitializer), workerPoolConfig);
    }

    public AtlasComputeService (RWorkerInitializer rWorkerInitializer) {
        workerPool = new GenericObjectPool(new RWorkerObjectFactory(rWorkerInitializer), workerPoolConfig);
    }


    /**
     * Executes task on a borrowed worker. Returns type specified in generic type parameter T to the method.
     * @param task   task to evaluate, {@link ComputeTask}
     * @param <T>    type that the task returns on completion
     * @return       T
     */
    public <T> T computeTask(ComputeTask<T> task) {
        T res = null;

        RServices R = null;

        try {
            log.info("Borrowing worker from local pool");
            R = (RServices) workerPool.borrowObject();
            log.info("Computing on " + R.getServantName());
            res = task.compute(R);
        } catch (Exception e) {
            log.error("Problem computing task!", e.getMessage());
        } finally {
            if(null != R) {
                try {
                    log.info("Returning worker " + R.getServantName() + " to local pool");
                    workerPool.returnObject(R);
                } catch (Exception e) {
                    log.error("Problem returning worker! {}", e.getMessage());
                }
            }
        }

        return res;
    }

    public void shutdown() {
        try {
            if(workerPool.getNumActive() > 0 )
                log.warn("Shutting down even though there are still some active compute workers");

            workerPool.clear();
            workerPool.close();
        } catch (Exception e) {
            log.error("Problem shutting down compute service", e.getMessage());
        }
    }

    private class RWorkerObjectFactory implements PoolableObjectFactory {
        private ServantProvider sp;
        private RWorkerInitializer workerInitializer = null;

        public RWorkerObjectFactory() {
            initializeServantProvider();
        }

        public RWorkerObjectFactory(RWorkerInitializer workerInitializer) {
            initializeServantProvider();
            this.workerInitializer = workerInitializer;
        }

        private void initializeServantProvider() {
            System.setProperty("naming.mode",                    AtlasProperties.getProperty("biocep.naming.mode"));
            System.setProperty("pools.provider.factory",         AtlasProperties.getProperty("biocep.pools.provider.factory"));
            System.setProperty("pools.dbmode.type",              AtlasProperties.getProperty("biocep.pools.dbmode.type"));
            System.setProperty("pools.dbmode.name",              AtlasProperties.getProperty("biocep.pools.dbmode.name"));
            System.setProperty("pools.dbmode.port",              AtlasProperties.getProperty("biocep.pools.dbmode.port"));
            System.setProperty("pools.dbmode.host",              AtlasProperties.getProperty("biocep.pools.dbmode.host"));
            System.setProperty("pools.dbmode.driver",            AtlasProperties.getProperty("biocep.pools.dbmode.driver"));
            System.setProperty("pools.dbmode.user",              AtlasProperties.getProperty("biocep.pools.dbmode.user"));
            System.setProperty("pools.dbmode.password",          AtlasProperties.getProperty("biocep.pools.dbmode.password"));
            System.setProperty("pools.dbmode.defaultpoolname",   AtlasProperties.getProperty("biocep.pools.dbmode.defaultpoolname"));
            System.setProperty("pools.dbmode.killused",          AtlasProperties.getProperty("biocep.pools.dbmode.killused"));

            sp = ServantProviderFactory.getFactory().getServantProvider();
        }

        public Object makeObject() throws Exception {
            log.info("Borrowing R worker from proxy...");
            RServices R = (RServices) sp.borrowServantProxyNoWait();

            if(null == R) throw new NoSuchElementException();

            log.info("Got worker " + R.getServantName());

            if(null != workerInitializer)
                workerInitializer.initializeWorker(R);

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
            } catch(RemoteException e) {
                log.info("R worker does not respond to ping correctly ({}). Invalidated.", e.getMessage());
                return false;
            }

            return true;
        }

        public void activateObject(Object o) throws Exception {}

        public void passivateObject(Object o) throws Exception {}
    }
}
