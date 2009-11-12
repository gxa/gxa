package ae3.service;

import ae3.dao.AtlasDao;
import ae3.model.AtlasGene;
import ae3.model.AtlasTuple;
import org.apache.commons.lang.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.gxa.netcdf.reader.NetCDFProxy;
import uk.ac.ebi.gxa.web.AtlasSearchService;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;

public class AtlasPlotter {
    private AtlasSearchService atlasSearchService;
    private File atlasNetCDFRepo;

    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final String[] altColors = {"#D8D8D8", "#E8E8E8"};
    private static final String[] markingColors = {"#F0FFFF", "#F5F5DC"};
    private static final Pattern startsOrEndsWithDigits = java.util.regex.Pattern.compile("^\\d+|\\d+$");

    public AtlasSearchService getAtlasSearchService() {
        return atlasSearchService;
    }

    public void setAtlasSearchService(AtlasSearchService atlasSearchService) {
        this.atlasSearchService = atlasSearchService;
    }

    public File getAtlasNetCDFRepo() {
        return atlasNetCDFRepo;
    }

    public void setAtlasNetCDFRepo(File atlasNetCDFRepo) {
        this.atlasNetCDFRepo = atlasNetCDFRepo;
    }

    public JSONObject getGeneInExpPlotData(final String geneIdKey,
                                           final String experimentID,
                                           final String ef,
                                           final String efv,
                                           final String plotType,
                                           final String gplotIds) {
        String efToPlot;

        AtlasDao dao = atlasSearchService.getAtlasSolrDAO();
        AtlasGene atlasGene = dao.getGeneById(StringUtils.split(geneIdKey, ",")[0]).getGene();

        // if ef is "default" fetch highest ranked EF using SOLR index
        if (ef.equals("default")) {
            efToPlot = atlasGene.getHighestRankEF(Long.valueOf(experimentID)).getFirst();
        }
        else {
            efToPlot = ef;
        }

        if (efToPlot == null) {
            return null;
        }

        log.debug("Plotting gene {}, experiment {}, factor {}", new Object[]{geneIdKey, experimentID, efToPlot});

        // lookup gene names, again using SOLR index
        ArrayList<String> geneNames = getGeneNames(geneIdKey);

        // lookup NetCDFFiles for this experiment
        File[] netCDFs = atlasNetCDFRepo.listFiles(new FilenameFilter() {

            public boolean accept(File file, String name) {
                return name.matches("^" + experimentID + "_[0-9]+(_ratios)?\\.nc$");
            }
        });

        // create NetCDFProxies for each of these files, to find the gene we want
        List<NetCDFProxy> proxies = new ArrayList<NetCDFProxy>();
        for (File netCDF : netCDFs) {
            proxies.add(new NetCDFProxy(netCDF));
        }

        // iterate over our proxies to find the one that contains the gene we're after
        try {
            // this is the NetCDF containing the gene we care about
            NetCDFProxy proxy = null;
            // this is a list of the indices marking the positions of design elements that correspond to the gene we're after
            List<Integer> geneIndices = new ArrayList<Integer>();
            for (NetCDFProxy next : proxies) {
                boolean found = false;
                int geneIndex = 0;
                for (int geneID : next.getGenes()) {
                    if (geneID == Integer.parseInt(geneIdKey)) {
                        proxy = next;
                        found = true;
                        geneIndices.add(geneIndex);
                        geneIndex++;
                    }
                }

                if (found) {
                    // fixme: breaking assumes that the gene will only be found in one NetCDF for this experiment
                    break;
                }
            }

            // make sure proxy isn't null - if it is, break
            if (proxy != null) {
                // and build up the plot, based on plotType parameter
                if (plotType.equals("thumb")) {
                    return createThumbnailJSON(proxy, efToPlot, efv, geneIndices);
                }
                else if (plotType.equals("big") || plotType.equals("large")) {
                    return createBigPlotJSON(proxy, efToPlot, geneNames, gplotIds, geneIndices);
                }
                else {
                    ArrayList<String> topFVs = new ArrayList<String>();

                    List<AtlasTuple> atlasTuples = atlasGene.getTopFVs(Long.valueOf(experimentID));

                    for (AtlasTuple at : atlasTuples) {
                        if (at.getEf().equalsIgnoreCase(efToPlot.substring(3)) && !at.getEfv().equals("V1")) {
                            topFVs.add(at.getEfv().toLowerCase());
                        }
                    }
                    return createJSON(proxy, efToPlot, topFVs, geneIndices);
                }

            }
            else {
                return null;
            }
        }
        catch (IOException e) {
            log.error("IOException whilst trying to read from NetCDF at " + atlasNetCDFRepo.getAbsolutePath() +
                    " for " + experimentID);
            return null;
        }
    }

