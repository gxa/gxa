package ae3.service.structuredquery;

import ae3.model.AtlasGene;
import ae3.ols.webservice.axis.Query;
import ae3.ols.webservice.axis.QueryServiceLocator;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.index.IndexReader;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.core.CoreContainer;
import org.apache.solr.core.SolrCore;

import javax.xml.rpc.ServiceException;
import java.rmi.RemoteException;
import java.util.*;

/**
 * Structured query support class
 * @author pashky
 */
public class AtlasStructuredQueryService {

    private static final int MAX_CONDITION_EFVS = 100;
    private static final String CORE_ATLAS = "atlas";
    private static final String CORE_EXPT = "expt";
    public static final String FIELD_FACTOR_PREFIX = "dwe_ba_";
    private static final int COLUMN_COLLAPSE_THRESHOLD = 5;
    public static final String FIELD_GENE_PROP_PREFIX = "gene_";

    private Log log = LogFactory.getLog(AtlasStructuredQueryService.class);

    private SolrCore coreExpt;
    private SolrCore coreAtlas;
    private SolrServer solrAtlas;
    private SolrServer solrExpt;
    private Set<String> allFactors;
    private final IValueListHelper efvListHelper;
    private final IValueListHelper geneListHelper;

    /**
     * Constructor. Requires SOLR core container reference to work.
     * @param coreContainer reference to core container with cores "expt" and "atlas"
     */
    public AtlasStructuredQueryService(CoreContainer coreContainer) {
        this.coreExpt = coreContainer.getCore(CORE_EXPT);
        this.coreAtlas = coreContainer.getCore(CORE_ATLAS);
        this.solrAtlas = new EmbeddedSolrServer(coreContainer, CORE_ATLAS);
        this.solrExpt = new EmbeddedSolrServer(coreContainer, CORE_EXPT);

        this.efvListHelper = new ExpFactorValueListHelper(solrExpt);
        this.geneListHelper = new GenePropValueListHelper(solrAtlas);
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

        final Iterable<ExpFactorResultCondition> conditions = appendEfvsQuery(query, solrq, queryEfvs);
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

                Set<String> expandableEfs = new HashSet<String>();
                EfvTree<Integer> trimmedEfvs = trimColumns(query, result, expandableEfs);
                result.setResultEfvs(trimmedEfvs);
                result.setExpandableEfs(expandableEfs);

                result.setEfvFacet(getEfvFacet(response, queryEfvs));
                for(GeneProperties.Prop p : GeneProperties.allDrillDowns()) {
                    Set<String> hasVals = new HashSet<String>();
                    for(GeneQueryCondition qc : query.getGeneQueries())
                        if(qc.getFactor().equals(p.id))
                            hasVals.addAll(qc.getFactorValues());

                    Iterable<FacetCounter> facet = getGeneFacet(response, p.facetField, hasVals);
                    if(facet.iterator().hasNext())                    
                        result.setGeneFacet(p.id, facet);
                }
                if(!query.getSpecies().iterator().hasNext())
                    result.setGeneFacet("species", getGeneFacet(response, "gene_species_exact", new HashSet<String>()));

            } catch (SolrServerException e) {
                log.error(e);
            }            
        }

        return result;
    }

    private EfvTree<Integer> trimColumns(final AtlasStructuredQuery query,
                                         final AtlasStructuredQueryResult result,
                                         Collection<String> expandableEfs)
    {
        final Set<String> expand = query.getExpandColumns();
        EfvTree<Integer> trimmedEfvs = new EfvTree<Integer>(result.getResultEfvs());
        if(expand.contains("*"))
            return trimmedEfvs; 

        for(EfvTree.Ef<Integer> ef : trimmedEfvs.getNameSortedTree())
        {
            if(expand.contains(ef.getEf()) || ef.getEfvs().size() < COLUMN_COLLAPSE_THRESHOLD)
                continue;

            Map<EfvTree.Efv<Integer>,Double> scores = new HashMap<EfvTree.Efv<Integer>,Double>();
            for(EfvTree.Efv<Integer> efv : ef.getEfvs())
                scores.put(efv, 0.0);

            for(StructuredResultRow row : result.getResults())
            {
                for(EfvTree.Efv<Integer> efv : ef.getEfvs())
                {
                    UpdownCounter c = row.getCounters().get(efv.getPayload());
                    scores.put(efv, scores.get(efv) + c.getDowns() * (1.0 - c.getMpvDn()) + c.getUps() * (1.0 - c.getMpvUp()));
                }
            }

            @SuppressWarnings("unchecked")
            Map.Entry<EfvTree.Efv<Integer>,Double>[] scoreset = scores.entrySet().toArray(new Map.Entry[1]);
            Arrays.sort(scoreset, new Comparator<Map.Entry<EfvTree.Efv<Integer>,Double>>() {
                public int compare(Map.Entry<EfvTree.Efv<Integer>, Double> o1, Map.Entry<EfvTree.Efv<Integer>, Double> o2) {
                    return o2.getValue().compareTo(o1.getValue());
                }
            });

            for(int i = COLUMN_COLLAPSE_THRESHOLD; i < scoreset.length; ++i)
            {
                trimmedEfvs.removeEfv(ef.getEf(), scoreset[i].getKey().getEfv());
                expandableEfs.add(ef.getEf());
            }
        }

        return trimmedEfvs;
    }

    private Iterable<ExpFactorResultCondition> appendEfvsQuery(AtlasStructuredQuery query,
                                                               StringBuffer solrq,
                                                               EfvTree<Integer> queryEfvs) {
        final List<ExpFactorResultCondition> conds = new ArrayList<ExpFactorResultCondition>();
        final StringBuffer efvq = new StringBuffer();
        final StringBuffer scores = new StringBuffer();

        scores.append("sum(");

        int number = 0;
        for(ExpFactorQueryCondition c : query.getConditions())
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

                // conds.add(new AtlasStructuredQueryResult.ExpFactorResultCondition(c, new EfvTree<Boolean>()));
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

                            queryEfvs.put(condEfv.getEf(), condEfv.getEfv(), number);

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
                                break;
                        }
                        efvq.append(")");
                    }
                    conds.add(new ExpFactorResultCondition(c, condEfvs));
                    if(number > MAX_CONDITION_EFVS)
                        break;
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
    	for(GeneQueryCondition geneQuery : query.getGeneQueries()) {
            if(solrq.length() > 0)
                solrq.append(geneQuery.isNegated() ? " NOT " : " AND ");

    		if(geneQuery.isAnyFactor()) {
                solrq.append("gene_ids:(").append(geneQuery.getJointFactorValues()).append(") ");
                solrq.append("gene_desc:(").append(geneQuery.getJointFactorValues()).append(") ");
    		} else {
                String field = GeneProperties.convertPropertyToSearchField(geneQuery.getFactor());
                if(field == null)
                    field = "gene_desc";

                solrq.append(field).append(":(").append(geneQuery.getJointFactorValues()).append(")");
                
                // Ugly hack!!!
                // SOLR doesn't do highlighting if there's no search in text field occured, so we need to do a fake search
                // if only string fields are matched in the query
                if(field.contains("_exact"))
                    solrq.append(" AND ")
                            .append(field.replace("_exact","")).append(":(")
                            .append(geneQuery.getJointFactorValues())
                            .append(")");
    		}
    	}

        return solrq;
    }

    private StringBuffer appendSpeciesQuery(AtlasStructuredQuery query, StringBuffer solrq) {
        if(query.getSpecies().iterator().hasNext())
        {
            if(solrq.length() > 0)
                solrq.append(" AND ");
            solrq.append("gene_species:(");
            for(String s : query.getSpecies())
            {
                solrq.append(" \"").append(s.replaceAll("[^ a-zA-Z]", "")).append("\"");
            }
            solrq.append(")");
        }
        return solrq;
    }

    private EfvTree<Boolean> getConditionEfvs(QueryCondition c) throws RemoteException, SolrServerException {
        if(c.isAnyValue())
            return getCondEfvsAllForFactor(c.getFactor());

        if(c.isAnyFactor())
            return getCondEfvsForFactor(null, c.getFactorValues());

        return getCondEfvsForFactor(c.getFactor(), c.getFactorValues());
    }

    private EfvTree<Boolean> getCondEfvsAllForFactor(String factor) {
        EfvTree<Boolean> condEfvs = new EfvTree<Boolean>();
        int i = 0;
        for(String v : getEfvListHelper().listAllValues(factor)) {
            condEfvs.put(factor, v, true);
            if(++i >= MAX_CONDITION_EFVS)
                break;
        }
        return condEfvs;
    }

    private EfvTree<Boolean> getCondEfvsForFactor(final String factor, final Iterable<String> values) throws RemoteException, SolrServerException {

        EfvTree<Boolean> condEfvs = new EfvTree<Boolean>();

        StringBuffer sb = new StringBuffer("exp_in_dw:true AND exp_factor_values:(");
        for(String val : values) {
            if(val.length() > 0) {
                sb.append("\"").append(val.replaceAll("[\\\\\"]", "\\\\\\$1")).append("\" ");
            }
        }
        sb.append(")");


        if(factor == null)
        {
            try {
                Query olsQuery = new QueryServiceLocator().getOntologyQuery();
                for(String val : values) {
                    if(val.length() > 0) {
                        @SuppressWarnings("unchecked")
                        HashMap<String,String> terms = olsQuery.getTermsByExactName(val, "EFO");
                        Set<String> ontologyExpansion = new TreeSet<String>();

                        for (String term : terms.keySet()) {
                            @SuppressWarnings("unchecked")
                            HashMap<String,String> termChildren = olsQuery.getTermChildren(term, "EFO", -1, new int[] {1,2,3,4});
                            ontologyExpansion.addAll(termChildren.values());
                        }
                        for(String oval : ontologyExpansion) {
                            sb.append(" exp_factor_values_exact:");
                            sb.append("\"").append(oval.replaceAll("[\\\\\"]", "\\\\\\$1")).append("\"");
                        }
                    }
                }
            } catch(ServiceException e) {
                log.error(e);
            }
        }

        Iterable<String> factors;
        if(factor == null || "".equals(factor)) {
            factors = getExperimentalFactorOptions();
        } else {
            factors = new ArrayList<String>(1);
            ((List<String>)factors).add(factor);
        }

        String idField = coreExpt.getSearcher().get().getSchema().getUniqueKeyField().getName();
        SolrQuery q = new SolrQuery(sb.toString());
        q.setHighlight(true);
        q.addHighlightField("exp_factor_values");
        q.addHighlightField("exp_factor_values_exact");
        q.setHighlightSnippets(1000);
        q.setHighlightSimplePre("");
        q.setHighlightSimplePost("");
        q.setHighlightRequireFieldMatch(true);
        q.setParam("hl.usePhraseHighlighter", "true");
        for(String f : factors)
            q.addField(FIELD_FACTOR_PREFIX + f);
        q.addField(idField);
        q.setRows(1000);
        q.setStart(0);
        QueryResponse qr = solrExpt.query(q);

        for(SolrDocument doc : qr.getResults())
        {
            String id = (String)doc.getFieldValue(idField);
            if(id != null) {
                for(String f : factors) {
                    Collection fvs = doc.getFieldValues(FIELD_FACTOR_PREFIX + f);
                    if(fvs != null) {
                        Map<String,List<String>> hl = qr.getHighlighting().get(id);
                        if(hl != null)
                            for(Collection<String> hls : hl.values())
                                if(hls != null)
                                    for(String efv1 : hls)
                                        for(Object efv2 : fvs)
                                            if(efv1.equalsIgnoreCase((String)efv2))
                                                condEfvs.put(f, (String)efv2, true);
                    }
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

        EfvTree.Creator<Integer> numberer = new EfvTree.Creator<Integer>() {
            private int num = 0;
            public Integer make() { return num++; }
        };

        String idField = coreAtlas.getSearcher().get().getSchema().getUniqueKeyField().getName();

        for(SolrDocument doc : docs) {
            String id = (String)doc.getFieldValue(idField);
            if(id == null)
                continue;

            AtlasGene gene = new AtlasGene(doc);
            if(response.getHighlighting() != null)
                gene.setGeneHighlights(response.getHighlighting().get(id));

            List<UpdownCounter> counters = new ArrayList<UpdownCounter>() {
                public UpdownCounter get(int index) {
                    if(index < size())
                        return super.get(index);
                    else
                        return new UpdownCounter(0, 0, 0, 0);
                }
            };

            if(!hasQueryEfvs) {
                for(String ef : getExperimentalFactorOptions()) {
                    Collection<Object> efvs;

                    efvs = doc.getFieldValues("efvs_up_" + EfvTree.encodeEfv(ef));
                    if(efvs != null)
                        for(Object efv : efvs) {
                            resultEfvs.getOrCreate(ef, (String)efv, numberer);
                        }

                    efvs = doc.getFieldValues("efvs_dn_" + EfvTree.encodeEfv(ef));
                    if(efvs != null)
                        for(Object efv : efvs) {
                            resultEfvs.getOrCreate(ef, (String)efv, numberer);
                        }
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
                    resultEfvs.put(efefv);
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
            q.addField("gene_species");
            for(GeneProperties.Prop p : GeneProperties.allDrillDowns()) {
                q.addField(p.searchField.replace("_exact", ""));
            }
        } else {
            q.addField("*");
        }
        q.setFacetLimit(5 + max);

        q.setFacetMinCount(2);
        q.setFacetSort(true);

        for(GeneProperties.Prop p : GeneProperties.allDrillDowns()) {
            q.addFacetField(p.facetField);
        }
        q.addFacetField("gene_species_exact");
        q.addFacetField("exp_up_ids");
        q.addFacetField("exp_dn_ids");

        Collection<String> efs = getExperimentalFactorOptions();
        for(String ef : efs)
        {
            q.addFacetField("efvs_up_" + ef);
            q.addFacetField("efvs_dn_" + ef);
        }

        q.setHighlight(true);
        q.setHighlightSnippets(100);
        q.setHighlightRequireFieldMatch(false);
        q.addHighlightField("gene_id");
        q.addHighlightField("gene_name");
        q.addHighlightField("gene_identifier");
        for(GeneProperties.Prop p : GeneProperties.allDrillDowns()) {
            q.addHighlightField(p.searchField.replace("_exact", ""));
        }
        return q;
    }

    private Iterable<FacetCounter> getGeneFacet(QueryResponse response, final String name, Set<String> values) {
        List<FacetCounter> facet = new ArrayList<FacetCounter>();
        FacetField ff = response.getFacetField(name);
        if(ff == null || ff.getValueCount() < 2)
            return new ArrayList<FacetCounter>();

        for (FacetField.Count ffc : ff.getValues())
            if(!values.contains(ffc.getName()))
                facet.add(new FacetCounter(ffc.getName(), (int)ffc.getCount()));
        if(facet.size() < 2)
            return new ArrayList<FacetCounter>();

        Collections.sort(facet);
        return facet.subList(0, Math.min(facet.size(), 5));

    }

    private EfvTree<FacetUpDn> getEfvFacet(QueryResponse response, EfvTree<Integer> queryEfvs) {
        EfvTree<FacetUpDn> efvFacet = new EfvTree<FacetUpDn>();
        EfvTree.Creator<FacetUpDn> creator = new EfvTree.Creator<FacetUpDn>() {
            public FacetUpDn make() { return new FacetUpDn(); }
        };
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
                            efvFacet.getOrCreate(ef, ffc.getName(), creator)
                                    .add(count, ff.getName().substring(5,7).equals("up"));
                        }
                    }
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


    /**
     * Returns list of gene expression options
     * @return list of arrays of two strings, first - id, second - human-readable description
     */
    public List<String[]> getGeneExpressionOptions() {
        return Expression.getOptionsList();
    }

    /**
     * Returns set of experimental factors
     * @return set of strings representing experimental factors
     */
    public Set<String> getExperimentalFactorOptions() {
        // lazy caching
        if(allFactors == null)
        {
            @SuppressWarnings("unchecked")
            Collection<String> fields = (Collection<String>)coreExpt.getSearcher().get().getReader().getFieldNames(IndexReader.FieldOption.ALL);
            Set<String> names = new TreeSet<String>();
            for(String i : fields) {
                if(i.startsWith(FIELD_FACTOR_PREFIX)) {
                    names.add(i.substring(FIELD_FACTOR_PREFIX.length()));
                }
            }

            allFactors = names;
        }
        
        return allFactors;
    }

    /**
     * Returns reference to EFV autocompletion and listing helper
     * @return IValueListHelper interface of the EFV helper
     */
    public IValueListHelper getEfvListHelper() {
        return efvListHelper;
    }

    /**
     * Returns reference to gene properties autocompletion and listing helper
     * @return IValueListHelper interface of the gene properties helper
     */
    public IValueListHelper getGeneListHelper() {
        return geneListHelper;
    }
    
    public Iterable<String> getGeneProperties() {
        return GeneProperties.allPropertyIds();
    }
}
