package ae3.service.structuredquery;

import java.util.*;
import java.rmi.RemoteException;

import ae3.ols.webservice.axis.Query;
import ae3.ols.webservice.axis.QueryServiceLocator;
import ae3.model.AtlasGene;
import ae3.util.QueryHelper;

import javax.xml.rpc.ServiceException;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.core.CoreContainer;
import org.apache.solr.core.SolrCore;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.index.IndexReader;

/**
 * @author pashky
 */
public class AtlasStructuredQueryService {

    private static final int MAX_CONDITION_EFVS = 100;
    private static final String CORE_ATLAS = "atlas";
    private static final String CORE_EXPT = "expt";
    public static final String FIELD_FACTOR_PREFIX = "dwe_ba_";

    public static final String[] GENE_FACETS = { "species", "goterm", "interproterm" };

    private Log log = LogFactory.getLog(AtlasStructuredQueryService.class);

    private SolrCore coreExpt;
    private SolrServer solrAtlas;
    private SolrServer solrExpt;
    private Set<String> allFactors;

    public AtlasStructuredQueryService(CoreContainer coreContainer) {
        this.coreExpt = coreContainer.getCore(CORE_EXPT);
        this.solrAtlas = new EmbeddedSolrServer(coreContainer, CORE_ATLAS);
        this.solrExpt = new EmbeddedSolrServer(coreContainer, CORE_EXPT);
        this.allFactors = getExperimentalFactorOptions();
    }

    /**
     * Process structured Atlas query
     * @param query parsed query
     * @return matching results
     * @throws java.io.IOException
     */
    public AtlasStructuredQueryResult doStructuredAtlasQuery(final AtlasStructuredQuery query) {

        final StringBuffer solrq = new StringBuffer();
        final EfvTree<Integer> queryEfvs = new EfvTree<Integer>();

        final Iterable<AtlasStructuredQueryResult.Condition> conditions = appendEfvsQuery(query, solrq, queryEfvs);
        appendGeneQuery(query, solrq);
        appendSpeciesQuery(query, solrq);

        boolean hasQueryEfvs = queryEfvs.getNumEfvs() > 0;

        log.info("Solr query is: " + solrq + " calling for EFVs: " + (hasQueryEfvs ? queryEfvs : "(none)"));

        AtlasStructuredQueryResult result = new AtlasStructuredQueryResult(query.getStart(), query.getRows());
        result.setConditions(conditions);

        if(solrq.length() > 0)
        {
            try {
                SolrQuery q = setupSolrQuery(query, solrq.toString(), queryEfvs);
                QueryResponse response = solrAtlas.query(q);

                processResultGenes(response, result, queryEfvs);

                result.setEfvFacet(getEfvFacet(response, queryEfvs));
                for(String s : GENE_FACETS) {
                    result.setGeneFacet(s, getGeneFacet(response, "gene_" + s + "_exact"));
                }
            } catch (SolrServerException e) {
                log.error(e);
            }            
        }

        return result;
    }