    private JSONObject createJSON(NetCDFProxy netCDF, String ef, List<String> topFVs, List<Integer> geneIndices) throws IOException {
        // the data for our plot
        JSONObject plotData = new JSONObject();
        try {
            // whats this for?
            JSONArray seriesList = new JSONArray();
            JSONArray seriesData = new JSONArray();

            // whats this for?
            JSONObject meanSeries = new JSONObject();
            JSONArray meanSeriesData = new JSONArray();

            // get factor values, ordered by assay
            String[] fvs = netCDF.getFactorValues(ef);

            // sort the factor values into a good order
            Integer[] sortedFVindexes = sortFVs(fvs);

//            HashMap<String, Double> fvMean_map = new HashMap<String, Double>();
//            int sampleIndex = 1;
            int colorCounter = 0;
            boolean inSigSeriesPresent = false;
            for (int i = 0; i < fvs.length; i++) {
                // work out which assay we're going to do first
                int assayIndex = sortedFVindexes[i];

                // get the factor value associated with this assay
                String fv = fvs[assayIndex];

                // create the series JSON object
                JSONObject series = new JSONObject();

                // now, fetch the data that corresponds to this assay, ordered by design element
                double[] data = netCDF.getExpressionDataForAssay(assayIndex);

                // cherry pick out the data at each geneIndex point (genes and design elements are indexed the same)
                int sampleIndex = 0;
                for (int geneIndex : geneIndices) {
                    double datapoint = data[geneIndex];

                    // create a new point
                    JSONArray point = new JSONArray();
                    point.put(i+1);
                    point.put(datapoint <= -1000000 ? null : datapoint);

                    // add this point to the series data
                    seriesData.put(point);

                    // increment the sample index

                }

                // iterate over the data and fetch individual data points
                for (double datapoint : data) {
                }

//                // loop over assays
//                for (int j = 0; j < DEdata[0].size(); j++) {//columns <==> samples with the same FV
//                    // loop over design elements
//                    for (int k = 0; k < DEdata.length; k++) {//rows <==> DEs
//                        JSONArray point = new JSONArray();
//                        point.put(sampleIndex);
//                        Double v = (Double) DEdata[k].get(j);
//                        point.put(v <= -1000000 ? null :
//                                v);// loop over available DEs and add data points to the same x point
//                        seriesData.put(point);
//
//
//                        if (!fvMean_map.containsKey(fv + "_de"))
//                            fvMean_map.put(fv + "_de", getMean(DEdata[k]));
//                        double fvMean = fvMean_map.get(fv + "_de");
//                        point = new JSONArray();
//                        point.put(sampleIndex);
//                        point.put(fvMean);
//                        meanSeriesData.put(point);
//
//                    }
//                    sampleIndex++;
//                }

                series.put("data", seriesData);
                series.put("bars", new JSONObject("{show:true, align: \"center\", fill:true}"));
                series.put("lines", new JSONObject("{show:false}"));
                series.put("points", new JSONObject("{show:false}"));
                series.put("label", fv);
                series.put("legend", new JSONObject("{show:true}"));

                //Choose series color
                if (!topFVs.contains(fv.toLowerCase())) {
                    series.put("color", altColors[colorCounter % 2]);
                    series.put("legend", new JSONObject("{show:false}"));
                    colorCounter++;
                    inSigSeriesPresent = true;
                }

                seriesList.put(series);
            }

            // create mean series
            meanSeries.put("data", meanSeriesData);
            meanSeries.put("lines", new JSONObject("{show:true,lineWidth:1.0,fill:false}"));
            meanSeries.put("bars", new JSONObject("{show:false}"));
            meanSeries.put("points", new JSONObject("{show:false}"));
            meanSeries.put("color", "#5e5e5e");
            meanSeries.put("label", "Mean");
            meanSeries.put("legend", new JSONObject("{show:false}"));
            meanSeries.put("hoverable", "false");
            meanSeries.put("shadowSize", 2);
            seriesList.put(meanSeries);


            plotData.put("series", seriesList);
            plotData.put("insigLegend", inSigSeriesPresent);
        }
        catch (JSONException e) {
            log.error("Error construction JSON!", e);
        }
        return plotData;
    }


