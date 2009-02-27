package ae3.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.solr.client.solrj.response.QueryResponse;

import ae3.dao.AtlasDao;
import ae3.dao.AtlasObjectNotFoundException;
import ae3.model.AtlasGene;
import ae3.model.AtlasTuple;

public class AtlasGeneService {
	protected static final Log log = LogFactory.getLog(AtlasGeneService.class);
	private static final String omittedEFs = "age,individual,time,dose,V1";
	
	/**
	 * Fetches atlas results for a gene from atlas table in the database
	 * @param gene_id_key
	 * @return AtlasResultSet
	 */
	public static AtlasResultSet getExprSummary(String gene_id_key){
		AtlasResultSet atlasResultSet = null;
		try {
			QueryResponse queryGeneResponse = ArrayExpressSearchService.instance().fullTextQueryGenes("gene_id:" + gene_id_key);
			
			atlasResultSet = ArrayExpressSearchService.instance().doAtlasQuery(queryGeneResponse, null, "", "");

		} catch (IOException e) {
//			log.error(e);
		}
		return atlasResultSet;
	}

	/**
	 * Fetches Atlas gene document from Atlas index
	 * @param gene_id_key
	 * @return
	 */
	public static AtlasGene getAtlasGene(String gene_id_key){
		
		AtlasGene atlasGene;
		try {
			atlasGene = AtlasDao.getGeneByIdentifier(gene_id_key);
		} catch (AtlasObjectNotFoundException e) {
			log.error(e);
			return null;
		}
		return atlasGene;
	}
	
	/**
	 * Fetches atlas results for a gene from atlas index (used for populating heatmap)
	 * @param gene_id_key
	 * @return ArrayList<HeatmapRow>
	 */
	public static ArrayList<HeatmapRow> getHeatMapRows(String gene_id_key){
		
		HeatmapRow heatmapRow;
		ArrayList<HeatmapRow> heatmap = new ArrayList<HeatmapRow>();
		AtlasGene atlasGene =  getAtlasGene(gene_id_key);
		for(String ef : getEFs(atlasGene)) {
           HashSet<Object> efvs = atlasGene.getAllFactorValues(ef);
           if(!efvs.isEmpty()){
        	   for(Object efv : efvs) {
        		   if(!omittedEFs.contains(efv.toString()) && !omittedEFs.contains(ef)){
        		   heatmapRow = new HeatmapRow(efv.toString(),
        				   					   ef,
        				   					   atlasGene.getCount_up(ef, efv.toString()),
        				   					   atlasGene.getCount_dn(ef, efv.toString()),
        				   					   atlasGene.getAvg_up(ef, efv.toString()),
        				   					   atlasGene.getAvg_dn(ef, efv.toString()));
        		   heatmap.add(heatmapRow);
        		   }
        	   }
           }
		}
		Collections.sort(heatmap,Collections.reverseOrder());
		return heatmap;
	}
	
	private  static HashSet<String> getEFs(AtlasGene atlasGene){
    	HashSet<String> efs = new HashSet<String>();
    	for (String field:atlasGene.getGeneSolrDocument().getFieldNames()){
    		
    		if(field.startsWith("efvs_"))
    			efs.add(field.substring(8));
    	}
    	return efs;
    }
	
	/**
	 * Function used by AtlasPlotter to retrieve topFVs for a gene in an experiment
	 * @param gene_id_key
	 * @param exp_id_key
	 * @return
	 */
	public static List<AtlasTuple> getTopFVs(String gene_id_key, String exp_id_key){
		String updn_filter = " and updn <> 0\n";
		String efvFilter ="";

		ArrayList<AtlasTuple> atlasTuples = new ArrayList<AtlasTuple>();

		String query = 
			"select * from( " +
			"select atlas.EF, atlas.EFV, atlas.UPDN, atlas.UPDN_PVALADJ, atlas.UPDN_TSTAT, " +
			"row_number() OVER( " +
			"					PARTITION BY atlas.EXPERIMENT_ID_KEY, atlas.GENE_ID_KEY, atlas.ef, atlas.EFV " +
			"					ORDER BY atlas.updn_pvaladj asc, UPDN_TSTAT desc " +
			"				  ) TopN " +
			"from ATLAS " +
			"where gene_id_key = "+gene_id_key+ 
			" and experiment_id_key = "+exp_id_key+") "+
			"order by updn_pvaladj asc ";
//		log.info(atlas_query_topN);

		atlasTuples = ArrayExpressSearchService.instance().getAtlasResults(query);
		return atlasTuples;

	}
}
