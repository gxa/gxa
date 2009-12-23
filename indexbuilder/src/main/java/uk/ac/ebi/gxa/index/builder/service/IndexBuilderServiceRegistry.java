package uk.ac.ebi.gxa.index.builder.service;

import uk.ac.ebi.microarray.atlas.dao.AtlasDAO;

import java.util.Map;
import java.util.HashMap;
import java.util.Collection;

import org.apache.solr.core.CoreContainer;

/**
 * Index builders registry
 * @author pashky
 */
public class IndexBuilderServiceRegistry {
    private static Map<String,IndexBuilderService.Factory> factoryMap = new HashMap<String, IndexBuilderService.Factory>();

    public static void registerFactory(IndexBuilderService.Factory factory) {
        if(factoryMap.containsKey(factory.getName()))
            throw new IllegalStateException("Index builder factory with name " + factory.getName() +
                    " already exists in registry for class " + factoryMap.get(factory.getName()).getClass().getName());
        factoryMap.put(factory.getName(), factory);
    }

    public static Collection<String> getAvailableFactories() {
        return factoryMap.keySet();
    }

    public static IndexBuilderService.Factory getFactoryByName(String name) {
        IndexBuilderService.Factory factory = factoryMap.get(name);
        if(factory == null)
            throw new IllegalArgumentException("Index builder not found for name " + name);
        return factory;
    }
}