    private JSONObject createThumbnailJSON(NetCDFProxy netCDF, String ef, String efv, List<Integer> geneIndices) throws IOException {
        JSONObject plotData = new JSONObject();
        JSONArray seriesList = new JSONArray();
        JSONObject series;
        JSONArray seriesData;

        try {
            int sampleIndex = 1;
            int startMark = 0, endMark = 0;
//            Set<String> fvs = getFactorValues(netCDF, ef);
//            final Object[] fvs_arr = fvs.toArray();
//            Integer[] sortedFVindexes = sortFVs(fvs_arr);
            String[] fvs = netCDF.getFactorValues(ef);
            Integer[] sortedFVindexes = sortFVs(fvs);
            series = new JSONObject();
            seriesData = new JSONArray();
            for (int i = 0; i < fvs.length; i++) {
                String fv = fvs[sortedFVindexes[i]];

//                ArrayList[] DEdata = eds.getDataByFV(ef, fv);
//                if (fv.equals(efv))
//                    startMark = sampleIndex;
//
//                for (int j = 0; j < DEdata[0].size(); j++) {//columns <==> samples with the same FV
//                    for (int k = 0; k < DEdata.length; k++) {//rows <==> DEs
//                        JSONArray point = new JSONArray();
//                        point.put(sampleIndex);
//                        Double v = (Double) DEdata[k].get(j);
//                        point.put(v <= -1000000 ? null :
//                                v);// loop over available DEs and add data points to the same x point
//                        seriesData.put(point);
//                    }
//                    sampleIndex++;
//                }
//                if (fv.equals(efv)) {
//                    endMark = sampleIndex;
//                }
            }
            series.put("data", seriesData);
            series.put("lines", new JSONObject("{show:true,lineWidth:2, fill:false}"));
            series.put("legend", new JSONObject("{show:false}"));
            seriesList.put(series);
            plotData.put("series", seriesList);
            plotData.put("startMarkIndex", startMark);
            plotData.put("endMarkIndex", endMark);

        }
        catch (JSONException e) {
            log.error("Error construction JSON!", e);
        }

        return plotData;
    }


    private JSONObject createBigPlotJSON(NetCDFProxy netCDF,
                                         String ef,
                                         ArrayList<String> geneNames,
                                         final String gplotIds,
                                         List<Integer> geneIndices) throws IOException {
        JSONObject plotData = new JSONObject();
        JSONArray seriesList = new JSONArray();
        JSONObject series;
        JSONArray seriesData;
        String markings = "";
        String[] genePlotOrder = gplotIds.split(",");

        try {
            int sampleIndex = 0;
            int startMark, endMark;

//			Set<String> fvs = getFactorValues(netCDF, ef);
//			final Object[] fvs_arr = fvs.toArray();
//			Integer[] sortedFVindexes = sortFVs(fvs_arr);

            // fetch the factor values for factor ef
            String[] fvs = netCDF.getFactorValues(ef);
            // sort the values - they should be grouped and ordered if possible
            Integer[] sortedFVindexes = sortFVs(fvs);
//            series = new JSONObject();
//            seriesData = new JSONArray();
            // fetch data matrix - should come from netCDF ordered by assay
//            double[][] data = eds.getExpressionMatrix();
            double[][] dataMatrix = netCDF.getExpressionMatrix();
//            Object[] assay_fvs = eds.getAssayFVs(ef); //fvs ordered by assay_id_key
//            Integer[] sortedAssayFVindexes = sortFVs(assay_fvs);//indexes of fvs sorted alphabetically
            Integer[] sortedAssayFVindexes = sortFVs(fvs);//indexes of fvs sorted alphabetically

            for (int j = 0; j < dataMatrix.length; j++) {
                double[] data = dataMatrix[j];
                series = new JSONObject();
                seriesData = new JSONArray();
                for (int k = 0; k < data.length; k++) {
                    JSONArray point = new JSONArray();
                    point.put(k + 0.5);
                    Double v = data[sortedAssayFVindexes[k]];
                    // loop over available DEs and add data points to the same x point
                    point.put(v <= -1000000 ? null : v);
                    seriesData.put(point);
                }
                series.put("data", seriesData);


                series.put("lines", new JSONObject("{show:true,lineWidth:2, fill:false, steps:false}"));
                series.put("points", new JSONObject("{show:true,fill:true}"));
                series.put("legend", new JSONObject("{show:true}"));
                series.put("label", geneNames.get(j));

                if (genePlotOrder[j] != null) {
                    series.put("color", Integer.parseInt(genePlotOrder[j]));
                }
                seriesList.put(series);
            }

            for (int i = 0; i < fvs.length; i++) {
                String fv = fvs[sortedFVindexes[i]];

//                ArrayList[] DEdata = eds.getDataByFV(ef, fv);
                double[] data = netCDF.getExpressionDataForAssay(i);

//                startMark = sampleIndex;
//                endMark = DEdata[0].size() + startMark;
//                sampleIndex = endMark;
//                if (fv.equals("")) {
//                    fv = "unannotated";
//                }
//                markings = markings.equals("") ? markings : markings + ",";
//                markings += "{xaxis:{from: " + startMark + ", to: " + endMark + "},label:\"" +
//                        fv.replaceAll("'", "").replaceAll(",", "") + "\" ,color: '" + markingColors[i % 2] + "' }";

            }

            JSONStringer sortedAssayIds = new JSONStringer();
            sortedAssayIds.array();
            int[] unSortedAssayIds = netCDF.getAssays();
            for (int i = 0; i < unSortedAssayIds.length; i++) {
                sortedAssayIds.value(unSortedAssayIds[sortedAssayFVindexes[i]]);
            }
            sortedAssayIds.endArray();


            plotData.put("series", seriesList);
            plotData.put("markings", markings);
            plotData.put("assay_ids", sortedAssayIds);
            ///////////////////////////////////////////////////////////////
            JSONStringer sampleChars = new JSONStringer();
            JSONStringer sampleCharValues = new JSONStringer();
            getCharacteristics(netCDF, sampleChars, sampleCharValues);

            plotData.put("sAttrs", getSampleAttributes(netCDF));
            plotData.put("assay2samples", getSampleAssayMap(netCDF, ef));
            plotData.put("characteristics", sampleChars);
            plotData.put("charValues", sampleCharValues);
            plotData.put("currEF", ef.substring(3));
//            plotData.put("ADid", eds.getArraydesign_id());
            plotData.put("ADid", netCDF.getArrayDesign());
            plotData.put("geneNames", getJSONarray(geneNames));
//            plotData.put("DEids", getJSONarray(eds.getDElist()));
            plotData.put("DEids", getJSONarrayFromIntArray(netCDF.getDesignElements()));
//            plotData.put("GNids", getJSONarray(eds.getGNids()));
            plotData.put("GNids", getJSONarrayFromIntArray(netCDF.getGenes()));
//            plotData.put("EFs", getJSONarray(Arrays.asList(eds.getFactors())));
            plotData.put("EFs", getJSONarrayFromStringArray(netCDF.getFactors()));
//			plotData.put("assay_ids", getJSONarray(eds.getAssayList()));


        }
        catch (JSONException e) {
            log.error("Error construction JSON!", e);
        }

        return plotData;
    }

