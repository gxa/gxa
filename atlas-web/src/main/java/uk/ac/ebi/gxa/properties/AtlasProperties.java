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

import java.util.List;
import java.util.Arrays;
import java.util.Properties;
import java.util.ArrayList;
import java.io.IOException;

/**
 * Atlas properties container class
 * @author pashky
 */
public class AtlasProperties {

    private Properties props;
    private Properties versionProps;

    private List<AtlasPropertiesListener> listeners = new ArrayList<AtlasPropertiesListener>();

    public AtlasProperties() {
        loadProperties();
    }

    /**
     * Causes properties to reload
     */
    public void loadProperties() {
        try {
            props = new Properties();
            props.load(AtlasProperties.class.getResourceAsStream("/atlas.properties"));
        }
        catch (IOException e) {
            throw new RuntimeException("Can't read properties file atlas.properties from resources!", e);
        }

        try {
            versionProps = new Properties();
            versionProps.load(getClass().getClassLoader().getResourceAsStream("atlas.version"));
        }
        catch (IOException e) {
            throw new RuntimeException("Cannot load atlas version properties - " +
                    "META-INF/atlas.version may be missing or invalid", e);
        }
        notifyListeners();
    }

    /**
     * Sets new property value
     * @param key property name
     * @param newValue property value
     */
    public void setProperty(String key, String newValue) {
        props.setProperty(key, newValue);
        notifyListeners();
    }

    /**
     * Returns property value
     * @param key property name
     * @return property value string or empty string if not found
     */
    public String getProperty(String key) {
        return props.getProperty(key) != null ? props.getProperty(key) : "";
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
                return Integer.valueOf(props.getProperty(key));
            }
        }
        catch (Exception e) {
            return 0;
        }
    }

    private boolean getBoolProperty(String key) {
        return props.containsKey(key)
                && !"".equals(props.getProperty(key))
                && !"no".equals(props.getProperty(key))
                && !"false".equals(props.getProperty(key))
                && !"0".equals(props.getProperty(key));
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
        String s = props.getProperty("atlas.software.version");
        if(s != null)
            return s;
        s = versionProps.getProperty("atlas.software.version");
        if(s != null)
            return s;
        return "unknown";
    }

    public String getSoftwareBuildNumber() {
        String s = props.getProperty("atlas.buildNumber");
        if(s != null)
            return s;
        s = versionProps.getProperty("atlas.buildNumber");
        if(s != null)
            return s;
        return "unknown";
    }

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
        return getListProperty("atlas.gene.autocomplete.ids");
    }

    public List<String> getGeneAutocompleteNameFields() {
        return getListProperty("atlas.gene.autocomplete.names");        
    }

    public List<String> getGeneAutocompleteDescFields() {
        return getListProperty("atlas.gene.autocomplete.descs");
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
        return getListProperty("atlas.gene.drilldowns");
    }

    /* Dump properties */

    public List<String> getDumpGeneIdFields() {
        return getListProperty("atlas.dump.geneidentifiers");
    }

    public String getDumpGeneIdentifiersFilename() {
        return getProperty("atlas.dump.geneidentifiers.filename");
    }

    public String getDumpEbeyeFilename() {
        return getProperty("atlas.dump.ebeye.filename");
    }

    /* EFs */

    public List<String> getIgnoredEfs(String category) {
        return getListProperty("atlas." + category + ".ignore.efs");
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

}
