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
import org.springframework.util.ReflectionUtils;
import uk.ac.ebi.gxa.utils.LazyKeylessMap;
import uk.ac.ebi.mydas.configuration.DataSourceConfiguration;
import uk.ac.ebi.mydas.configuration.GlobalConfiguration;
import uk.ac.ebi.mydas.configuration.Mydasserver;
import uk.ac.ebi.mydas.configuration.ServerConfiguration;
import uk.ac.ebi.mydas.controller.DataSourceManager;
import uk.ac.ebi.mydas.controller.MydasServlet;

import java.lang.reflect.Field;
import java.util.*;

/**
 * Atlas properties container class
 * @author pashky
 */
public class AtlasProperties  {

    private Storage storage;

    private final Logger log = LoggerFactory.getLogger(AtlasProperties.class);

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
     *
     * @param key      property name
     * @param newValue property value or null if property customization should be deleted
     */
    public void setProperty(String key, String newValue) {
        if (!key.equals("atlas.dasbase") || updateDasBaseURL(newValue)) {
            // Update atlas.dasbase property only if we managed to MydasServer code was updated successfully
            storage.setProperty(key, newValue);
            cache.put(key, newValue);
            notifyListeners();
        }
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
        return StringUtils.trimToNull(key) == null ?
                Collections.<String>emptyList()
                : Arrays.asList(getProperty(key).split(","));
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

    public Map<String,String> getGenePropertyLinks() {
        return new LazyKeylessMap<String, String>() {
            @Override
            protected String map(String property) {
                return getGenePropertyLink(property);
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

    public Map<String,String> getCuratedEfs() {
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

    public Map<String,String> getCuratedGeneProperties() {
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
        for(String property : getAvailablePropertyNames()) {
            if(property.startsWith(prefix))
                result.setProperty(property.substring(prefix.length()), getProperty(property));
        }
        return result;
    }

    public String getHtmlBodyStart() {
        return getProperty("atlas.look.html.body.start");
    }

	public String getConfigurationDirectoryPath() {
        return getProperty("atlas.config.dir");
	}

	public boolean isLookCacheEnabled() {
        return getBoolProperty("atlas.look.cache.enabled");
	}

    @SuppressWarnings("unused") // used in look/footer.html file, with homegrown {@link ae3.util.TemplateTag}
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

    /**
     * MydasServlet, used by Atlas to expose its data as a DAS source, is configured at start up via MydasServerConfig.xml.
     * Maven build replaces atlas.dasbase placeholder in MydasServerConfig.xml with a value set in atlas-web/pom.xml
     * atlas.dasbase property is also configurable via AtlasProperties, but since MydasServlet code does not currently
     * provide access to its internal fields using atlas.dasbase, the only current way to re-configure MydasServlet code after
     * an AtlasProperties change to atlas.dasbase is vai the reflection hack below.
     * TODO replace this method with direct calls to MydasServlet code once setter methods are provided by the DAS team
     *
     * @param dasBaseURL base URL for DAS
     * @return true if all fields were updated via reflection successfully; false otherwise
     */
    public boolean updateDasBaseURL(String dasBaseURL) {
        boolean success = false;
        try {
            Field field = ReflectionUtils.findField(MydasServlet.class, "DATA_SOURCE_MANAGER");
            field.setAccessible(true);
            Object dataSourceManager = ReflectionUtils.getField(field, null);
            if (dataSourceManager != null) {
                // GxaS4DasDataSource has been accessed at least once since Atlas startup and MydasServerConfig.xml was already
                // read in by MydasServlet - need to update the relevant object fields via reflection
                // web.xml is now configured to load MydasServlet at Atlas startup - if it is not loaded by the time
                // this method runs 
                ServerConfiguration serverConfiguration = ((DataSourceManager) dataSourceManager).getServerConfiguration();

                // Set baseUrl to dasBaseURL - c.f. <baseurl>${atlas.dasbase}/</baseurl> in MydasServerConfig.xml
                GlobalConfiguration globalConfiguration = serverConfiguration.getGlobalConfiguration();
                Field baseUrl = globalConfiguration.getClass().getDeclaredField("baseURL");
                baseUrl.setAccessible(true);
                baseUrl.set(globalConfiguration, dasBaseURL);
                log.debug("Setting <baseurl> MydasServerConfig.xml to: dasBaseURL");

                // Update all capability fields with the new dasBaseURL - c.f. (in MydasServerConfig.xml)
                //    <capability type="das1:sources" query_uri="${atlas.dasbase}/s4" />
                //    <capability type="das1:types" query_uri="${atlas.dasbase}/s4/types" />
                //    <capability type="das1:features" query_uri="${atlas.dasbase}/s4/features?segment=ENSG00000162552" />
                Map<String, DataSourceConfiguration> dataSourceConfigMap = serverConfiguration.getDataSourceConfigMap();
                for (DataSourceConfiguration dsConfig : dataSourceConfigMap.values()) {
                    List<Mydasserver.Datasources.Datasource.Version> versions = dsConfig.getConfig().getVersion();
                    for (Mydasserver.Datasources.Datasource.Version version : versions) {
                        List<Mydasserver.Datasources.Datasource.Version.Capability> capabilities = version.getCapability();
                        for (Mydasserver.Datasources.Datasource.Version.Capability capability : capabilities) {
                            String queryUri = capability.getQueryUri();
                            String newQueryUri = queryUri.replaceFirst(".*\\/", dasBaseURL + "/");
                            log.debug("Setting query_uri of capability type: " + capability.getType() + " in MydasServerConfig.xml to " + newQueryUri);
                            capability.setQueryUri(newQueryUri);
                        }
                    }
                }
                success = true;
            }
        } catch (Exception e) {
            log.error("Failed to update dasBaseUrl to : " + dasBaseURL, e);
        }
        return success;
    }
}