    private JSONStringer getSampleAttributes(NetCDFProxy netCDF) throws JSONException, IOException {
        JSONStringer sampleAttrs = new JSONStringer();
//        int[] sample_ids = eds.getSampleList();
        int[] sample_ids = netCDF.getSamples();
//        Set<String> characteristics = eds.getSampleCharacteristics();
        String[] characteristics = netCDF.getCharacteristics();
        List<String> characteristicsSet = Arrays.asList(characteristics);

        sampleAttrs.object();
        for (int i = 0; i < sample_ids.length; i++) {
            int sample_key = sample_ids[i];
            sampleAttrs.key("s" + Integer.toString(sample_key));
            sampleAttrs.object();
            for (String scar : characteristics) {
//                String charValue = eds.getSampleCharValues(scar)[i].toString();
                String charValue = netCDF.getCharacteristicValues(scar)[i];

                sampleAttrs.key(scar.substring(3)).value(charValue);
            }
            sampleAttrs.endObject();
        }

//        int[] assay_ids = eds.getAssayList();
        int[] assays = netCDF.getAssays();
//        String[] EFs = eds.getFactors();
        String[] efs = netCDF.getFactors();
        for (int i = 0; i < assays.length; i++) {
            int assay_id = assays[i];
            sampleAttrs.key("a" + Integer.toString(assay_id));
            sampleAttrs.object();
            for (String ef : efs) {
                if (!characteristicsSet.contains("bs_" + ef.substring(3))) {
//                    String factorValue = eds.getAssayFVs(ef)[i].toString();
                    String factorValue = netCDF.getFactorValues(ef)[i];
                    sampleAttrs.key(ef.substring(3)).value(factorValue);
                }
            }
            sampleAttrs.endObject();
        }

        sampleAttrs.endObject();
        return sampleAttrs;
    }