    private Iterable<AtlasStructuredQueryResult.Condition> appendEfvsQuery(AtlasStructuredQuery query,
                                                                           StringBuffer solrq,
                                                                           EfvTree<Integer> queryEfvs) {
        final List<AtlasStructuredQueryResult.Condition> conds = new ArrayList<AtlasStructuredQueryResult.Condition>();
        final StringBuffer efvq = new StringBuffer();
        final StringBuffer scores = new StringBuffer();

        scores.append("sum(");

        int number = 0;
        out: for(AtlasStructuredQuery.Condition c : query.getConditions())
        {
            if(c.isAnything()) {
                if(efvq.length() > 0) {
                    efvq.append(" AND ");
                }
                switch(c.getExpression())
                {
                    case UP:
                        efvq.append("exp_up_ids:[1 TO *]");
                        scores.append("exp_up_ids");
                        break;
                    case DOWN:
                        efvq.append("exp_dn_ids:[1 TO *]");
                        scores.append("exp_dn_ids");
                        break;
                    case UP_DOWN:
                        // nothing special, just match the gene
                        break;
                }

                // conds.add(new AtlasStructuredQueryResult.Condition(c, new EfvTree<Boolean>()));
            } else {
                try {
                    EfvTree<Boolean> condEfvs = getConditionEfvs(c);
                    if(condEfvs.getNumEfvs() > 0)
                    {
                        if(efvq.length() > 0) {
                            efvq.append(" AND ");
                        }
                        efvq.append("(");
                        for(EfvTree.EfEfv<Boolean> condEfv : condEfvs.getNameSortedList())
                        {
                            efvq.append(" ");
                            if(number > 0)
                                scores.append(",");

                            queryEfvs.getOrCreate(condEfv.getEf(), condEfv.getEfv(), number);

                            String efefvId = condEfv.getEfEfvId();
                            String cnt = "cnt_efv_" + efefvId;
                            String pvl = "avgpval_efv_" + efefvId;

                            switch(c.getExpression())
                            {
                                case UP:
                                    efvq.append(cnt).append("_up:[1 TO *]");
                                    scores.append("product(").append(cnt).append("_up,linear(")
                                            .append(pvl).append("_up,-1,1)),")
                                            .append("product(").append(cnt).append("_dn,linear(")
                                            .append(pvl).append("_dn,1,-1))");
                                    break;

                                case DOWN:
                                    efvq.append(cnt).append("_dn:[1 TO *]");
                                    scores.append("product(").append(cnt).append("_up,linear(")
                                            .append(pvl).append("_up,1,-1)),")
                                            .append("product(").append(cnt).append("_dn,linear(")
                                            .append(pvl).append("_dn,-1,1))");
                                    break;

                                case UP_DOWN:
                                    efvq.append(cnt).append("_up:[1 TO *] ")
                                            .append(cnt).append("_dn:[1 TO *]");
                                    scores.append("product(").append(cnt).append("_up,linear(")
                                            .append(pvl).append("_up,-1,1)),")
                                            .append("product(").append(cnt).append("_dn,linear(")
                                            .append(pvl).append("_dn,-1,1))");
                                    break;

                                default:
                                    throw new IllegalArgumentException("Unknown regulation option specified " + c.getExpression());
                            }

                            ++number;

                            if(number > MAX_CONDITION_EFVS)
                                break out;
                        }
                        efvq.append(")");
                    }
                    conds.add(new AtlasStructuredQueryResult.Condition(c, condEfvs));
                } catch (SolrServerException e) {
                    log.error(e);
                } catch (RemoteException e) {
                    log.error(e);
                }
            }
        }
        scores.append(")");

        if(efvq.length() > 0) {
            if(solrq.length() > 0)
                solrq.append(" AND ");
            solrq.append(efvq);
            solrq.append(" AND _val_:\"").append(scores).append("\"");
        }

        return conds;
    }

    private StringBuffer appendGeneQuery(AtlasStructuredQuery query, StringBuffer solrq) {
        if(query.getGene().matches(".*\\S.*"))
        {
            if(solrq.length() > 0)
                solrq.append(" AND ");
            solrq.append("gene_desc:(").append(query.getGene()).append(")");
        }

        return solrq;
    }

    private StringBuffer appendSpeciesQuery(AtlasStructuredQuery query, StringBuffer solrq) {
        if(!query.getSpecies().isEmpty())
        {
            if(solrq.length() > 0)
                solrq.append(" AND ");
            solrq.append("gene_species:(");
            for(String s : query.getSpecies())
            {
                solrq.append(" \"").append(s).append("\"");
            }
            solrq.append(")");
        }
        return solrq;
    }

    private EfvTree<Boolean> getConditionEfvs(AtlasStructuredQuery.Condition c) throws RemoteException, SolrServerException {
        if(c.isAnyValue())
            return getCondEfvsAllForFactor(c.getFactor());

        if(c.isAnyFactor())
            return getCondEfvsForAnyFactor(c.getFactorValues());

        return getCondEfvsForFactor(c.getFactor(), c.getFactorValues());
    }

