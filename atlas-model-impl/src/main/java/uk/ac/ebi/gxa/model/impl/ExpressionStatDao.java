package uk.ac.ebi.gxa.model.impl;

import uk.ac.ebi.gxa.model.*;
import uk.ac.ebi.gxa.utils.Pair;
import uk.ac.ebi.gxa.utils.EscapeUtil;
import uk.ac.ebi.gxa.utils.FilterIterator;
import uk.ac.ebi.gxa.utils.MappingIterator;
import static uk.ac.ebi.gxa.utils.EscapeUtil.nullzero;
import uk.ac.ebi.ae3.indexbuilder.ExperimentsTable;
import uk.ac.ebi.ae3.indexbuilder.Experiment;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.common.SolrDocument;
import org.apache.commons.lang.StringUtils;

import java.util.*;

/**
 * @author pashky
 */
public class ExpressionStatDao {
    private SolrServer geneServer;
    private Dao dao;

    public void setGeneServer(SolrServer geneServer) {
        this.geneServer = geneServer;
    }

    public void setDao(Dao dao) {
        this.dao = dao;
    }

    
    private static final EnumMap<ExpressionQuery,String> SCORE_EXP_MAP = new EnumMap<ExpressionQuery,String>(ExpressionQuery.class);

    static {
        SCORE_EXP_MAP.put(ExpressionQuery.UP, "_up");
        SCORE_EXP_MAP.put(ExpressionQuery.DOWN, "_dn");
        SCORE_EXP_MAP.put(ExpressionQuery.UP_OR_DOWN, "_ud");
    }
    
    private class SolrQueryBuilder {
        private StringBuilder queryPart = new StringBuilder();
        private StringBuilder scorePart = new StringBuilder();

        private Set<Pair<String,String>> allEfEfvs = new HashSet<Pair<String, String>>();
        private Set<String> allExps = new HashSet<String>();

        public SolrQueryBuilder andGeneProperty(String property, Iterable<String> values) {
            if(and())
                return this;

            final String valuesString = EscapeUtil.escapeSolrValueList(values);
            if("".equals(property)) {
                queryPart.append("(gene_ids:(").append(valuesString).append(")")
                        .append(" gene_desc:(").append(valuesString).append("))");
            } else {
                // TODO: rewrite this part
                final String field = "id".equals(property) ? "gene_id" : GeneProperties.convertPropertyToSearchField(property);
                if(field == null)
                    throw new NullPointerException("Can't find property");

                queryPart.append(field).append(":(").append(valuesString).append(")");
            }
            return this;
        }

        public SolrQueryBuilder andActive(ExpressionQuery expq, String property, Iterable<String> values) {
            if(and())
                return this;

            boolean first = true;
            
            for(String v : values) {
                allEfEfvs.add(new Pair<String, String>(property, v));
                if(first)
                    queryPart.append("(");
                first = false;

                final String efefvId = EscapeUtil.encode(property, v);
                String field = "cnt_" + efefvId;
                switch(expq)
                {
                    case UP: queryPart.append(field).append("_up:[* TO *]"); break;
                    case DOWN: queryPart.append(field).append("_dn:[* TO *]"); break;
                    case UP_OR_DOWN: queryPart.append(field).append("_up:[* TO *] ")
                            .append(field).append("_dn:[* TO *]"); break;
                    default:
                        throw new IllegalArgumentException("Unknown regulation option specified " + expq);
                }
                if(scorePart.length() > 0)
                    scorePart.append(",");
                scorePart.append("s_").append(efefvId).append(SCORE_EXP_MAP.get(expq));
            }

            if(!first)
                queryPart.append(")");
            
            return this;
        }

        public SolrQueryBuilder andExperiment(Iterator<String> experiments, ExpressionQuery expq) {
            if(and())
                return this;

            String expIds = StringUtils.join(experiments, " ");
            Collections.addAll(allExps, expIds.split(" ")); // TODO: optimize this

            if(expIds.length() > 0) {
                queryPart.append("(");
                if(expq == ExpressionQuery.UP || expq == ExpressionQuery.UP_OR_DOWN)
                    queryPart.append("exp_up_ids:(").append(expIds).append(") ");
                if(expq == ExpressionQuery.DOWN || expq == ExpressionQuery.UP_OR_DOWN)
                    queryPart.append("exp_dn_ids:(").append(expIds).append(") ");
                queryPart.append(")");
            }
            return this;
        }

