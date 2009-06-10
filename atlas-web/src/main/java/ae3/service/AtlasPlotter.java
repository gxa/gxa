package ae3.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.lucene.analysis.compound.hyphenation.CharVector;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.util.HtmlUtils;

import ae3.model.AtlasGene;
import ae3.model.AtlasTuple;
import ae3.util.HtmlHelper;

import ds.server.DataServerAPI;
import ds.server.ExpressionDataSet;

public class AtlasPlotter {
    private final Logger log = LoggerFactory.getLogger(getClass());

	private static AtlasPlotter _instance = null;
	private static final String[] altColors= {"#F0FFFF","#F5F5DC"};  
	final java.util.regex.Pattern startsOrEndsWithDigits = java.util.regex.Pattern.compile("^\\d+|\\d+$");
	public static AtlasPlotter instance() {
		if(null == _instance) {
			_instance = new AtlasPlotter();
		}

		return _instance;
	}

	public JSONObject getGeneInExpPlotData(final String geneIdKey, final String expIdKey, final String EF, final String EFV, final String plotType, final String gplotIds) {
        String efToPlot = null;

		if(EF.equals("default")){
            HashMap rankInfo = ArrayExpressSearchService.instance().getHighestRankEF(expIdKey, geneIdKey);

            if (null != rankInfo)
                efToPlot = "ba_" + rankInfo.get("expfactor").toString();
		} else {
            efToPlot = EF;
        }

        if (efToPlot == null)
            return null;
			
        log.debug("Plotting gene {}, experiment {}, factor {}", new Object[] {geneIdKey, expIdKey, efToPlot});

		ArrayList<String> geneNames = getGeneNames(geneIdKey);
		
		ExpressionDataSet ds = DataServerAPI.retrieveExpressionDataSet(geneIdKey, expIdKey, efToPlot);
		
		if(plotType.equals("thumb"))
			return createThumbnailJSON(ds, efToPlot, EFV);
		else if(plotType.equals("big") || plotType.equals("large"))
			return createBigPlotJSON(ds, efToPlot, EFV,geneNames,gplotIds);
		
		ArrayList<String> topFVs = new ArrayList<String>();
		List<AtlasTuple> atlusTuples = AtlasGeneService.getTopFVs(geneIdKey, expIdKey);

        for (AtlasTuple at : atlusTuples) {
            if (at.getEf().equalsIgnoreCase(efToPlot.substring(3)) && !at.getEfv().equals("V1")) {
                topFVs.add(at.getEfv().toLowerCase());
            }
        }
        return createJSON(ds, efToPlot, topFVs);
	}

