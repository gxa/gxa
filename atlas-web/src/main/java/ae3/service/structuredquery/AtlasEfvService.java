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

package ae3.service.structuredquery;

import ae3.service.AtlasStatisticsQueryService;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.params.FacetParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import uk.ac.ebi.gxa.dao.PropertyDAO;
import uk.ac.ebi.gxa.dao.exceptions.RecordNotFoundException;
import uk.ac.ebi.gxa.index.builder.IndexBuilder;
import uk.ac.ebi.gxa.index.builder.IndexBuilderEventHandler;
import uk.ac.ebi.gxa.properties.AtlasProperties;
import uk.ac.ebi.gxa.statistics.EfvAttribute;
import uk.ac.ebi.gxa.statistics.StatisticsType;
import uk.ac.ebi.microarray.atlas.model.Property;
import uk.ac.ebi.microarray.atlas.model.PropertyValue;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import static com.google.common.base.Strings.isNullOrEmpty;
import static uk.ac.ebi.gxa.exceptions.LogUtil.createUnexpected;


/**
 * EFVs listing and autocompletion helper implementation
 *
 * @author pashky
 * @see AutoCompleter
 */
public class AtlasEfvService implements AutoCompleter, IndexBuilderEventHandler, DisposableBean {
    private SolrServer solrServerProp;
    private AtlasProperties atlasProperties;
    private IndexBuilder indexBuilder;
    private AtlasStatisticsQueryService atlasStatisticsQueryService;
    private PropertyDAO propertyDAO;

    final private Logger log = LoggerFactory.getLogger(getClass());

    private final Map<String, PrefixNode> prefixTrees = new HashMap<String, PrefixNode>();
    private Set<String> allFactors = new HashSet<String>();

    public void setSolrServerProp(SolrServer solrServerProp) {
        this.solrServerProp = solrServerProp;
    }

    public void setAtlasStatisticsQueryService(AtlasStatisticsQueryService atlasStatisticsQueryService) {
        this.atlasStatisticsQueryService = atlasStatisticsQueryService;
    }

    public void setAtlasProperties(AtlasProperties atlasProperties) {
        this.atlasProperties = atlasProperties;
    }

    public void setPropertyDAO(PropertyDAO propertyDAO) {
        this.propertyDAO = propertyDAO;
    }

    public Set<String> getOptionsFactors() {
        return getFilteredFactors(atlasProperties.getOptionsIgnoredEfs());
    }

    private Set<String> getFilteredFactors(Collection<String> ignored) {
        Set<String> result = new TreeSet<String>();
        result.addAll(getAllFactors());
        result.removeAll(ignored);
        return result;
    }

    public Set<String> getAllFactors() {
        if (allFactors.isEmpty()) {
            SolrQuery q = new SolrQuery("*:*");
            q.setRows(0);
            q.addFacetField("property_f");
            q.setFacet(true);
            q.setFacetLimit(-1);
            q.setFacetMinCount(1);
            q.setFacetSort(FacetParams.FACET_SORT_COUNT);
            try {
                QueryResponse qr = solrServerProp.query(q);
                if (qr.getFacetFields().get(0).getValues() != null)
                    for (FacetField.Count ffc : qr.getFacetFields().get(0).getValues()) {
                        allFactors.add(ffc.getName());
                    }
            } catch (SolrServerException e) {
                throw createUnexpected("Can't fetch all factors", e);
            }
        }
        return allFactors;
    }

    private PrefixNode treeGetOrLoad(String property) {
        PrefixNode root;
        synchronized (prefixTrees) {
            if (!prefixTrees.containsKey(property)) {
                log.info("Loading factor values and counts for " + property);

                root = new PrefixNode();
                try {
                    final Property p = propertyDAO.getByName(property);
                    for (PropertyValue pv : p.getValues()) {
                        EfvAttribute attr = new EfvAttribute(pv.getDefinition().getName(), pv.getValue(), StatisticsType.UP_DOWN);
                        int geneCount = atlasStatisticsQueryService.getBioEntityCountForEfvAttribute(attr, StatisticsType.UP_DOWN);
                        if (geneCount > 0) {
                            root.add(attr.getEfv(), geneCount);
                        }
                    }
                    prefixTrees.put(property, root);
                    log.info("Done loading factor values and counts for " + property);
                } catch (RecordNotFoundException e) {
                    throw createUnexpected(e.getMessage(), e);
                }
            }
            root = prefixTrees.get(property);
        }
        return root;
    }

    public Collection<String> listAllValues(String property) {
        final List<String> result = new ArrayList<String>();
        PrefixNode.WalkResult rc = new PrefixNode.WalkResult() {
            public void put(String name, int count) {
                result.add(name);
            }

            public boolean enough() {
                return false;
            }
        };
        PrefixNode root = treeGetOrLoad(property);
        if (root != null)
            root.collect("", rc);
        return result;
    }

    public Collection<AutoCompleteItem> autoCompleteValues(String property, @Nonnull String prefix, int limit) {
        return autoCompleteValues(property, prefix, limit, null);
    }

    public Collection<AutoCompleteItem> autoCompleteValues(final String property, @Nonnull String prefix, final int limit, @Nullable Map<String, String> filters) {
        boolean everywhere = isNullOrEmpty(property);

        Collection<String> properties = everywhere ? getOptionsFactors() :
               (getOptionsFactors().contains(property) ? Arrays.asList(property) : Collections.<String>emptyList());
        return treeAutocomplete(properties, prefix.toLowerCase(), limit);
    }

    private Collection<AutoCompleteItem> treeAutocomplete(Collection<String> properties, final @Nonnull String prefix, final int limit) {
        final List<AutoCompleteItem> result = new ArrayList<AutoCompleteItem>();

        for (final String property : properties) {
            PrefixNode root = treeGetOrLoad(property);
            if (root != null) {
                root.walk(prefix, 0, "", new PrefixNode.WalkResult() {
                    public void put(String name, int count) {
                        result.add(
                                new EfvAutoCompleteItem(
                                        property,
                                        curatedName(property),
                                        name,
                                        (long) count,
                                        new Rank(1.0 * prefix.length() / name.length())));
                    }

                    public boolean enough() {
                        return limit >= 0 && result.size() >= limit;
                    }
                });
            }
        }
        return result;
    }

    private String curatedName(String property) {
        String curated = atlasProperties.getCuratedEf(property);
        return curated == null ? property : curated;
    }

    public void setIndexBuilder(IndexBuilder indexBuilder) {
        this.indexBuilder = indexBuilder;
        indexBuilder.registerIndexBuildEventHandler(this);
    }

    public void onIndexBuildFinish() {
        allFactors.clear();
        prefixTrees.clear();
    }

    public void onIndexBuildStart() {

    }

    public void destroy() throws Exception {
        if (indexBuilder != null)
            indexBuilder.unregisterIndexBuildEventHandler(this);
    }
}