    private EfvTree<Boolean> getCondEfvsAllForFactor(String factor) {
        EfvTree<Boolean> condEfvs = new EfvTree<Boolean>();
        for(String v : autoCompleteFactorValues(factor, null, MAX_CONDITION_EFVS).keySet()) {
            condEfvs.getOrCreate(factor, v, true);
        }
        return condEfvs;
    }

    private EfvTree<Boolean> getCondEfvsForFactor(String factor, Iterable<String> values) throws RemoteException, SolrServerException {

        EfvTree<Boolean> condEfvs = new EfvTree<Boolean>();
        for(String val : values) {
            if(val.length() > 0) {
                for(String v : autoCompleteFactorValues(factor, val, MAX_CONDITION_EFVS).keySet()) {
                    condEfvs.getOrCreate(factor, v, true);
                }
            }
        }
        return condEfvs;
    }

    private EfvTree<Boolean> getCondEfvsForAnyFactor(Iterable<String> values) throws RemoteException, SolrServerException {
        StringBuffer expQuery = new StringBuffer();
        Query olsQuery = null;
        try {
            olsQuery = new QueryServiceLocator().getOntologyQuery();
        } catch(ServiceException e) {
            log.error(e);
        }

        EfvTree<Boolean> condEfvs = new EfvTree<Boolean>();
        for(String fv : values) {
            if(fv.length() > 0) {
                expQuery.append(" exp_factor_values:").append("\"").append(fv).append("\"");
                if(olsQuery != null) {
                    @SuppressWarnings("unchecked")
                    HashMap<String,String> terms = olsQuery.getTermsByExactName(fv, "EFO");
                    Set<String> ontologyExpansion = new TreeSet<String>();

                    for (String term : terms.keySet()) {
                        @SuppressWarnings("unchecked")
                        HashMap<String,String> termChildren = olsQuery.getTermChildren(term, "EFO", -1, new int[] {1,2,3,4});
                        ontologyExpansion.addAll(termChildren.values());
                    }

                    for (String term : ontologyExpansion) {
                        expQuery.append(" exp_factor_values_exact:").append("\"").append(term).append("\"");
                    }
                }
            }
        }

        SolrQuery q = new SolrQuery(expQuery.toString());
        q.setHighlight(true);
        q.addHighlightField("exp_factor_values");
        q.addHighlightField("exp_factor_values_exact");
        q.setHighlightSnippets(100);
        q.setRows(500);
        q.setStart(0);
        q.setFilterQueries("exp_in_dw:true");

        QueryResponse response = solrExpt.query(q);
        if (response == null || response.getResults().getNumFound() == 0)
            return condEfvs;

        Map<String, SolrDocument> docmap = QueryHelper.convertSolrDocumentListToMap(response,
                coreExpt.getSearcher().get().getSchema().getUniqueKeyField().getName());

        for (Map.Entry<String,Map<String, List<String>>> hdoc : response.getHighlighting().entrySet()) {
            if (hdoc.getValue() == null || hdoc.getValue().size() == 0)
                continue;
            for(List<String> efvs : hdoc.getValue().values()) {
                for(String efv : efvs) {
                    efv = efv.replaceAll("</{0,1}em>","");
                    String ef = null;
                    SolrDocument doc = docmap.get(hdoc.getKey());
                    for(String f : allFactors)
                    {
                        Collection fvs = doc.getFieldValues(FIELD_FACTOR_PREFIX + f);
                        if(fvs != null && fvs.contains(efv)) {
                            ef = f;
                            break;
                        }
                    }
                    if(ef != null)
                        condEfvs.getOrCreate(ef, efv, true);
                }
            }
        }
        return condEfvs;
    }

