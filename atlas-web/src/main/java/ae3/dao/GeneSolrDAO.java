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

package ae3.dao;

import ae3.model.AtlasGene;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import org.apache.lucene.search.BooleanQuery;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.gxa.properties.AtlasProperties;
import uk.ac.ebi.gxa.utils.EscapeUtil;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import static uk.ac.ebi.gxa.exceptions.LogUtil.createUnexpected;
import static uk.ac.ebi.gxa.utils.EscapeUtil.escapeSolr;

/**
 * Atlas basic model elements access class
 *
 * @author ostolop, mdylag, pashky
 */
public class GeneSolrDAO {
    private static final Logger log = LoggerFactory.getLogger(GeneSolrDAO.class);

    private AtlasProperties atlasProperties;

    private ExecutorService executorService;

    private SolrServer geneSolr;

    public void setGeneSolr(SolrServer geneSolr) {
        this.geneSolr = geneSolr;
    }

    public void setAtlasProperties(AtlasProperties atlasProperties) {
        this.atlasProperties = atlasProperties;
    }

    public void setExecutorService(ExecutorService executorService) {
        this.executorService = executorService;
    }

    /**
     * Finds gene by id
     *
     * @param id gene id
     * @return atlas gene result
     */
    public AtlasGeneResult getGeneById(final long id) {
        return futureWrap(new Callable<AtlasGeneResult>() {
            @Override
            public AtlasGeneResult call() throws SolrServerException {
                return getGeneByQuery("id:" + id);
            }
        });
    }

    /**
     * Returns number of genes in index
     *
     * @return total number of indexed genes
     */
    public long getGeneCount() {
        return futureWrap(new Callable<Long>() {
            @Override
            public Long call() throws SolrServerException {
                final SolrQuery q = new SolrQuery("*:*");
                q.setRows(0);

                QueryResponse queryResponse = geneSolr.query(q);
                SolrDocumentList documentList = queryResponse.getResults();
                return documentList.getNumFound();
            }
        });
    }

    /**
     * Returns AtlasGenes corresponding to the specified gene identifiers,
     * i.e. matching one of the terms in the "gene_ids" field in Solr schema.
     *
     * @param geneIds Collection of ids
     * @return List<AtlasGene>
     */
    public List<AtlasGene> getGenesByIds(final Collection<Integer> geneIds) {
        return futureWrap(new Callable<List<AtlasGene>>() {
            @Override
            public List<AtlasGene> call() throws SolrServerException {
                List<AtlasGene> genes = new ArrayList<AtlasGene>();
                for (SolrQuery query : getSolrQueriesForGenes(geneIds)) {
                    query.setRows(Integer.MAX_VALUE);
                    QueryResponse queryResponse = geneSolr.query(query);
                    SolrDocumentList documentList = queryResponse.getResults();
                    for (SolrDocument d : documentList) {
                        AtlasGene g = new AtlasGene(d);
                        genes.add(g);
                    }
                }
                return genes;
            }
        });
    }

    /**
     * Returns the AtlasGene corresponding to the specified gene identifier, i.e. matching one of the terms in the
     * "gene_ids" field in Solr schema.
     *
     * @param geneIdentifier primary identifier
     * @return AtlasGene
     */
    public AtlasGeneResult getGeneByIdentifier(final String geneIdentifier) {
        return futureWrap(new Callable<AtlasGeneResult>() {
            @Override
            public AtlasGeneResult call() throws SolrServerException {
                return getGeneByQuery(identifierQuery(geneIdentifier));
            }
        });
    }

    /**
     * Fetch list of orthologs for specified gene
     *
     * @param atlasGene specified gene to look orthologs for
     * @return list of ortholog genes
     */
    public List<AtlasGene> getOrthoGenes(final AtlasGene atlasGene) {
        return futureWrap(new Callable<List<AtlasGene>>() {
            @Override
            public List<AtlasGene> call() throws SolrServerException {
                List<AtlasGene> result = new ArrayList<AtlasGene>();
                for (String orth : atlasGene.getOrthologs()) {
                    AtlasGeneResult orthoGene = getGeneByQuery(identifierQuery(orth));
                    if (orthoGene.isFound()) {
                        result.add(orthoGene.getGene());
                    }

                    if (orthoGene.isMulti()) {
                        log.info("Multiple genes found for ortholog " + orth + " of " + atlasGene.getGeneIdentifier());
                    }
                }
                return result;
            }
        });
    }

