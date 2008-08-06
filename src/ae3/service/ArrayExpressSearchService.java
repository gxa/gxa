package ae3.service;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.core.MultiCore;
import org.apache.solr.schema.SchemaField;
import org.apache.lucene.index.IndexReader;
import uk.ac.ebi.ae3.indexbuilder.Constants;

import javax.sql.DataSource;
import java.io.*;
import java.sql.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ae3.model.AtlasExperiment;
import ae3.model.AtlasGene;
import ae3.model.AtlasTuple;
import ae3.dao.AtlasDao;
import ae3.dao.AtlasObjectNotFoundException;
import ae3.ols.webservice.axis.QueryServiceLocator;
import ae3.ols.webservice.axis.Query;
import net.sf.ehcache.CacheManager;

/**
 * User: ostolop
 * Date: 12-Feb-2008
 * <p/>
 *
 * The singleton application class for servicing ArrayExpress searches
 *
 * TODO: Add specialized exceptions.
 *
 * EBI Microarray Informatics Team (c) 2007, 2008
 */
public class ArrayExpressSearchService {
    protected final Log log = LogFactory.getLog(getClass());

    //// Full-text Search Index Instances

    private MultiCore  multiCore;
    private SolrServer solr_gene;
    private SolrServer solr_expt;
    private String solrIndexLocation;

    //// RDBMS Sources

    // ArrayExpress (AEW/Atlas) RDBMS DataSource
    private DataSource theAEDS;

    // In-memory (local ArrayExpress AEW/Atlas helper) RDBMS Datasource
    private DataSource memAEDS;

    private QueryRunner theAEQueryRunner;
    private QueryRunner memAEQueryRunner;

    //// DataServer Instance(s)

    // private ArrayExpresssDataServer aeds;


    //// AtlasResultSet Cache
    // TODO: Refactor to an external cache (e.g., ehcache) for caching AtlasResultSets, evicting LRU ones.
    //       When an element is removed from the cache, it needs to get removed from the in-memory DB as
    //       well (see the remove method on AtlasResultSetCache).
    private AtlasResultSetCache arsCache = new AtlasResultSetCache();

    private ArrayExpressSearchService() {};
    private static ArrayExpressSearchService _instance = null;

    /**
     * Returns the singleton instance.
     *
     * @return Singleton instance of AtlasSearch
     */
    public static ArrayExpressSearchService instance() {
      if(null == _instance) {
         _instance = new ArrayExpressSearchService();
      }

      return _instance;
    }

    public void initialize() {
        Connection conn = null;

        try {
	        multiCore = new MultiCore(solrIndexLocation, new File(solrIndexLocation, "multicore.xml"));
            solr_gene = new EmbeddedSolrServer(multiCore,"gene");
            solr_expt = new EmbeddedSolrServer(multiCore,"expt");

            conn = getMEMConnection();
            PreparedStatement stmt = conn.prepareStatement(
                    "CREATE TABLE atlas (" +
                            "idkey uuid, " +
                            "experiment_id int, " +
                            "experiment_accession varchar(255), " +
                            "experiment_description varchar(300), " +
                            "gene_id int, " +
                            "gene_name varchar(255), " +
                            "gene_identifier varchar(255), " +
                            "gene_species varchar(255)," +
                            "ef varchar(255), " +
                            "efv varchar(255), " +
                            "updn int, " +
                            "updn_pvaladj double," +
                            "gene_highlights varchar(4000) )"
            );
            stmt.execute();
            stmt.close();
        } catch (Exception e) {
            log.error(e);
        } finally {
            if (conn != null) try {
                conn.close();
            } catch (SQLException e) {}
        }

        theAEQueryRunner = new QueryRunner(theAEDS);
        memAEQueryRunner = new QueryRunner(memAEDS);

        Map<String, SchemaField> fieldMap = multiCore.getCore("expt").getSchema().getFields();
        log.info(fieldMap);
        Collection names = multiCore.getCore("expt").getSearcher().get().getReader().getFieldNames(IndexReader.FieldOption.ALL);
        log.info(names);

        CacheManager.create();
        arsCache.syncWithDB();
    }

    /**
     * Should be called when app is going down.
     */
    public void shutdown() {
        log.info("Shutting down ArrayExpressSearchService.");

        arsCache.syncWithDB();
        log.info("Shutting down AtlasResultSet cache: " + arsCache.size() + " result sets");
        CacheManager.getInstance().shutdown();

        log.info("Shutting down DB connections and indexes");
        try {
            if (theAEDS != null) theAEDS = null;
            if (memAEDS != null) memAEDS = null;

            if (multiCore != null) {
            	multiCore.shutdown();
            	solr_gene = null;
            	solr_expt = null;
            }
        } catch (Exception e) {
            log.error("Error shutting down ArrayExpressSearchService!", e);
        }
    }

