/*
 * Copyright 2008-2010 Microarray Informatics Team, EMBL-European Bioinformatics Institute
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *
 * For further details of the Gene Expression Atlas project, including source code,
 * downloads and documentation, please see:
 *
 * http://gxa.github.com/gxa
 */

package uk.ac.ebi.gxa.netcdf.reader;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.ma2.Array;
import ucar.ma2.ArrayChar;
import ucar.ma2.ArrayFloat;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.dataset.NetcdfDataset;
import uk.ac.ebi.microarray.atlas.model.Expression;
import uk.ac.ebi.microarray.atlas.model.ExpressionAnalysis;

import javax.annotation.Nullable;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * An object that proxies an Atlas NetCDF file and provides convenience methods for accessing the data from within. This
 * class should be used when trying to read data on-the-fly out of a NetCDF directly.
 * <p/>
 * The NetCDFs for Atlas are structured as follows:
 * <pre>
 *    long AS(AS) ;
 *    long BS(BS) ;
 *    int BS2AS(BS, AS) ;
 *    long DE(DE) ;
 *    long GN(GN) ;
 *    int DE2GN(DE, GN) ;
 *    char EF(EF, EFlen) ;
 *    char EFV(EF, AS, EFlen) ;
 *    char uEFV(uEFV, EFlen) ;
 *    int uEFVnum(EF) ;
 *    char SC(SC, SClen) ;
 *    char SCV(SC, BS, SClen) ;
 *    float BDC(DE, AS) ;
 *    float PVAL(DE, uEFV) ;
 *    float TSTAT(DE, uEFV) ;
 * </pre>
 *
 * @author Tony Burdett
 */
public class NetCDFProxy implements Closeable {
    public static final String NCDF_EF_EFV_SEP = "\\|\\|";
    private final Logger log = LoggerFactory.getLogger(getClass());

    private final File pathToNetCDF;
    private final NetcdfFile netCDF;

    public NetCDFProxy(File file) throws IOException {
        this.pathToNetCDF = file.getAbsoluteFile();
        this.netCDF = NetcdfDataset.acquireFile(file.getAbsolutePath(), null);
    }

    public Long getExperimentId() {
        return Long.valueOf(getId().split("_")[0]);
    }

    public String getId() {
        return pathToNetCDF.getName();
    }

    public String getExperiment() throws IOException {
        return netCDF.findGlobalAttribute("experiment_accession").getStringValue();
    }

    public String getArrayDesignAccession() throws IOException {
        return netCDF.findGlobalAttribute("ADaccession").getStringValue();
    }

    public Long getArrayDesignID() throws IOException {
        if (netCDF.findGlobalAttribute("ADid") == null) {
            return null;
        }

        Number value = netCDF.findGlobalAttribute("ADid").getNumericValue();
        if (value == null) {
            return null;
        }

        return value.longValue();
    }

    private long[] getLongArray1(final String variableName) throws IOException {
        final Variable var = netCDF.findVariable(variableName);
        if (var == null) {
            return new long[0];
        } else {
            return (long[]) var.read().get1DJavaArray(long.class);
        }
    }

    private float[] getFloatArrayForDesignElementAtIndex(int designElementIndex, String variableName, String readableName) throws IOException {
        Variable variable = netCDF.findVariable(variableName);
        if (variable == null) {
            return new float[0];
        }

        int[] shape = variable.getShape();
        int[] origin = {designElementIndex, 0};
        int[] size = new int[]{1, shape[1]};
        try {
            return (float[]) variable.read(origin, size).get1DJavaArray(float.class);
        } catch (InvalidRangeException e) {
            log.error("Error reading from NetCDF - invalid range at " + designElementIndex + ": " + e.getMessage());
            throw new IOException("Failed to read " + readableName + " data for design element at " + designElementIndex +
                    ": caused by " + e.getClass().getSimpleName() + " [" + e.getMessage() + "]");
        }
    }

    public long[] getAssays() throws IOException {
        return getLongArray1("AS");
    }

    public long[] getSamples() throws IOException {
        return getLongArray1("BS");
    }