	private JSONObject createJSON(ExpressionDataSet eds, String EF, ArrayList<String> topFVs){
		JSONObject plotData = new JSONObject();
		try {
			JSONObject series;
			JSONArray seriesList = new JSONArray();
			JSONArray seriesData;
			Set<String> fvs = eds.getFactorValues(EF);
			final Object[] fvs_arr = fvs.toArray();
			Integer[] sortedFVindexes = sortFVs(fvs_arr);
			
			JSONObject meanSeries = new JSONObject();
			JSONArray meanSeriesData = new JSONArray();
			
			HashMap<String, Double>  fvMean_map = new HashMap<String, Double>(); 
			int sampleIndex=1;
			int c=0;
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
		} catch (JSONException e) {
            log.error("Error construction JSON!", e);
		}
		return plotData;
	}

	
	private JSONObject createThumbnailJSON(ExpressionDataSet eds, String EF, String EFV){
		JSONObject plotData = new JSONObject();
		JSONArray seriesList = new JSONArray();
		JSONObject series;
		JSONArray seriesData;
		
		try {
			int sampleIndex=1;
			int startMark=0, endMark=0;
			Set<String> fvs = eds.getFactorValues(EF);
			final Object[] fvs_arr = fvs.toArray();
			Integer[] sortedFVindexes = sortFVs(fvs_arr);
			series = new JSONObject();
			seriesData = new JSONArray();
			for (int i=0; i<fvs_arr.length; i++){
				
				String fv = fvs_arr[sortedFVindexes[i]].toString();
				
				ArrayList[] DEdata = eds.getDataByFV(EF, fv);
				if(fv.equals(EFV))
					startMark = sampleIndex;
				
				for(int j=0; j<DEdata[0].size(); j++){//columns <==> samples with the same FV
					for(int k=0; k<DEdata.length; k++){//rows <==> DEs
						JSONArray point = new JSONArray();
						point.put(sampleIndex);
						point.put(DEdata[k].get(j));// loop over available DEs and add data points to the same x point
						seriesData.put(point);
					}
					sampleIndex++;
				}
				if(fv.equals(EFV))
					endMark = sampleIndex;
			}
			series.put("data", seriesData);
			series.put("lines", new JSONObject("{show:true,lineWidth:2, fill:false}"));
			series.put("legend",new JSONObject("{show:false}"));
			seriesList.put(series);
			plotData.put("series", seriesList);
			plotData.put("startMarkIndex",startMark);
			plotData.put("endMarkIndex",endMark);
			
		} catch (JSONException e) {
			log.error("Error construction JSON!", e);
		}
		
		return plotData;
	}
	
	
	private JSONObject createBigPlotJSON(ExpressionDataSet eds, String EF, String EFV, ArrayList<String> geneNames,final String gplotIds){
		JSONObject plotData = new JSONObject();
		JSONArray seriesList = new JSONArray();
		JSONObject series;
		JSONArray seriesData;
		String markings="";
		String[] genePlotOrder = gplotIds.split(",");

		try {
			int sampleIndex=0;
			int startMark=0, endMark=0;
			Set<String> fvs = eds.getFactorValues(EF);
			final Object[] fvs_arr = fvs.toArray();
			Integer[] sortedFVindexes = sortFVs(fvs_arr);
			series = new JSONObject();
			seriesData = new JSONArray();
			ArrayList<String> deKeys =  eds.getDElist();
			double[][] data = eds.getExpressionMatrix();
			Object[] assay_fvs = eds.getAssayFVs(EF); //fvs ordered by assay_id_key
			Integer[] sortedAssayFVindexes = sortFVs(assay_fvs);//indexes of fvs sorted alphabetically
			for(int j=0; j<data.length; j++){
				double[] deData = data[j];
				series = new JSONObject();
				seriesData = new JSONArray();
				for(int k=0; k<deData.length; k++){
					JSONArray point = new JSONArray();
					point.put(k+0.5);
					point.put(deData[sortedAssayFVindexes[k]]);
					seriesData.put(point);
				}
				series.put("data", seriesData);
				
			
				series.put("lines", new JSONObject("{show:true,lineWidth:2, fill:false, steps:true}"));
				series.put("points", new JSONObject("{show:true,fill:true}"));
				series.put("legend",new JSONObject("{show:true}"));
				series.put("label", geneNames.get(j));
				
				if(genePlotOrder[j] != null)
					series.put("color", Integer.parseInt(genePlotOrder[j]));
				seriesList.put(series);
			}
			
			for (int i=0; i<fvs_arr.length; i++){
				
				String fv = fvs_arr[sortedFVindexes[i]].toString();
				
				ArrayList[] DEdata = eds.getDataByFV(EF, fv);
				startMark = sampleIndex;
				endMark = DEdata[0].size()+startMark;
				sampleIndex=endMark;
				if(fv.equals(""))fv="unannotated";
				markings= markings.equals("") ? markings : markings+",";
				markings+= "{xaxis:{from: "+startMark+", to: "+endMark+"},label:\""+fv.replaceAll("'", "").replaceAll(",", "")+"\" ,color: '"+altColors[i%2]+"' }";
				
			}

			plotData.put("series", seriesList);
			plotData.put("markings",markings);
			
			///////////////////////////////////////////////////////////////
			
			plotData.put("sAttrs", getSampleAttributes(eds,EF));
			plotData.put("assay2samples", getSampleAssayMap(eds,EF));
			
			JSONStringer sampleChars = new JSONStringer();
			JSONStringer sampleCharValues = new JSONStringer();
			sampleChars.array();
			sampleCharValues.object();
			for(String sampChar: eds.getSampleCharacteristics()){
				sampleChars.value(sampChar.substring(3));
				sampleCharValues.key(sampChar.substring(3));
				sampleCharValues.array();
				TreeSet charValuesSet = new TreeSet(Arrays.asList(eds.getSampleCharValues(sampChar)));
			
				for(Object charValue: charValuesSet){
					sampleCharValues.value(charValue.toString());
				}
				sampleCharValues.endArray();
			}
			sampleCharValues.endObject();
			sampleChars.endArray();
			plotData.put("characteristics", sampleChars);
			plotData.put("charValues", sampleCharValues);
			plotData.put("currEF", EF.substring(3));
			plotData.put("ADid", eds.getArraydesign_id());
			plotData.put("geneNames", getJSONarray(geneNames));
			plotData.put("DEids", getJSONarray(eds.getDElist()));
			plotData.put("GNids", getJSONarray(eds.getGNids()));
			
			
			
		} catch (JSONException e) {
			log.error("Error construction JSON!", e);
		}
		
		return plotData;
	}
	
