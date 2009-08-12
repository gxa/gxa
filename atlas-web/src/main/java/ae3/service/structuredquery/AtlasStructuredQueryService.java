package ae3.service.structuredquery;

import ae3.dao.AtlasDao;
import ae3.model.AtlasExperiment;
import ae3.model.AtlasGene;
import ae3.model.ListResultRow;
import ae3.model.ListResultRowExperiment;
import ae3.util.AtlasProperties;
import ae3.util.Pair;
import ae3.util.EscapeUtil;
import org.apache.commons.lang.StringUtils;
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
import org.apache.solr.search.SolrIndexSearcher;
import org.apache.solr.util.RefCounted;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.ae3.indexbuilder.Efo;
import uk.ac.ebi.ae3.indexbuilder.IndexField;
import uk.ac.ebi.ae3.indexbuilder.Constants;
import uk.ac.ebi.ae3.indexbuilder.Experiment;

import java.util.*;

/**
 * Structured query support class
 * @author pashky
 */
public class AtlasStructuredQueryService {

    private static final int MAX_EFV_COLUMNS = 120;
    public static final String FIELD_FACTOR_PREFIX = "dwe_ba_";
    private final static String[] EXP_SEARCH_FIELDS = {
            "aer_txt_expaccession",
            "dwe_exp_description",
            "dwe_exp_accession",
            "aer_txt_expname",
            "dwe_exp_id" };

    final private Logger log = LoggerFactory.getLogger(getClass());

    private SolrServer solrAtlas;
    private SolrServer solrExpt;

    private Set<String> allFactors = new TreeSet<String>();
    
    private final ExpFactorValueListHelper efvListHelper;
    private final GenePropValueListHelper geneListHelper;
    private final EfoValueListHelper efoListHelper;

    private final String atlasIdField;
    private final String exptIdField;

    private final AtlasDao atlasDao;

    private final CoreContainer coreContainer;
    private final Set<String> cacheFill = new HashSet<String>();

    private void controlCache() {
        synchronized (cacheFill) {
            if(cacheFill.size() > 500) {
                SolrCore core = coreContainer.getCore(Constants.CORE_ATLAS);
                if( core != null ) {
                    core.closeSearcher();
                    core.close();
                }
                cacheFill.clear();
            }
        }
    }

    private void notifyCache(String field) {
        synchronized (cacheFill) {
            cacheFill.add(field);
        }
    }

    /**
     * Constructor. Requires SOLR core container reference to work.
     * @param coreContainer reference to core container with cores "expt" and "atlas"
     */
    public AtlasStructuredQueryService(CoreContainer coreContainer) {
        this.coreContainer = coreContainer;

        SolrCore coreExpt = coreContainer.getCore(Constants.CORE_EXPT);
        SolrCore coreAtlas = coreContainer.getCore(Constants.CORE_ATLAS);

        RefCounted<SolrIndexSearcher> searcher = coreAtlas.getSearcher();
        atlasIdField = searcher.get().getSchema().getUniqueKeyField().getName();
        searcher.decref();

        searcher = coreExpt.getSearcher();
        exptIdField = searcher.get().getSchema().getUniqueKeyField().getName();

        @SuppressWarnings("unchecked")
        Collection<String> fields = (Collection<String>)searcher.get().getReader().getFieldNames(IndexReader.FieldOption.ALL);
        searcher.decref();
        for(String i : fields) {
            if(i.startsWith(FIELD_FACTOR_PREFIX)) {
                allFactors.add(i.substring(FIELD_FACTOR_PREFIX.length()));
            }
        }

        coreExpt.close();
        coreAtlas.close();

        this.solrAtlas = new EmbeddedSolrServer(coreContainer, Constants.CORE_ATLAS);
        this.solrExpt = new EmbeddedSolrServer(coreContainer, Constants.CORE_EXPT);

        this.efvListHelper = new ExpFactorValueListHelper(solrAtlas, solrExpt, getExperimentalFactorOptions());
        this.geneListHelper = new GenePropValueListHelper(solrAtlas);
        this.efoListHelper = new EfoValueListHelper(solrAtlas);

        this.atlasDao = new AtlasDao(solrAtlas, solrExpt);
    }

    public AtlasDao getAtlasDao() {
        return atlasDao;
    }

    private static class SolrQueryBuilder {
        private StringBuffer solrq = new StringBuffer();
        private StringBuffer scores = new StringBuffer();

        private static final EnumMap<QueryExpression,String> SCORE_EXP_MAP = new EnumMap<QueryExpression,String>(QueryExpression.class);