    public int[][] getSamplesToAssays() throws IOException {
        // read BS2AS
        Variable bs2as = netCDF.findVariable("BS2AS");
        if (bs2as == null) {
            return new int[0][0];
        }

        // copy to an int array - BS2AS is 2d array so this should drop out
        return (int[][]) bs2as.read().copyToNDJavaArray();
    }

    public long[] getDesignElements() throws IOException {
        return getLongArray1("DE");
    }

    private String getGlobalAttribute(String attribute) {
        ucar.nc2.Attribute a = netCDF.findGlobalAttribute(attribute);
        return null == a ? null : a.getStringValue();
    }

    public String getExperimentDescription() {
        return getGlobalAttribute("experiment_description");
    }

    public String getExperimentLab() {
        return getGlobalAttribute("experiment_lab");
    }

    public String getExperimentPerformer() {
        return getGlobalAttribute("experiment_performer");
    }

    public String getExperimentPubmedID() {
        return getGlobalAttribute("experiment_pmid");
    }

    public String getArticleAbstract() {
        return getGlobalAttribute("experiment_abstract");
    }

    /**
     * @param deIndex the index of element to retrieve
     * @return design element Id corresponding to deIndex
     * @throws IOException if index is out of range
     */
    public long getDesignElementId(int deIndex) throws IOException {
        final long[] des = getDesignElements();
        if (deIndex < des.length) {
            return des[deIndex];
        } else {
            throw new IOException("Design element index: " + deIndex + " out of range: " + des.length);
        }
    }

    /**
     * Gets the array of gene IDs from this NetCDF
     *
     * @return an long[] representing the one dimensional array of gene identifiers
     * @throws IOException if accessing the NetCDF failed
     */
    public long[] getGenes() throws IOException {
        return getLongArray1("GN");
    }

    private String[] getArrayOfStrings(String variable) throws IOException {
        if (netCDF.findVariable(variable) == null) {
            return new String[0];
        }
        ArrayChar deacc = (ArrayChar) netCDF.findVariable(variable).read();
        ArrayChar.StringIterator si = deacc.getStringIterator();
        String[] result = new String[deacc.getShape()[0]];
        for (int i = 0; i < result.length && si.hasNext(); ++i)
            result[i] = si.next();
        return result;
    }

    public String[] getDesignElementAccessions() throws IOException {
        return getArrayOfStrings("DEacc");
    }

    public String[] getAssayAccessions() throws IOException {
        return getArrayOfStrings("ASacc");
    }

    public String[] getSampleAccessions() throws IOException {
        return getArrayOfStrings("BSacc");
    }

    public String[] getFactors() throws IOException {
        if (netCDF.findVariable("EF") == null) {
            return new String[0];
        }

        // create a array of characters from the "EF" dimension
        ArrayChar efs = (ArrayChar) netCDF.findVariable("EF").read();
        // convert to a string array and return
        Object[] efsArray = (Object[]) efs.make1DStringArray().get1DJavaArray(String.class);
        String[] result = new String[efsArray.length];
        for (int i = 0; i < efsArray.length; i++) {
            result[i] = (String) efsArray[i];
            if (result[i].startsWith("ba_"))
                result[i] = result[i].substring(3);
        }
        return result;
    }

    public String[] getFactorValues(String factor) throws IOException {
        Integer efIndex = findEfIndex(factor);
        return efIndex == null ? new String[0] : getSlice3D("EFV", efIndex);
    }


    public String[] getFactorValueOntologies(String factor) throws IOException {
        Integer efIndex = findEfIndex(factor);
        return efIndex == null ? new String[0] : getSlice3D("EFVO", efIndex);
    }

    private Integer findEfIndex(String factor) throws IllegalArgumentException, IOException {
        String[] efs = getFactors();
        for (int i = 0; i < efs.length; i++) {
            // todo: note flexible matching for ba_<factor> or <factor> - this is hack to work around old style netcdfs
            if (factor.matches("(ba_)?" + efs[i])) {
                return i;
            }
        }
        return null;
    }