    /**
     * Gives a connection from the pool. Don't forget to close.
     * TODO: DbUtils
     *
     * @return a connection from the pool
     * @throws SQLException
     */
    public Connection getMEMConnection() throws SQLException {
        if (memAEDS != null)
            return memAEDS.getConnection();

        return null;
    }

    public QueryResponse fullTextQueryGenes(String query) {
        if (query == null || query.equals(""))
            return null;

        try {
           // for now just search on the first 500 query terms
           if ( query.split("\\s").length > 500 ) {
               Pattern pattern = Pattern.compile("(\\p{Alnum}+\\s){500}");
               Matcher matcher = pattern.matcher(query);

               if(matcher.find()) {
                   String qi = matcher.group();
                   SolrQuery q = new SolrQuery(query);
                   q.setRows(100);
                   return solr_gene.query(q);
               }
           } else {
               SolrQuery q = new SolrQuery(query);
               q.setRows(500);
               q.setHighlight(true);
               q.addHighlightField("gene_goterm");
               q.addHighlightField("gene_interproterm");
               q.addHighlightField("gene_keyword");
               q.addHighlightField("gene_name");
               q.addHighlightField("gene_synonym");
               q.setHighlightSnippets(100);
               QueryResponse queryResponse = solr_gene.query(q);
               return queryResponse;
           }
        } catch (SolrServerException e) {
            log.error(e);
        }

        return null;
   }

    /**
     * Performs a full text SOLR search on experiments.
     *
     * @param query query terms, can be a complete Lucene query or just a bit of text
     * @return {@link org.w3c.dom.Document}
     */
    public QueryResponse fullTextQueryExpts(String query) {
    	return fullTextQueryExpts(query, 0, 50);
    }

    /**
     * Performs pagination and full text SOLR search on experiments.
     * @param query - A lucene query
     * @param start - a start record
     * @param rows - maximum number of Documents
     * @return
     */
  

    public QueryResponse fullTextQueryExpts(String query, int start, int rows)
    {
        if (query == null || query.equals(""))
            return null;
//
//        if (query.length()>500)
//            query = query.substring(0,500);

        try {
            if (query.indexOf("exp_factor_values:") == -1) {
                query = "exp_factor_values:(" + query + ")";
            }
            SolrQuery q = new SolrQuery(query);
            q.setHighlight(true);
            q.addHighlightField("exp_factor_values");
            q.setHighlightSnippets(100);
            q.setRows(rows);
            q.setStart(start);
            q.setFilterQueries("exp_in_dw:true");

            return solr_expt.query(q);
        } catch (SolrServerException e) {
            log.error(e);
        }

        return null;

    }

    public QueryResponse factorQueryExpts(String factor, Collection<String> values)
    {
        if (factor == null || factor.equals(""))
            return null;

        try {
            StringBuffer query = new StringBuffer(Constants.FIELD_FACTOR_PREFIX);
            query.append(factor).append(":(").append(StringUtils.join(values, " ")).append(")");
            SolrQuery q = new SolrQuery(query.toString());
            q.setHighlight(true);
            q.addHighlightField(Constants.FIELD_FACTOR_PREFIX + factor);
            q.setHighlightSnippets(100);
            q.setRows(50);
            q.setStart(0);
            q.setFilterQueries("exp_in_dw:true");

            return solr_expt.query(q);
        } catch (SolrServerException e) {
            log.error(e);
        }

        return null;

    }
  
    public QueryResponse queryExptsByField(String value, String field, int start, int rows){
    	 if (value == null || value.equals("") || field == null)
             return null;
    	 try {
			String query = field +":"+value;
			 SolrQuery q = new SolrQuery(query);
			 q.setRows(rows);
			 q.setStart(start);
			 return solr_expt.query(q);
		} catch (SolrServerException e) {
			log.error(e);
		}
		return null;
    	 
    }
    
    public QueryResponse query(SolrQuery query)
    {
      try {
           return solr_expt.query(query);
        } catch (SolrServerException e) {
            log.error(e);
        }

        return null;

    }

    public TreeSet<String> autoComplete(String query, String type) {
        if(query == null || query.equals(""))
            return null;

        if (type.equals("expt")) {
            return autoCompleteExpt(query);
        } else {
            return autoCompleteGene(query);
        }
    }

