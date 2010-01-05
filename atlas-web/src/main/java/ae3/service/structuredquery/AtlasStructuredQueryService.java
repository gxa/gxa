package ae3.service.structuredquery;

import ae3.dao.AtlasDao;
import ae3.model.AtlasExperiment;
import ae3.model.AtlasGene;
import ae3.model.ListResultRow;
import ae3.model.ListResultRowExperiment;
import ae3.util.AtlasProperties;
import ae3.util.MappingIterator;
import ae3.util.Pair;
import org.apache.commons.lang.StringUtils;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.core.CoreContainer;
import org.apache.solr.core.SolrCore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.ae3.indexbuilder.Experiment;
import uk.ac.ebi.ae3.indexbuilder.ExperimentsTable;
import uk.ac.ebi.ae3.indexbuilder.efo.Efo;
import uk.ac.ebi.ae3.indexbuilder.efo.EfoTerm;
import uk.ac.ebi.gxa.utils.EscapeUtil;

import java.util.*;

/**
 * Structured query support class
 * @author pashky
 */
public class AtlasStructuredQueryService {

    private static final int MAX_EFV_COLUMNS = 120;

    final private Logger log = LoggerFactory.getLogger(getClass());

    private SolrServer solrServerAtlas;
    private SolrServer solrServerExpt;
    private SolrServer solrServerProp;

    private Set<String> allFactors = new TreeSet<String>();
    
    private AtlasEfvService efvService;
    private AtlasEfoService efoService;

    private AtlasDao atlasSolrDAO;

    private CoreContainer coreContainer;