    private Integer findScIndex(String factor) throws IOException {
        String[] scs = getCharacteristics();
        for (int i = 0; i < scs.length; i++) {
            // todo: note flexible matching for bs_<factor> or <factor> - this is hack to work around old style netcdfs
            if (factor.matches("(bs_)?" + scs[i])) {
                return i;
            }
        }
        return null;
    }

    //read variable as 3D array of chars, and return
    //slice (by dimension = 0) at index as array of strings
    private String[] getSlice3D(String variable, int index) throws IOException {
        // if the EFV variable is empty
        if (netCDF.findVariable(variable) == null) {
            return new String[0];
        }
        // now we have index of our ef, so take a read from efv for this index
        Array efvs = netCDF.findVariable(variable).read();
        // slice this array on dimension '0' (this is EF dimension), retaining only these efvs ordered by assay
        ArrayChar ef_efv = (ArrayChar) efvs.slice(0, index);

        // convert to a string array and return
        Object[] ef_efvArray = (Object[]) ef_efv.make1DStringArray().get1DJavaArray(String.class);
        String[] result = new String[ef_efvArray.length];
        for (int i = 0; i < ef_efvArray.length; i++) {
            result[i] = (String) ef_efvArray[i];
        }
        return result;
    }

    public String[] getUniqueFactorValues() throws IOException {
        // create a array of characters from the "SC" dimension
        if (netCDF.findVariable("uEFV") == null) {
            return new String[0];
        }

        ArrayChar uefv = (ArrayChar) netCDF.findVariable("uEFV").read();

        // convert to a string array and return
        Object[] uefvArray = (Object[]) uefv.make1DStringArray().get1DJavaArray(String.class);
        return Arrays.copyOf(uefvArray, uefvArray.length, String[].class);
    }

    public String[] getCharacteristics() throws IOException {
        if (netCDF.findVariable("SC") == null) {
            return new String[0];
        }

        // create a array of characters from the "SC" dimension
        ArrayChar scs = (ArrayChar) netCDF.findVariable("SC").read();
        // convert to a string array and return
        Object[] scsArray = (Object[]) scs.make1DStringArray().get1DJavaArray(String.class);
        String[] result = new String[scsArray.length];
        for (int i = 0; i < scsArray.length; i++) {
            result[i] = (String) scsArray[i];
            if (result[i].startsWith("bs_"))
                result[i] = result[i].substring(3);
        }
        return result;
    }

    public String[] getCharacteristicValues(String characteristic) throws IOException {
        Integer scIndex = findScIndex(characteristic);
        return scIndex == null ? new String[0] : getSlice3D("SCV", scIndex);
    }

    public String[] getCharacteristicValueOntologies(String characteristic) throws IOException {
        Integer scIndex = findScIndex(characteristic);
        return scIndex == null ? new String[0] : getSlice3D("SCVO", scIndex);
    }

    /**
     * Gets a single row from the expression data matrix representing all expression data for a single design element.
     * This is obtained by retrieving all data from the given row in the expression matrix, where the design element
     * index supplied is the row number.  As the expression value matrix has the same ordering as the design element
     * array, you can iterate over the design element array to retrieve the index of the row you want to fetch.
     *
     * @param designElementIndex the index of the design element which we're interested in fetching data for
     * @return the double array representing expression values for this design element
     * @throws IOException if the NetCDF could not be accessed
     */
    public float[] getExpressionDataForDesignElementAtIndex(int designElementIndex) throws IOException {
        return getFloatArrayForDesignElementAtIndex(designElementIndex, "BDC", "expression");
    }

    public float[] getPValuesForDesignElement(int designElementIndex) throws IOException {
        return getFloatArrayForDesignElementAtIndex(designElementIndex, "PVAL", "p-value");
    }

    public float[] getPValuesForUniqueFactorValue(int uniqueFactorValueIndex) throws IOException {
        Variable pValVariable = netCDF.findVariable("PVAL");

        if (pValVariable == null) {
            return new float[0];
        }

        int[] pValShape = pValVariable.getShape();
        int[] origin = {0, uniqueFactorValueIndex};
        int[] size = new int[]{pValShape[0], 1};
        try {
            return (float[]) pValVariable.read(origin, size).get1DJavaArray(float.class);
        } catch (InvalidRangeException e) {
            log.error("Error reading from NetCDF - invalid range at " + uniqueFactorValueIndex + ": " +
                    e.getMessage());
            throw new IOException("Failed to read p-value data for unique factor value at " +
                    uniqueFactorValueIndex + ": caused by " + e.getClass().getSimpleName() + " " +
                    "[" + e.getMessage() + "]");
        }
    }