    private TreeSet<String> autoCompleteExpt(String query) {
        try {
            SolrQuery q = new SolrQuery("(suggest_token:"+query+" suggest_full:"+query+") AND exp_in_dw:true");
            q.setRows(0);
            q.setFacet(true);
            q.addFacetField("exp_factor_values_exact");
            q.setFacetLimit(-1);
            q.setFacetMinCount(1);

            QueryResponse qr = solr_expt.query(q);

            if (qr.getResults().getNumFound()==0)
                return null;
            TreeSet<String> s = new TreeSet<String>();
            SolrDocumentList docList = qr.getResults();

            for (FacetField.Count ffc : qr.getFacetFields().get(0).getValues()) {
                if(ffc.getName().toLowerCase().contains(query))
            	s.add(ffc.getName() + "|" + ffc.getCount());
            }

            return s;
        } catch (SolrServerException e) {
            log.error(e);
        }

        return null;
    }


    public List<String> autoCompleteFactorValues(String factor, String query, String limit) {
        try {
            SolrQuery q = new SolrQuery("exp_in_dw:true");
            q.setRows(0);
            q.setFacet(true);
            q.addFacetField(Constants.FIELD_FACTOR_PREFIX + factor);
            q.setFacetPrefix(query);

            int nlimit = 100;
            try {
                nlimit = Integer.parseInt(limit);
                if(nlimit > 1000)
                    nlimit = 1000;
            } catch(NumberFormatException e) {
                // just ignore
            }
 
            q.setFacetLimit(nlimit);
            q.setFacetSort(false);
            QueryResponse qr = solr_expt.query(q);

            if (null == qr.getFacetFields().get(0).getValues())
                return null;

            ArrayList<String> s = new ArrayList<String>();
            int i = 0;            
            for (FacetField.Count ffc : qr.getFacetFields().get(0).getValues()) {
                s.add(ffc.getName() + "|" + ffc.getCount());
            }

            return s;
        } catch (SolrServerException e) {
            log.error(e);
        }

        return null;
    }

private TreeSet<String> autoCompleteGene(String query) {

        try {
            SolrQuery q = new SolrQuery("gene_id:[0 TO *]");
            q.setRows(0);
            q.setFacet(true);
            q.addFacetField("gene_ids");
            q.addFacetField("gene_desc");
            q.setFacetLimit(20);
            q.setFacetPrefix(query);

            QueryResponse qr = solr_gene.query(q);

            if (null == qr.getFacetFields().get(0).getValues() &&
                null == qr.getFacetFields().get(1).getValues())
                return null;

            TreeSet<String> s = new TreeSet<String>();

            if (null != qr.getFacetFields().get(0).getValues()) {
                for (FacetField.Count ffc : qr.getFacetFields().get(0).getValues()) {
                    s.add(ffc.getName() + "|" + ffc.getCount());
                }
            }

            if (null != qr.getFacetFields().get(1).getValues()) {
                for (FacetField.Count ffc : qr.getFacetFields().get(1).getValues()) {
                    s.add(ffc.getName() + "|" + ffc.getCount());
                }
            }

            return s;
        } catch (SolrServerException e) {
            log.error(e);
        }

        return null;
    }

    public SolrDocumentList getNumDocAer(String query, boolean countSpecies) throws SolrServerException
    {

        SolrQuery q = new SolrQuery(query);
        if (countSpecies)
        {
            q.setFacet(true);
            q.setFacetLimit(-1);
            q.setFields(Constants.FIELD_AER_SAAT_VALUE);
            q.setFilterQueries(Constants.FIELD_AER_SAAT_CAT + ":Organism");
        }
        q.setRows(1);
        q.setStart(0);
        QueryResponse queryResponse = solr_expt.query(q);
        
        SolrDocumentList l=queryResponse.getResults();
        return l;
    }

