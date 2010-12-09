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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.ma2.*;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.dataset.NetcdfDataset;
import uk.ac.ebi.microarray.atlas.model.ExpressionAnalysis;

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
 * @date 11-Nov-2009
 */
public class NetCDFProxy {
    // this is false if opening a connection to the netcdf file failed
    private boolean proxied;
    private File pathToNetCDF;

    private NetcdfFile netCDF;

    private final Logger log = LoggerFactory.getLogger(getClass());
    private long experimentId;

    public NetCDFProxy(File netCDF) {
        this.pathToNetCDF = netCDF.getAbsoluteFile();
        try {
            this.netCDF = NetcdfDataset.acquireFile(netCDF.getAbsolutePath(), null);
            this.experimentId = Long.valueOf(netCDF.getName().split("_")[0]);
            proxied = true;
        } catch (IOException e) {
            proxied = false;
        }
    }

    /**
     * eg. pathToNetCDF: ~/Documents/workspace/atlas-data/netCDF/223403015_221532256.nc
     *
     * @return fileName (i.e. substring after the last '/', e.g. "223403015_221532256.nc")
     */
    public String getId() {
        return pathToNetCDF.getName();
    }

    public String getExperiment() throws IOException {
        if (!proxied) {
            throw new IOException("Unable to open NetCDF file at " + pathToNetCDF);
        }

        return netCDF.findGlobalAttribute("experiment_accession").getStringValue();
    }

    public String getArrayDesignAccession() throws IOException {
        if (!proxied) {
            throw new IOException("Unable to open NetCDF file at " + pathToNetCDF);
        }

        return netCDF.findGlobalAttribute("ADaccession").getStringValue();
    }

    public long getArrayDesignID() throws IOException {
        if (!proxied) {
            throw new IOException("Unable to open NetCDF file at " + pathToNetCDF);
        }

        if (netCDF.findGlobalAttribute("ADid") != null) {
            Number value = netCDF.findGlobalAttribute("ADid").getNumericValue();
            if (value != null)
                return value.longValue();
            return -1;
        }

        return -1;
    }

    private long[] getLongArray1(final String variableName) throws IOException {
        if (!proxied) {
            throw new IOException("Unable to open NetCDF file at " + pathToNetCDF);
        }

        final Variable var = netCDF.findVariable(variableName);
        if (var == null) {
            return new long[0];
        } else {
            return (long[]) var.read().get1DJavaArray(long.class);
        }
    }

    private float[] getFloatArrayForDesignElementAtIndex(int designElementIndex, String variableName, String readableName) throws IOException {
        if (!proxied) {
            throw new IOException("Unable to open NetCDF file at " + pathToNetCDF);
        }

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
        if (!proxied) {
            throw new IOException("Unable to open NetCDF file at " + pathToNetCDF);
        }

        // read BS2AS
        if (netCDF.findVariable("BS2AS") == null) {
            return new int[0][0];
        } else {

            Array bs2as = netCDF.findVariable("BS2AS").read();
            // copy to an int array - BS2AS is 2d array so this should drop out
            return (int[][]) bs2as.copyToNDJavaArray();
        }
    }