    /**
     * Searches gene by id (numerical), identifier (primary) or any of specified set of properties
     * supposedly containing other identifiers
     *
     * @param gene_identifier identifier
     * @param additionalIds   additional properties to search for
     * @return atlas gene search result
     */
    public AtlasGeneResult getGeneByAnyIdentifier(final String gene_identifier, final List<String> additionalIds) {
        return futureWrap(new Callable<AtlasGeneResult>() {
            @Override
            public AtlasGeneResult call() throws SolrServerException {
                final String id = EscapeUtil.escapeSolr(gene_identifier);
                StringBuilder sb = new StringBuilder("id:" + id + " identifier:" + id);
                for (String idprop : additionalIds)
                    sb.append(" property_").append(idprop).append(":").append(id);
                return getGeneByQuery(sb.toString());
            }
        });
    }

    /**
     * Returns genes that can be iterated
     *
     * @return Iterable<AtlasGene>
     */
    public Iterable<AtlasGene> getAllGenes() {
        return createIteratorForQuery(new SolrQuery("*:*"));
    }

    /**
     * @param name name of genes to search for
     * @return Iterable of AtlasGenes matching (gene) name in Solr gene index
     */
    public Iterable<AtlasGene> getGenesByName(String name) {
        return createIteratorForQuery(new SolrQuery(" name:" + escapeSolr(name)));
    }

    /**
     * Returns AtlasGenes corresponding to the specified gene identifiers, i.e. matching one of the terms in the
     * "gene_ids" field in Solr schema.
     *
     * @param ids           Collection of ids
     * @param additionalIds additional properties to search for
     * @return Iterable<AtlasGene>
     */
    private Iterable<AtlasGene> getGenesByAnyIdentifiers(Collection<String> ids, List<String> additionalIds) {
        if (ids.isEmpty()) return Collections.emptyList();

        StringBuilder sb = new StringBuilder();
        for (String id : ids) {
            sb.append(" id:").append(id).append(" identifier:").append(id);
            for (String idprop : additionalIds)
                sb.append(" property_").append(idprop).append(":").append(id);
        }
        return createIteratorForQuery(new SolrQuery(sb.toString()));
    }

    /**
     * Returns AtlasGenes corresponding to the specified gene identifiers, i.e. matching one of the terms in the
     * "gene_ids" field in Solr schema.
     *
     * @param ids Collection of ids
     * @return Iterable<AtlasGene>
     */
    public Iterable<AtlasGene> getGenesByIdentifiers(Collection<String> ids) {
        return getGenesByAnyIdentifiers(ids, Collections.<String>emptyList());
    }

    public List<Long> findGeneIds(Collection<String> query) {
        List<Long> genes = Lists.newArrayList();

        for (String text : query) {
            if (Strings.isNullOrEmpty(text)) {
                continue;
            }
            Iterator<AtlasGene> res = getGenesByAnyIdentifiers(Collections.singleton(text), atlasProperties.getGeneAutocompleteIdFields()).iterator();
            if (!res.hasNext()) {
                for (AtlasGene gene : getGenesByName(text)) {
                    genes.add((long) gene.getGeneId());
                }
            } else {
                while (res.hasNext())
                    genes.add((long) res.next().getGeneId());
            }
        }
        return genes;
    }

    private static String identifierQuery(String geneIdentifier) {
        final String id = escapeSolr(geneIdentifier);
        return "id:" + id + " identifier:" + id;
    }