    private final Set<String> cacheFill = new HashSet<String>();
    private Set<String> allGeneProperties;
    private Set<String> nameGeneProperties;
    private Set<String> drilldownGeneProperties;
    private SortedSet<String> allSpecies = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);

    /**
     * Hack: prevents OOMs by clearing cache
     */
    private void controlCache() {
        if(coreContainer == null)
            return;

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

    public SolrServer getSolrServerAtlas() {
        return solrServerAtlas;
    }

    public void setSolrServerAtlas(SolrServer solrServerAtlas) {
        this.solrServerAtlas = solrServerAtlas;
    }

    public SolrServer getSolrServerExpt() {
        return solrServerExpt;
    }

    public void setSolrServerExpt(SolrServer solrServerExpt) {
        this.solrServerExpt = solrServerExpt;
    }

    public SolrServer getSolrServerProp() {
        return solrServerProp;
    }

    public void setSolrServerProp(SolrServer solrServerProp) {
        this.solrServerProp = solrServerProp;
    }

    public CoreContainer getCoreContainer() {
        return coreContainer;
    }

    public void setCoreContainer(CoreContainer coreContainer) {
        this.coreContainer = coreContainer;
    }

    public AtlasEfvService getEfvService() {
        return efvService;
    }

    public void setEfvService(AtlasEfvService efvService) {
        this.efvService = efvService;
    }

    public AtlasEfoService getEfoService() {
        return efoService;
    }

    public void setEfoService(AtlasEfoService efoService) {
        this.efoService = efoService;
    }

    public AtlasDao getAtlasSolrDAO() {
        return atlasSolrDAO;
    }

    public void setAtlasSolrDAO(AtlasDao solrAtlasDAO) {
        this.atlasSolrDAO = solrAtlasDAO;
    }

    /**
     * Constructor. Requires SOLR core container reference to work.
     * @param coreContainer reference to core container with cores "expt" and "atlas"
     */
    public AtlasStructuredQueryService() {
        this.allGeneProperties = new HashSet<String>();
        this.allGeneProperties.addAll(AtlasProperties.getListProperty("atlas.gene.autocomplete.ids"));
        this.allGeneProperties.addAll(AtlasProperties.getListProperty("atlas.gene.autocomplete.descs"));

        this.drilldownGeneProperties = new HashSet<String>(AtlasProperties.getListProperty("atlas.gene.drilldowns"));
        this.nameGeneProperties = new HashSet<String>(AtlasProperties.getListProperty("atlas.gene.autocomplete.names"));
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

        log.info("Structured query for " + query.getApiUrl() + ": " + qstate.toString());

        AtlasStructuredQueryResult result = new AtlasStructuredQueryResult(query.getStart(), query.getRowsPerPage(), query.getExpsPerGene());
        result.setConditions(conditions);

        if(!qstate.isEmpty())
        {
            try {

                controlCache();

                SolrQuery q = setupSolrQuery(query, qstate);
                QueryResponse response = solrServerAtlas.query(q);
                
                processResultGenes(response, result, qstate, query);

                Set<String> expandableEfs = new HashSet<String>();
                EfvTree<Integer> trimmedEfvs = trimColumns(query, result, expandableEfs);
                result.setResultEfvs(trimmedEfvs);
                result.setExpandableEfs(expandableEfs);

                if(response.getFacetFields() != null) {
                    result.setEfvFacet(getEfvFacet(response, qstate));
                    for(String p : drilldownGeneProperties) {
                        Set<String> hasVals = new HashSet<String>();
                        for(GeneQueryCondition qc : query.getGeneConditions())
                            if(qc.getFactor().equals(p))
                                hasVals.addAll(qc.getFactorValues());

                        Iterable<FacetCounter> facet = getGeneFacet(response, "property_f_" + p, hasVals);
                        if(facet.iterator().hasNext())
                            result.setGeneFacet(p, facet);
                    }
                    if(!query.getSpecies().iterator().hasNext())
                        result.setGeneFacet("species", getGeneFacet(response, "species", new HashSet<String>()));
                }
            } catch (SolrServerException e) {
                log.error("Error in structured query!", e);
            }            
        }

        return result;
    }

    public AtlasStructuredQueryResult findGenesForExperiment(Object geneIds, String eAcc, int start, int rows) {
        return doStructuredAtlasQuery(new AtlasStructuredQueryBuilder()
                .andGene(geneIds)
                .andUpdnIn(Constants.EXP_FACTOR_NAME, EscapeUtil.optionalQuote(eAcc))
                .viewAs(ViewType.LIST)
                .rowsPerPage(rows)
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
        
        SolrQuery q = new SolrQuery(query);
        q.addField("*");
        q.setRows(50);
        q.setStart(0);

        QueryResponse qr = solrServerExpt.query(q);
        for(SolrDocument doc : qr.getResults()) {
            String id = String.valueOf(doc.getFieldValue("id"));
            if(id != null) {
                result.add(id);
                for(String name : doc.getFieldNames())
                    if(name.startsWith("a_property_"))
                        for(Object val : doc.getFieldValues(name))
                            condEfvs.put(name.substring("a_property_".length()), String.valueOf(val), true);
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
                    Collection<List<AtlasEfoService.EfoTermCount>> efoPaths = new ArrayList<List<AtlasEfoService.EfoTermCount>>();
                    Collection<EfvTree.Efv<Boolean>> condEfos = condEfvs.getEfvs(Constants.EFO_FACTOR_NAME);
                    for(EfvTree.Efv<Boolean> efv : condEfos) {
                        efoPaths.addAll(efoService.getTermParentPaths(efv.getEfv()));
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
                solrq.append("(").append(geneQuery.getSolrEscapedFactorValues()).append(") ");
            } else if(Constants.GENE_PROPERTY_NAME.equals(geneQuery.getFactor())) {
                solrq.append("(name:(").append(geneQuery.getSolrEscapedFactorValues()).append(") ");
                solrq.append("identifier:(").append(geneQuery.getSolrEscapedFactorValues()).append(") ");
                for(String nameProp : AtlasProperties.getProperty("atlas.gene.autocomplete.names").split(","))
                    solrq.append("property_" + nameProp + ":(").append(geneQuery.getSolrEscapedFactorValues()).append(") ");
                solrq.append(")");
            } else if(allGeneProperties.contains(geneQuery.getFactor())) {
                String field = "property_" + geneQuery.getFactor();
                solrq.append(field).append(":(").append(geneQuery.getSolrEscapedFactorValues()).append(")");
            }
    	}
    }

    private void appendSpeciesQuery(AtlasStructuredQuery query, SolrQueryBuilder solrq) {
        if(query.getSpecies().iterator().hasNext())
        {
            solrq.appendAnd().append("species:(");
            for(String s : query.getSpecies())
                if(allSpecies.contains(s))
                    solrq.append(EscapeUtil.escapeSolr(s)).append(" ");
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
            for (String v : efvService.listAllValues(factor)) {
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
                for(EfoTerm term : efo.searchTerm(v)) {
                    condEfvs.put(Constants.EFO_FACTOR_NAME, term.getId(), true);
                }
            }
        }

        if(Constants.EFO_FACTOR_NAME.equals(factor))
            return condEfvs;

        String queryString = EscapeUtil.escapeSolrValueList(values);
        if(factor != null)
            queryString = "(" + queryString + ") AND property:" + EscapeUtil.escapeSolr(factor);

        SolrQuery q = new SolrQuery(queryString);
        q.setRows(10000);
        q.setStart(0);
        q.setFields("*");

        QueryResponse qr = solrServerProp.query(q);

        for(SolrDocument doc : qr.getResults())
        {
            String ef = (String)doc.getFieldValue("property");
            String efv = (String)doc.getFieldValue("value");
            condEfvs.put(ef, efv, true);
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

        Iterable<String> autoFactors = query.isFullHeatmap() ? getExperimentalFactors() : getConfiguredFactors("anycondition");

        for(SolrDocument doc : docs) {
            Integer id = (Integer)doc.getFieldValue("id");
            if(id == null)
                continue;

            AtlasGene gene = new AtlasGene(doc);
            if(response.getHighlighting() != null)
                gene.setGeneHighlights(response.getHighlighting().get(id.toString()));

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

                    values = doc.getFieldValues("efvs_up_" + EscapeUtil.encode(ef));
                    if(values != null)
                        for(Object efv : values) {
                            resultEfvs.getOrCreate(ef, (String)efv, numberer);
                        }

                    values = doc.getFieldValues("efvs_dn_" + EscapeUtil.encode(ef));
                    if(values != null)
                        for(Object efv : values) {
                            resultEfvs.getOrCreate(ef, (String)efv, numberer);
                        }
                }

                int threshold = 0;

                if(!query.isFullHeatmap()) {
                    if(resultEfos.getNumExplicitEfos() > 0)
                        threshold = 1;
                    else if(resultEfos.getNumExplicitEfos() > 20)
                        threshold = 3;
                }

                values = doc.getFieldValues("efos_up");
                if(values != null)
                    for(Object efoo : values) {
                        String efo = (String)efoo;
                        if(EscapeUtil.nullzero((Short)doc.getFieldValue("cnt_efo_" + efo + "_s_up")) > threshold)
                            resultEfos.add(efo, numberer, false);
                    }

                values = doc.getFieldValues("efos_dn");
                if(values != null)
                    for(Object efoo : values) {
                        String efo = (String)efoo;
                        if(EscapeUtil.nullzero((Short)doc.getFieldValue("cnt_efo_" + efo + "_s_dn")) > threshold)
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
                        EscapeUtil.nullzero((Short)doc.getFieldValue("cnt_" + cellId + "_up")),
                        EscapeUtil.nullzero((Short)doc.getFieldValue("cnt_" + cellId + "_dn")),
                        EscapeUtil.nullzero((Float)doc.getFieldValue("minpval_" + cellId + "_up")),
                        EscapeUtil.nullzero((Float)doc.getFieldValue("minpval_" + cellId + "_dn")));

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

//            result.addResult(new StructuredResultRow(doc, response.getHighlighting() == null ? null : response.getHighlighting().get(id.toString()), counters));

            result.addResult(new StructuredResultRow(gene, counters));
        }

        result.setResultEfvs(resultEfvs);
        result.setResultEfos(resultEfos);

        log.info("Retrieved query completely: " + result.getSize() + " records of " +
                result.getTotal() + " total starting from " + result.getStart() );

        log.debug("Resulting EFVs are: " + resultEfvs);
        log.debug("Resulting EFOs are: " + resultEfos);

    }

    /**
     * TODO
     * @param result
     * @param efvTree
     * @param efoTree
     * @param experiments
     */
    private void loadListExperiments(AtlasStructuredQueryResult result, AtlasGene gene, final EfvTree<Integer> efvTree, final EfoTree<Integer> efoTree, Set<String> experiments) {
        Iterable<Experiment> exps = null;
        ExperimentsTable table = gene.getExperimentsTable();

        if(efvTree.getNumEfvs() + efoTree.getNumExplicitEfos() > 0) {
            Iterable<String> efviter = new Iterable<String>() {
                public Iterator<String> iterator() {
                    return new MappingIterator<EfvTree.EfEfv<Integer>, String>(efvTree.getNameSortedList().iterator()) {
                        public String map(EfvTree.EfEfv<Integer> efEfv) {
                            return efEfv.getEfEfvId();
                        }
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
            exps = table.findByEfEfvEfoSet(efviter, efoiter);
        } else {
            exps = table.getAll();
        }

        Map<Pair<String,String>,List<ListResultRowExperiment>> map = new HashMap<Pair<String,String>, List<ListResultRowExperiment>>();
        for(Experiment exp : exps) {
        	if(!experiments.isEmpty() && !experiments.contains(String.valueOf(exp.getId())))
        		continue;
            AtlasExperiment aexp = atlasSolrDAO.getExperimentById(exp.getId());
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
        result.removeAll(AtlasProperties.getListProperty("atlas." + category + ".ignore.efs"));
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
            q.addField("id");
            q.addField("name");
            q.addField("identifier");
            q.addField("species");
            q.addField("exp_info");
            for(String p : allGeneProperties)
                q.addField("property_" + p);
            for(String p : nameGeneProperties)
                q.addField("property_" + p);
        } else {
            q.addField("*");
        }
        q.setFacetLimit(5 + max);

        q.setFacetMinCount(2);
        q.setFacetSort(true);

        for(String p : drilldownGeneProperties) {
            q.addFacetField("property_f_" + p);
        }

        q.addFacetField("species");
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
        q.addHighlightField("id");
        q.addHighlightField("name");
        q.addHighlightField("synonym");
        q.addHighlightField("identifier");
        for(String p : allGeneProperties)
            q.addHighlightField("property_" + p);
        for(String p : nameGeneProperties)
            q.addHighlightField("property_" + p);
        return q;
    }

    private Iterable<FacetCounter> getGeneFacet(QueryResponse response, final String name, Set<String> values) {
        List<FacetCounter> facet = new ArrayList<FacetCounter>();
        FacetField ff = response.getFacetField(name);
        if(ff == null || ff.getValueCount() < 2 || ff.getValues() == null)
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
        for (FacetField ff : response.getFacetFields()) {
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
                            AtlasExperiment exp = atlasSolrDAO.getExperimentById(ffc.getName());
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

    public List<String> getGenePropertyOptions() {
        List<String> result = new ArrayList<String>();
        result.addAll(allGeneProperties);
        result.add(Constants.GENE_PROPERTY_NAME);
        return result;
    }

    /**
     * Returns set of experimental factors
     * @return set of strings representing experimental factors
     */
    public Set<String> getExperimentalFactors() {
        if(allFactors.isEmpty()) {
            SolrQuery q = new SolrQuery("value_id:[* TO *]");
            q.setRows(0);
            q.addFacetField("property_f");
            q.setFacet(true);
            q.setFacetLimit(-1);
            q.setFacetMinCount(1);
            q.setFacetSort(true);
            try {
                QueryResponse qr = solrServerProp.query(q);
                if (qr.getFacetFields().get(0).getValues() != null) {
                    for (FacetField.Count ffc : qr.getFacetFields().get(0).getValues()) {
                        allFactors.add(ffc.getName());
                    }
                }
            } catch(SolrServerException e) {
                throw new RuntimeException("Can't fetch all factors", e);
            }
        }
        return allFactors;
    }

    public SortedSet<String> getSpeciesOptions() {
        if(allSpecies.isEmpty()) {
            SolrQuery q = new SolrQuery("id:[* TO *]");
            q.setRows(0);
            q.addFacetField("species");
            q.setFacet(true);
            q.setFacetLimit(-1);
            q.setFacetMinCount(1);
            q.setFacetSort(true);
            try {
                QueryResponse qr = solrServerAtlas.query(q);
                if (qr.getFacetFields().get(0).getValues() != null) {
                    for (FacetField.Count ffc : qr.getFacetFields().get(0).getValues()) {
                        allSpecies.add(ffc.getName());
                    }
                }
            } catch(SolrServerException e) {
                throw new RuntimeException("Can't fetch all factors", e);
            }
        }
        return allSpecies;
    }
}
