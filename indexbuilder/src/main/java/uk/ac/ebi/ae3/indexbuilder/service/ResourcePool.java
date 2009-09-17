package uk.ac.ebi.ae3.indexbuilder.service;

import org.apache.commons.lang.mutable.MutableBoolean;

import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.Semaphore;

/**
 * @author pashky
 */
public abstract class ResourcePool<Resource> {
    protected final Semaphore available;
    protected Map<Resource, MutableBoolean> pool = new HashMap<Resource,MutableBoolean>();
    
    public ResourcePool(int number) {
        available =  new Semaphore(number, true);
    }

    public Resource getItem() throws InterruptedException {
        available.acquire();
        return getNextAvailableItem();
    }

    public void putItem(Resource x) {
        if (markAsUnused(x))
            available.release();
    }

    private synchronized Resource getNextAvailableItem() {
        for(Map.Entry<Resource, MutableBoolean> e : pool.entrySet())
            if(e.getValue().booleanValue()) {
                pool.get(e.getKey()).setValue(false);
                return e.getKey();
            }

        Resource resource = createResource();
        pool.put(resource, new MutableBoolean(false));
        return resource;
    }

    public void close() {
        for(Resource resource : pool.keySet())
            closeResource(resource);
    }

    public abstract void closeResource(Resource resource);

    public abstract Resource createResource();

    private synchronized boolean markAsUnused(Resource item) {
        MutableBoolean b = pool.get(item);
        if(b != null && !b.booleanValue()) {
            b.setValue(true);
            return true;
        }
        return false;
    }
}