    public float[] getTStatisticsForDesignElement(int designElementIndex) throws IOException {
        return getFloatArrayForDesignElementAtIndex(designElementIndex, "TSTAT", "t-statistics");
    }

    public float[] getTStatisticsForUniqueFactorValue(int uniqueFactorValueIndex) throws IOException {
        Variable tStatVariable = netCDF.findVariable("TSTAT");

        if (tStatVariable == null) {
            return new float[0];
        }

        int[] tStatShape = tStatVariable.getShape();
        int[] origin = {0, uniqueFactorValueIndex};
        int[] size = new int[]{tStatShape[0], 1};
        try {
            return (float[]) tStatVariable.read(origin, size).get1DJavaArray(float.class);
        } catch (InvalidRangeException e) {
            log.error("Error reading from NetCDF - invalid range at " + uniqueFactorValueIndex + ": " +
                    e.getMessage());
            throw new IOException("Failed to read t-statistic data for unique factor value at " +
                    uniqueFactorValueIndex + ": caused by " + e.getClass().getSimpleName() + " " +
                    "[" + e.getMessage() + "]");
        }
    }

    /**
     * Closes the proxied NetCDF file
     *
     * @throws java.io.IOException on close errors
     */
    public void close() throws IOException {
        if (netCDF != null)
            netCDF.close();
    }


    /**
     * /**
     * For each gene in the keySet() of geneIdsToDEIndexes, and each efv in uEF_EFVs,
     * find the design element with a minPvalue and store it as an ExpressionAnalysis object in
     * geneIdsToEfToEfvToEA if the minPvalue found in this proxy is better than the one already in
     * geneIdsToEfToEfvToEA.
     *
     * @param geneIdsToDEIndexes geneId -> list of design element indexes containing data for that gene
     * @return geneId -> ef -> efv -> ea of best pValue for this geneid-ef-efv combination
     *         Note that ea contains proxyId and designElement index from which it came, so that
     *         the actual expression values can be easily retrieved later
     * @throws IOException in case of I/O errors
     */
    public Map<Long, Map<String, Map<String, ExpressionAnalysis>>> getExpressionAnalysesForDesignElementIndexes(
            final Map<Long, List<Integer>> geneIdsToDEIndexes) throws IOException {
        return getExpressionAnalysesForDesignElementIndexes(geneIdsToDEIndexes, null, null, Expression.ANY);
    }

