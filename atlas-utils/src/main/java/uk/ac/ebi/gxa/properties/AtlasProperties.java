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

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.gxa.utils.LazyKeylessMap;

import javax.annotation.concurrent.GuardedBy;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyVetoException;
import java.beans.VetoableChangeListener;
import java.util.*;

/**
 * Atlas properties container class
 *
 * @author pashky
 */
public class AtlasProperties {

    private Storage storage;

    private final Logger log = LoggerFactory.getLogger(AtlasProperties.class);

    /**
     * Set storage for use
     *
     * @param storage storage reference
     */
    public void setStorage(Storage storage) {
        this.storage = storage;
    }

    private final List<AtlasPropertiesListener> listeners = new ArrayList<AtlasPropertiesListener>();
    private final List<VetoableChangeListener> vetoableChangeListeners = new ArrayList<VetoableChangeListener>();
    @GuardedBy("this")
    private final Map<String, String> cache = new HashMap<String, String>();

    /**
     * Returns available properties names list, calculated in constructor
     *
     * @return set of property names
     */
    public Collection<String> getAvailablePropertyNames() {
        return storage.getAvailablePropertyNames();
    }

    public synchronized void reload() {
        storage.reload();
        cache.clear();
        notifyListeners();
    }

    /**
     * Sets new (or removes) property value
     *
     * @param key      property name
     * @param newValue property value or null if property customization should be deleted
     */
    public synchronized void setProperty(String key, String newValue) {
        try {
            notifyListeners(key, getProperty(key), newValue);
            storage.setProperty(key, newValue);
            cache.put(key, newValue);
            notifyListeners();
        } catch (PropertyVetoException e) {
            log.warn("Property change vetoed", e);
        }
    }

    /**
     * Returns property value
     *
     * @param key property name
     * @return property value string or empty string if not found
     */
    public synchronized String getProperty(String key) {
        String cached = cache.get(key);
        if (cached != null)
            return cached;
        String result = storage.getProperty(key) != null ? storage.getProperty(key) : "";
        cache.put(key, result);
        return result;
    }

    private List<String> getListProperty(String key) {
        return StringUtils.trimToNull(key) == null ?
                Collections.<String>emptyList()
                : Arrays.asList(getProperty(key).split(","));
    }


    private int getIntProperty(String key) {
        try {
            if (key.equals("atlas.last.experiment")) {
                return -1; // fixme: actually read this from DB somewhere
            } else {
                return Integer.valueOf(storage.getProperty(key));
            }
        } catch (Exception e) {
            return 0;
        }
    }

    private boolean getBoolProperty(String key) {
        String value = storage.getProperty(key);
        return !"".equals(value) && !"no".equals(value) && !"false".equals(value) && !"0".equals(value);
    }

    /**
     * Register property update listener
     *
     * @param listener listener reference
     */
    public synchronized void registerListener(AtlasPropertiesListener listener) {
        if (!listeners.contains(listener))
            listeners.add(listener);
    }

    /**
     * Unregister property update listener
     *
     * @param listener listener reference
     */
    public synchronized void unregisterListener(AtlasPropertiesListener listener) {
        listeners.remove(listener);
    }

    /**
     * Register property update listener
     *
     * @param listener listener reference
     */
    public synchronized void registerListener(VetoableChangeListener listener) {
        if (!vetoableChangeListeners.contains(listener))
            vetoableChangeListeners.add(listener);
    }

    /**
     * Unregister property update listener
     *
     * @param listener listener reference
     */
    public synchronized void unregisterListener(VetoableChangeListener listener) {
        vetoableChangeListeners.remove(listener);
    }

    private synchronized void notifyListeners() {
        for (AtlasPropertiesListener listener : listeners)
            listener.onAtlasPropertiesUpdate(this);
    }

    private synchronized void notifyListeners(String property, String oldValue, String newValue) throws PropertyVetoException {
        final PropertyChangeEvent event = new PropertyChangeEvent(this, property, oldValue, newValue);
        for (VetoableChangeListener listener : vetoableChangeListeners) {
            listener.vetoableChange(event);
        }
    }