        static {
            SCORE_EXP_MAP.put(QueryExpression.UP, "_up");
            SCORE_EXP_MAP.put(QueryExpression.DOWN, "_dn");
            SCORE_EXP_MAP.put(QueryExpression.UP_DOWN, "_ud");
        }

        public SolrQueryBuilder appendAnd() {
            if(solrq.length() > 0)
                solrq.append(" AND ");
            return this;
        }

        public SolrQueryBuilder append(String s) {
            solrq.append(s);
            return this;
        }

        public SolrQueryBuilder append(Object s) {
            solrq.append(s);
            return this;
        }

        public SolrQueryBuilder append(StringBuffer s) {
            solrq.append(s);
            return this;
        }

        public SolrQueryBuilder appendScore(String s) {
            if(scores.length() > 0)
                scores.append(",");
            scores.append(s);
            return this;
        }

        public SolrQueryBuilder appendExpFields(String prefix, String id, QueryExpression e) {
            switch(e)
            {
                case UP: solrq.append(prefix).append(id).append("_up:[* TO *]"); break;
                case DOWN: solrq.append(prefix).append(id).append("_dn:[* TO *]"); break;
                case UP_DOWN: solrq.append(prefix).append(id).append("_up:[* TO *] ")
                        .append(prefix).append(id).append("_dn:[* TO *]"); break;
                default:
                    throw new IllegalArgumentException("Unknown regulation option specified " + e);
            }
            return this;
        }

        public SolrQueryBuilder appendExpScores(String prefix, String id, QueryExpression e) {
            if(scores.length() > 0)
                scores.append(",");
            scores.append(prefix).append(id).append(SCORE_EXP_MAP.get(e));
            return this;
        }


        @Override
        public String toString() {
            StringBuffer sb = new StringBuffer(solrq);
            if(scores.length() > 0)
                sb.append(" AND _val_:\"sum(").append(scores).append(")\"");
            else
                sb.append(" AND _val_:\"sum(cnt_efo_EFO_0000001_up,cnt_efo_EFO_0000001_dn)\"");
            return sb.toString();
        }

        public boolean isEmpty() {
            return solrq.length() == 0;
        }
    }

    private class QueryState {
        private final SolrQueryBuilder solrq = new SolrQueryBuilder();
        private final EfvTree<Integer> efvs = new EfvTree<Integer>();
        private final EfoTree<Integer> efos = new EfoTree<Integer>(getEfo());
        private final Set<String> experiments = new HashSet<String>();

        private EfoEfvPayloadCreator<Integer> numberer = new EfoEfvPayloadCreator<Integer>() {
            private int num = 0;
            public Integer make() { return num++; }
        };

        public SolrQueryBuilder getSolrq() {
            return solrq;
        }

        public void addExperiments(Collection<String> ids) {
            experiments.addAll(ids);
        }

        public void addEfv(String ef, String efv) {
            efvs.getOrCreate(ef, efv, numberer);
        }

        public void addEfo(String id) {
            efos.add(id, numberer, true);
        }

        public Set<String> getExperiments() {
            return experiments;
        }

        public EfvTree<Integer> getEfvs() {
            return efvs;
        }

        public EfoTree<Integer> getEfos() {
            return efos;
        }

        public boolean isEmpty() {
            return solrq.isEmpty();
        }

        public boolean hasQueryEfoEfvs() {
            return efvs.getNumEfvs() + efos.getNumEfos() > 0;
        }

        @Override
        public String toString() {
            return "SOLR query: <" + solrq.toString() + ">, Experiments: [" + StringUtils.join(experiments, ", ") + "], "
                    + "EFVs: [" + efvs.toString() + "], EFOs: [" + efos.toString() + "]";
        }
    }


