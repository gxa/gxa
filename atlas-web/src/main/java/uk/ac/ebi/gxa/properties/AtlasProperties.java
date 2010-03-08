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

import java.lang.annotation.Retention;
import java.lang.reflect.Method;
import java.util.*;

/**
 * Atlas properties container class
 * @author pashky
 */
public class AtlasProperties {

    private Storage storage;

    /**
     * Set storage for use
     * @param storage storage reference
     */
    public void setStorage(Storage storage) {
        this.storage = storage;
    }

    private final Set<String> propertyNames = new HashSet<String>();
    private final List<AtlasPropertiesListener> listeners = new ArrayList<AtlasPropertiesListener>();

    public AtlasProperties() {
        storage = new Storage() {
            public String getProperty(String name) {
                propertyNames.add(name);
                return "";
            }

            public void setProperty(String name, String value) { }
            public boolean isWritePersistent() { return false; }
            public void reload() { }
        };

        for (Method m : getClass().getMethods())
            if (m.isAnnotationPresent(ExportProperty.class))
                try {
                    m.invoke(this);
                } catch (Throwable ex) {
                    //
                }

        storage = null;
    }

    /**
     * Returns available properties names list, calculated in constructor
     * @return set of property names
     */
    public Set<String> getAvailablePropertyNames() {
        return propertyNames;
    }

    public void reload() {
        storage.reload();
        notifyListeners();
    }

    /**
     * Sets new property value
     * @param key property name
     * @param newValue property value
     */
    public void setProperty(String key, String newValue) {
        storage.setProperty(key, newValue);
        notifyListeners();
    }

    /**
     * Returns property value
     * @param key property name
     * @return property value string or empty string if not found
     */
    public String getProperty(String key) {
        return storage.getProperty(key) != null ? storage.getProperty(key) : "";
    }

    private List<String> getListProperty(String key) {
        return Arrays.asList(getProperty(key).split(","));
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

    @ExportProperty
    public String getSoftwareVersion() {
        return storage.getProperty("atlas.software.version");
    }

    @ExportProperty
    public String getSoftwareBuildNumber() {
        return storage.getProperty("atlas.buildNumber");
    }

    /**
     * Marker annotation
     */
    @Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
    private @interface ExportProperty { }

    /* Data release */

    @ExportProperty
    public String getDataRelease() {
        return getProperty("atlas.data.release");
    }

    /* Gene autocompleter properties */

    @ExportProperty
    public boolean isGeneListCacheAutoGenerate() {
        return getBoolProperty("atlas.gene.list.cache.autogenerate");
    }

    @ExportProperty
    public int getGeneAutocompleteIdLimit() {
        return getIntProperty("atlas.gene.autocomplete.ids.limit");
    }

    @ExportProperty
    public int getGeneAutocompleteNameLimit() {
        return getIntProperty("atlas.gene.autocomplete.names.limit");
    }

    @ExportProperty
    public List<String> getGeneAutocompleteIdFields() {
        return getListProperty("atlas.gene.autocomplete.ids");
    }

    @ExportProperty
    public List<String> getGeneAutocompleteNameFields() {
        return getListProperty("atlas.gene.autocomplete.names");        
    }

    @ExportProperty
    public List<String> getGeneAutocompleteDescFields() {
        return getListProperty("atlas.gene.autocomplete.descs");
    }

    /* Query properties */

    @ExportProperty
    public int getQueryDrilldownMinGenes() {
        return getIntProperty("atlas.drilldowns.mingenes");
    }

    @ExportProperty
    public int getQueryPageSize() {
        return getIntProperty("atlas.query.pagesize");
    }

    @ExportProperty
    public int getQueryListSize() {
        return getIntProperty("atlas.query.listsize");
    }

    @ExportProperty
    public int getQueryExperimentsPerGene() {
        return getIntProperty("atlas.query.expsPerGene");
    }

    @ExportProperty
    public List<String> getQueryDrilldownGeneFields() {
        return getListProperty("atlas.gene.drilldowns");
    }

    /* Dump properties */

    @ExportProperty
    public List<String> getDumpGeneIdFields() {
        return getListProperty("atlas.dump.geneidentifiers");
    }

    @ExportProperty
    public String getDumpGeneIdentifiersFilename() {
        return getProperty("atlas.dump.geneidentifiers.filename");
    }

    @ExportProperty
    public String getDumpEbeyeFilename() {
        return getProperty("atlas.dump.ebeye.filename");
    }

    /* EFs */

    @ExportProperty
    public List<String> getOptionsIgnoredEfs() {
        return getListProperty("atlas.options.ignore.efs");
    }

    @ExportProperty
    public List<String> getAnyConditionIgnoredEfs() {
        return getListProperty("atlas.anycondition.ignore.efs");
    }

    @ExportProperty
    public List<String> getFacetIgnoredEfs() {
        return getListProperty("atlas.facet.ignore.efs");
    }

    /* Feedback mail */

    @ExportProperty
    public String getFeedbackSmtpHost() {
        return getProperty("atlas.feedback.smtp.host");
    }

    @ExportProperty
    public String getFeedbackFromAddress() {
        return getProperty("atlas.feedback.from.address");
    }

    @ExportProperty
    public String getFeedbackToAddress() {
        return getProperty("atlas.feedback.to.address");
    }

    @ExportProperty
    public String getFeedbackSubject() {
        return getProperty("atlas.feedback.subject");
    }

}