    /**
     * Executes an atlas query to retrieve expt acc, desc, ef, efv, gene, updn, and p-value for requested gene ids,
     * optionally restricting to a supplied list of experiment ids.
     */
    public AtlasResultSet doAtlasQuery(final QueryResponse geneHitsResponse,
                                       final QueryResponse exptHitsResponse,
                                       final String geneExprFilter,
                                       final String geneSpeciesFilter) throws IOException {
        if (geneHitsResponse == null && exptHitsResponse == null)
            return null;

        if (geneHitsResponse != null && geneHitsResponse.getResults().getNumFound() == 0)
            return null;

        if (exptHitsResponse != null && exptHitsResponse.getResults().getNumFound() == 0)
            return null;

        String updn_filter = " and updn <> 0\n";

        if (geneExprFilter.equals("up"))
            updn_filter = " and updn = 1\n";

        if (geneExprFilter.equals("down"))
            updn_filter = " and updn = -1\n";

//        String gene_species_filter = "";
//        if (geneSpeciesFilter != null)
//            gene_species_filter = " and UPPER(ad_species.value)='" + geneSpeciesFilter + "'";

        final Map<String, SolrDocument> solrExptMap = convertSolrDocumentListToMap(exptHitsResponse, Constants.FIELD_DWEXP_ID);
        final Map<String, SolrDocument> solrGeneMap = convertSolrDocumentListToMap(geneHitsResponse, "gene_id");

        String inGeneIds = (solrGeneMap == null ? "" : StringUtils.join(solrGeneMap.keySet(), ","));
        String inExptIds = (solrExptMap == null ? "" : StringUtils.join(solrExptMap.keySet(), ","));

        String efvFilter = "";

        if (exptHitsResponse != null /*&& inGeneIds.length() == 0*/ ) {
            Set ss = new HashSet<String>();
            Map<String,Map<String, List<String>>> hl = exptHitsResponse.getHighlighting();

            for ( Map<String, List<String>> vals : hl.values() ) {
                if (vals == null || vals.size() == 0) continue;
                for(String s : vals.get("exp_factor_values")) {
                    ss.add("'" + s.replaceAll("</{0,1}em>","").replaceAll("'", "''") + "'");
                }
            }

            if (ss.size() > 0)
                efvFilter = "and atlas.efv IN (" + StringUtils.join(ss.toArray(), ",") + ") \n";
        }

        String geneReqUrl = geneHitsResponse == null ? "" : (String) ((NamedList) geneHitsResponse.getHeader().get("params")).get("q");
        String exptReqUrl = exptHitsResponse == null ? "" : (String) ((NamedList) exptHitsResponse.getHeader().get("params")).get("q");

        final String arsCacheKey = geneReqUrl + exptReqUrl + efvFilter + updn_filter + geneSpeciesFilter;

        if (arsCache.containsKey(arsCacheKey)) {
            log.info("Cache hit for " + arsCacheKey);
            return arsCache.get(arsCacheKey);
        }

        String atlas_query_topN = "SELECT * FROM (\n" +
                " SELECT\n" +
                "         atlas.experiment_id_key,\n" +
//                "         expt.experiment_identifier,\n" +
//                "         expt.experiment_description, \n" +
                "         atlas.gene_id_key, \n" +
//                "         gene.gene_name,\n" +
//                "         gene.gene_identifier,\n" +
//                "         gene.designelement_name,\n" +
//                "         gene_species.value as species,\n" +
                "         atlas.ef, \n" +
                "         atlas.efv,\n" +
                "         atlas.updn,\n" +
                "         atlas.updn_tstat,\n" +
                "         atlas.updn_pvaladj,\n" +
                "         row_number()\n" +
                "            OVER (\n" +
                "              PARTITION BY atlas.EXPERIMENT_ID_KEY, atlas.ef, atlas.efv\n" +
                "              ORDER BY atlas.updn_pvaladj asc, UPDN_TSTAT desc\n" +
                "            ) TopN \n"+ //,\n" +
//                "         min(UPDN_PVALADJ) over (partition by efv) as min_updn_pvaladj_PER_EFV,\n" +
//                "         count(case when updn = -1 then null else 1 end) over (partition by efv) as countup_PER_EFV,\n" +
//                "         count(case when updn =  1 then null else 1 end) over (partition by efv) as countdn_PER_EFV,\n" +
//                "         min(UPDN_PVALADJ) over (partition by atlas.GENE_ID_KEY) as min_updn_pvaladj_PER_GENE,\n" +
//                "         count(case when updn = -1 then null else 1 end) over (partition by atlas.gene_id_key) as countup_PER_GENE,\n" +
//                "         count(case when updn =  1 then null else 1 end) over (partition by atlas.gene_id_key) as countdn_PER_GENE\n" +
                "        from aemart.atlas atlas \n" + //, aemart.ae2__designelement__main gene , aemart.ae1__experiment__main expt, aemart.AE2__GENE_SPECIES__DM gene_species \n" +
                "        where 1 = 1" + // atlas.designelement_id_key=gene.designelement_id_key \n" +
//                " and atlas.experiment_id_key=expt.experiment_id_key\n" +
//                " and gene.gene_identifier is not null\n" +
                " and atlas.experiment_id_key NOT IN (211794549,215315583,384555530,411493378,411512559) \n" + // ignore E-TABM-145a,b,c
//                " and ad_species.arraydesign_id_key=atlas.arraydesign_id_key\n" +
//                " and gene_species.gene_id_key=gene.gene_id_key \n" +
                (inGeneIds.length() != 0 ? "and atlas.gene_id_key       IN (" + inGeneIds + ") \n" : "" ) +
                (inExptIds.length() != 0 ? "and atlas.experiment_id_key IN (" + inExptIds + ") \n" : "" ) +
                updn_filter +
                efvFilter   +
//                gene_species_filter +
                ")\n" +
                " WHERE \n" +
                " TopN <= 20 and gene_id_key is not null\n" +
                " ORDER by updn_pvaladj asc, updn_tstat desc";

        log.info(atlas_query_topN);

        AtlasResultSet arset = null;
        try {
            arset = (AtlasResultSet) theAEQueryRunner.query(atlas_query_topN, new ResultSetHandler() {
                public AtlasResultSet handle(ResultSet rs) throws SQLException {
                    AtlasResultSet arset = new AtlasResultSet(arsCacheKey);

                    while(rs.next()) {
                        AtlasResult atlasResult = new AtlasResult();

                        AtlasExperiment expt = null;
                        AtlasGene gene       = null;

                        String experiment_id_key = rs.getString("experiment_id_key");
                        String gene_id_key = rs.getString("gene_id_key");

                        try {
                            if (solrExptMap != null && solrExptMap.containsKey(experiment_id_key)) {
                                expt = AtlasDao.getExperimentByIdDw(solrExptMap.get(experiment_id_key), exptHitsResponse);
                            } else {
                                expt = AtlasDao.getExperimentByIdDw(experiment_id_key);
                            }

                            if (solrGeneMap != null && solrGeneMap.containsKey(gene_id_key)) {
                                gene = AtlasDao.getGene(solrGeneMap.get(gene_id_key), geneHitsResponse);
                            } else {
                                gene = AtlasDao.getGene(gene_id_key);
                            }
                        } catch (AtlasObjectNotFoundException e) {
                            log.error(e);
                        }

                        AtlasTuple atuple = new AtlasTuple(rs.getString("ef"), rs.getString("efv"), rs.getInt("updn"), rs.getDouble("updn_pvaladj"));

                        //TODO: disabled highlighting in experiments, it's broken on SOLR side, it seems. Example: query for liver.
                        if ( expt != null && expt.getExperimentHighlights() != null) {
                            List<String> s = expt.getExperimentHighlights().get("exp_factor_values");
                            if (s != null ) {
                                for (String efv : s) {
                                    if(atuple.getEfv().equals(efv.replaceAll("</{0,1}em>",""))) {
                                        atuple.setEfv(efv);
                                        break;
                                    }
                                }
                            }
                        }


    //
    //                    if (geneHitsResponse != null) {
    //                        Map<String, List<String>> hilites = geneHitsResponse.getHighlighting().get(atlasResult.getGene().getGeneId());
    //
    //                        Set<String> hls = new HashSet<String>();
    //                        for (String hlf : hilites.keySet()) {
    //                            hls.add(hlf + ": " + StringUtils.join(hilites.get(hlf), ";"));
    //                        }
    //
    //                        if(hls.size() > 0)
    //                            atlasResult.getGene().setGeneHighlights(StringUtils.join(hls,"<br/>"));
    //                    }
    //

                        if( ( expt != null && gene != null )
                                &&
                            ( geneSpeciesFilter == null || geneSpeciesFilter.equals("") || geneSpeciesFilter.equals("any") || geneSpeciesFilter.equalsIgnoreCase(gene.getGeneSpecies())) ) {
                                atlasResult.setExperiment(expt);
                                atlasResult.setGene(gene);
                                atlasResult.setAtuple(atuple);

                            arset.add(atlasResult);
                        }
                    }

                    return arset;
                }
            } );

            log.info("Retrieved query completely: " + arset.size() + " records" );
            arsCache.put(arset);
        } catch (SQLException e) {
            log.error(e);
        }

        return arset;
    }