        private boolean and() {
            if(queryPart.length() > 0)
                queryPart.append(" AND ");
            return false;
        }

        public String toSolrQuery() {
            return queryPart.toString() + (scorePart.length() > 0 ? " AND _val_:\"sum(" + scorePart.toString() + ")\"" : "");
        }

        boolean hasExperiment(String experiment) {
            return allExps.contains(experiment);
        }

        boolean hasEfEfv(String ef, String efv) {
            return allEfEfvs.contains(new Pair<String, String>(ef, efv));
        }
    }

    public <T extends ExpressionStat> FacetQueryResultSet<T, ExpressionStatFacet> getExpressionStat(ExpressionStatQuery atlasExpressionStatQuery, PageSortParams pageSortParams) throws GxaException {
        SolrQueryBuilder sqb = new SolrQueryBuilder();


        for(GeneQuery geneq : atlasExpressionStatQuery.getGeneQueries()) {
            appendGeneQuery(sqb, geneq);
        }

        for(Pair<ExpressionQuery,PropertyQuery> propq : atlasExpressionStatQuery.getActivityQueries()) {
            QueryResultSet<Property> props = dao.getProperty(propq.getSecond().isAssayProperty(true));
            for(Property property : props.getItems()) {
                sqb.andActive(propq.getFirst(), property.getAccession(), property.getValues());
            }
        }

        SolrQuery solrq = new SolrQuery(sqb.toSolrQuery());
        solrq.addField("*");
        solrq.addField("score");
        solrq.setRows(pageSortParams.getRows());
        solrq.setStart(pageSortParams.getStart());
                
        final Set<String> autoFactors = new HashSet<String>();
        for(Property property : dao.getProperty(new PropertyQuery().isAssayProperty(true), PageSortParams.ALL).getItems())
            if(property.getAccession() != null)
                autoFactors.add(property.getAccession());

        if(atlasExpressionStatQuery.isFacets()) {
            solrq.setFacet(true);
            solrq.setFacetMinCount(2);
            solrq.setFacetLimit(100);

            solrq.setFacetSort(true);
            for(String factor : autoFactors) {
                solrq.addFacetField("efvs_up_" + factor);
                solrq.addFacetField("efvs_ud_" + factor);
                solrq.addFacetField("exp_up_ids");
                solrq.addFacetField("exp_dn_ids");
            }
            for(GeneProperties.Prop p : GeneProperties.allDrillDowns()) {
                solrq.addFacetField(p.facetField);
            }
        }

        try {
            QueryResponse response = geneServer.query(solrq);
            List<T> items = new ArrayList<T>();
            for(SolrDocument document : response.getResults()) {
                final SolrDocument sd = document;


                final Iterable<Property> properties = new Iterable<Property>() {
                    public Iterator<Property> iterator() {
                        return new Iterator<Property>() {
                            private Iterator<Pair<String,Iterator>> fIter = new FilterIterator<String,Pair<String,Iterator>>(autoFactors.iterator()) {
                                public Pair<String, Iterator> map(String factor) {
                                    Collection vals = sd.getFieldValues("efvs_ud_" + factor);
                                    return vals == null || vals.isEmpty() ? null : new Pair<String, Iterator>(factor, vals.iterator());
                                }
                            };

                            private Pair<String,Iterator> current = null;

                            public boolean hasNext() {
                                return fIter.hasNext() || (current != null && current.getSecond().hasNext());
                            }

                            public Property next() {
                                if(current == null) {
                                    current = fIter.next();
                                }
                                final String factor = current.getFirst();
                                final String value = current.getSecond().next().toString();
                                if(!current.getSecond().hasNext())
                                    current = fIter.next();
                                
                                return new Property() {
                                    public int getId() {
                                        return 0;
                                    }

                                    public String getAccession() {
                                        return factor;
                                    }

                                    public Collection<String> getValues() {
                                        return Collections.singletonList(value);
                                    }
                                };
                            }

                            public void remove() { }
                        };
                    }
                };

                GeneExpressionStat<PropertyExpressionStat<ExperimentExpressionStat>> geneStat = new GeneExpressionStat<PropertyExpressionStat<ExperimentExpressionStat>>() {
                    private CountCache countCache = null;

                    public String getGene() {
                        return sd.getFirstValue("gene_id").toString();
                    }

                    public Float getRank() {
                        return Float.valueOf(sd.getFieldValue("score").toString());
                    }

                    public Integer getUpExperimentsCount() {
                        return countCache != null ? countCache.upExperimentsCount : (countCache = sumCache(getDrillDown())).upExperimentsCount;
                    }

                    public Integer getDnExperimentsCount() {
                        return countCache != null ? countCache.dnExperimentsCount : (countCache = sumCache(getDrillDown())).dnExperimentsCount;
                    }

                    public Double getUpPvalue() {
                        return countCache != null ? countCache.upPvalue : (countCache = sumCache(getDrillDown())).upPvalue;
                    }

                    public Double getDnPvalue() {
                        return countCache != null ? countCache.dnPvalue : (countCache = sumCache(getDrillDown())).dnPvalue;
                    }

                    public Iterable<PropertyExpressionStat<ExperimentExpressionStat>> getDrillDown() {
                        return new Iterable<PropertyExpressionStat<ExperimentExpressionStat>>() {
                            public Iterator<PropertyExpressionStat<ExperimentExpressionStat>> iterator() {
                                return new MappingIterator<Property, PropertyExpressionStat<ExperimentExpressionStat>>(properties.iterator()) {
                                    @Override
                                    public PropertyExpressionStat<ExperimentExpressionStat> map(final Property property) {
                                        final String factor = property.getAccession();
                                        final String value = property.getValues().iterator().next();
                                        final String fieldId = EscapeUtil.encode(factor, value);
                                        return new PropertyExpressionStat<ExperimentExpressionStat>() {
                                            public Property getProperty() {
                                                return property;
                                            }

                                            public Float getRank() {
                                                return 2.0f - (float)(getUpPvalue() + getDnPvalue());
                                            }

                                            public Integer getUpExperimentsCount() {
                                                return nullzero((Short)sd.getFieldValue("cnt_" + fieldId + "_up"));
                                            }

                                            public Integer getDnExperimentsCount() {
                                                return nullzero((Short)sd.getFieldValue("cnt_" + fieldId + "_dn"));
                                            }

                                            public Double getUpPvalue() {
                                                return nullzero((Float)sd.getFieldValue("minpval_" + fieldId + "_up"));
                                            }

                                            public Double getDnPvalue() {
                                                return nullzero((Float)sd.getFieldValue("minpval_" + fieldId + "_dn"));
                                            }

                                            public Iterable<ExperimentExpressionStat> getDrillDown() {
                                                final ExperimentsTable table = ExperimentsTable.deserialize((String)sd.getFieldValue("exp_info"));
                                                return new Iterable<ExperimentExpressionStat>() {
                                                    public Iterator<ExperimentExpressionStat> iterator() {
                                                        return new FilterIterator<Experiment, ExperimentExpressionStat>(table.findByEfEfv(factor, value).iterator()) {
                                                            @Override
                                                            public ExperimentExpressionStat map(final Experiment experimentExpression) {
                                                                return new ExperimentExpressionStat() {
                                                                    public String getExperiment() {
                                                                        try {
                                                                            return dao.getExperimentByAccession(new AccessionQuery<AccessionQuery>().hasId(String.valueOf(experimentExpression.getId()))).getAccession();
                                                                        } catch(GxaException e) {
                                                                            throw new RuntimeException(e);
                                                                        }
                                                                    }

                                                                    public Float getRank() {
                                                                        return 2.0f - (float)(getUpPvalue() + getDnPvalue());
                                                                    }

                                                                    public Integer getUpExperimentsCount() { return experimentExpression.getExpression().isUp() ? 1 : 0; }
                                                                    public Integer getDnExperimentsCount() { return experimentExpression.getExpression().isUp() ? 0 : 1; }

                                                                    public Double getUpPvalue() {
                                                                        return experimentExpression.getPvalue();
                                                                    }

                                                                    public Double getDnPvalue() {
                                                                        return experimentExpression.getPvalue();
                                                                    }

                                                                    public Iterable<ExpressionStat> getDrillDown() { return null; }
                                                                };
                                                            }
                                                        };
                                                    }
                                                };
                                            }
                                        };
                                    }
                                };
                            }
                        };
                    }
                };
                items.add((T)geneStat);
            }

            final FacetQueryResultSet<T, ExpressionStatFacet> resultSet = new FacetQueryResultSet<T, ExpressionStatFacet>();
            resultSet.setItems(items);
            resultSet.setIsMulti(true);
            resultSet.setStartingFrom(pageSortParams.getStart());
            resultSet.setTotalResults((int)response.getResults().getNumFound());

            if(atlasExpressionStatQuery.isFacets() && response.getFacetFields() != null) {
                Map<String, ExpressionStatFacet> facetsMap = new HashMap<String, ExpressionStatFacet>();
                for(FacetField ff : response.getFacetFields()) {
                    if(ff.getValueCount() > 1) {
                        if(ff.getName().startsWith("efvs_")) {
                            String ef = ff.getName().substring(8);
                            ExpressionStatFacet facet = facetsMap.get(ef);
                            if(facet == null)
                                facetsMap.put(ef, facet = new ExpressionStatFacet(ef));

                            for (FacetField.Count ffc : ff.getValues())
                                if(!sqb.hasEfEfv(ef, ffc.getName()))
                                {
                                    int count = (int)ffc.getCount();
                                    facet.getOrCreateValue(ffc.getName())
                                            .add(count, ff.getName().substring(5,7).equals("up"));
                                }
                        } else if(ff.getName().startsWith("exp_")) {
                            ExpressionStatFacet facet = facetsMap.get("experiment");
                            if(facet == null)
                                facetsMap.put("experiment", facet = new ExpressionStatFacet("experiment"));
                            for (FacetField.Count ffc : ff.getValues())
                                if(!sqb.hasExperiment(ffc.getName()))
                                {
                                    try {
                                        uk.ac.ebi.gxa.model.Experiment exp = dao.getExperiment(new ExperimentQuery().hasId(ffc.getName())).getItem();
                                        if(exp != null) {
                                            String expName = exp.getAccession();
                                            if(expName != null)
                                            {
                                                int count = (int)ffc.getCount();
                                                facet.getOrCreateValue(expName)
                                                        .add(count, ff.getName().substring(4,6).equals("up"));
                                            }
                                        }
                                    } catch (GxaException e) {
                                        throw new RuntimeException(e);
                                    }
                                }
                        } else if(ff.getName().startsWith("gene_")) {
                            String id = GeneProperties.findPropByFacetField(ff.getName()).id;
                            ExpressionStatFacet facet = facetsMap.get(id);
                            if(facet == null)
                                facetsMap.put(id, facet = new ExpressionStatFacet(id));
                            for (FacetField.Count ffc : ff.getValues()) {
                                int count = (int)ffc.getCount();
                                facet.getOrCreateValue(ffc.getName()).add(count, true);
                                facet.getOrCreateValue(ffc.getName()).add(count, false);
                            }
                        }
                    }

                }

                for(ExpressionStatFacet facet : facetsMap.values())
                    resultSet.addFacet(facet);
            }

            return resultSet;
        } catch(SolrServerException e) {
            throw new GxaException("Solr server exception", e);
        }
    }

