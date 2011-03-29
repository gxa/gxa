package uk.ac.ebi.gxa.statistics;

import it.uniroma3.mat.extendedset.ConciseSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
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

    public ConciseSet getIndexesForObjects(Collection<ObjectIdType> objectids) {
        ConciseSet indexes = new ConciseSet();
        for (ObjectIdType obj : objectids) {
            try {
                indexes.add(object2pos.get(obj));
            } catch (NullPointerException npe) {
                // This can occur when attempting to retrieve gene ids from this class that don't exist
                // in any ncdf. Such gene ids may come from Atlas gene index, populated via with genes
                // retrieved from DB via getAtlasDAO().getAllGenesFast()
                log.debug("Failed to find index for object: " + obj + " in ObjectIndex");
            }
        }

        return indexes;
    }
}