    /**
     * @param geneIds - a collection of gene Ids to find genes by
     * @return A collection of SolrQuery's - since Lucene has limitation on a maximum
     *         number of boolean clauses in a query, we need to split Ids into chunks that Lucene can manage.
     */
    private List<SolrQuery> getSolrQueriesForGenes(Collection<Integer> geneIds) {
        List<SolrQuery> solrQueries = new ArrayList<SolrQuery>();

        final int maxQueryCount = BooleanQuery.getMaxClauseCount();
        StringBuilder sb = new StringBuilder();
        int cnt = 1;
        for (Integer id : geneIds) {
            if (cnt % maxQueryCount == 0) {
                solrQueries.add(new SolrQuery(sb.toString()));
                sb = new StringBuilder();
            }
            sb.append(" id:").append(id);
            cnt++;
        }
        if (!Strings.isNullOrEmpty(sb.toString())) {
            solrQueries.add(new SolrQuery(sb.toString()));
        }
        return solrQueries;
    }

    private AtlasGeneResult getGeneByQuery(String query) throws SolrServerException {
        SolrQuery q = new SolrQuery(query);
        q.setRows(1);
        q.setFields("*");
        QueryResponse queryResponse = geneSolr.query(q);
        SolrDocumentList documentList = queryResponse.getResults();

        if (documentList == null || documentList.size() == 0) {
            return new AtlasGeneResult(null, false);
        }
        return new AtlasGeneResult(new AtlasGene(documentList.get(0)), documentList.getNumFound() > 1);
    }

    private Iterable<AtlasGene> createIteratorForQuery(final SolrQuery q) {
        final Long total = futureWrap(new Callable<Long>() {
            @Override
            public Long call() throws SolrServerException {
                q.setRows(0);
                QueryResponse queryResponse = geneSolr.query(q);
                SolrDocumentList documentList = queryResponse.getResults();
                return documentList.getNumFound();
            }
        });

        return new Iterable<AtlasGene>() {
            public Iterator<AtlasGene> iterator() {
                return new Iterator<AtlasGene>() {
                    private Iterator<AtlasGene> genes = null;
                    private int totalSeen = 0;

                    public boolean hasNext() {
                        if (null == genes
                                ||
                                (!genes.hasNext() && totalSeen < total)) {
                            getNextGeneBatch();
                        }

                        return totalSeen < total && genes.hasNext();
                    }

                    public AtlasGene next() {
                        if (null == genes
                                ||
                                (!genes.hasNext() && totalSeen < total)) {
                            getNextGeneBatch();
                        }

                        totalSeen++;
                        return genes.next();
                    }

                    public void remove() {
                    }

                    private void getNextGeneBatch() {
                        log.debug("Loading next batch of genes, seen " + totalSeen + " out of " + total);
                        genes = futureWrap(new Callable<Iterator<AtlasGene>>() {
                            @Override
                            public Iterator<AtlasGene> call() throws SolrServerException {
                                List<AtlasGene> geneList = new ArrayList<AtlasGene>();
                                q.setRows(50);
                                q.setStart(totalSeen);

                                QueryResponse queryResponse = geneSolr.query(q);
                                SolrDocumentList documentList = queryResponse.getResults();

                                for (SolrDocument d : documentList) {
                                    AtlasGene g = new AtlasGene(d);
                                    geneList.add(g);
                                }
                                return geneList.iterator();
                            }
                        });
                    }
                };
            }
        };
    }

    private <T> T futureWrap(Callable<T> task) {
        Future<T> f = executorService.submit(task);
        try {
            return f.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            // InterruptedException can happen due to a cancel action by the user downloading
            // e.g. analytics off the experiment page
            return null;
        } catch (ExecutionException e) {
            throw createUnexpected("Getting gene data from solr failure", e.getCause());
        }
    }

    public void shutdown() {
        executorService.shutdown();
    }

    public static class AtlasGeneResult {
        private AtlasGene gene;
        private boolean multi;

        private AtlasGeneResult(AtlasGene gene, boolean multi) {
            this.gene = gene;
            this.multi = multi;
        }

        public AtlasGene getGene() {
            return gene;
        }

        public boolean isMulti() {
            return multi;
        }

        public boolean isFound() {
            return gene != null;
        }
    }
}
