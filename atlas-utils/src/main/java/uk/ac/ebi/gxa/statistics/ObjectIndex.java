package uk.ac.ebi.gxa.statistics;

import it.uniroma3.mat.extendedset.ConciseSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private ConcurrentMap<ObjectIdType, Integer> object2pos = new ConcurrentHashMap<ObjectIdType, Integer>();
    private ConcurrentMap<Integer, ObjectIdType> pos2object = new ConcurrentHashMap<Integer, ObjectIdType>();

    private static final Logger log = LoggerFactory.getLogger(ObjectIndex.class);

    synchronized public Integer addObject(ObjectIdType objectid) {
        if (object2pos.containsKey(objectid)) {
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

    public ObjectIdType getObjectForIndex(Integer index) {
        return pos2object.get(index);
    }

    public Collection<ObjectIdType> getObjectsForIndexes(Collection<Integer> index) {
        Collection<ObjectIdType> objects = new ArrayList<ObjectIdType>(index.size());
        for (Integer pos : index) {
            if (pos2object.get(pos) != null) {
                objects.add(pos2object.get(pos));
            } else {
                log.error("Failed to find object for index: " + pos + " in ObjectIndex");
            }
        }

        return objects;
    }

    public ConciseSet getIndexesForObjects(Collection<ObjectIdType> objectids) {
        ConciseSet indexes = new ConciseSet(objectids.size());
        for (ObjectIdType obj : objectids) {
            if (object2pos.get(obj) != null) {
                indexes.add(object2pos.get(obj));
            } else {
                // This can occur when attempting to retrieve gene ids from this class that don't exist
                // in any ncdf. Such gene ids may come from Atlas gene index, populated via with genes
                // retrieved from DB via getAtlasDAO().getAllGenesFast()               
                log.debug("Failed to find index for object: " + obj + " in ObjectIndex");
            }
        }

        return indexes;
    }
}
