package ae3.service;

import ae3.dao.AtlasDao;
import ae3.dao.AtlasObjectNotFoundException;
import ae3.dao.MultipleGeneException;
import ae3.model.AtlasGene;
import ae3.model.AtlasTuple;
import org.apache.solr.common.SolrDocumentList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

public class AtlasGeneService {
	protected static final Logger log = LoggerFactory.getLogger(AtlasGeneService.class);
	private static final String omittedEFs = "age,individual,time,dose,V1";

	/**
	 * Fetches Atlas gene document from Atlas index
	 * @param gene_id_key
	 * @return
	 */
	public static AtlasGene getAtlasGene(String gene_id_key){
		AtlasGene atlasGene;

		try {
			atlasGene = AtlasDao.getGeneByIdentifier(gene_id_key);
			retrieveOrthoGenes(atlasGene);
		} catch (AtlasObjectNotFoundException e) {
			log.error("Failed to get gene with id " + gene_id_key, e);
			return null;
		}
		catch (MultipleGeneException em) {
			log.error("More than one gene was found for id " + gene_id_key, em);
			return null;
		}
		return atlasGene;
	}

	/**
	 * Retrieves genes from index corresponding to the list of ortholog ids for the specified AtlasGene. Retrieved genes are added to the list of orthoGenes of the specified AtlasGene.
	 * @param atlasGene
	 * @throws AtlasObjectNotFoundException
	 * @throws MultipleGeneException
	 */
	public static void retrieveOrthoGenes(AtlasGene atlasGene) throws AtlasObjectNotFoundException, MultipleGeneException{
		AtlasGene orthoGene;
		ArrayList<String> orthoGenes = atlasGene.getOrthologs();
		if(orthoGenes != null){
			for (String orth: orthoGenes){
				orthoGene = AtlasDao.getGeneByIdentifier(orth);
				if(orthoGene != null)
					atlasGene.addOrthoGene(orthoGene);
			}
		}
	}

	/**
	 * Checks if the identifier specified hits multiple gene entries in the index
	 * @param gene_identifier
	 * @return boolean
	 */
	public static boolean hitMultiGene(String gene_identifier){
		boolean multi=false;
		try {
			SolrDocumentList documentList = AtlasDao.getDocListForGene(gene_identifier);
			multi = documentList.size()>1;
		} catch (AtlasObjectNotFoundException e) {
            log.error("Failed to get gene with identifier " + gene_identifier, e);
		}
		return multi;
	}

	/**
	 * Fetches atlas results for a gene from atlas index (used for populating heatmap)
	 * @param gene_id_key
	 * @return ArrayList<HeatmapRow>
	 */
	public static ArrayList<HeatmapRow> getHeatMapRows(String gene_id_key) {
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

		atlasTuples = ArrayExpressSearchService.instance().getAtlasResults(query);
		return atlasTuples;

	}
}