    private void processResultGenes(QueryResponse response,
                                    AtlasStructuredQueryResult result,
                                    EfvTree<Integer> queryEfvs) throws SolrServerException {

        SolrDocumentList docs = response.getResults();
        result.setTotal(docs.getNumFound());

        boolean hasQueryEfvs = queryEfvs.getNumEfvs() > 0;
        EfvTree<Integer> resultEfvs = new EfvTree<Integer>();

        Iterable<EfvTree.EfEfv<Integer>> efvList = queryEfvs.getValueSortedList();

        int number = 0;
        for(SolrDocument doc : docs) {
            AtlasGene gene = new AtlasGene(doc);
            List<UpdownCounter> counters = new ArrayList<UpdownCounter>() {
                public UpdownCounter get(int index) {
                    if(index < size())
                        return super.get(index);
                    else
                        return new UpdownCounter(0, 0, 0, 0);
                }
            };

            if(!hasQueryEfvs) {
                for(String ef : allFactors) {
                    Collection<Object> efvs;

                    efvs = doc.getFieldValues("efvs_up_" + EfvTree.encodeEfv(ef));
                    if(efvs != null)
                        for(Object efv : efvs)
                            resultEfvs.getOrCreate(ef, (String)efv, number++);

                    efvs = doc.getFieldValues("efvs_dn_" + EfvTree.encodeEfv(ef));
                    if(efvs != null)
                        for(Object efv : efvs)
                            resultEfvs.getOrCreate(ef, (String)efv, number++);
                }
                efvList = resultEfvs.getValueSortedList();
            }

            for(EfvTree.EfEfv<Integer> efefv : efvList)
            {
                String efefvId = efefv.getEfEfvId();
                UpdownCounter counter = new UpdownCounter(
                        nullzero((Integer)doc.getFieldValue("cnt_efv_" + efefvId + "_up")),
                        nullzero((Integer)doc.getFieldValue("cnt_efv_" + efefvId + "_dn")),
                        nullzero((Double)doc.getFieldValue("avgpval_efv_" + efefvId + "_up")),
                        nullzero((Double)doc.getFieldValue("avgpval_efv_" + efefvId + "_dn"))
                );
                counters.add(counter);

                if(hasQueryEfvs && counter.getUps() + counter.getDowns() > 0)
                    resultEfvs.getOrCreate(efefv);
            }
            
            result.addResult(new StructuredResultRow(gene, counters));
        }

        result.setResultEfvs(resultEfvs);

        log.info("Retrieved query completely: " + result.getSize() + " records of " +
                result.getTotal() + " total starting from " + result.getStart() );

        log.info("Resulting EFVs are: " + resultEfvs);

    }

    private SolrQuery setupSolrQuery(AtlasStructuredQuery query, String solrq, EfvTree<Integer> queryEfvs) {
        SolrQuery q = new SolrQuery(solrq);

        q.setRows(query.getRows());
        q.setStart(query.getStart());
        q.setSortField("score", SolrQuery.ORDER.desc);

        q.setFacet(true);

        boolean hasQueryEfvs = queryEfvs.getNumEfvs() > 0;

        int max = 0;
        if(hasQueryEfvs)
        {
            for(EfvTree.Ef<Integer> ef : queryEfvs.getNameSortedTree())
            {
                if(max < ef.getEfvs().size())
                    max = ef.getEfvs().size();
                for(EfvTree.Efv<Integer> efv : ef.getEfvs()) {
                    q.addField("cnt_efv_" + EfvTree.getEfEfvId(ef, efv) + "_up");
                    q.addField("cnt_efv_" + EfvTree.getEfEfvId(ef, efv) + "_dn");
                    q.addField("avgpval_efv_" + EfvTree.getEfEfvId(ef, efv) + "_up");
                    q.addField("avgpval_efv_" + EfvTree.getEfEfvId(ef, efv) + "_dn");
                }
            }

            q.addField("score");
            q.addField("gene_id");
            q.addField("gene_name");
            q.addField("gene_identifier");
            for(String s : GENE_FACETS) {
                q.addField("gene_" + s);
            }
        } else {
            q.addField("*");
        }
        q.setFacetLimit(5 + max);

        q.setFacetMinCount(1);
        q.setFacetSort(true);

        for(String s : GENE_FACETS) {
            q.addFacetField("gene_" + s + "_exact");
        }
        q.addFacetField("exp_up_ids");
        q.addFacetField("exp_dn_ids");

        Collection<String> efs = getExperimentalFactorOptions();
        for(String ef : efs)
        {
            q.addFacetField("efvs_up_" + ef);
            q.addFacetField("efvs_dn_" + ef);
        }
        return q;
    }