    /* Version properties */
    public String getSoftwareVersion() {
        return storage.getProperty("atlas.software.version");
    }

    public String getSoftwareDate() {
        return storage.getProperty("atlas.software.date");
    }

    /* Data release */
    public String getDataRelease() {
        return getProperty("atlas.data.release");
    }

    public String getDasBase() {
        return getProperty("atlas.dasbase");
    }

    public String getLastReleaseDate() {
        return getProperty("atlas.data.release.lastdate");
    }

    /* Gene autocompleter properties */
    public boolean isGeneListCacheAutoGenerate() {
        return getBoolProperty("atlas.gene.list.autogenerate.cache");
    }

    public int getGeneAutocompleteIdLimit() {
        return getIntProperty("atlas.gene.autocomplete.ids.limit");
    }

    public int getGeneAutocompleteNameLimit() {
        return getIntProperty("atlas.gene.autocomplete.names.limit");
    }

    public int getGeneAutocompleteNamesPerSpeciesLimit() {
        return getIntProperty("atlas.gene.autocomplete.names.per_species.limit");
    }

    public List<String> getGeneAutocompleteSpeciesOrder() {
        return getListProperty("atlas.gene.autocomplete.species.order");
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

    public List<String> getGeneTooltipFields() {
        return getListProperty("atlas.gene.properties.tooltip.display");
    }

    public List<String> getGenePageIgnoreFields() {
        return getListProperty("atlas.gene.properties.genepage.ignore");
    }

    public List<String> getGenePageDefaultFields() {
        return getListProperty("atlas.gene.properties.genepage.displaydefault");
    }

    public String getGenePropertyLink(String property) {
        return getProperty("geneproperty.link." + property);
    }

    public Map<String, String> getGenePropertyLinks() {
        return new LazyKeylessMap<String, String>() {
            @Override
            protected String map(String property) {
                return getGenePropertyLink(property);
            }
        };
    }

    /**
     * @param property identifier for an external resource
     * @return a url that can be used to access the external resource via gene identifier
     */
    public String getGeneIdentifierLink(String property) {
        return getProperty("geneidentifier.link." + property);
    }

    public Map<String, String> getGeneIdentifierLinks() {
        return new LazyKeylessMap<String, String>() {
            @Override
            protected String map(String property) {
                return getGeneIdentifierLink(property);
            }
        };
    }

    public List<String> getGeneApiIgnoreFields() {
        return getListProperty("atlas.gene.properties.api.ignore");
    }

    public String getGeneApiFieldName(String property) {
        return getProperty("geneproperty.apiname." + property);
    }

    /* Query properties */

    public int getQueryDrilldownMinGenes() {
        return getIntProperty("atlas.drilldowns.mingenes");
    }

    public int getQueryDefaultPageSize() {
        return getIntProperty("atlas.query.default.pagesize");
    }

    public int getAPIQueryMaximumPageSize() {
        return getIntProperty("atlas.api.query.maximum.pagesize");
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

    // List of fields that should be excluded from the dump (if they had been
    // included, they would have ended up in the addtional_fields section)

    public List<String> getDumpExcludeFields() {
        return getListProperty("atlas.dump.exclude.fields");
    }

    public String getDumpGeneIdentifiersFilename() {
        return getProperty("atlas.dump.geneidentifiers.filename");
    }

    public String getDumpEbeyeFilename() {
        return getProperty("atlas.dump.ebeye.filename");
    }

    public String getExperimentsDumpEbeyeFilename() {
        return getProperty("atlas.experiments.dump.ebeye.filename");
    }

    public String getExperimentsToPropertiesDumpFilename() {
        return getProperty("atlas.experiments.properties.dump.filename");
    }

    public String getGenesDumpEbeyeFilename() {
        return getProperty("atlas.genes.dump.ebeye.filename");
    }

    /* EFs */

    public List<String> getOptionsIgnoredEfs() {
        return getListProperty("atlas.ignore.efs.options");
    }

    public List<String> getAnyConditionIgnoredEfs() {
        return getListProperty("atlas.ignore.efs.anycondition");
    }

    public List<String> getGeneHeatmapIgnoredEfs() {
        return getListProperty("atlas.ignore.efs.gene.heatmap");
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
        return getProperty("factor.curatedname." + ef);
    }

    public Map<String, String> getCuratedEfs() {
        return new LazyKeylessMap<String, String>() {
            @Override
            protected String map(String ef) {
                return getCuratedEf(ef);
            }
        };
    }


    public String getCuratedGeneProperty(String geneProperty) {
        return getProperty("geneproperty.curatedname." + geneProperty);
    }

    public Map<String, String> getCuratedGeneProperties() {
        return new LazyKeylessMap<String, String>() {
            @Override
            protected String map(String property) {
                return getCuratedGeneProperty(property);
            }
        };
    }

    public List<String> getLoaderPossibleQuantitaionTypes() {
        return getListProperty("atlas.loader.possible.qtypes");
    }

    public List<String> getLoaderGeneIdPriority() {
        return getListProperty("atlas.loader.gene.identifier.priority");
    }

    public boolean isGeneListAfterIndexAutogenerate() {
        return getBoolProperty("atlas.gene.list.autogenerate.afterindex");
    }


    /* R & Biocep */
    public String getRMode() {
        return getProperty("atlas.rservice.mode");
    }

    public Properties getRProperties() {
        final String prefix = "atlas.rservice.";
        Properties result = new Properties();
        for (String property : getAvailablePropertyNames()) {
            if (property.startsWith(prefix))
                result.setProperty(property.substring(prefix.length()), getProperty(property));
        }
        return result;
    }

    public String getRLibDir() {
        return getProperty("atlas.rservice.rlibdir");
    }

    public String getConfigurationDirectoryPath() {
        return getProperty("atlas.config.dir");
    }

    public boolean isLafCacheEnabled() {
        return getBoolProperty("atlas.look.cache.enabled");
    }

    public String getLafTemplatesPath() {
        return getProperty("atlas.look.templates.path");
    }

    public String getLafResourcesDir() {
        return getProperty("atlas.look.resources.dir");
    }

    @SuppressWarnings("unused")
    // used in look/templates.stg.html file, with homegrown {@link ae3.util.StringTemplateTag}
    public String getGoogleAnalyticsAccount() {
        return getProperty("atlas.googleanalytics.account");
    }

    public Integer getGeneAtlasIndexBuilderNumberOfThreads() {
        return getIntProperty("atlas.indexbuilder.geneindex.numthreads");
    }

    public Integer getGeneAtlasIndexBuilderChunksize() {
        return getIntProperty("atlas.indexbuilder.geneindex.chunksize");
    }

    public Integer getGeneAtlasIndexBuilderCommitfreq() {
        return getIntProperty("atlas.indexbuilder.geneindex.commitfreq");
    }

    public String getTheMOTD() {
        return getProperty("atlas.look.motd");
    }

    public String getAlertNotice() {
        return getProperty("atlas.look.alertnotice");
    }

    public List<String> getDasFactors() {
        return getListProperty("atlas.dasfactors");
    }

    public Integer getMaxEfvsPerEfInHeatmap() {
        return getIntProperty("atlas.max.efvs.per.ef.in.heatmap");
    }

    /**
     * @return The restriction of the overall experiment count for (max. atlas.query.pagesize) sorted bioentities to be displayed
     *         in a heatmap. This value is a crude (though more accurate than the overall amount of genes matched by user's query) measure of
     *         the cost of constructing a heatmap, short of actually constructing that heatmap. For more info see atlas.properties
     */
    public Integer getMaxExperimentCountForStructuredQuery() {
        return getIntProperty("atlas.structured.query.max.experiment.count");
    }

    /**
     * @return The restriction of the total amount of mappings for all efo's to be shown in heatmap to their experiment-efv pairs.
     *         This again is a crude measure of how much work AtlasStatisticsQueryService will need to perform in order to retrieve experiment
     *         counts and pvals/tstats from bit index, for each cell of the heatmap.
     */
    public Integer getMaxEfoMappingsCountForStructuredQuery() {
        return getIntProperty("atlas.structured.query.max.efo.mappings.count");
    }
}