    private JSONStringer getSampleAssayMap(NetCDFProxy netCDF, String ef) throws JSONException, IOException {
        JSONStringer map = new JSONStringer();
//        int[] assay_ids = eds.getAssayList();
        int[] assays = netCDF.getAssays();
//        Object[] fvs = eds.getAssayFVs(ef); //fvs ordered by assay_id_key
        String[] efvs = netCDF.getFactorValues(ef);
        Integer[] sortedFVindexes = sortFVs(efvs);//indexes of fvs sorted alphabetically
//        TreeMap<Integer, ArrayList<Integer>> assays2samples = eds.getAssays2Samples();
        int[][] assays2samples = netCDF.getAssaysToSamples();

        map.array();
        for (int i = 0; i < efvs.length; i++) {
//            int assay_id = assays[sortedFVindexes[i]];
//            ArrayList<Integer> sample_ids = assays2samples.get(assay_id);
            int assayIndex = sortedFVindexes[i];
            int[] samples = assays2samples[assayIndex];
            map.array();
            for (Integer sample : samples) {
                map.value("s" + sample);
            }
            map.endArray();
        }
        map.endArray();
        return map;
    }

    private void getCharacteristics(NetCDFProxy netCDF, JSONStringer sampleChars, JSONStringer sampleCharValues)
            throws JSONException, IOException {

        sampleChars.array();
        sampleCharValues.object();
//        for (String sampChar : eds.getSampleCharacteristics()) {
        for (String characteristic : netCDF.getCharacteristics()) {
            sampleChars.value(characteristic.substring(3));
            sampleCharValues.key(characteristic.substring(3));
            sampleCharValues.array();

//            TreeSet charValuesSet = new TreeSet(Arrays.asList(eds.getSampleCharValues(sampChar)));
//            for (Object charValue : charValuesSet) {
            for (String charValue : netCDF.getCharacteristicValues(characteristic)) {
                sampleCharValues.value(charValue);
            }
            sampleCharValues.endArray();
        }
        sampleCharValues.endObject();
        sampleChars.endArray();
    }

    private Integer[] sortFVs(final String[] fvs) {

        Integer[] fso = new Integer[fvs.length];
        for (int i = 0; i < fvs.length; i++) {
            fso[i] = i;
        }

        Arrays.sort(fso,
                new Comparator() {

                    public int compare(Object o1, Object o2) {
                        String s1 = fvs[((Integer) o1)];
                        String s2 = fvs[((Integer) o2)];

                        // want to make sure that empty strings are pushed to the back
                        if (s1.equals("") && s2.equals("")) {
                            return 0;
                        }
                        if (s1.equals("") && !s2.equals("")) {
                            return 1;
                        }
                        if (!s1.equals("") && s2.equals("")) {
                            return -1;
                        }

                        java.util.regex.Matcher m1 = startsOrEndsWithDigits.matcher(s1);
                        java.util.regex.Matcher m2 = startsOrEndsWithDigits.matcher(s2);

                        if (m1.find() && m2.find()) {
                            Long i1 = new Long(s1.substring(m1.start(), m1.end()));
                            Long i2 = new Long(s2.substring(m2.start(), m2.end()));

                            if (i1.compareTo(i2) == 0) {
                                return s1.compareToIgnoreCase(s2);
                            }
                            else {
                                return i1.compareTo(i2);
                            }
                        }

                        return s1.compareToIgnoreCase(s2);
                    }

                });
        return fso;
    }

    private double getMean(ArrayList<Double> test) {
        double sum = 0.0;

        for (int i = 0; i < test.size(); i++) {
            double v = test.get(i);
            if (v > -1000000) {
                sum += test.get(i);
            }
        }

        return sum / test.size();
    }

    private ArrayList<String> getGeneNames(String gids) {
        AtlasDao dao = atlasSearchService.getAtlasSolrDAO();

        ArrayList<String> geneNames = new ArrayList<String>();
        String[] ids = gids.split(",");
        for (String gid : ids) {
            AtlasDao.AtlasGeneResult gene = dao.getGeneById(gid);
            if (gene.isFound()) {
                geneNames.add(gene.getGene().getGeneName());
            }
        }
        return geneNames;
    }

    private JSONStringer getJSONarray(List<String> geneNames) throws JSONException {
        JSONStringer JSONarray = new JSONStringer();
        JSONarray.array();
        for (String gene : geneNames) {
            JSONarray.value(gene);
        }
        JSONarray.endArray();
        return JSONarray;
    }


    private JSONStringer getJSONarrayFromStringArray(String[] names) throws JSONException {
        JSONStringer JSONarray = new JSONStringer();
        JSONarray.array();
        for (String name : names) {
            JSONarray.value(name);
        }
        JSONarray.endArray();
        return JSONarray;
    }

    private JSONStringer getJSONarrayFromIntArray(int[] values) throws JSONException {
        JSONStringer JSONarray = new JSONStringer();
        JSONarray.array();
        for (int value : values) {
            JSONarray.value(value);
        }
        JSONarray.endArray();
        return JSONarray;
    }
}