    /**
     * Process structured Atlas query
     * @param query parsed query
     * @return matching results
     * @throws java.io.IOException
     */
    public AtlasStructuredQueryResult doStructuredAtlasQuery(final AtlasStructuredQuery query) {
        final QueryState qstate = new QueryState();

        final Iterable<ExpFactorResultCondition> conditions = appendEfvsQuery(query, qstate);

        appendGeneQuery(query, qstate.getSolrq());
        appendSpeciesQuery(query, qstate.getSolrq());

        log.info("Structured query is: " + qstate.toString());

        AtlasStructuredQueryResult result = new AtlasStructuredQueryResult(query.getStart(), query.getRowsPerPage(), query.getExpsPerGene());
        result.setConditions(conditions);

        if(!qstate.isEmpty())
        {
            try {

                controlCache();

                SolrQuery q = setupSolrQuery(query, qstate);
                QueryResponse response = solrAtlas.query(q);
                
                processResultGenes(response, result, qstate, query);

                Set<String> expandableEfs = new HashSet<String>();
                EfvTree<Integer> trimmedEfvs = trimColumns(query, result, expandableEfs);
                result.setResultEfvs(trimmedEfvs);
                result.setExpandableEfs(expandableEfs);

                result.setEfvFacet(getEfvFacet(response, qstate));
                for(GeneProperties.Prop p : GeneProperties.allDrillDowns()) {
                    Set<String> hasVals = new HashSet<String>();
                    for(GeneQueryCondition qc : query.getGeneConditions())
                        if(qc.getFactor().equals(p.id))
                            hasVals.addAll(qc.getFactorValues());

                    Iterable<FacetCounter> facet = getGeneFacet(response, p.facetField, hasVals);
                    if(facet.iterator().hasNext())                    
                        result.setGeneFacet(p.id, facet);
                }
                if(!query.getSpecies().iterator().hasNext())
                    result.setGeneFacet("species", getGeneFacet(response, "gene_species_exact", new HashSet<String>()));

            } catch (SolrServerException e) {
                log.error("Error in structured query!", e);
            }            
        }

        return result;
    }

    public AtlasStructuredQueryResult findGenesForExperiment(Object geneIds, String eAcc, int start) {
        return doStructuredAtlasQuery(new AtlasStructuredQueryBuilder()
                .andGene(geneIds)
                .andUpdnIn(Constants.EXP_FACTOR_NAME, EscapeUtil.optionalQuote(eAcc))
                .viewAs(ViewType.LIST)
                .rowsPerPage(AtlasProperties.getIntProperty("atlas.query.listsize"))
                .startFrom(start)
                .expsPerGene(AtlasProperties.getIntProperty("atlas.query.expsPerGene")).query());
    }
        
    private Efo getEfo() {
        return Efo.getEfo();
    }

    private EfvTree<Integer> trimColumns(final AtlasStructuredQuery query,
                                         final AtlasStructuredQueryResult result,
                                         Collection<String> expandableEfs)
    {
        final Set<String> expand = query.getExpandColumns();
        EfvTree<Integer> trimmedEfvs = new EfvTree<Integer>(result.getResultEfvs());
        if(expand.contains("*"))
            return trimmedEfvs;

        if(trimmedEfvs.getNumEfvs() < MAX_EFV_COLUMNS)
            return trimmedEfvs;


        int threshold = MAX_EFV_COLUMNS / trimmedEfvs.getNumEfs();

        for(EfvTree.Ef<Integer> ef : trimmedEfvs.getNameSortedTree())
        {
            if(expand.contains(ef.getEf()) || ef.getEfvs().size() < threshold)
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

            for(int i = threshold; i < scoreset.length; ++i)
            {
                trimmedEfvs.removeEfv(ef.getEf(), scoreset[i].getKey().getEfv());
                expandableEfs.add(ef.getEf());
            }
        }

        return trimmedEfvs;
    }

    private Collection<String> findExperiments(String query, EfvTree<Boolean> condEfvs) throws SolrServerException {

        List<String> result = new ArrayList<String>();
        if(query.length() == 0)
            return result;
        
        StringBuffer sb = new StringBuffer("exp_in_dw:true AND (");
        for(String f : EXP_SEARCH_FIELDS) {
            sb.append(f).append(":(").append(query).append(") ");
        }
        sb.append(")");
        SolrQuery q = new SolrQuery(sb.toString());
        q.addField("*");
        q.setRows(50);
        q.setStart(0);

        QueryResponse qr = solrExpt.query(q);
        for(SolrDocument doc : qr.getResults()) {
            String id = String.valueOf(doc.getFieldValue("dwe_exp_id"));
            if(id != null) {
                result.add(id);
                for(String name : doc.getFieldNames())
                    if(name.startsWith(FIELD_FACTOR_PREFIX))
                        for(Object val : doc.getFieldValues(name))
                            condEfvs.put(name.substring(FIELD_FACTOR_PREFIX.length()), String.valueOf(val), true);
            }
        }

        return result;
    }

    private StringBuffer makeExperimentsQuery(Iterable<String> ids, QueryExpression e) {
        StringBuffer sb = new StringBuffer();
        String idss = StringUtils.join(ids.iterator(), " ");
        if(idss.length() == 0)
            return sb;
        if(e == QueryExpression.UP || e == QueryExpression.UP_DOWN)
            sb.append("exp_up_ids:(").append(idss).append(") ");
        if(e == QueryExpression.DOWN || e == QueryExpression.UP_DOWN)
            sb.append("exp_dn_ids:(").append(idss).append(")");
        return sb;
    }

