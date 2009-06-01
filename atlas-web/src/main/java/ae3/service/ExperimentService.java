package ae3.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.xml.bind.v2.runtime.unmarshaller.XsiNilLoader.Array;

import ae3.dao.AtlasDao;
import ae3.dao.AtlasObjectNotFoundException;
import ae3.model.AtlasExperiment;
import ae3.service.structuredquery.AtlasStructuredQuery;
import ae3.service.structuredquery.AtlasStructuredQueryResult;
import ae3.service.structuredquery.ExpFactorQueryCondition;
import ae3.service.structuredquery.GeneQueryCondition;
import ae3.service.structuredquery.QueryExpression;
import ae3.util.AtlasProperties;


public class ExperimentService {
	protected static final Logger log = LoggerFactory.getLogger(ExperimentService.class);
	public static AtlasExperiment getAtlasExperiment(String accession){
		AtlasExperiment experiment;
		
		try {
			experiment = AtlasDao.getExperimentByAccession(accession);
		} catch (AtlasObjectNotFoundException e) {
			log.error("Failed to get experiment with accession " + accession, e);
			return null;
		}
		return experiment;
	}
		
	public static void getBioSamples(String experiment_id){
		
	}
	
	public static AtlasStructuredQueryResult getGenesForExperiment(ArrayList<String> geneIds, String eAcc, int start){
		AtlasStructuredQuery query = new AtlasStructuredQuery();
		if(!geneIds.isEmpty()){
			GeneQueryCondition geneQuery = new GeneQueryCondition();
			geneQuery.setFactor("");
			geneQuery.setFactorValues(geneIds);
			List<GeneQueryCondition> geneQueries = new ArrayList<GeneQueryCondition>();
			geneQueries.add(geneQuery);
			query.setGeneConditions(geneQueries);
		}
		
		
		ExpFactorQueryCondition condition = new ExpFactorQueryCondition();
		condition.setExpression(QueryExpression.UP_DOWN);
		condition.setFactor("experiment");
		ArrayList<String> fvalues = new ArrayList<String>();
		fvalues.add(eAcc);
		condition.setFactorValues(fvalues);
		List<ExpFactorQueryCondition> conditions = new ArrayList<ExpFactorQueryCondition>();
		conditions.add(condition);
		
		query.setConditions(conditions);
		query.setView("list");
		query.setRowsPerPage(AtlasProperties.getIntProperty("atlas.query.listsize"));
		query.setStart(start);
		query.setSpecies(new ArrayList<String>());
		query.setExpandColumns(new HashSet<String>());
		AtlasStructuredQueryResult atlasResult = ArrayExpressSearchService.instance().getStructQueryService().doStructuredAtlasQuery(query);
		return atlasResult;
	}

}