    /**
     */
    public AtlasResultSet doExtendedAtlasQuery(final AtlasStructuredQuery query) throws IOException {
        final QueryResponse geneHitsResponse = ArrayExpressSearchService.instance().fullTextQueryGenes(query.getGene());
        if (geneHitsResponse != null && geneHitsResponse.getResults().getNumFound() == 0)
            return null;

        if(geneHitsResponse == null && query.getConditions().size() == 0)
            return null;

        final Map<String, SolrDocument> solrGeneMap = convertSolrDocumentListToMap(geneHitsResponse, "gene_id");

        String inGeneIds = (solrGeneMap == null ? "" : StringUtils.join(solrGeneMap.keySet(), ","));


        String geneReqUrl = geneHitsResponse == null ? "" : (String) ((NamedList) geneHitsResponse.getHeader().get("params")).get("q");

        final Map<String, SolrDocument> solrExptMap = new HashMap<String, SolrDocument>();
        final Map<String, QueryResponse> solrExptResponseMap = new HashMap<String, QueryResponse>();
        StringBuffer conditionsWhere = new StringBuffer();
        Iterator<AtlasStructuredQuery.Condition> i = query.getConditions().iterator();
        while(i.hasNext())
        {
            AtlasStructuredQuery.Condition c = i.next();
            StringBuffer condition = new StringBuffer();
            condition.append("(atlas.");
            switch(c.getExpression())
            {
                case UP: condition.append("updn = 1"); break;
                case DOWN: condition.append("updn = -1"); break;
                case NOT_UP: condition.append("updn <> 1"); break;
                case NOT_DOWN: condition.append("updn <> -1"); break;
                case UP_DOWN: condition.append("updn <> 0"); break;
                case NOT_EXPRESSED: condition.append("updn = 0"); break;
                default: throw new IllegalArgumentException("Unknown regulation option specified " + c.getExpression());
            }

            QueryResponse response = ArrayExpressSearchService.instance().factorQueryExpts(c.getFactor(), c.getFactorValues());
            if (response == null || response.getResults().getNumFound() == 0)
                break;

            final Map<String, SolrDocument> map = convertSolrDocumentListToMap(response, Constants.FIELD_DWEXP_ID);
            condition.append(" AND atlas.experiment_id_key IN (").append(StringUtils.join(map.keySet(), ",")).append(")");

            Set<String> factorValues = new HashSet<String>();
            for (Map<String, List<String>> vals : response.getHighlighting().values()) {
                if (vals == null || vals.size() == 0)
                    continue;
                for(String s : vals.get(Constants.FIELD_FACTOR_PREFIX + c.getFactor())) {
                    factorValues.add("'" + s.replaceAll("</{0,1}em>","").replaceAll("'", "''") + "'");
                }
            }

            condition.append(" AND atlas.efv IN (").append(StringUtils.join(factorValues, ",")).append("))");

            conditionsWhere.append(condition);
            if(i.hasNext())
                conditionsWhere.append(" OR ");

            for(String key : map.keySet())
                solrExptResponseMap.put(key, response);
            solrExptMap.putAll(map);
        }

        if(geneHitsResponse == null && conditionsWhere.length() == 0)
            return null;

        final String arsCacheKey = geneReqUrl + conditionsWhere;

        if (arsCache.containsKey(arsCacheKey)) {
            log.info("Cache hit for " + arsCacheKey);
            return arsCache.get(arsCacheKey);
        }

        String atlas_query_topN = "SELECT * FROM (\n" +
                " SELECT\n" +
                "         atlas.experiment_id_key,\n" +
                "         atlas.gene_id_key, \n" +
                "         atlas.ef, \n" +
                "         atlas.efv,\n" +
                "         atlas.updn,\n" +
                "         atlas.updn_tstat,\n" +
                "         atlas.updn_pvaladj,\n" +
                "         row_number()\n" +
                "            OVER (\n" +
                "              PARTITION BY atlas.EXPERIMENT_ID_KEY, atlas.ef, atlas.efv\n" +
                "              ORDER BY atlas.updn_pvaladj asc, UPDN_TSTAT desc\n" +
                "            ) TopN \n"+ //,\n" +
                "        from aemart.atlas atlas \n" +
                "        where 1 = 1" +
                " and atlas.experiment_id_key NOT IN (211794549,215315583,384555530,411493378,411512559) \n" + // ignore E-TABM-145a,b,c
                (inGeneIds.length() != 0 ? "and atlas.gene_id_key       IN (" + inGeneIds + ") \n" : "" ) +
                (conditionsWhere.length() != 0 ? " and " + conditionsWhere.toString() : "") +
                ")\n" +
                " WHERE \n" +
                " TopN <= 20 and gene_id_key is not null\n" +
                " ORDER by updn_pvaladj asc, updn_tstat desc";

        log.info(atlas_query_topN);

        AtlasResultSet arset = null;
        try {
            arset = (AtlasResultSet) theAEQueryRunner.query(atlas_query_topN, new ResultSetHandler() {
                public AtlasResultSet handle(ResultSet rs) throws SQLException {
                    AtlasResultSet arset = new AtlasResultSet(arsCacheKey);

                    while(rs.next()) {
                        AtlasResult atlasResult = new AtlasResult();

                        AtlasExperiment expt = null;
                        AtlasGene gene       = null;

                        String experiment_id_key = rs.getString("experiment_id_key");
                        String gene_id_key = rs.getString("gene_id_key");

                        try {
                            if (solrExptMap != null && solrExptMap.containsKey(experiment_id_key)) {
                                expt = AtlasDao.getExperimentByIdDw(solrExptMap.get(experiment_id_key),
                                        solrExptResponseMap.get(experiment_id_key));
                            } else {
                                expt = AtlasDao.getExperimentByIdDw(experiment_id_key);
                            }

                            if (solrGeneMap != null && solrGeneMap.containsKey(gene_id_key)) {
                                gene = AtlasDao.getGene(solrGeneMap.get(gene_id_key), geneHitsResponse);
                            } else {
                                gene = AtlasDao.getGene(gene_id_key);
                            }
                        } catch (AtlasObjectNotFoundException e) {
                            log.error(e);
                        }

                        AtlasTuple atuple = new AtlasTuple(rs.getString("ef"), rs.getString("efv"), rs.getInt("updn"), rs.getDouble("updn_pvaladj"));

                        if(( expt != null && gene != null )
                                && ( query.getSpecies().isEmpty() || query.getSpecies().contains(gene.getGeneSpecies())) ) {
                            atlasResult.setExperiment(expt);
                            atlasResult.setGene(gene);
                            atlasResult.setAtuple(atuple);

                            arset.add(atlasResult);
                        }
                    }

                    return arset;
                }
            } );

            log.info("Retrieved query completely: " + arset.size() + " records" );
            arsCache.put(arset);
        } catch (SQLException e) {
            log.error(e);
        }

        return arset;
    }

