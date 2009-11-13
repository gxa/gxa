package uk.ac.ebi.gxa.model.impl;

import uk.ac.ebi.gxa.model.*;
import uk.ac.ebi.gxa.utils.Pair;
import uk.ac.ebi.gxa.utils.EscapeUtil;
import uk.ac.ebi.gxa.utils.FilterIterator;
import uk.ac.ebi.gxa.utils.MappingIterator;
import uk.ac.ebi.ae3.indexbuilder.ExperimentsTable;
import uk.ac.ebi.ae3.indexbuilder.Experiment;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;

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

        public SolrQueryBuilder andGeneProperty(String property, Iterable<String> values) {
            if(and())
                return this;

            // TODO: rewrite this part
            final String field = "id".equals(property) ? "gene_id" : GeneProperties.convertPropertyToSearchField(property);
            if(field == null)
                throw new NullPointerException("Can't find property");
            
            queryPart.append(field).append(":(").append(EscapeUtil.escapeSolrValueList(values)).append(")");
            return this;
        }

        public SolrQueryBuilder andActive(ExpressionQuery expq, String property, Iterable<String> values) {
            if(and())
                return this;

            boolean first = true;
            
            for(String v : values) {
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

        public SolrQueryBuilder andExperiment(String experiment) {
            // TODO: experiment support
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
    }

    public <T extends ExpressionStat> QueryResultSet<T> getExpressionStat(ExpressionStatQuery atlasExpressionStatQuery, PageSortParams pageSortParams) throws GxaException {
        SolrQueryBuilder sqb = new SolrQueryBuilder();

        for(GeneQuery geneq : atlasExpressionStatQuery.getGeneQueries()) {
            for(PropertyQuery propertyQuery : geneq.getPropertyQueries()) {
                List<String> values = new ArrayList<String>(propertyQuery.getValues());
                values.addAll(propertyQuery.getFullTextQueries());
                sqb.andGeneProperty(propertyQuery.getAccession(), values);
            }
            for(ExperimentQuery expq : geneq.getExperimentQueries()) {
                QueryResultSet<uk.ac.ebi.gxa.model.Experiment> exps = dao.getExperiment(expq);
                for(uk.ac.ebi.gxa.model.Experiment experiment : exps.getItems())
                    sqb.andExperiment(experiment.getAccession());
            }
            if(geneq.getId() != null)
                sqb.andGeneProperty("id", Collections.singleton(geneq.getId()));
            if(geneq.getAccession() != null)
                sqb.andGeneProperty("identifier", Collections.singleton(geneq.getAccession()));
        }

        for(Pair<ExpressionQuery,PropertyQuery> propq : atlasExpressionStatQuery.getActivityQueries()) {
            QueryResultSet<Property> props = dao.getProperty(propq.getSecond());
            for(Property property : props.getItems())
                sqb.andActive(propq.getFirst(), property.getAccession(), property.getValues());
        }

        SolrQuery solrq = new SolrQuery(sqb.toSolrQuery());
        solrq.addField("*");
        solrq.addField("score");
        solrq.setRows(pageSortParams.getRows());
        solrq.setStart(pageSortParams.getStart());

        final Set<String> autoFactors = new HashSet<String>();
        for(Property property : dao.getProperty(new PropertyQuery().isAssayProperty(true)).getItems())
            autoFactors.add(property.getAccession());

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
                                    return new Pair<String, Iterator>(factor, sd.getFieldValues("efvs_ud_" + factor).iterator());
                                }
                            };

                            private Pair<String,Iterator> current = null;

                            public boolean hasNext() {
                                return fIter.hasNext() && current != null && current.getSecond().hasNext();
                            }

                            public Property next() {
                                if(current == null) {
                                    current = fIter.next();
                                }
                                final String factor = current.getFirst();
                                final String value = current.getSecond().next().toString();
                                return new Property() {
                                    public int getId() {
                                        return 0;
                                    }

                                    public String getAccession() {
                                        return factor;
                                    }

                                    public String getName() {
                                        return getAccession();
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
                        return sd.getFirstValue("gene_identifier").toString();
                    }

                    public Float getRank() {
                        return Float.valueOf(sd.getFieldValue("score").toString());
                    }

                    public Integer getUpExperimentsCount() {
                        return countCache != null ? countCache.upExperimentsCount : (countCache = sumCache(drillDown())).upExperimentsCount;
                    }

                    public Integer getDnExperimentsCount() {
                        return countCache != null ? countCache.dnExperimentsCount : (countCache = sumCache(drillDown())).dnExperimentsCount;
                    }

                    public Double getUpPvalue() {
                        return countCache != null ? countCache.upPvalue : (countCache = sumCache(drillDown())).upPvalue;
                    }

                    public Double getDnPvalue() {
                        return countCache != null ? countCache.dnPvalue : (countCache = sumCache(drillDown())).dnPvalue;
                    }

                    public Iterable<PropertyExpressionStat<ExperimentExpressionStat>> drillDown() {
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
                                                return Integer.valueOf(sd.getFieldValue("cnt_up_" + fieldId).toString());
                                            }

                                            public Integer getDnExperimentsCount() {
                                                return Integer.valueOf(sd.getFieldValue("cnt_dn_" + fieldId).toString());
                                            }

                                            public Double getUpPvalue() {
                                                return Double.valueOf(sd.getFieldValue("minpval_up_" + fieldId).toString());
                                            }

                                            public Double getDnPvalue() {
                                                return Double.valueOf(sd.getFieldValue("minpval_dn_" + fieldId).toString());
                                            }

                                            public Iterable<ExperimentExpressionStat> drillDown() {
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

                                                                    public Integer getUpExperimentsCount() { return null; }
                                                                    public Integer getDnExperimentsCount() { return null; }

                                                                    public Double getUpPvalue() {
                                                                        return experimentExpression.getPvalue();
                                                                    }

                                                                    public Double getDnPvalue() {
                                                                        return experimentExpression.getPvalue();
                                                                    }

                                                                    public Iterable<ExpressionStat> drillDown() { return null; }
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
            final QueryResultSet<T> resultSet = new QueryResultSet<T>();
            resultSet.setItems(items);
            resultSet.setIsMulti(true);
            resultSet.setStartingFrom(pageSortParams.getStart());
            resultSet.setTotalResults((int)response.getResults().getNumFound());
            return resultSet;
        } catch(SolrServerException e) {
            throw new GxaException("Solr server exception", e);
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

    public <T extends ExpressionStat> QueryResultSet<T> getExpressionStat(ExpressionStatQuery atlasExpressionStatQuery) throws GxaException {
        return getExpressionStat(atlasExpressionStatQuery, new PageSortParams());
    }

}
