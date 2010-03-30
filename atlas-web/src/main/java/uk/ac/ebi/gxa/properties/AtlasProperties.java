/*
 * Copyright 2008-2010 Microarray Informatics Team, EMBL-European Bioinformatics Institute
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *
 * For further details of the Gene Expression Atlas project, including source code,
 * downloads and documentation, please see:
 *
 * http://gxa.github.com/gxa
 */
package uk.ac.ebi.gxa.properties;

import java.util.*;

/**
 * Atlas properties container class
 * @author pashky
 */
public class AtlasProperties  {

    private Storage storage;

    /**
     * Set storage for use
     * @param storage storage reference
     */
    public void setStorage(Storage storage) {
        this.storage = storage;
    }

    private final List<AtlasPropertiesListener> listeners = new ArrayList<AtlasPropertiesListener>();
    private final Map<String,String> cache = new HashMap<String, String>();

    /**
     * Returns available properties names list, calculated in constructor
     * @return set of property names
     */
    public Collection<String> getAvailablePropertyNames() {
        return storage.getAvailablePropertyNames();
    }

    public void reload() {
        storage.reload();
        cache.clear();
        notifyListeners();
    }

    /**
     * Sets new (or removes) property value
     * @param key property name
     * @param newValue property value or null if property customization should be deleted
     */
    public void setProperty(String key, String newValue) {
        storage.setProperty(key, newValue);
        cache.put(key, newValue);
        notifyListeners();
    }

    /**
     * Returns property value
     * @param key property name
     * @return property value string or empty string if not found
     */
    public String getProperty(String key) {
        String cached = cache.get(key);
        if(cached != null)
            return cached;
        String result = storage.getProperty(key) != null ? storage.getProperty(key) : "";
        cache.put(key, result);
        return result;
    }

    private List<String> getListProperty(String key) {
        return Arrays.asList(getProperty(key).split(","));
    }


    private List<String> getListPropertyLowerCase(String key) {
        return Arrays.asList(getProperty(key).toLowerCase().split(","));
    }

    private int getIntProperty(String key) {
        try {
            if (key.equals("atlas.last.experiment")) {
                return -1; // fixme: actually read this from DB somewhere
            }
            else {
                return Integer.valueOf(storage.getProperty(key));
            }
        }
        catch (Exception e) {
            return 0;
        }
    }

    private boolean getBoolProperty(String key) {
        String value = storage.getProperty(key);
        return !"".equals(value) && !"no".equals(value) && !"false".equals(value) && !"0".equals(value);
    }

    /**
     * Register property update listener
     * @param listener listener reference
     */
    public synchronized void registerListener(AtlasPropertiesListener listener) {
        if(!listeners.contains(listener))
            listeners.add(listener);
    }

    /**
     * Unregister property update listener
     * @param listener listener reference
     */
    public synchronized void unregisterListener(AtlasPropertiesListener listener) {
        listeners.remove(listener);
    }

    private synchronized void notifyListeners() {
        for(AtlasPropertiesListener listener : listeners)
            listener.onAtlasPropertiesUpdate(this);
    }

    /* Version properties */

    public String getSoftwareVersion() {
        return storage.getProperty("atlas.software.version");
    }

    public String getSoftwareBuildNumber() {
        return storage.getProperty("atlas.software.buildnumber");
    }

    /* Data release */

    public String getDataRelease() {
        return getProperty("atlas.data.release");
    }

    /* Gene autocompleter properties */

    public boolean isGeneListCacheAutoGenerate() {
        return getBoolProperty("atlas.gene.list.cache.autogenerate");
    }

    public int getGeneAutocompleteIdLimit() {
        return getIntProperty("atlas.gene.autocomplete.ids.limit");
    }

    public int getGeneAutocompleteNameLimit() {
        return getIntProperty("atlas.gene.autocomplete.names.limit");
    }

    public List<String> getGeneAutocompleteIdFields() {
        return getListPropertyLowerCase("atlas.gene.autocomplete.ids");
    }

    public List<String> getGeneAutocompleteNameFields() {
        return getListPropertyLowerCase("atlas.gene.autocomplete.names");
    }

    public List<String> getGeneAutocompleteDescFields() {
        return getListPropertyLowerCase("atlas.gene.autocomplete.descs");
    }

    /* Query properties */

    public int getQueryDrilldownMinGenes() {
        return getIntProperty("atlas.drilldowns.mingenes");
    }

    public int getQueryPageSize() {
        return getIntProperty("atlas.query.pagesize");
    }

    public int getQueryListSize() {
        return getIntProperty("atlas.query.listsize");
    }

    public int getQueryExperimentsPerGene() {
        return getIntProperty("atlas.query.expsPerGene");
    }

    public List<String> getQueryDrilldownGeneFields() {
        return getListPropertyLowerCase("atlas.gene.drilldowns");
    }

    /* Dump properties */

    public List<String> getDumpGeneIdFields() {
        return getListPropertyLowerCase("atlas.dump.geneidentifiers");
    }

    public String getDumpGeneIdentifiersFilename() {
        return getProperty("atlas.dump.geneidentifiers.filename");
    }

    public String getDumpEbeyeFilename() {
        return getProperty("atlas.dump.ebeye.filename");
    }

    /* EFs */

    public List<String> getOptionsIgnoredEfs() {
        return getListProperty("atlas.options.ignore.efs");
    }

    public List<String> getAnyConditionIgnoredEfs() {
        return getListProperty("atlas.anycondition.ignore.efs");
    }

    public List<String> getFacetIgnoredEfs() {
        return getListProperty("atlas.facet.ignore.efs");
    }

    /* Feedback mail */

    public String getFeedbackSmtpHost() {
        return getProperty("atlas.feedback.smtp.host");
    }

    public String getFeedbackFromAddress() {
        return getProperty("atlas.feedback.from.address");
    }

    public String getFeedbackToAddress() {
        return getProperty("atlas.feedback.to.address");
    }

    public String getFeedbackSubject() {
        return getProperty("atlas.feedback.subject");
    }

    public String getCuratedEf(String ef) {
        return getProperty("head.ef." + ef);
    }

    public String getCuratedGeneProperty(String geneProperty) {
        return getProperty("head.gene." + geneProperty);
    }

    public List<String> getPossibleQuantitaionTypes() {
        return getListProperty("atlas.loader.possible.qtypes");
    }

    /* R & Biocep */
    public String getRMode() {
        return getProperty("atlas.rservice.mode");
    }

    public Properties getRProperties() {
        final String prefix = "atlas.rservice.";
        Properties result = new Properties();
        for(String property : getAvailablePropertyNames()) {
            if(property.startsWith(prefix))
                result.setProperty(property.substring(prefix.length()), getProperty(property));
        }
        return result;
    }
}