    public ArrayList getAtlasResults(String query){
    	 ArrayList<AtlasTuple> atlasTuples = null;
         try {
        	 atlasTuples =  (ArrayList<AtlasTuple>)theAEQueryRunner.query(query, new ResultSetHandler() {
                 public ArrayList<AtlasTuple> handle(ResultSet rs) throws SQLException {
                    ArrayList<AtlasTuple> atlasTuples = new ArrayList<AtlasTuple>();

                     while(rs.next()) {
                         

                         AtlasTuple atuple = new AtlasTuple(rs.getString("ef"), rs.getString("efv"), rs.getInt("updn"), rs.getDouble("updn_pvaladj"));
                         atlasTuples.add(atuple);
                        
                         
                     }

                     return atlasTuples;
                 }
             } );

//             log.info("Retrieved query completely: " + arset.size() + " records" );
//             arsCache.put(arset);
         } catch (SQLException e) {
             log.error(e);
         }
         return atlasTuples;
    	
    }
    
    public ArrayList getRankedGeneExperiments(String gene_id_key){
   	 ArrayList<AtlasExperiment> atlasExps = null;
   	 String query = "select experiment_id_key, MIN(atlas.UPDN_PVALADJ) as minp " +
   	 		"from ATLAS " +
   	 		"where gene_id_key = "+gene_id_key +
   	 	    " and atlas.experiment_id_key NOT IN (211794549,215315583,384555530,411493378,411512559)"+
   	 		" group by experiment_id_key " +
   	 		"order by minp asc";
        try {
        	atlasExps =  (ArrayList<AtlasExperiment>)theAEQueryRunner.query(query, new ResultSetHandler() {
                public ArrayList<AtlasExperiment> handle(ResultSet rs) throws SQLException {
                   ArrayList<AtlasExperiment> atlasExps = new ArrayList<AtlasExperiment>();

                    try {
						while(rs.next()) {
						    

						    AtlasExperiment atlasExp = AtlasDao.getExperimentByIdDw(String.valueOf((rs.getInt("experiment_id_key"))));
						    if(atlasExp != null)
						    atlasExps.add(atlasExp);
						   
						    
						}
					} catch (AtlasObjectNotFoundException e) {
						log.error(e);
					}

                    return atlasExps;
                }
            } );

//            log.info("Retrieved query completely: " + arset.size() + " records" );
//            arsCache.put(arset);
        } catch (SQLException e) {
            log.error(e);
        }
        return atlasExps;
   	
   }

