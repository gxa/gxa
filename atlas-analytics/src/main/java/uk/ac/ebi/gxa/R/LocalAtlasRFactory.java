package uk.ac.ebi.gxa.R;

import org.kchine.r.server.RServices;
import server.DirectJNI;

import java.util.concurrent.Semaphore;

/**
 * A concrete implementation of {@link uk.ac.ebi.gxa.R.AtlasRFactory} that generates RServices that run on the local
 * machine.  As local R installations are not thread-safe, only one computation can be calculated at a time.  Therefore,
 * createRServices() will release at most one RService for use at any one time, and repeat requests will block until the
 * previously acquired RService has been released.
 *
 * @author Tony Burdett
 * @date 17-Nov-2009
 */
public class LocalAtlasRFactory implements AtlasRFactory {
    private final BootableSemaphore r = new BootableSemaphore(1, true);

    public RServices createRServices() throws AtlasRServicesException {
        if (r.availablePermits() == 0) {
            throw new AtlasRServicesException("R resources have been released");
        }

        // create a R service - DirectJNI gets an R service on the local machine
        try {
            r.acquire();
            return DirectJNI.getInstance().getRServices();
        }
        catch (InterruptedException e) {
            throw new AtlasRServicesException(e);
        }
    }

    public void recycleRServices(RServices rServices) {
        // release lock
        r.release();
    }

    public void releaseResources() {
        // interrupt all threads waiting for a permit
        r.bootAllWaiting();
    }

    private class BootableSemaphore extends Semaphore {
        public BootableSemaphore(int i) {
            super(i);
        }

        public BootableSemaphore(int i, boolean b) {
            super(i, b);
        }

        public void bootAllWaiting() {
            for (Thread t : getQueuedThreads()) {
                t.interrupt();
            }
            reducePermits(availablePermits());
        }
    }
}
