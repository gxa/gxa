package uk.ac.ebi.gxa.utils;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: rpetry
 * Date: Nov 10, 2010
 * Time: 10:47:53 AM
 * Class to store a Map-based cache capable of storing up to maxNumOfEntries entries - for example use see AtlasStatisticsQueryService
 */
public class SizeBoundedLinkedHashMap<KeyIdType, ValueIdType> extends LinkedHashMap<KeyIdType, ValueIdType>{

    private Integer maxNumOfEntries;

    public SizeBoundedLinkedHashMap(Integer maxNumOfEntries) {
           this.maxNumOfEntries = maxNumOfEntries;
    }

    protected boolean removeEldestEntry(Map.Entry eldest) {
       return size() > maxNumOfEntries;
    }

}