    public void setAEDataSource(DataSource aeds) {
        this.theAEDS = aeds;
    }

    public void setMEMDataSource(DataSource memds) {
        this.memAEDS = memds;
    }

    public void setSolrIndexLocation(String solrIndexLocation) {
        this.solrIndexLocation = solrIndexLocation;
    }

//    public long writeAtlasQuery(QueryResponse geneHitsResponse, QueryResponse exptHitsResponse, String updn_filter, HtmlTableWriter tw) throws IOException {
//        if (geneHitsResponse == null && exptHitsResponse == null)
//            return 0;
//
//        String inGeneIds = getSqlInClauseFromSolrHits(geneHitsResponse, "gene_id");
//        String inExptIds = getSqlInClauseFromSolrHits(exptHitsResponse, "exp_id");
//
//        return writeAtlasQuery(inGeneIds, inExptIds, exptHitsResponse, geneHitsResponse, updn_filter, tw);
//    }


    private static Map<String, SolrDocument> convertSolrDocumentListToMap (QueryResponse queryResponse, String idField ) {
        if (queryResponse == null)
            return null;

        SolrDocumentList hits = queryResponse.getResults();
        Map<String, SolrDocument> idMap = new HashMap<String, SolrDocument>();

        for (SolrDocument doc : hits) {
            String id = String.valueOf(doc.getFieldValue(idField));
            if(id != null) idMap.put(id, doc);
        }

        return idMap;
    }