	private JSONStringer getSampleAttributes(ExpressionDataSet eds, String EF) throws JSONException{
		JSONStringer sampleAttrs = new JSONStringer();
		int[] sample_ids = eds.getSampleList();
		Set<String> characteristics = eds.getSampleCharacteristics();
		sampleAttrs.object();
		for(int i=0; i<sample_ids.length; i++){
			int sample_key = sample_ids[i];
			sampleAttrs.key("s"+Integer.toString(sample_key));
			sampleAttrs.object();
			for(String scar:characteristics){
				String charValue = eds.getSampleCharValues(scar)[i].toString();
				
				sampleAttrs.key(scar.substring(3)).value(charValue);
			}
			sampleAttrs.endObject();
		}
		sampleAttrs.endObject();
		return sampleAttrs;
	}
	
	private JSONStringer getSampleAssayMap(ExpressionDataSet eds, String EF) throws JSONException{
		JSONStringer map = new JSONStringer();
		int[] assay_ids = eds.getAssayList();
		Object[] fvs = eds.getAssayFVs(EF); //fvs ordered by assay_id_key
		Integer[] sortedFVindexes = sortFVs(fvs);//indexes of fvs sorted alphabetically
		TreeMap<Integer, ArrayList<Integer>> assays2samples = eds.getAssays2Samples();
		
		map.array();
		for(int i=0; i<fvs.length; i++){
			int assay_id = assay_ids[sortedFVindexes[i]];
			ArrayList<Integer> sample_ids = assays2samples.get(assay_id);
			map.array();
			for(Integer sample:sample_ids){
				map.value("s"+sample);
			}
			map.endArray();
		}
		map.endArray();
		return map;
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
	
	private ArrayList<String> getGeneNames(String gids){
		ArrayList<String> geneNames = new ArrayList<String>();
		String[] ids = gids.split(",");
		for(String gid:ids){
			AtlasGene gene = AtlasGeneService.getAtlasGene(gid);
			geneNames.add(gene.getGeneName());
		}
		return geneNames;
	}
	
	private JSONStringer getJSONarray(ArrayList<String> geneNames) throws JSONException{
		JSONStringer JSONarray = new JSONStringer();
		JSONarray.array();
		for(String gene:geneNames){
			JSONarray.value(gene);
		}
		JSONarray.endArray();
		return JSONarray;
	}
	
	
}