    /**
     * For each gene in the keySet() of geneIdsToDEIndexes,  and for either efVal-efvVal or (if both arguments are not null)
     * for each efv in uEF_EFVs, find the design element with a minPvalue and store it as an ExpressionAnalysis object in
     * geneIdsToEfToEfvToEA - if the minPvalue found in this proxy is better than the one already in
     * geneIdsToEfToEfvToEA.
     *
     * @param geneIdsToDEIndexes geneId -> list of design element indexes containing data for that gene
     * @param efVal              ef to retrieve ExpressionAnalyses for
     * @param efvVal             efv to retrieve ExpressionAnalyses for; if either efVal or efvVal are null,
     *                           ExpressionAnalyses for all ef-efvs will be retrieved
     * @param expression         desired expression; used only when efVal-efvVal are specified
     * @return geneId -> ef -> efv -> ea of best pValue for this geneid-ef-efv combination
     *         Note that ea contains proxyId and designElement index from which it came, so that
     *         the actual expression values can be easily retrieved later
     * @throws IOException in case of I/O errors
     */
    public Map<Long, Map<String, Map<String, ExpressionAnalysis>>> getExpressionAnalysesForDesignElementIndexes(
            final Map<Long, List<Integer>> geneIdsToDEIndexes,
            @Nullable final String efVal,
            @Nullable final String efvVal,
            final Expression expression)
            throws IOException {

        Map<Long, Map<String, Map<String, ExpressionAnalysis>>> geneIdsToEfToEfvToEA = new HashMap<Long, Map<String, Map<String, ExpressionAnalysis>>>();
        ExpressionAnalysisHelper eaHelper = createExpressionAnalysisHelper();

        for (Map.Entry<Long, List<Integer>> entry : geneIdsToDEIndexes.entrySet()) {
            final Long geneId = entry.getKey();

            if (geneId == 0) continue; // skip geneid = 0

            if (!geneIdsToEfToEfvToEA.containsKey(geneId)) {
                Map<String, Map<String, ExpressionAnalysis>> efToEfvToEA = new HashMap<String, Map<String, ExpressionAnalysis>>();
                geneIdsToEfToEfvToEA.put(geneId, efToEfvToEA);
            }
            for (Integer deIndex : entry.getValue()) {
                List<ExpressionAnalysis> eaList = new ArrayList<ExpressionAnalysis>();
                if (efVal != null && efvVal != null) {
                    ExpressionAnalysis ea = eaHelper.getByDesignElementIndex(deIndex).getByEF(efVal, efvVal);
                    if (ea != null &&
                            (expression == Expression.ANY ||
                                    (expression == Expression.UP && ea.isUp()) ||
                                    (expression == Expression.DOWN && ea.isDown()) ||
                                    (expression == Expression.NONDE && ea.isNo()))) {
                        eaList.add(ea);
                    }
                } else {
                    eaList.addAll(eaHelper.getByDesignElementIndex(deIndex).getAll());
                }

                for (ExpressionAnalysis ea : eaList) {
                    String ef = ea.getEfName();
                    String efv = ea.getEfvName();

                    if (geneIdsToEfToEfvToEA.get(geneId).get(ef) == null) {
                        Map<String, ExpressionAnalysis> efvToEA = new HashMap<String, ExpressionAnalysis>();
                        geneIdsToEfToEfvToEA.get(geneId).put(ef, efvToEA);
                    }

                    ExpressionAnalysis prevBestPValueEA =
                            geneIdsToEfToEfvToEA.get(geneId).get(ef).get(efv);
                    if ((prevBestPValueEA == null ||
                            // Mo stats were available in the previously seen ExpressionAnalysis
                            Float.isNaN(prevBestPValueEA.getPValAdjusted()) ||  Float.isNaN(prevBestPValueEA.getTStatistic()) ||
                            // Stats are available for ea, an it has a better pValue than the previous  ExpressionAnalysis
                            (!Float.isNaN(ea.getPValAdjusted()) && prevBestPValueEA.getPValAdjusted() > ea.getPValAdjusted()) ||
                            // Stats are available for ea, both pValues are equals, then the better one is the one with the higher absolute tStat
                            (!Float.isNaN(ea.getPValAdjusted()) &&
                                    !Float.isNaN(ea.getTStatistic()) &&
                                    prevBestPValueEA.getPValAdjusted() == ea.getPValAdjusted() &&
                                    Math.abs(prevBestPValueEA.getTStatistic()) < Math.abs(ea.getTStatistic())))
                            ) {
                        if (ea.getPValAdjusted() > 1) {
                            // As the NA pvals/tstats  currently come back from ncdfs as 1.0E30, we convert them to Float.NaN
                            ea.setPValAdjusted(Float.NaN);
                            ea.setTStatistic(Float.NaN);

                        }
                        geneIdsToEfToEfvToEA.get(geneId).get(ef).put(efv, ea);
                    }
                }
            }
        }
        return geneIdsToEfToEfvToEA;
    }

    public ExpressionAnalysisHelper createExpressionAnalysisHelper() throws IOException {
        return (new ExpressionAnalysisHelper()).prepare();
    }

    //TODO: temporary solution; should be replaced in the future releases

    public static abstract class ExpressionAnalysisResult {
        private float[] p;
        private float[] t;
        private int deIndex;

