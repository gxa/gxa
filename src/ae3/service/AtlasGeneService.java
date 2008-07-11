package ae3.service;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.dbutils.ResultSetHandler;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.solr.client.solrj.response.QueryResponse;

import ae3.dao.AtlasDao;
import ae3.dao.AtlasObjectNotFoundException;
import ae3.model.AtlasExperiment;
import ae3.model.AtlasGene;
import ae3.model.AtlasTuple;

public class AtlasGeneService {
//	protected static final Log log = LogFactory.getLog(getClass());

//	public static AtlasGene getGeneInfo(String gene_id_key){

//	}

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

	public static List<AtlasTuple> getAtlasResult(String gene_id_key, String exp_id_key){
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