    private Iterable<FacetCounter> getGeneFacet(QueryResponse response, final String name) {
        List<FacetCounter> facet = new ArrayList<FacetCounter>();
        FacetField ff = response.getFacetField(name);
        if(ff == null || ff.getValueCount() < 2)
            return new ArrayList<FacetCounter>();
        for (FacetField.Count ffc : ff.getValues())
        {
            facet.add(new FacetCounter(
                    ffc.getName().substring(0,1).toUpperCase() + ffc.getName().substring(1).toLowerCase(),
                    (int)ffc.getCount()));
        }
        Collections.sort(facet);
        return facet.subList(0, Math.min(facet.size(), 5));

    }

    private EfvTree<FacetUpDn> getEfvFacet(QueryResponse response, EfvTree<Integer> queryEfvs) {
        EfvTree<FacetUpDn> efvFacet = new EfvTree<FacetUpDn>();
        for (FacetField ff : response.getFacetFields())
        {
            if(ff.getValueCount() > 1) {
                if(ff.getName().startsWith("efvs_")) {
                    String ef = ff.getName().substring(8);
                    for (FacetField.Count ffc : ff.getValues())
                    {
                        if(!queryEfvs.has(ef, ffc.getName()))
                        {
                            int count = (int)ffc.getCount();
                            if(ff.getName().substring(5,7).equals("up"))
                                efvFacet.getOrCreate(ef, ffc.getName(),
                                        new FacetUpDn()).addUp(count);
                            else
                                efvFacet.getOrCreate(ef, ffc.getName(),
                                        new FacetUpDn()).addDown(count);
                        }
                    }

                } else if(ff.getName().startsWith("exp_")) {

                } else if(ff.getName().startsWith("gene_")) {

                }
            }
        }
        return efvFacet;
    }

    private static int nullzero(Integer i)
    {
        return i == null ? 0 : i;
    }

    private static double nullzero(Double d)
    {
        return d == null ? 0.0d : d;
    }


    public List<String[]> getGeneExpressionOptions() {
        return AtlasStructuredQuery.Expression.getOptionsList();
    }

    public Set<String> getExperimentalFactorOptions() {
        @SuppressWarnings("unchecked")
        Collection<String> fields = (Collection<String>)coreExpt.getSearcher().get().getReader().getFieldNames(IndexReader.FieldOption.ALL);
        Set<String> names = new TreeSet<String>();
        for(String i : fields) {
            if(i.startsWith(FIELD_FACTOR_PREFIX)) {
                names.add(i.substring(FIELD_FACTOR_PREFIX.length()));
            }
        }

        return names;
    }

    public Map<String, Long> autoCompleteFactorValues(String factor, String query, int limit) {
        Map<String,Long> s = new TreeMap<String,Long>();
        try {
            String solrq = "exp_in_dw:true";
            if(query != null && query.length() > 0) {
                query = query.toLowerCase();
                String qquery = "\"" + query.replaceAll("\"", "\\\"") + "\"";
                solrq = "(suggest_token:"+qquery+" suggest_full:"+qquery+") AND " + solrq;
            }

            SolrQuery q = new SolrQuery(solrq);
            
            q.setRows(0);
            q.setFacet(true);
            q.setFacetMinCount(1);

            if (factor == null || factor.equals(""))
                factor = "exp_factor_values_exact";
            else
                factor = AtlasStructuredQueryService.FIELD_FACTOR_PREFIX  + factor;
            q.addFacetField(factor);

            q.setFacetLimit(-1);
            q.setFacetSort(true);
            QueryResponse qr = solrExpt.query(q);

            if (null == qr.getFacetFields().get(0).getValues())
                return s;

            for (FacetField.Count ffc : qr.getFacetFields().get(0).getValues()) {
                if(query == null || query.length() == 0 || ffc.getName().toLowerCase().contains(query)) {
                    s.put(ffc.getName(), ffc.getCount());
                    if(s.size() >= limit)
                        break;
                }
            }

        } catch (SolrServerException e) {
            log.error(e);
        }

        return s;
    }
}