    private Iterable<ExpFactorResultCondition> appendEfvsQuery(final AtlasStructuredQuery query, final QueryState qstate) {
        final List<ExpFactorResultCondition> conds = new ArrayList<ExpFactorResultCondition>();
        SolrQueryBuilder solrq = qstate.getSolrq();

        for(ExpFactorQueryCondition c : query.getConditions())
        {
            boolean isExperiment = Constants.EXP_FACTOR_NAME.equals(c.getFactor());
            if(c.isAnything() || (isExperiment && c.isAnyValue())) {
                // do nothing
            } else {
                try {
                    boolean nonemptyQuery = false;
                    EfvTree<Boolean> condEfvs = isExperiment ? new EfvTree<Boolean>() : getConditionEfvs(c);
                    if(condEfvs.getNumEfvs() > 0)
                    {
                        solrq.appendAnd().append("(");
                        for(EfvTree.EfEfv<Boolean> condEfv : condEfvs.getNameSortedList())
                        {
                            solrq.append(" ");

                            String efefvId = condEfv.getEfEfvId();
                            solrq.appendExpFields("cnt_", efefvId, c.getExpression());
                            solrq.appendExpScores("s_", efefvId, c.getExpression());

                            notifyCache(efefvId + c.getExpression());
                            
                            if(Constants.EFO_FACTOR_NAME.equals(condEfv.getEf())) {
                                qstate.addEfo(condEfv.getEfv());
                            } else {
                                qstate.addEfv(condEfv.getEf(), condEfv.getEfv());
                            }
                        }
                        solrq.append(")");
                        nonemptyQuery = true;
                    } else if(c.isAnyFactor() || isExperiment) {
                        // try to search for experiment too if no matching conditions are found
                        Collection<String> experiments = findExperiments(c.getSolrEscapedFactorValues(), condEfvs);
                        qstate.addExperiments(experiments);
                        StringBuffer expq = makeExperimentsQuery(experiments, c.getExpression());
                        if(expq.length() > 0) {
                            for(EfvTree.EfEfv<Boolean> condEfv : condEfvs.getNameSortedList())
                            {
                                qstate.addEfv(condEfv.getEf(), condEfv.getEfv());
                                solrq.appendExpScores("s_efv_", condEfv.getEfEfvId(), c.getExpression());
                            }
                            solrq.appendAnd().append(expq);
                            nonemptyQuery = true;
                        }
                    }
                    Collection<List<EfoValueListHelper.EfoTermCount>> efoPaths = new ArrayList<List<EfoValueListHelper.EfoTermCount>>();
                    Collection<EfvTree.Efv<Boolean>> condEfos = condEfvs.getEfvs(Constants.EFO_FACTOR_NAME);
                    for(EfvTree.Efv<Boolean> efv : condEfos) {
                        efoPaths.addAll(getEfoListHelper().getTermParentPaths(efv.getEfv()));
                    }
                    conds.add(new ExpFactorResultCondition(c, efoPaths, !nonemptyQuery));
                } catch (SolrServerException e) {
                    log.error("Error querying Atlas index", e);
                }
            }
        }

        return conds;
    }

    private void appendGeneQuery(AtlasStructuredQuery query, SolrQueryBuilder solrq) {
    	for(GeneQueryCondition geneQuery : query.getGeneConditions()) {
            solrq.appendAnd();
            if(geneQuery.isNegated())
                solrq.append(" NOT ");

            if(geneQuery.isAnyFactor()) {
                solrq.append("(gene_ids:(").append(geneQuery.getSolrEscapedFactorValues()).append(") ");
                solrq.append("gene_desc:(").append(geneQuery.getSolrEscapedFactorValues()).append("))");
            } else if(GeneProperties.isNameProperty(geneQuery.getFactor())) {
                solrq.append("(gene_name:(").append(geneQuery.getSolrEscapedFactorValues()).append(") ");
                solrq.append("gene_synonym:(").append(geneQuery.getSolrEscapedFactorValues()).append("))");
            } else {
                String field = GeneProperties.convertPropertyToSearchField(geneQuery.getFactor());
                if(field == null)
                    field = "gene_desc";

                solrq.append(field).append(":(").append(geneQuery.getSolrEscapedFactorValues()).append(")");
                
                // Ugly hack!!!
                // SOLR doesn't do highlighting if there's no search in text field occured, so we need to do a fake search
                // if only string fields are matched in the query
                if(field.contains("_exact"))
                    solrq.append(" AND ")
                            .append(field.replace("_exact","")).append(":(")
                            .append(geneQuery.getSolrEscapedFactorValues())
                            .append(")");
    		}
    	}
    }