    public long[] getDesignElements() throws IOException {
        return getLongArray1("DE");
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

    public String[] getDesignElementAccessions() throws IOException {
        if (!proxied)
            throw new IOException("Unable to open NetCDF file at " + pathToNetCDF);

        if (netCDF.findVariable("DEacc") == null) {
            return new String[0];
        } else {
            ArrayChar deacc = (ArrayChar) netCDF.findVariable("DEacc").read();
            ArrayChar.StringIterator si = deacc.getStringIterator();
            String[] result = new String[deacc.getShape()[0]];
            for (int i = 0; i < result.length && si.hasNext(); ++i)
                result[i] = si.next();
            return result;
        }
    }

    public String[] getFactors() throws IOException {
        if (!proxied) {
            throw new IOException("Unable to open NetCDF file at " + pathToNetCDF);
        }

        if (netCDF.findVariable("EF") == null) {
            return new String[0];
        } else {
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
    }

    public String[] getFactorValues(String factor) throws IOException {
        if (!proxied) {
            throw new IOException("Unable to open NetCDF file at " + pathToNetCDF);
        }

        // get all factors
        String[] efs = getFactors();

        // iterate over factors to find the index of the one we're interested in
        int efIndex = 0;
        boolean efFound = false;
        for (String ef : efs) {
            // todo: note flexible matching for ba_<factor> or <factor> - this is hack to work around old style netcdfs
            if (factor.matches("(ba_)?" + ef)) {
                efFound = true;
                break;
            } else {
                efIndex++;
            }
        }

        // if we couldn't match the factor we're looking for, return empty array
        if (!efFound) {
            log.warn("Couldn't locate index of " + factor + " in " + pathToNetCDF);
            return new String[0];
        }

        // if the EFV variable is empty
        if (netCDF.findVariable("EFV") == null) {
            return new String[0];
        } else {
            // now we have index of our ef, so take a read from efv for this index
            Array efvs = netCDF.findVariable("EFV").read();
            // slice this array on dimension '0' (this is EF dimension), retaining only these efvs ordered by assay
            ArrayChar ef_efv = (ArrayChar) efvs.slice(0, efIndex);

            // convert to a string array and return
            Object[] ef_efvArray = (Object[]) ef_efv.make1DStringArray().get1DJavaArray(String.class);
            String[] result = new String[ef_efvArray.length];
            for (int i = 0; i < ef_efvArray.length; i++) {
                result[i] = (String) ef_efvArray[i];
            }
            return result;
        }
    }

    public String[] getUniqueFactorValues() throws IOException {
        if (!proxied) {
            throw new IOException("Unable to open NetCDF file at " + pathToNetCDF);
        }

        // create a array of characters from the "SC" dimension
        if (netCDF.findVariable("uEFV") == null) {
            return new String[0];
        } else {
            ArrayChar uefv = (ArrayChar) netCDF.findVariable("uEFV").read();

            // convert to a string array and return
            Object[] uefvArray = (Object[]) uefv.make1DStringArray().get1DJavaArray(String.class);
            return Arrays.copyOf(uefvArray, uefvArray.length, String[].class);
        }
    }

    public String[] getCharacteristics() throws IOException {
        if (!proxied) {
            throw new IOException("Unable to open NetCDF file at " + pathToNetCDF);
        }

        if (netCDF.findVariable("SC") == null) {
            return new String[0];
        } else {
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
    }

    public String[] getCharacteristicValues(String characteristic) throws IOException {
        if (!proxied) {
            throw new IOException("Unable to open NetCDF file at " + pathToNetCDF);
        }

        // get all characteristics
        String[] scs = getCharacteristics();

        // iterate over factors to find the index of the one we're interested in
        int scIndex = 0;
        boolean scFound = false;
        for (String sc : scs) {
            // todo: note flexible matching for ba_<factor> or <factor> - this is hack to work around old style netcdfs
            if (characteristic.matches("(bs_)?" + sc)) {
                scFound = true;
                break;
            } else {
                scIndex++;
            }
        }

        // if we couldn't match the characteristic we're looking for, return empty array
        if (!scFound) {
            log.error("Couldn't locate index of " + characteristic + " in " + pathToNetCDF);
            return new String[0];
        }

        if (netCDF.findVariable("SCV") == null) {
            return new String[0];
        } else {
            // now we have index of our sc, so take a read from scv for this index
            ArrayChar scvs = (ArrayChar) netCDF.findVariable("SCV").read();
            // slice this array on dimension '0' (this is SC dimension), retaining only these scvs ordered by sample
            ArrayChar sc_scv = (ArrayChar) scvs.slice(0, scIndex);
            // convert to a string array and return
            Object[] sc_scvArray = (Object[]) sc_scv.make1DStringArray().get1DJavaArray(String.class);
            String[] result = new String[sc_scvArray.length];
            for (int i = 0; i < sc_scvArray.length; i++) {
                result[i] = (String) sc_scvArray[i];
            }
            return result;
        }
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

    /**
     * Gets a single column from the expression data matrix representing all expression data for a single assay. This is
     * obtained by retrieving all data from the given column in the expression matrix, where the assay index supplied is
     * the column number.  As the expression value matrix has the same ordering as the assay array, you can iterate over
     * the assay array to retrieve the index of the column you want to fetch.
     *
     * @param assayIndex the index of the assay which we're interested in fetching data for
     * @return the double array representing expression values for this assay
     * @throws IOException if the NetCDF could not be accessed
     */
    public float[] getExpressionDataForAssay(int assayIndex) throws IOException {
        if (!proxied) {
            throw new IOException("Unable to open NetCDF file at " + pathToNetCDF);
        }

        Variable bdcVariable = netCDF.findVariable("BDC");

        if (bdcVariable == null) {
            return new float[0];
        } else {
            int[] bdcShape = bdcVariable.getShape();
            int[] origin = {0, assayIndex};
            int[] size = new int[]{bdcShape[0], 1};
            try {
                return (float[]) bdcVariable.read(origin, size).get1DJavaArray(float.class);
            } catch (InvalidRangeException e) {
                log.error("Error reading from NetCDF - invalid range at " + assayIndex + ": " + e.getMessage());
                throw new IOException("Failed to read expression data for assay at " + assayIndex +
                        ": caused by " + e.getClass().getSimpleName() + " [" + e.getMessage() + "]");
            }
        }
    }

    public float[] getPValuesForDesignElement(int designElementIndex) throws IOException {
        return getFloatArrayForDesignElementAtIndex(designElementIndex, "PVAL", "p-value");
    }

    public float[] getPValuesForUniqueFactorValue(int uniqueFactorValueIndex) throws IOException {
        if (!proxied) {
            throw new IOException("Unable to open NetCDF file at " + pathToNetCDF);
        }

        Variable pValVariable = netCDF.findVariable("PVAL");

        if (pValVariable == null) {
            return new float[0];
        } else {
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
    }

    public float[] getTStatisticsForDesignElement(int designElementIndex) throws IOException {
        return getFloatArrayForDesignElementAtIndex(designElementIndex, "TSTAT", "t-statistics");
    }

    public float[] getTStatisticsForUniqueFactorValue(int uniqueFactorValueIndex) throws IOException {
        if (!proxied) {
            throw new IOException("Unable to open NetCDF file at " + pathToNetCDF);
        }

        Variable tStatVariable = netCDF.findVariable("TSTAT");

        if (tStatVariable == null) {
            return new float[0];
        } else {
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
    }

    /**
     * Closes the proxied NetCDF file
     */
    public void close() {

        try {
            if (this.netCDF != null)
                this.netCDF.close();
        } catch (IOException ioe) {
            log.error("Failed to close proxy: " + getId(), ioe);
        }
    }

    public Map<Long, List<ExpressionAnalysis>> getExpressionAnalysesForGenes() throws IOException {

        final Map<Long, List<ExpressionAnalysis>> geas = new HashMap<Long, List<ExpressionAnalysis>>();
        final String[] uEFVs = getUniqueFactorValues();

        final long[] genes = getGenes();
        final long[] des = getDesignElements();
        final ArrayFloat pval = (ArrayFloat) netCDF.findVariable("PVAL").read();
        final ArrayFloat tstat = (ArrayFloat) netCDF.findVariable("TSTAT").read();

        IndexIterator pvalIter = pval.getIndexIterator();
        IndexIterator tstatIter = tstat.getIndexIterator();

        for (int i = 0; i < genes.length; i++) {
            List<ExpressionAnalysis> eas;

            if (0 != genes[i] &&
                    !geas.containsKey(genes[i]))
                eas = new LinkedList<ExpressionAnalysis>();
            else
                eas = geas.get(genes[i]);

            for (String uEFV : uEFVs) {
                if (!pvalIter.hasNext() || !tstatIter.hasNext()) {
                    throw new RuntimeException("Unexpected end of expression analytics data in " + pathToNetCDF);
                }

                float pval_ = pvalIter.getFloatNext();
                float tstat_ = tstatIter.getFloatNext();

                if (genes[i] == 0) continue; // skip geneid = 0

                ExpressionAnalysis ea = new ExpressionAnalysis();

                String[] efefv = uEFV.split("\\|\\|");

                ea.setDesignElementID(des[i]);
                ea.setEfName(efefv[0]);
                ea.setEfvName(efefv.length == 2 ? efefv[1] : "");
                ea.setPValAdjusted(pval_);
                ea.setTStatistic(tstat_);
                ea.setExperimentID(getExperimentId());

                eas.add(ea);
            }

            if (genes[i] != 0)  // skip geneid = 0
                geas.put(genes[i], eas);
        }

        return geas;
    }


    /**
     * @param deIndex
     * @return ExpressionAnalysis with the lowest pValue across all ef-efvs in design element index: deIndex
     * @throws IOException
     */
    public ExpressionAnalysis getBestExpressionAnalysisFromDEIndex(Integer deIndex) throws IOException {

        ExpressionAnalysis bestEA = null;
        Float bestPVal = null;
        Float bestTStat = null;
        final String[] uEFVs = getUniqueFactorValues();

        final long[] des = getDesignElements();
        final float[] pval = getPValuesForDesignElement(deIndex);
        final float[] tstat = getTStatisticsForDesignElement(deIndex);

        for (int j = 0; j < uEFVs.length; j++) {
            if ((pval[j] > 0 || tstat[j] > 0) // exclude expressions with pVal == 0 && tstat = 0
                    && pval[j] <= 1 // NA pValues in NetCDF are represented by a special number, (much) larger than 1  - exclude these also
                    && (bestPVal == null || bestPVal > pval[j] ||
                    // Note that if both pValues are 0 then the better one is the one with the higher absolute pValue
                    (bestPVal == 0 && pval[j] == 0 && Math.abs(bestTStat) < Math.abs(tstat[j])))
                    ) {
                bestPVal = pval[j];
                bestTStat = tstat[j];
                bestEA = new ExpressionAnalysis();
                String[] efefv = uEFVs[j].split("\\|\\|");
                bestEA.setDesignElementID(des[deIndex]);
                bestEA.setEfName(efefv[0]);
                bestEA.setEfvName(efefv.length == 2 ? efefv[1] : "");
                bestEA.setPValAdjusted(pval[j]);
                bestEA.setTStatistic(tstat[j]);
                bestEA.setExperimentID(getExperimentId());
                bestEA.setProxyId(getId());
                bestEA.setDesignElementIndex(deIndex);
            }
        }
        return bestEA;
    }

    public long getExperimentId() {
        return experimentId;
    }

    public void setExperimentId(long experimentId) {
        this.experimentId = experimentId;
    }


    /**
     * For each gene in the keySet() of geneIdsToDEIndexes, and each efv in uEF_EFVs, find
     * the design element with a minPvalue and store it as an ExpressionAnalysis object in
     * geneIdsToEfToEfvToEA if the minPvalus found in this proxy is better than the one already in
     * geneIdsToEfToEfvToEA. This method cane be called for multiple proxies in turn, accumulating
     * data with the best pValues across all proxies.
     *
     * @param geneIdsToDEIndexes geneId -> list of desinglemenet indexes containing data for that gene
     * @return geneId -> ef -> efv -> ea of best pValue for this geneid-ef-efv combination
     *         Note that ea contains proxyId and designElement index from which it came, so that
     *         the actual expression values can be easily retrieved later
     * @throws IOException
     */
    public Map<Long, Map<String, Map<String, ExpressionAnalysis>>> getExpressionAnalysesForDesignElementIndexes(
            final Map<Long, List<Integer>> geneIdsToDEIndexes
    ) throws IOException {

        Map<Long, Map<String, Map<String, ExpressionAnalysis>>> geneIdsToEfToEfvToEA = new HashMap<Long, Map<String, Map<String, ExpressionAnalysis>>>();
        ExpressionAnalysisHelper eaHelper = createExpressionAnalysisHelper();

        for (Long geneId : geneIdsToDEIndexes.keySet()) {

            if (geneId == 0) continue; // skip geneid = 0

            if (!geneIdsToEfToEfvToEA.containsKey(geneId)) {
                Map<String, Map<String, ExpressionAnalysis>> efToEfvToEA = new HashMap<String, Map<String, ExpressionAnalysis>>();
                geneIdsToEfToEfvToEA.put(geneId, efToEfvToEA);
            }
            for (Integer deIndex : geneIdsToDEIndexes.get(geneId)) {

                List<ExpressionAnalysis> eaList = (eaHelper.getByDesignElementIndex(deIndex)).getAll();
                for (ExpressionAnalysis ea : eaList) {
                    String ef = ea.getEfName();
                    String efv = ea.getEfvName();

                    if (geneIdsToEfToEfvToEA.get(geneId).get(ef) == null) {
                        Map<String, ExpressionAnalysis> efvToEA = new HashMap<String, ExpressionAnalysis>();
                        geneIdsToEfToEfvToEA.get(geneId).put(ef, efvToEA);
                    }

                    ExpressionAnalysis prevBestPValueEA =
                            geneIdsToEfToEfvToEA.get(geneId).get(ef).get(efv);

                    if ((ea.getPValAdjusted() > 0 || ea.getTStatistic() > 0) // exclude expressions with pVal == 0 && tstat = 0
                            && ea.getPValAdjusted() <= 1 // NA pValues in NetCDF are represented by a special number, (much) larger than 1  - exclude these also
                            && (prevBestPValueEA == null || prevBestPValueEA.getPValAdjusted() > ea.getPValAdjusted() ||
                            // Note that if both pValues are 0 then the better one is the one with the higher absolute pValue
                            (prevBestPValueEA.getPValAdjusted() == 0 && ea.getPValAdjusted() == 0 && Math.abs(prevBestPValueEA.getTStatistic()) < Math.abs(ea.getTStatistic()))
                    )) {
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
            return list(new Predicate<Integer>() {
                public boolean apply(Integer o) {
                    return true;
                }
            });
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
                String[] arr = uEFV.split("\\|\\|");
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
                    if (uEF_EFV[0].equals(efName)) {
                        return efvName == null || uEF_EFV[1].equals(efvName);
                    }
                    return false;
                }
            };
        }
    }
}
