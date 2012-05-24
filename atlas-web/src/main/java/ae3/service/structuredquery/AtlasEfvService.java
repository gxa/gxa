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
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
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
import java.util.*;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.common.collect.Sets.newTreeSet;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
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

    public Set<Property> getOptionsFactors() {
        return getFilteredFactors(atlasProperties.getOptionsIgnoredEfs());
    }

    private SortedSet<Property> getFilteredFactors(Collection<String> ignored) {
        SortedSet<Property> result = newTreeSet();
        for (Property property : propertyDAO.getAll()) {
            if (!ignored.contains(property.getName()))
                result.add(property);
        }
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
                    //ToDo: to get rid of propertyDAO.getValues() method use propertyValueDAO.findValuesForProperty(property)
                    for (PropertyValue pv : propertyDAO.getValues(p)) {
                        EfvAttribute attr = new EfvAttribute(pv.getDefinition().getName(), pv.getValue());
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

    public Collection<AutoCompleteItem> autoCompleteValues(final String name, @Nonnull String prefix, final int limit, @Nullable Map<String, String> filters) {
        final List<AutoCompleteItem> result = new ArrayList<AutoCompleteItem>();
        for (Multiset.Entry<String> entry : treeAutocomplete(getPropertyList(name), prefix.toLowerCase(), limit).entrySet())
            result.add(
                    new AutoCompleteItem(
                            null,
                            entry.getElement(),
                            entry.getElement(),
                            (long) entry.getCount(),
                            new Rank(1.0 * prefix.length() / entry.getElement().length())));

        return result;
    }

    private Collection<Property> getPropertyList(String name) {
        if (isNullOrEmpty(name))
            return getOptionsFactors();

        try {
            final Property property = propertyDAO.getByName(name);

            if (isProhibited(property))
                return emptyList();

            return asList(property);
        } catch (RecordNotFoundException e) {
            log.warn("Unknown property name requested: " + name, e);
            return emptyList();
        }
    }

    private boolean isProhibited(Property property) {
        return !getOptionsFactors().contains(property);
    }

    private Multiset<String> treeAutocomplete(Collection<Property> properties, final @Nonnull String prefix, final int limit) {
        final Multiset<String> result = HashMultiset.create();

        for (final Property property : properties) {
            PrefixNode root = treeGetOrLoad(property.getName());
            if (root != null) {
                root.walk(prefix, 0, "", new PrefixNode.WalkResult() {
                    public void put(String name, int count) {
                        result.add(name, count);

                    }

                    public boolean enough() {
                        return limit >= 0 && result.elementSet().size() >= limit;
                    }
                });
            }
        }
        return result;
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