    private void appendGeneQuery(SolrQueryBuilder sqb, GeneQuery geneq) throws GxaException {
        
        for(GenePropertyQuery propertyQuery : geneq.getPropertyQueries()) {
            List<String> values = new ArrayList<String>(propertyQuery.getValues());
            values.addAll(propertyQuery.getFullTextQueries());
            sqb.andGeneProperty(propertyQuery.getAccession(), values);
        }

        for(ExperimentQuery expq : geneq.getExperimentQueries()) {
            QueryResultSet<uk.ac.ebi.gxa.model.Experiment> exps = dao.getExperiment(expq);
            if(exps.isFound())
                sqb.andExperiment(new MappingIterator<uk.ac.ebi.gxa.model.Experiment,String>(exps.getItems().iterator()) {
                    public String map(uk.ac.ebi.gxa.model.Experiment experiment) {
                        return String.valueOf(experiment.getId());
                    }
                }, ExpressionQuery.UP_OR_DOWN);
        }

        if(!geneq.getSpecies().isEmpty())
            sqb.andGeneProperty("species", geneq.getSpecies());

        if(geneq.getId() != null)
            sqb.andGeneProperty("id", Collections.singleton(geneq.getId()));
        if(geneq.getAccession() != null)
            sqb.andGeneProperty("identifier", Collections.singleton(geneq.getAccession()));
    }

