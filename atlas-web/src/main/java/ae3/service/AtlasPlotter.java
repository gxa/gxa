package ae3.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.core.io.DescriptiveResource;

import ae3.model.AtlasTuple;

import ds.server.DataServerAPI;
import ds.server.ExpressionDataSet;



public class AtlasPlotter {
	private AtlasPlotter() {};
	private static AtlasPlotter _instance = null;
	private static final String[] altColors= {"#D8D8D8","#F2F2F2"};  
	final java.util.regex.Pattern startsOrEndsWithDigits = java.util.regex.Pattern.compile("^\\d+|\\d+$");
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
		ArrayList<String> topFVs = new ArrayList<String>();
		List<AtlasTuple> atlusTuples = AtlasGeneService.getTopFVs(geneIdKey, expIdKey);
		for(int i=0; i<atlusTuples.size(); i++){

			AtlasTuple at = atlusTuples.get(i);
			if(at.getEf().equalsIgnoreCase(EF.substring(3)) && !at.getEfv().equals("V1")){
				topFVs.add(at.getEfv().toLowerCase());
			}
		}
		
		ExpressionDataSet ds = DataServerAPI.retrieveExpressionDataSet(geneIdKey, expIdKey, EF);
//		double[][] data = ds.getSortedExpressionMatrix().get("ba_"+EF);
		JSONObject jsonString = createJSON(ds,EF,geneIdKey, expIdKey, topFVs);
		return jsonString;
	}

	private JSONObject createJSON(ExpressionDataSet eds, String EF, String gid, String eid, ArrayList<String> topFVs){
		JSONObject plotData = new JSONObject();
		try {
			JSONObject series = new JSONObject();
			JSONArray seriesList = new JSONArray();
			JSONArray seriesData = new JSONArray();
			Set<String> fvs = eds.getFactorValues(EF);
			final Object[] fvs_arr = fvs.toArray();
			Integer[] sortedFVindexes = sortFVs(fvs_arr);
			
			JSONObject meanSeries = new JSONObject();
			JSONArray meanSeriesData = new JSONArray();
			
			HashMap<String, Double>  fvMean_map = new HashMap<String, Double>(); 
			int sampleIndex=1;
			int c=0;
			boolean unDiffPresent = false;
			for (int i=0; i<fvs_arr.length; i++){
				
				String fv = fvs_arr[sortedFVindexes[i]].toString();
				
				ArrayList[] DEdata = eds.getDataByFV(EF, fv);
				series = new JSONObject();
				seriesData = new JSONArray();
				for(int j=0; j<DEdata[0].size(); j++){//columns <==> samples with the same FV
					for(int k=0; k<DEdata.length; k++){//rows <==> DEs
						JSONArray point = new JSONArray();
						point.put(sampleIndex);
						point.put(DEdata[k].get(j));// loop over available DEs and add data points to the same x point
						seriesData.put(point);
						
						
						if(!fvMean_map.containsKey(fv+"_de"))
							fvMean_map.put(fv+"_de",getMean(DEdata[k]));
						double fvMean = fvMean_map.get(fv+"_de");
						point = new JSONArray();
						point.put(sampleIndex);
						point.put(fvMean);
						meanSeriesData.put(point);
						
					}
					sampleIndex++;
				}
				series.put("data", seriesData);
				series.put("bars", new JSONObject("{show:true, align: \"center\", fill:true}"));
				series.put("lines", new JSONObject("{show:false,lineWidth:2, fill:true}"));
				series.put("points", new JSONObject("{show:false,radius:1}"));
				series.put("label", fv);
				series.put("legend",new JSONObject("{show:true}"));
				
				//Choose series color
				if(!topFVs.contains(fv.toLowerCase())){
					series.put("color", altColors[c%2]);
					series.put("legend",new JSONObject("{show:false}"));
					c++;
				}
				
//				if(EF.equals("ba_time")){
//					series.put("bars", new JSONObject("{show:false, align: \"center\", fill:true}"));
//					series.put("lines", new JSONObject("{show:true,lineWidth:2, fill:false}"));
//				}
				

				seriesList.put(series);
			}
			//Create mean series
			
			meanSeries.put("data", meanSeriesData);
			meanSeries.put("lines", new JSONObject("{show:true,lineWidth:1.0}"));
			meanSeries.put("points", new JSONObject("{show:false}"));
			meanSeries.put("color", "#5e5e5e");
			meanSeries.put("label", "Mean");
			meanSeries.put("legend",new JSONObject("{show:false}"));
			meanSeries.put("hoverable", "false");
			meanSeries.put("shadowSize","1");
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

	
	private Integer[] sortFVs(final Object[] fvs){
		
		Integer[] fso = new Integer[fvs.length];
        for (int i = 0; i < fvs.length; i++) {
          fso[i] = i;
        }
        
        Arrays.sort(fso,
                new Comparator() {

					public int compare(Object o1, Object o2) {
						String s1 = fvs[((Integer) o1).intValue()].toString();
		                  String s2 = fvs[((Integer) o2).intValue()].toString();
		                  
		                  // want to make sure that empty strings are pushed to the back
		                  if (s1.equals("") && s2.equals("")) return 0;
		                  if (s1.equals("") && !s2.equals("")) return 1;
		                  if (!s1.equals("") && s2.equals("")) return -1;
		                  
		                  java.util.regex.Matcher m1 = startsOrEndsWithDigits.matcher(s1);
		                  java.util.regex.Matcher m2 = startsOrEndsWithDigits.matcher(s2);
		                  
		                  if (m1.find() && m2.find()) {
		                      Long i1 = new Long(s1.substring(m1.start(), m1.end()));
		                      Long i2 = new Long(s2.substring(m2.start(), m2.end()));
		                      
		                      if (i1.compareTo(i2) == 0)
		                          return s1.compareToIgnoreCase(s2);
		                      else
		                          return i1.compareTo(i2);
		                  }
		                  
		                  return s1.compareToIgnoreCase(s2);
					}
        	
        });
        return fso;
	}
	
	private double getMean(ArrayList<Double> test ){
		double sum=0.0;
		for(int i=0; i<test.size(); i++){
			sum+= test.get(i);
		}
		return sum/test.size();

	}

	

}
