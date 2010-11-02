package uk.ac.ebi.gxa.statistics;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Created by IntelliJ IDEA.
 * User: rpetry
 * Date: Oct 26, 2010
 * Time: 5:32:58 PM
 * Class for objects of type ObjectIdType to unique Integer values for the purpose of either:
 * - storage in ConciseSet (Gene ids)
 * - minimising of space consumption in StatisticsStorage (Experiments)
 */
public class ObjectIndex<ObjectIdType> implements Serializable {
    private ConcurrentMap<ObjectIdType,Integer> object2pos = new ConcurrentHashMap<ObjectIdType,Integer>();
    private ConcurrentMap<Integer,ObjectIdType> pos2object = new ConcurrentHashMap<Integer,ObjectIdType>();
    private static final long serialVersionUID = -8050952027393810684L;

    synchronized public Integer addObject(ObjectIdType objectid) {
        if(object2pos.containsKey(objectid)) {
            return object2pos.get(objectid);
        } else {
            Integer pos = object2pos.size() + 1;

            object2pos.put(objectid, pos);
            pos2object.put(pos, objectid);

            return pos;
        }
    }

    public Integer getIndexForObject(ObjectIdType objectid) {
        return object2pos.get(objectid);
    }

    public Integer getNumberOfObjects() {
        return object2pos.size();
    }

    public ObjectIdType getObjectForIndex(Integer index) {
        return pos2object.get(index);
    }

    public Collection<ObjectIdType> getObjectsForIndexes(Collection<Integer> index) {
        Collection<ObjectIdType>    objects = new ArrayList<ObjectIdType>(index.size());
        for (Integer pos : index) objects.add(pos2object.get(pos));

        return objects;
    }

    public Collection<ObjectIdType> getObjectsForIndexes(Collection<Integer> index, final Set<ObjectIdType> objectIds) {
        Collection<ObjectIdType> objects = new ArrayList<ObjectIdType>(index.size());
        for (Integer pos : index) {
            ObjectIdType objectId = pos2object.get(pos);
            if (objectIds.contains(objectId)) {
                objects.add(objectId);
            }
        }

        return objects;
    }

    public Set<ObjectIdType> getObjectIdsInIndex() {
        return object2pos.keySet();
    }
}
