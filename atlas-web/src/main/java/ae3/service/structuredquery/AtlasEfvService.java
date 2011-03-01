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

import com.google.common.base.Strings;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.params.FacetParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import uk.ac.ebi.gxa.index.builder.IndexBuilder;
import uk.ac.ebi.gxa.index.builder.IndexBuilderEventHandler;
import uk.ac.ebi.gxa.properties.AtlasProperties;
import uk.ac.ebi.gxa.utils.EscapeUtil;

import java.util.*;

import static uk.ac.ebi.gxa.exceptions.LogUtil.logUnexpected;


/**
 * EFVs listing and autocompletion helper implementation
 *
 * @author pashky
 * @see AutoCompleter
 */
public class AtlasEfvService implements AutoCompleter, IndexBuilderEventHandler, DisposableBean {
    private SolrServer solrServerAtlas;
    private SolrServer solrServerProp;
    private AtlasProperties atlasProperties;
    private IndexBuilder indexBuilder;

    final private Logger log = LoggerFactory.getLogger(getClass());

    private final Map<String, PrefixNode> prefixTrees = new HashMap<String, PrefixNode>();
    private Set<String> allFactors = new HashSet<String>();

    public void setSolrServerAtlas(SolrServer solrServerAtlas) {
        this.solrServerAtlas = solrServerAtlas;
    }

    public void setSolrServerProp(SolrServer solrServerProp) {
        this.solrServerProp = solrServerProp;
    }

    public void setAtlasProperties(AtlasProperties atlasProperties) {
        this.atlasProperties = atlasProperties;
    }

    public Set<String> getOptionsFactors() {
        return getFilteredFactors(atlasProperties.getOptionsIgnoredEfs());
    }

    public Set<String> getAnyConditionFactors() {
        return getFilteredFactors(atlasProperties.getAnyConditionIgnoredEfs());
    }

    public Set<String> getFacetFactors() {
        return getFilteredFactors(atlasProperties.getAnyConditionIgnoredEfs());
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
                throw logUnexpected("Can't fetch all factors", e);
            }
        }
        return allFactors;
    }

    private PrefixNode treeGetOrLoad(String property) {
        PrefixNode root;
        synchronized (prefixTrees) {
            if (!prefixTrees.containsKey(property)) {
                log.info("Loading factor values and counts for " + property);
                SolrQuery q = new SolrQuery("*:*");
                q.setRows(0);
                q.setFacet(true);
                q.setFacetMinCount(1);
                q.setFacetLimit(-1);
                q.setFacetSort(FacetParams.FACET_SORT_COUNT);

                try {
                    Map<String, String> valMap = new HashMap<String, String>();
                    q.addFacetField("efvs_ud_" + EscapeUtil.encode(property));

                    QueryResponse qr = solrServerAtlas.query(q);
                    root = new PrefixNode();
                    if (qr.getFacetFields() != null && qr.getFacetFields().get(0) != null
                            && qr.getFacetFields().get(0).getValues() != null) {
                        for (FacetField.Count ffc : qr.getFacetFields().get(0).getValues())
                            if (ffc.getName().length() > 0 && ffc.getCount() > 0) {
                                if (valMap.size() == 0)
                                    root.add(ffc.getName(), (int) ffc.getCount());
                                else if (valMap.containsKey(ffc.getName()))
                                    root.add(valMap.get(ffc.getName()), (int) ffc.getCount());
                            }
                    }
                    prefixTrees.put(property, root);

                } catch (SolrServerException e) {
                    throw logUnexpected("General Solr problem", e);
                }
                log.info("Done loading factor values and counts for " + property);
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

    public Collection<AutoCompleteItem> autoCompleteValues(String property, String query, int limit) {
        return autoCompleteValues(property, query, limit, null);
    }

    public Collection<AutoCompleteItem> autoCompleteValues(final String property, String query, final int limit, Map<String, String> filters) {

        boolean hasPrefix = query != null && !"".equals(query);
        if (hasPrefix)
            query = query.toLowerCase();

        boolean anyProp = Strings.isNullOrEmpty(property);

        Collection<AutoCompleteItem> result;
        if (anyProp) {
            result = new TreeSet<AutoCompleteItem>();
            for (final String prop : getOptionsFactors())
                treeAutocomplete(prop, query, limit, result);
        } else {
            result = new ArrayList<AutoCompleteItem>();
            if (getOptionsFactors().contains(property))
                treeAutocomplete(property, query, limit, result);
        }
        return result;
    }

    private void treeAutocomplete(final String property, String query, final int limit, final Collection<AutoCompleteItem> result) {
        PrefixNode root = treeGetOrLoad(property);
        if (root != null) {
            root.walk(query, 0, "", new PrefixNode.WalkResult() {
                public void put(String name, int count) {
                    result.add(new AutoCompleteItem(property, name, name, (long) count));
                }

                public boolean enough() {
                    return limit >= 0 && result.size() >= limit;
                }
            });
        }
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
