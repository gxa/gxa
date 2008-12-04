package ae3.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.core.io.DescriptiveResource;

import ds.server.DataServerAPI;
import ds.server.ExpressionDataSet;



public class AtlasPlotter {
	private AtlasPlotter() {};
	private static AtlasPlotter _instance = null;
	public static AtlasPlotter instance() {
		if(null == _instance) {
			_instance = new AtlasPlotter();
		}

		return _instance;
	}

	public JSONObject getGeneInExpPlotData(String geneIdKey, String expIdKey, String EF) throws Exception{

		AtlasRanker ranker = new AtlasRanker();
		if(EF.equals("default")){
			HashMap rankInfo =  ranker.getHighestRankEF(expIdKey, geneIdKey);
			EF = "ba_"+rankInfo.get("expfactor").toString();
		}
			
		System.out.println(EF);
		ExpressionDataSet ds = DataServerAPI.retrieveExpressionDataSet(geneIdKey, expIdKey, EF);
//		double[][] data = ds.getSortedExpressionMatrix().get("ba_"+EF);
		JSONObject jsonString = createJSON(ds,EF,geneIdKey, expIdKey);
		return jsonString;
	}

	private JSONObject createJSON(ExpressionDataSet eds, String EF, String gid, String eid){
		JSONObject plotData = new JSONObject();
		try {
			JSONObject series = new JSONObject();
			JSONArray seriesList = new JSONArray();
			JSONArray seriesData = new JSONArray();
			Set<String> fvs = eds.getFactorValues(EF);
			
			JSONObject meanSeries = new JSONObject();
			JSONArray meanSeriesData = new JSONArray();
			
			HashMap<String, Double>  fvMean_map = new HashMap<String, Double>(); 
			int sampleIndex=1;
			for (String fv: fvs){
				ArrayList[] DEdata = eds.getDataByFV(EF, fv);
				series = new JSONObject();
				seriesData = new JSONArray();
				for(int i=0; i<DEdata[0].size(); i++){//columns <==> samples with the same FV
					for(int k=0; k<DEdata.length; k++){//rows <==> DEs
						JSONArray point = new JSONArray();
						point.put(sampleIndex);
						point.put(DEdata[k].get(i));// loop over available DEs and add data points to the same x point
						seriesData.put(point);
						
						
						if(!fvMean_map.containsKey(fv+"_de"+k))
							fvMean_map.put(fv+"_de"+k,getMean(DEdata[k]));
						double fvMean = fvMean_map.get(fv+"_de"+k);
						point = new JSONArray();
						point.put(sampleIndex);
						point.put(fvMean);
						meanSeriesData.put(point);
						
					}
					sampleIndex++;
				}
				series.put("data", seriesData);
				series.put("bars", new JSONObject("{show:true, align: \"center\", fill:true}"));
				series.put("lines", new JSONObject("{show:false,lineWidth:2, fill:false}"));
				series.put("points", new JSONObject("{show:false,radius:1}"));
				series.put("label", fv);
//				series.put("color", "#1f1f1f");
				seriesList.put(series);
			}
			//Create mean series
			
			meanSeries.put("data", meanSeriesData);
			meanSeries.put("lines", new JSONObject("{show:true,lineWidth:1.0}"));
			meanSeries.put("points", new JSONObject("{show:false}"));
			meanSeries.put("color", "#1f1f1f");
			meanSeries.put("label", "Mean");
			seriesList.put(meanSeries);
			
			
			plotData.put("series", seriesList);
			int noOfCols = (fvs.size()>=5)? 2: fvs.size();
//			JSONObject options = new JSONObject("{ xaxis:{ticks:0}, " +
//												  " legend:{ position:\"sw\", container: \"#"+gid+"_"+eid+"_legend\", extContainer: \"#"+gid+"_"+eid+"_legend_ext\"  }," +
//												  "	grid:{ backgroundColor: '#fafafa',	autoHighlight: true, hoverable: true		}," +
//												  "	bars:{fill:0.7}," +
//												  " selection: { mode: \"x\" } }");
//			JSONObject legend = new JSONObject();
////			legend.put("container", "#"+gid+"_"+eid+"_legend");
////			options.put("legend", legend);
//			plotData.put("options", options);
			
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return plotData;
	}

	
	private double getMean(ArrayList<Double> test ){
		double sum=0.0;
		for(int i=0; i<test.size(); i++){
			sum+= test.get(i);
		}
		return sum/test.size();

	}

	

}
