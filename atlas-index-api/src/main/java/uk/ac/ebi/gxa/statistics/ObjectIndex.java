package uk.ac.ebi.gxa.statistics;

import java.io.Serializable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Class for objects of type ObjectIdType to unique Integer values for the purpose of either:
 * - storage in ConciseSet (Gene ids)
 * - minimising of space consumption in StatisticsStorage (Experiments)
 */
public class ObjectIndex<ObjectIdType> implements Serializable {
    private static final long serialVersionUID = 8209692110534761622L;
    private ConcurrentMap<ObjectIdType, Integer> object2pos = new ConcurrentHashMap<ObjectIdType, Integer>();
    private ConcurrentMap<Integer, ObjectIdType> pos2object = new ConcurrentHashMap<Integer, ObjectIdType>();

    synchronized public Integer addObject(ObjectIdType objectid) {
        if (object2pos.containsKey(objectid)) {
            return object2pos.get(objectid);
        } else {
            Integer pos = object2pos.size() + 1;

            object2pos.put(objectid, pos);
            // TODO: note that index from an Integer (0-based, incremental, no gaps) to an object
            // is essentialy a random-access List, and can be implemented with ArrayList instead
            pos2object.put(pos, objectid);
            // TODO: Moreover, the object-to-pos map needs not to be persistent:
            // you can re-create it in readResolve()

            return pos;
        }
    }

    public Integer getIndexForObject(ObjectIdType objectid) {
        return object2pos.get(objectid);
    }

    public ObjectIdType getObjectForIndex(Integer index) {
        return pos2object.get(index);
    }
}