    private void appendSpeciesQuery(AtlasStructuredQuery query, SolrQueryBuilder solrq) {
        if(query.getSpecies().iterator().hasNext())
        {
            solrq.appendAnd().append("gene_species:(");
            for(String s : query.getSpecies())
            {
                solrq.append(" \"").append(s.replaceAll("[^ a-zA-Z]", "")).append("\"");
            }
            solrq.append(")");
        }
    }

    private EfvTree<Boolean> getConditionEfvs(QueryCondition c) throws SolrServerException {
        if(c.isAnyValue())
            return getCondEfvsAllForFactor(c.getFactor());

        if(c.isAnyFactor())
            return getCondEfvsForFactor(null, c.getFactorValues());

        return getCondEfvsForFactor(c.getFactor(), c.getFactorValues());
    }

    private EfvTree<Boolean> getCondEfvsAllForFactor(String factor) {
        EfvTree<Boolean> condEfvs = new EfvTree<Boolean>();
        if(Constants.EFO_FACTOR_NAME.equals(factor)) {
            Efo efo = getEfo();
            int i = 0;
            for (String v : efo.getRootIds()) {
                condEfvs.put(Constants.EFO_FACTOR_NAME, v, true);
                if (++i >= MAX_EFV_COLUMNS) {
                    break;
                }
            }
        } else {
            int i = 0;
            for (String v : getEfvListHelper().listAllValues(factor)) {
                condEfvs.put(factor, v, true);
                if (++i >= MAX_EFV_COLUMNS) {
                    break;
                }
            }
        }
        return condEfvs;
    }