    private static String getSafeSolrFieldValue(SolrDocument sd, String name) {
        return sd.getFieldValue(name) != null ? sd.getFieldValue(name).toString() : null;
    }

    private static Collection<String> getSafeSolrFieldValues(SolrDocument sd, String name) {
        return sd.getFieldValues(name) != null ? (Collection)sd.getFieldValues(name) : new ArrayList<String>();
    }

    public QueryResultSet<Gene> getGene(GeneQuery geneQuery, PageSortParams pageSortParams) throws GxaException {
        SolrQueryBuilder sqb = new SolrQueryBuilder();
        appendGeneQuery(sqb, geneQuery);

        SolrQuery solrq = new SolrQuery(sqb.toSolrQuery());
        solrq.addField("*");
        solrq.addField("score");
        solrq.setRows(pageSortParams.getRows());
        solrq.setStart(pageSortParams.getStart());

        try {
            QueryResponse response = geneServer.query(solrq);
            List<Gene> genes = new ArrayList<Gene>();
            for(SolrDocument document : response.getResults()) {
                final String species = getSafeSolrFieldValue(document, "gene_species");
                final String id = getSafeSolrFieldValue(document, "gene_id");
                final String accession = getSafeSolrFieldValue(document, "gene_name");

                final Map<String,Property> propertiesMap = new HashMap<String,Property>();

                for(GeneProperties.Prop propertyDesc : GeneProperties.allProperties()) {
                    final String name = propertyDesc.id;
                    final Collection<String> values = getSafeSolrFieldValues(document, propertyDesc.searchField);
                    propertiesMap.put(name, new Property() {

                        public Collection<String> getValues() {
                            return values;
                        }

                        public int getId() {
                            return 0;  // TODO: solve this
                        }

                        public String getAccession() {
                            return name;
                        }
                    });
                }

                final PropertyCollection properties = new PropertyCollection() {
                    public Collection<Property> getProperties() {
                        return propertiesMap.values();
                    }

                    public Property getByAccession(String accession) {
                        return propertiesMap.get(accession);
                    }
                };

                genes.add(new Gene() {
                    public String getSpecies() {
                        return species;
                    }

                    public int getId() {
                        return Integer.valueOf(id);
                    }

                    public String getAccession() {
                        return accession;
                    }

                    public PropertyCollection getProperties() {
                        return properties;
                    }
                });
            }

            final QueryResultSet<Gene> resultSet = new QueryResultSet<Gene>();
            resultSet.setItems(genes);
            resultSet.setIsMulti(true);
            resultSet.setStartingFrom(pageSortParams.getStart());
            resultSet.setTotalResults((int)response.getResults().getNumFound());
            return resultSet;
        } catch (SolrServerException sse) {
            throw new GxaException(sse);
        }
    }

    private static class CountCache {
        int upExperimentsCount = 0;
        int dnExperimentsCount = 0;
        double upPvalue = 1;
        double dnPvalue = 1;
    }

    static private CountCache sumCache(Iterable<? extends ExpressionStat> tosum) {
        CountCache result = new CountCache();
        for(ExpressionStat es : tosum) {
            result.upExperimentsCount += es.getUpExperimentsCount();
            result.dnExperimentsCount += es.getDnExperimentsCount();
            result.upPvalue = Math.min(es.getUpPvalue(), result.upPvalue);
            result.dnPvalue = Math.min(es.getDnPvalue(), result.dnPvalue);
        }
        return result;
    }

}
