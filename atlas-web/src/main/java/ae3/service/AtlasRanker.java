package ae3.service;

import java.sql.ResultSet;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ae3.util.DBhandler;



public class AtlasRanker {
	protected final Log log = LogFactory.getLog(getClass());
	public AtlasRanker() {
		// TODO Auto-generated constructor stub
	}
	
	public HashMap getHighestRankEF(String expIdKey, String geneIdKey){
//		String rank_query = "select /*+INDEX(atlas atlas_de)*/ nvl(atlas.fpvaladj,999.0) as rank, atlas.ef as expfactor " +
//        					"from aemart.atlas atlas, aemart.ae2__designelement__main gene, aemart.ae1__experiment__main exp " +
//        					"where atlas.designelement_id_key=gene.designelement_id_key " +
//        					"and gene.gene_identifier = '"+geneAccession+"' "+
//        					"and exp.experiment_id_key = atlas.experiment_id_key "+
//        					"and exp.experiment_identifier = '"+expAccession+"' "+
//        					
//        					"order by rank";
		
		String rank_query = "select nvl(atlas.fpvaladj,999.0) as rank, atlas.ef as expfactor " +
							"from aemart.atlas atlas " +
							"where atlas.experiment_id_key = "+expIdKey +
							" and gene_id_key = "+ geneIdKey +
							" order by rank";
		HashMap rankInfo = new HashMap(); 
		try {
			Connection connection = DBhandler.instance().getAE_ORAConnection();
			Statement stmt = connection.createStatement();
			ResultSet rset = stmt.executeQuery (rank_query);
			Double smallest_rank=null;
			String smallest_rank_expfactor="";
			if(rset.next()){
				smallest_rank = rset.getDouble ("rank");
		        smallest_rank_expfactor = rset.getString ("expfactor");
			}
			
			rankInfo.put("expfactor", smallest_rank_expfactor);
			rankInfo.put("rank", smallest_rank);
			connection.close();
			
		} catch (SQLException e) {
			
			log.error(e);
		}
		return rankInfo;
		
	}

}