    private EfvTree<Boolean> getCondEfvsForFactor(final String factor, final Iterable<String> values) throws SolrServerException {

        EfvTree<Boolean> condEfvs = new EfvTree<Boolean>();

        if(Constants.EFO_FACTOR_NAME.equals(factor) || null == factor) {
            Efo efo = getEfo();
            for(String v : values) {
                for(Efo.Term term : efo.searchTerm(v)) {
                    condEfvs.put(Constants.EFO_FACTOR_NAME, term.getId(), true);
                }
            }
        }

        if(Constants.EFO_FACTOR_NAME.equals(factor))
            return condEfvs;

        StringBuffer sb = new StringBuffer("exp_in_dw:true AND (exp_factor_values:(");
        for(String val : values) {
            if(val.length() > 0) {
                sb.append("\"").append(val.replaceAll("[\\\\\"]", "\\\\\\$1")).append("\" ");
            }
        }
        sb.append("))");

        Iterable<String> factors;
        if(factor == null) {
            factors = getExperimentalFactors();
        } else {
            factors = Collections.singletonList(factor);
        }

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
        q.addField(exptIdField);
        q.setRows(1000);
        q.setStart(0);
        QueryResponse qr = solrExpt.query(q);

        for(SolrDocument doc : qr.getResults())
        {
            Object id = doc.getFieldValue(exptIdField);
            if(id != null) {
                for(String f : factors) {
                    Collection fvs = doc.getFieldValues(FIELD_FACTOR_PREFIX + f);
                    if(fvs != null) {
                        Map<String,List<String>> hl = qr.getHighlighting().get(id.toString());
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
                                    QueryState qstate, AtlasStructuredQuery query) throws SolrServerException {

        SolrDocumentList docs = response.getResults();
        result.setTotal(docs.getNumFound());
        EfvTree<Integer> resultEfvs = new EfvTree<Integer>();
        EfoTree<Integer> resultEfos = qstate.getEfos();

        Iterable<EfvTree.EfEfv<Integer>> efvList = qstate.getEfvs().getValueSortedList();
        Iterable<EfoTree.EfoItem<Integer>> efoList = qstate.getEfos().getValueOrderedList();
        boolean hasQueryEfvs = qstate.hasQueryEfoEfvs();

        EfoEfvPayloadCreator<Integer> numberer = new EfoEfvPayloadCreator<Integer>() {
            private int num = 0;
            public Integer make() { return num++; }
        };

        Iterable<String> autoFactors = getConfiguredFactors("anycondition");

        for(SolrDocument doc : docs) {
            String id = (String)doc.getFieldValue(atlasIdField);
            if(id == null)
                continue;

            AtlasGene gene = new AtlasGene(doc);
            if(response.getHighlighting() != null)
                gene.setGeneHighlights(response.getHighlighting().get(id));

            List<UpdownCounter> counters = new ArrayList<UpdownCounter>() {
                @Override
                public UpdownCounter get(int index) {
                    if(index < size())
                        return super.get(index);
                    else
                        return new UpdownCounter(0, 0, 0, 0);
                }
            };

            if(!hasQueryEfvs && query.getViewType() != ViewType.LIST) {
                Collection<Object> values;

                for(String ef : autoFactors) {

                    values = doc.getFieldValues("efvs_up_" + IndexField.encode(ef));
                    if(values != null)
                        for(Object efv : values) {
                            resultEfvs.getOrCreate(ef, (String)efv, numberer);
                        }

                    values = doc.getFieldValues("efvs_dn_" + IndexField.encode(ef));
                    if(values != null)
                        for(Object efv : values) {
                            resultEfvs.getOrCreate(ef, (String)efv, numberer);
                        }
                }

                int threshold = 0;
                if(resultEfos.getNumExplicitEfos() > 0)
                    threshold = 1;
                else if(resultEfos.getNumExplicitEfos() > 20)
                    threshold = 3;

                values = doc.getFieldValues("efos_up");
                if(values != null)
                    for(Object efoo : values) {
                        String efo = (String)efoo;
                        if(IndexField.nullzero((Short)doc.getFieldValue("cnt_efo_" + efo + "_s_up")) > threshold)
                            resultEfos.add(efo, numberer, false);
                    }

                values = doc.getFieldValues("efos_dn");
                if(values != null)
                    for(Object efoo : values) {
                        String efo = (String)efoo;
                        if(IndexField.nullzero((Short)doc.getFieldValue("cnt_efo_" + efo + "_s_dn")) > threshold)
                            resultEfos.add(efo, numberer, false);
                    }

                efvList = resultEfvs.getValueSortedList();
                efoList = resultEfos.getValueOrderedList();
            }

            Iterator<EfvTree.EfEfv<Integer>> itEfv = efvList.iterator();
            Iterator<EfoTree.EfoItem<Integer>> itEfo = efoList.iterator();
            EfvTree.EfEfv<Integer> efv = null;
            EfoTree.EfoItem<Integer> efo = null;
            while(itEfv.hasNext() || itEfo.hasNext())
            {
                if(itEfv.hasNext() && efv == null)
                    efv = itEfv.next();
                if(itEfo.hasNext() && efo == null)
                    efo = itEfo.next();

                String cellId;
                boolean usingEfv = efo == null || (efv != null && efv.getPayload().compareTo(efo.getPayload()) < 0);
                if(usingEfv) {
                    cellId = efv.getEfEfvId();
                } else {
                    cellId = "efo_" + efo.getId();
                }

                UpdownCounter counter = new UpdownCounter(
                        IndexField.nullzero((Short)doc.getFieldValue("cnt_" + cellId + "_up")),
                        IndexField.nullzero((Short)doc.getFieldValue("cnt_" + cellId + "_dn")),
                        IndexField.nullzero((Float)doc.getFieldValue("minpval_" + cellId + "_up")),
                        IndexField.nullzero((Float)doc.getFieldValue("minpval_" + cellId + "_dn")));

                counters.add(counter);

                boolean nonZero = (counter.getUps() + counter.getDowns() > 0);

                if (usingEfv) {
                    if (hasQueryEfvs && nonZero)
                        resultEfvs.put(efv);
                    efv = null;
                } else {
                    if (nonZero)
                        resultEfos.mark(efo.getId());
                    efo = null;
                }
            }

            if(query.getViewType() == ViewType.LIST) {
                loadListExperiments(result, gene, resultEfvs, resultEfos, qstate.getExperiments());
            }

            result.addResult(new StructuredResultRow(gene, counters));
        }

        result.setResultEfvs(resultEfvs);
        result.setResultEfos(resultEfos);

        log.info("Retrieved query completely: " + result.getSize() + " records of " +
                result.getTotal() + " total starting from " + result.getStart() );

        log.info("Resulting EFVs are: " + resultEfvs);
        log.info("Resulting EFOs are: " + resultEfos);

    }

    /**
     * TODO
     * @param result
     * @param gene
     * @param efvTree
     * @param efoTree
     * @param experiments
     */
    private void loadListExperiments(AtlasStructuredQueryResult result, AtlasGene gene, final EfvTree<Integer> efvTree, final EfoTree<Integer> efoTree, Set<String> experiments) {
        Iterable<Experiment> exps = null;

        if(efvTree.getNumEfvs() + efoTree.getNumExplicitEfos() > 0) {
            Iterable<String> efviter = new Iterable<String>() {
                public Iterator<String> iterator() {
                    return new Iterator<String>() {
                        private Iterator<EfvTree.EfEfv<Integer>> treeit = efvTree.getNameSortedList().iterator();
                        public boolean hasNext() { return treeit.hasNext(); }
                        public String next() { return treeit.next().getEfEfvId(); }
                        public void remove() { }
                    };
                }
            };

            Iterable<String> efoiter = new Iterable<String>() {
                public Iterator<String> iterator() {
                    return new Iterator<String>() {
                        private Iterator<String> explit = efoTree.getExplicitEfos().iterator();
                        private Iterator<String> childit;
                        public boolean hasNext() {
                            return explit.hasNext() || (childit != null && childit.hasNext());
                        }
                        public String next() {
                            if(childit != null) {
                                String r = childit.next();
                                if(!childit.hasNext() && explit.hasNext())
                                    childit = Efo.getEfo().getTermAndAllChildrenIds(explit.next()).iterator();
                                return r;
                            } else {
                                childit = Efo.getEfo().getTermAndAllChildrenIds(explit.next()).iterator();
                                return next();
                            }
                        }
                        public void remove() { }
                    };
                }
            };
            exps = gene.getExpermientsTable().findByEfEfvEfoSet(efviter, efoiter);
        } else {
            exps = gene.getExpermientsTable().getAll();
        }

        Map<Pair<String,String>,List<ListResultRowExperiment>> map = new HashMap<Pair<String,String>, List<ListResultRowExperiment>>();
        for(Experiment exp : exps) {
        	if(!experiments.isEmpty() && !experiments.contains(String.valueOf(exp.getId())))
        		continue;
            AtlasExperiment aexp = getAtlasDao().getExperimentById(String.valueOf(exp.getId()));
            if(aexp != null) {
                Pair<String,String> key = new Pair<String,String>(exp.getEf(), exp.getEfv());
                if(!map.containsKey(key))
                    map.put(key, new ArrayList<ListResultRowExperiment>());
                map.get(key).add(new ListResultRowExperiment(exp.getId(), 
                        aexp.getDwExpAccession(),
                        aexp.getDwExpDescription(),
                        exp.getPvalue(), exp.getExpression()));
            }
        }

        int listRowsPerGene = 0;
        for(Map.Entry<Pair<String,String>,List<ListResultRowExperiment>> e : map.entrySet()) {
            if(listRowsPerGene++ >= result.getRowsPerGene())
                break;
            
            int cup = 0, cdn = 0;
            double pup = 1, pdn = 1;
            for(ListResultRowExperiment exp : e.getValue())
                if(exp.getUpdn().isUp()) {
                    ++cup;
                    pup = Math.min(pup, exp.getPvalue());
                } else {
                    ++cdn;
                    pdn = Math.min(pdn, exp.getPvalue());
                }

            ListResultRow row = new ListResultRow(e.getKey().getFirst(), e.getKey().getSecond(), cup, cdn, pup, pdn);
            row.setGene(gene);
            Collections.sort(e.getValue(), new Comparator<ListResultRowExperiment>() {
                public int compare(ListResultRowExperiment o1, ListResultRowExperiment o2) {
                    return Double.valueOf(o1.getPvalue()).compareTo(o2.getPvalue());
                }
            });
            row.setExp_list(e.getValue());
            result.addListResult(row);

        }
    }

    
    private Set<String> getConfiguredFactors(String category)
    {
        Set<String> result = new TreeSet<String>();
        result.addAll(getExperimentalFactors());
        result.removeAll(Arrays.asList(StringUtils.split(StringUtils.trim(AtlasProperties.getProperty("atlas." + category + ".ignore.efs")), ",")));
        return result;
    }

    private SolrQuery setupSolrQuery(AtlasStructuredQuery query, QueryState qstate) {
        SolrQuery q = new SolrQuery(qstate.getSolrq().toString());

        q.setRows(query.getRowsPerPage());
        q.setStart(query.getStart());
        q.setSortField("score", SolrQuery.ORDER.desc);

        q.setFacet(true);

        int max = 0;
        if(qstate.hasQueryEfoEfvs())
        {
            for(EfvTree.Ef<Integer> ef : qstate.getEfvs().getNameSortedTree())
            {
                if(max < ef.getEfvs().size())
                    max = ef.getEfvs().size();

                for(EfvTree.Efv<Integer> efv : ef.getEfvs()) {
                    q.addField("cnt_" + EfvTree.getEfEfvId(ef, efv) + "_up");
                    q.addField("cnt_" + EfvTree.getEfEfvId(ef, efv) + "_dn");
                    q.addField("minpval_" + EfvTree.getEfEfvId(ef, efv) + "_up");
                    q.addField("minpval_" + EfvTree.getEfEfvId(ef, efv) + "_dn");
                    q.addField("exp_info_efv_" + EfvTree.getEfEfvId(ef, efv));
                }
            }

            if (max < qstate.getEfos().getNumEfos()) {
                max = qstate.getEfos().getNumEfos();
            }
            for(String id : qstate.getEfos().getEfoIds())
            {
                q.addField("cnt_efo_" + id + "_up");
                q.addField("cnt_efo_" + id + "_dn");
                q.addField("minpval_efo_" + id + "_up");
                q.addField("minpval_efo_" + id + "_dn");
            }

            q.addField("score");
            q.addField("gene_id");
            q.addField("gene_name");
            q.addField("gene_identifier");
            q.addField("gene_species");
            q.addField("exp_info");
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

        for(String ef : getConfiguredFactors("facet"))
        {
            q.addFacetField("efvs_up_" + ef);
            q.addFacetField("efvs_dn_" + ef);
        }

        q.setHighlight(true);
        q.setHighlightSnippets(100);
        q.setParam("hl.usePhraseHighlighter", "true");
        q.setParam("hl.mergeContiguous", "true");
        q.setHighlightRequireFieldMatch(false);
        q.addHighlightField("gene_id");
        q.addHighlightField("gene_name");
        q.addHighlightField("gene_synonym");
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

    private EfvTree<FacetUpDn> getEfvFacet(QueryResponse response, QueryState qstate) {
        EfvTree<FacetUpDn> efvFacet = new EfvTree<FacetUpDn>();
        EfoEfvPayloadCreator<FacetUpDn> creator = new EfoEfvPayloadCreator<FacetUpDn>() {
            public FacetUpDn make() { return new FacetUpDn(); }
        };
        for (FacetField ff : response.getFacetFields())
        {
            if(ff.getValueCount() > 1) {
                if(ff.getName().startsWith("efvs_")) {
                    String ef = ff.getName().substring(8);
                    for (FacetField.Count ffc : ff.getValues())
                    {
                        if(!qstate.efvs.has(ef, ffc.getName()))
                        {
                            int count = (int)ffc.getCount();
                            efvFacet.getOrCreate(ef, ffc.getName(), creator)
                                    .add(count, ff.getName().substring(5,7).equals("up"));
                        }
                    }
                } else if(ff.getName().startsWith("exp_")) {
                    for (FacetField.Count ffc : ff.getValues())
                        if(!qstate.getExperiments().contains(ffc.getName()))
                        {
                            AtlasExperiment exp = getAtlasDao().getExperimentById(ffc.getName());
                            if(exp != null) {
                                String expName = exp.getDwExpAccession();
                                if(expName != null)
                                {
                                    int count = (int)ffc.getCount();
                                    efvFacet.getOrCreate(Constants.EXP_FACTOR_NAME, expName, creator)
                                            .add(count, ff.getName().substring(4,6).equals("up"));
                                }
                            }
                        }
                }
            }
        }
        return efvFacet;
    }


    /**
     * Returns list of gene expression options
     * @return list of arrays of two strings, first - id, second - human-readable description
     */
    public List<String[]> getGeneExpressionOptions() {
        return QueryExpression.getOptionsList();
    }

    /**
     * Returns set of experimental factor for drop-down, fileterd by config
     * @return set of strings representing experimental factors
     */
    public Collection<String> getExperimentalFactorOptions() {
        List<String> factors = new ArrayList<String>();
        factors.addAll(getConfiguredFactors("options"));
        factors.add(Constants.EXP_FACTOR_NAME);
        Collections.sort(factors, String.CASE_INSENSITIVE_ORDER);
        return factors;
    }

    /**
     * Returns set of experimental factors
     * @return set of strings representing experimental factors
     */
    public Set<String> getExperimentalFactors() {
        return allFactors;
    }

    /**
     * Returns reference to EFV autocompletion and listing helper
     * @return IValueListHelper interface of the EFV helper
     */
    public ExpFactorValueListHelper getEfvListHelper() {
        return efvListHelper;
    }

    /**
     * Returns reference to gene properties autocompletion and listing helper
     * @return IValueListHelper interface of the gene properties helper
     */
    public GenePropValueListHelper getGeneListHelper() {
        return geneListHelper;
    }

    /**
     * Returns reference to EFO terms autocompletion and listing helper
     * @return IValueListHelper interface of the gene properties helper
     */
    public EfoValueListHelper getEfoListHelper() {
        return efoListHelper;
    }

    public Iterable<String> getGeneProperties() {
        return GeneProperties.optionPropertyIds();
    }
}