        private ExpressionAnalysisResult(int deIndex, float[] p, float[] t) {
            this.deIndex = deIndex;
            this.p = p;
            this.t = t;
        }

        private List<ExpressionAnalysis> list(Predicate<Integer> predicate) {
            List<ExpressionAnalysis> list = new ArrayList<ExpressionAnalysis>();

            for (int j = 0; j < p.length; j++) {
                if (predicate.apply(j)) {
                    ExpressionAnalysis ea = createExpressionAnalysis(deIndex, j);
                    ea.setPValAdjusted(p[j]);
                    ea.setTStatistic(t[j]);
                    list.add(ea);
                }
            }

            return list;
        }

        public List<ExpressionAnalysis> getAll() {
            return list(Predicates.<Integer>alwaysTrue());
        }

        public ExpressionAnalysis getByEF(final String efName, final String efvName) {
            List<ExpressionAnalysis> list = list(new Predicate<Integer>() {
                public boolean apply(Integer o) {
                    return isIndexValid(o, efName, efvName);
                }
            });
            return list.isEmpty() ? null : list.get(0);
        }

        public abstract ExpressionAnalysis createExpressionAnalysis(int deIndex, int efIndex);

        public abstract boolean isIndexValid(int index, String efName, String efvName);
    }

    public class ExpressionAnalysisHelper {

        private List<String[]> uEF_EFVs = new ArrayList<String[]>();
        private long[] designElementIds;

        private ExpressionAnalysisHelper() {
        }

        private ExpressionAnalysisHelper prepare() throws IOException {
            String[] uEFVs = getUniqueFactorValues();

            for (String uEFV : uEFVs) {
                String[] arr = uEFV.split(NCDF_EF_EFV_SEP);
                uEF_EFVs.add(arr.length == 1 ? new String[]{arr[0], ""} : arr);
            }

            designElementIds = getDesignElements();
            return this;
        }

        public ExpressionAnalysisResult getByDesignElementIndex(int deIndex) throws IOException {

            float[] p = getPValuesForDesignElement(deIndex);
            float[] t = getTStatisticsForDesignElement(deIndex);

            return new ExpressionAnalysisResult(deIndex, p, t) {

                @Override
                public ExpressionAnalysis createExpressionAnalysis(int deIndex, int efIndex) {
                    String[] uEF_EFV = uEF_EFVs.get(efIndex);
                    String ef = uEF_EFV[0];
                    String efv = uEF_EFV[1];

                    ExpressionAnalysis ea = new ExpressionAnalysis();
                    ea.setEfName(ef);
                    ea.setEfvName(efv);
                    ea.setDesignElementID(designElementIds[deIndex]);
                    ea.setExperimentID(getExperimentId());
                    ea.setDesignElementIndex(deIndex);
                    ea.setProxyId(getId());
                    return ea;
                }

                @Override
                public boolean isIndexValid(int efIndex, String efName, String efvName) {
                    String[] uEF_EFV = uEF_EFVs.get(efIndex);
                    return uEF_EFV[0].equals(efName) &&
                            (efvName == null || uEF_EFV[1].equals(efvName));
                }
            };
        }
    }

    public ArrayFloat.D2 getTStatistics() throws IOException {
        Variable tStatVariable = netCDF.findVariable("TSTAT");
        if (tStatVariable == null) {
            return new ArrayFloat.D2(0, 0);
        }

        return (ArrayFloat.D2) tStatVariable.read();
    }

    public ArrayFloat.D2 getPValues() throws IOException {
        Variable pValVariable = netCDF.findVariable("PVAL");
        if (pValVariable == null) {
            return new ArrayFloat.D2(0, 0);
        }

        return (ArrayFloat.D2) pValVariable.read();
    }

    public Map<String, Collection<String>> getActualEfvTree() throws IOException {
        Multimap<String, String> efvs = HashMultimap.create();
        for (String s : getUniqueFactorValues()) {
            String[] nameValue = s.split(NCDF_EF_EFV_SEP);
            String name = nameValue[0];
            String value = nameValue.length > 1 ? nameValue[1] : "";
            efvs.put(name, value);
        }
        return efvs.asMap();
    }
}
