package ae3.service;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.StringEscapeUtils;
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
import javax.xml.rpc.ServiceException;
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
import static ae3.util.StructuredQueryHelper.encodeEfv;
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
    private SolrServer solr_expt;
    private SolrServer solr_atlas;
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

    private static final int MAX_STRUCTURED_CONDITIONS = 20;

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
            solr_expt = new EmbeddedSolrServer(multiCore,"expt");
            solr_atlas = new EmbeddedSolrServer(multiCore,"atlas");

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
                   return solr_atlas.query(q);
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
               QueryResponse queryResponse = solr_atlas.query(q);
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
        try {
            if (factor == null || factor.equals(""))
                factor = "exp_factor_values";
            else
                factor = Constants.FIELD_FACTOR_PREFIX  + factor;

            ArrayList<String> vals = new ArrayList<String>();
            for(String v : values) {
                vals.add(v.matches("^.*[\"*?].*$") ? v.replace("*", "?*") : "\"" + v + "\"");                
            }
            StringBuffer query = new StringBuffer()
                    .append(factor).append(":(").append(StringUtils.join(vals, " ")).append(")");
            SolrQuery q = new SolrQuery(query.toString());
            q.setHighlight(true);
            q.addHighlightField(factor);
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


    public Map<String, Long> autoCompleteFactorValues(String factor, String query, int limit) {
        try {
            SolrQuery q = new SolrQuery("exp_in_dw:true");
            q.setRows(0);
            q.setFacet(true);
            if (factor == null || factor.equals(""))
                factor = "exp_factor_values";
            else
                factor = Constants.FIELD_FACTOR_PREFIX  + factor;
            q.addFacetField(factor);
            q.setFacetPrefix(query);

            q.setFacetLimit(limit);
            q.setFacetSort(true);
            QueryResponse qr = solr_expt.query(q);

            if (null == qr.getFacetFields().get(0).getValues())
                return null;

            Map<String,Long> s = new TreeMap<String,Long>();
            int i = 0;            
            for (FacetField.Count ffc : qr.getFacetFields().get(0).getValues()) {
                s.put(ffc.getName(), ffc.getCount());
            }

            return s;
        } catch (SolrServerException e) {
            log.error(e);
        }

        return null;
    }

private TreeSet<String> autoCompleteGene(String query) {

        try {
        	SolrQuery q = new SolrQuery("suggest_token:"+query+" suggest_full:"+query);
        	SolrQuery q_name = new SolrQuery("gene_name:"+query+" gene_synonym:"+query);
        	q_name.setFields("gene_name");
            q.setRows(0);
            q.setFacet(true);
//            q.addFacetField("gene_ids");
//            q.addFacetField("gene_name_exact");
            q.addFacetField("gene_disease_exact");
            q.addFacetField("gene_goterm_exact");
//            q.addFacetField("gene_protein_exact");
            q.setFacetLimit(20);
//            q.setFacetSort(false);
//            q.setFacetPrefix(query);
            q.setFacetMinCount(1);

            QueryResponse qr = solr_atlas.query(q);
            QueryResponse qr_name = solr_atlas.query(q_name);
            
            if (qr.getResults().getNumFound()==0 && qr_name.getResults().getNumFound()==0)
                return null;
            TreeSet<String> s = new TreeSet<String>();
            SolrDocumentList docList = qr_name.getResults();
            for(SolrDocument doc:docList){
            	s.add(doc.getFieldValue("gene_name").toString() + "|" +"1");
            }
            

            if (null != qr.getFacetFields().get(0).getValues())
            for (FacetField.Count ffc : qr.getFacetFields().get(0).getValues()) {
                if(ffc.getName().toLowerCase().contains(query.toLowerCase()) || query.toLowerCase().contains(ffc.getName().toLowerCase()))
            	s.add(ffc.getName() + "|" + ffc.getCount());
            }
            if (null != qr.getFacetFields().get(1).getValues())
            for (FacetField.Count ffc : qr.getFacetFields().get(1).getValues()) {
                if(ffc.getName().toLowerCase().contains(query.toLowerCase()) || query.toLowerCase().contains(ffc.getName().toLowerCase()))
            	s.add(ffc.getName() + "|" + ffc.getCount());
            }
//            if (null != qr.getFacetFields().get(2).getValues())
//            for (FacetField.Count ffc : qr.getFacetFields().get(2).getValues()) {
//                if(ffc.getName().toLowerCase().contains(query.toLowerCase()) || query.toLowerCase().contains(ffc.getName().toLowerCase()))
//            	s.add(ffc.getName() + "|" + ffc.getCount());
//            }

//            if (null == qr.getFacetFields().get(0).getValues() &&
//                null == qr.getFacetFields().get(1).getValues())
//                return null;
//
//            TreeSet<String> s = new TreeSet<String>();
//
//            if (null != qr.getFacetFields().get(0).getValues()) {
//                for (FacetField.Count ffc : qr.getFacetFields().get(0).getValues()) {
//                    s.add(ffc.getName() + "|" + ffc.getCount());
//                }
//            }
//
//            if (null != qr.getFacetFields().get(1).getValues()) {
//                for (FacetField.Count ffc : qr.getFacetFields().get(1).getValues()) {
//                    s.add(ffc.getName() + "|" + ffc.getCount());
//                }
//            }

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
                "        where gene_id_key <> 0 " + // atlas.designelement_id_key=gene.designelement_id_key \n" +
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

    private static int nullzero(Integer i)
    {
        return i == null ? 0 : i;
    }

    private static double nullzero(Double d)
    {
        return d == null ? 0.0d : d;
    }

    /**
     * Process structured Atlas query
     * @param query parsed query
     * @return matching results
     * @throws IOException
     */
    public AtlasStructuredQueryResult doStructuredAtlasQuery(final AtlasStructuredQuery query) throws IOException {
        final StringBuffer solrq = new StringBuffer();
        final StringBuffer scores = new StringBuffer();

        final List<String> fields = new ArrayList<String>();

        scores.append("sum(");

        EfvTree<Boolean> queryEfvs = new EfvTree<Boolean>();

        Set<String> allFactors = getExperimentalFactorOptions();

        int number = 0;
        for(AtlasStructuredQuery.Condition c : query.getConditions())
        {
            if(number > 0) {
                solrq.append(" AND ");
            }

            solrq.append("(");

            try {

                StringBuffer expQuery = new StringBuffer();
                if(c.getFactor().length() > 0) {
                    expQuery.append(Constants.FIELD_FACTOR_PREFIX).append(c.getFactor())
                            .append(":(");
                    boolean anyVal = false;
                    for(String v : c.getFactorValues())
                        if(v.equals("*")) {
                            anyVal = true;
                            break;
                        }
                    if(anyVal) {
                        for(String v : autoCompleteFactorValues(c.getFactor(), "", 20).keySet()) {
                            expQuery.append(" ").append("\"").append(v).append("\"");
                        }
                    } else {
                        for(String v : c.getFactorValues()) {
                            expQuery.append(" ").append(v.matches("^.*[\"*?].*$") ? v.replace("*", "?*") : "\"" + v + "\"");
                        }
                    }                    
                    expQuery.append(")");
                } else {
                    Query olsQuery = null;
                    try {
                        olsQuery = new QueryServiceLocator().getOntologyQuery();
                    } catch(ServiceException e) {
                        log.error(e);
                    }
                    for(String fv : c.getFactorValues()) {
                        expQuery.append(" ").append(fv.replace("*", "?*"));
                        if(olsQuery != null) {
                            HashMap<String,String> terms = olsQuery.getTermsByExactName(fv, "EFO");
                            Set<String> ontologyExpansion = new TreeSet<String>();

                            for (String term : terms.keySet()) {
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
                if(c.getFactor().length() > 0) {
                    q.addHighlightField(Constants.FIELD_FACTOR_PREFIX + c.getFactor());
                } else {
                    q.addHighlightField("exp_factor_values_exact");
                    q.addHighlightField("exp_factor_values");
                }
                q.setHighlightSnippets(100);
                q.setRows(50);
                q.setStart(0);
                q.setFilterQueries("exp_in_dw:true");

                QueryResponse response = solr_expt.query(q);
                if (response == null || response.getResults().getNumFound() == 0)
                    continue;

                EfvTree<Boolean> condEfvs = new EfvTree<Boolean>();
                if(c.getFactor().length() > 0)
                {
                    for(Map<String, List<String>> vals : response.getHighlighting().values())
                    {
                        for(String s : vals.values().iterator().next()) {
                           condEfvs.getOrCreate(c.getFactor(), s.replaceAll("</?em>", ""), true);
                        }
                    }
                } else {
                    Map<String,SolrDocument> docmap = convertSolrDocumentListToMap(response,
                            multiCore.getCore("expt").getSearcher().get().getSchema().getUniqueKeyField().getName());

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
                                    Collection fvs = doc.getFieldValues(Constants.FIELD_FACTOR_PREFIX + f);
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

                }

                for(EfvTree.EfEfv<Boolean> condEfv : condEfvs.getNameSortedList())
                {
                    solrq.append(" ");
                    if(number > 0)
                        scores.append(",");

                    queryEfvs.getOrCreate(condEfv.getEf(), condEfv.getEfv(), true);

                    String efefvId = condEfv.getEfEfvId();
                    String cnt = "cnt_efv_" + efefvId;
                    String pvl = "avgpval_efv_" + efefvId;

                    fields.add(cnt + "_up");
                    fields.add(cnt + "_dn");
                    fields.add(pvl + "_up");
                    fields.add(pvl + "_dn");

                    switch(c.getExpression())
                    {
                        case UP:
                            solrq.append(cnt).append("_up:[1 TO *]");
                            scores.append("product(").append(cnt).append("_up,linear(")
                                    .append(pvl).append("_up,-1,1)),")
                                    .append("product(").append(cnt).append("_dn,linear(")
                                    .append(pvl).append("_dn,1,-1))");
                            break;

                        case DOWN:
                            solrq.append(cnt).append("_dn:[1 TO *]");
                            scores.append("product(").append(cnt).append("_up,linear(")
                                    .append(pvl).append("_up,1,-1)),")
                                    .append("product(").append(cnt).append("_dn,linear(")
                                    .append(pvl).append("_dn,-1,1))");
                            break;

                        case UP_DOWN:
                            solrq.append(cnt).append("_up:[1 TO *] ")
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

                    if(number >= MAX_STRUCTURED_CONDITIONS)
                        break;

                }

            } catch (SolrServerException e) {
                log.error(e);
            }

            solrq.append(")");
        }
        scores.append(")");


        if(number == 0)
            return null;

        if(query.getGene().matches(".*\\S.*"))
            solrq.append(" AND gene_desc:(").append(query.getGene()).append(")");

        if(!query.getSpecies().isEmpty())
        {
            solrq.append(" AND gene_species:(");
            for(String s : query.getSpecies())
            {
                solrq.append(" \"").append(s).append("\"");
            }
            solrq.append(")");
        }

        solrq.append(" AND _val_:\"").append(scores).append("\"");

        fields.add("score");
        fields.add("gene_id");
        fields.add("gene_name");
        fields.add("gene_identifier");
        fields.add("gene_species");

        log.info("Solr query is: " + solrq);

        try {
            AtlasStructuredQueryResult result = new AtlasStructuredQueryResult(queryEfvs,
                    query.getStart(),
                    query.getRows());

            SolrQuery q = new SolrQuery(solrq.toString());

            q.setRows(query.getRows());
            q.setStart(query.getStart());
            q.setSortField("score", SolrQuery.ORDER.desc);
            q.setFields(fields.toArray(new String[fields.size()]));

            q.setFacet(true);

            int max = 0;
            for(EfvTree.Ef<Boolean> s : queryEfvs.getNameSortedTree())
                  if(max < s.getEfvs().size())
                    max = s.getEfvs().size();
            q.setFacetLimit(5 + max);

            q.setFacetMinCount(1);
            q.setFacetSort(true);
            q.addFacetField("gene_keyword");
            q.addFacetField("gene_goterm");
            q.addFacetField("gene_species");
            q.addFacetField("exp_up_ids");
            q.addFacetField("exp_dn_ids");

            Collection<String> efs = getExperimentalFactorOptions();
            for(String ef : efs)
            {
                q.addFacetField("efvs_up_" + ef);
                q.addFacetField("efvs_dn_" + ef);
            }

            QueryResponse response = solr_atlas.query(q);
            SolrDocumentList docs = response.getResults();
            result.setTotal(response.getResults().getNumFound());
            for(SolrDocument doc : docs) {
                AtlasGene gene = new AtlasGene(doc);
                List<AtlasStructuredQueryResult.UpdownCounter> counters
                        = new ArrayList<AtlasStructuredQueryResult.UpdownCounter>();

                for(EfvTree.EfEfv<Boolean> efefv : queryEfvs.getNameSortedList())
                {
                    String efefvId = efefv.getEfEfvId();
                    counters.add(new AtlasStructuredQueryResult.UpdownCounter(
                            nullzero((Integer)doc.getFieldValue("cnt_efv_" + efefvId + "_up")),
                            nullzero((Integer)doc.getFieldValue("cnt_efv_" + efefvId + "_dn")),
                            nullzero((Double)doc.getFieldValue("avgpval_efv_" + efefvId + "_up")),
                            nullzero((Double)doc.getFieldValue("avgpval_efv_" + efefvId + "_dn")),
                            efefv.getEf(),
                            efefv.getEfv()));
                }

                result.addResult(new AtlasStructuredQueryResult.GeneResult(gene, counters));
            }

            log.info("Retrieved query completely: " + result.getSize() + " records of " +
                    result.getTotal() + " total starting from " + result.getStart() );

            EfvTree<AtlasStructuredQueryResult.FacetUpDn> efvFacet = new EfvTree<AtlasStructuredQueryResult.FacetUpDn>();
            for (FacetField ff : response.getFacetFields())
            {
                if(ff.getValueCount() > 1) {
                    if(ff.getName().startsWith("efvs_")) {
                        String ef = ff.getName().substring(8);
                        for (FacetField.Count ffc : ff.getValues())
                        {
                            if(!queryEfvs.has(ef, ffc.getName()))
                            {
                                if(ff.getName().substring(5,7).equals("up"))
                                    efvFacet.getOrCreate(ef, ffc.getName(), new AtlasStructuredQueryResult.FacetUpDn()).addUp((int)ffc.getCount());
                                else
                                    efvFacet.getOrCreate(ef, ffc.getName(), new AtlasStructuredQueryResult.FacetUpDn()).addDown((int)ffc.getCount());
                            }
                        }

                    } else if(ff.getName().startsWith("exp_")) {

                    } else if(ff.getName().startsWith("gene_")) {

                    }
                }
            }

            result.setEfvFacet(efvFacet);
            
            return result;
        } catch (Exception e) {
            log.error(e);
        }

        return null;
    }

    public List<AtlasExperimentRow> getExperiments(String gene_id_key, String factor, String factorValue, String updn)
    {
        final List<AtlasExperimentRow> results = new ArrayList<AtlasExperimentRow>();
        try {
            String sql = "SELECT experiment_id_key, avg(updn_pvaladj) as updn_pvaladj FROM aemart.atlas" +
                    " WHERE gene_id_key = " + gene_id_key.replaceAll("[^0-9]","") +
                    (factor.length() > 0 ? " AND ef = '" + StringEscapeUtils.escapeSql(factor) + "'" : "") +
                    " AND efv = '" + StringEscapeUtils.escapeSql(factorValue) + "'" +
                    " AND updn = " + ("1".equals(updn) ? "1" : "-1") +
                    //" and experiment_id_key NOT IN (211794549,215315583,384555530,411493378,411512559)" +
                    " GROUP BY experiment_id_key";
            log.info(sql);
            theAEQueryRunner.query(sql,
                    new ResultSetHandler() {
                        public Object handle(ResultSet rs) throws SQLException {
                            while(rs.next()) {
                                try {
                                    AtlasExperiment experiment = AtlasDao.getExperimentByIdDw(rs.getString("experiment_id_key"));
                                    if(experiment != null)
                                        results.add(new AtlasExperimentRow(
                                                experiment.getAerExpId(),
                                                experiment.getAerExpName(),
                                                experiment.getAerExpAccession(),
                                                experiment.getAerExpDescription(),
                                                rs.getDouble("updn_pvaladj")));
                                } catch (AtlasObjectNotFoundException e) {
                                    log.error(e);
                                }
                            }
                            return results;
                        }
                    });
         } catch (SQLException e) {
            log.error(e);
         }
        return results;
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
   	 String query = "select distinct experiment_id_key, MIN(atlas.UPDN_PVALADJ) as minp " +
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

    public Set<String> getExperimentalFactorOptions() {
        @SuppressWarnings("unchecked")
        Set<String> names = new TreeSet<String>();
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

            Set<String> ontologyExpansion = new TreeSet<String>();
            StringBuffer s = new StringBuffer(q_expt);

            for (String term : terms.keySet()) {
                HashMap<String,String> termChildren = olsQuery.getTermChildren(term, "EFO", -1, new int[] {1,2,3,4});
                ontologyExpansion.addAll(termChildren.values());
            }

            for (String term : ontologyExpansion) {
                term = "\""  + term + "\"";
                s.append(" exp_factor_values_exact:").append(term);
            }
            
            String expanded_q_expt = s.toString();
            log.info("Expanding experiments query with EFO to: " + expanded_q_expt);

            QueryResponse qr = fullTextQueryExpts(expanded_q_expt);

            if(null != qr) {
                qr.getHeader().add("expanded_efo", expanded_q_expt.replaceAll(" exp_factor_values_exact:", " "));
                qr.getHeader().add("expanded_efo_src", ontologyExpansion);
            }
            return qr;
        } catch (Exception e) {
            log.error("Failed to expand query with EFO, proceeding with normal query", e);
            return fullTextQueryExpts(q_expt);
        }
    }
}