    protected void finalize() throws Throwable {
        shutdown();
    }

    public List<String[]> getGeneExpressionOptions() {
        return AtlasStructuredQuery.Expression.getOptionsList();
    }

    public List<String> getExperimentalFactorOptions() {
        @SuppressWarnings("unchecked")
        List<String> names = new ArrayList<String>();
        for(String i : (Collection<String>)multiCore.getCore("expt").getSearcher().get().getReader().getFieldNames(IndexReader.FieldOption.ALL)) {
            if(i.startsWith(Constants.FIELD_FACTOR_PREFIX)) {
                names.add(i.substring(Constants.FIELD_FACTOR_PREFIX.length()));
            }
        }
                
        return names;
    }        
    
    public SortedSet<String> getAllAvailableAtlasSpecies() {
        SortedSet<String> species = null;

        try {
            species = (SortedSet<String>) theAEQueryRunner.query(
                    "SELECT DISTINCT value FROM ae2__arraydesign_species WHERE arraydesign_id_key IN (SELECT DISTINCT arraydesign_id FROM ae1__sample__main) ORDER BY value DESC",
                    new ResultSetHandler() {
                        public Object handle(ResultSet rs) throws SQLException {
                            SortedSet<String> species = new TreeSet<String>();
                            while(rs.next()) {
                                species.add(rs.getString(1));
//                                species.add(s.substring(0,1).toUpperCase() + s.substring(1).toLowerCase());
                            }

                            return species;
                        }
                    }
                );
        } catch (SQLException e) {
            log.error(e);
        }

        return species;
    }

    public List<HashMap> getFullGeneEFVCounts() throws SQLException {
        String querySQL = "select  count(distinct case when updn=1 then gene_id_key else null end) gup_count,\n" +
                "        count(distinct case when updn=1 then experiment_id_key else null end) eup_count, \n" +
                "        count(distinct case when updn=-1 then gene_id_key else null end) gdn_count,\n" +
                "        count(distinct case when updn=-1 then experiment_id_key else null end) edn_count, \n" +
                "        ef, \n" +
                "        efv \n" +
                "from atlas\n" +
                "where\n" +
                "gene_id_key is not null\n" +
                "group by ef, efv";

        log.info(querySQL);

        List<HashMap> geneEFVCounts = (List<HashMap>) theAEQueryRunner.query(querySQL, new ResultSetHandler() {
            public List<HashMap> handle(ResultSet rs) throws SQLException {
                List<HashMap> geneEFVCounts = new ArrayList<HashMap>();
                while(rs.next()) {
                    HashMap m = new HashMap();

                    m.put("gup_count", rs.getInt("gup_count"));
                    m.put("eup_count", rs.getInt("eup_count"));
                    m.put("gdn_count", rs.getInt("gdn_count"));
                    m.put("edn_count", rs.getInt("edn_count"));

                    m.put("ef", rs.getString("ef"));
                    m.put("efv", rs.getString("efv"));

                    geneEFVCounts.add(m);
                }

                return geneEFVCounts;
            }
        } );

        log.info("Query executed..." + geneEFVCounts.size() + " results found.");

        return geneEFVCounts;
    }

    public QueryResponse fullTextQueryExptsWithOntologyExpansion(String q_expt) {
        try {
            Query olsQuery = new QueryServiceLocator().getOntologyQuery();
            HashMap<String,String> terms = olsQuery.getTermsByExactName(q_expt, "EFO");

            Set<String> ontologyExpansion = new HashSet<String>();
            StringBuffer s = new StringBuffer(q_expt);

            for (String term : terms.keySet()) {
                HashMap<String,String> termChildren = olsQuery.getTermChildren(term, "EFO", -1, null);
                ontologyExpansion.addAll(termChildren.values());
            }

            for (String term : ontologyExpansion) {
                term = "\""  + term + "\"";
                s.append(" exp_factor_values_exact:").append(term);
            }
            
            String expanded_q_expt = s.toString();
            log.info("Expanding experiments query with EFO to: " + expanded_q_expt);

            QueryResponse qr = fullTextQueryExpts(expanded_q_expt);

            if(null != qr) qr.getHeader().add("expanded_efo", expanded_q_expt.replaceAll(" exp_factor_values_exact:", " "));
            return qr;
        } catch (Exception e) {
            log.error("Failed to expand query with EFO, proceeding with normal query", e);
            return fullTextQueryExpts(q_expt);
        }
    }
}
