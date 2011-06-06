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

package ae3.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import uk.ac.ebi.gxa.requesthandlers.base.restutil.RestOut;
import uk.ac.ebi.gxa.utils.EfvTree;
import uk.ac.ebi.microarray.atlas.model.Experiment;
import uk.ac.ebi.gxa.netcdf.reader.AtlasNetCDFDAO;

import uk.ac.ebi.gxa.netcdf.reader.NetCDFProxy;

import javax.annotation.Nullable;
import java.util.*;
import java.io.*;

import uk.ac.ebi.gxa.web.filter.ResourceWatchdogFilter;
import uk.ac.ebi.gxa.exceptions.LogUtil;
import uk.ac.ebi.gxa.utils.EscapeUtil;

/**
 * NetCDF experiment data representation class
 *
 * @author pashky
 */
public class ExperimentalData implements Closeable {
    private static final Logger log = LoggerFactory.getLogger(ExperimentalData.class);

    /**
     * Load experimental data using default path
     *
     * @param atlasNetCDFDAO      netCDF DAO
     * @param experiment data accession
     * @return either constructed object or null, if no data files was found for this accession
     * @throws IOException if i/o error occurs
     */
    public static ExperimentalData loadExperiment(AtlasNetCDFDAO atlasNetCDFDAO, Experiment experiment) throws IOException {
        ExperimentalData experimentalData = null;
        for (File file : atlasNetCDFDAO.listNetCDFs(experiment)) {
            if (experimentalData == null)
                experimentalData = new ExperimentalData(experiment);

            log.info("loadArrayDesign from " + file.getAbsolutePath());

            final NetCDFProxy proxy = new NetCDFProxy(file);
            experimentalData.addProxy(proxy);
        }
        return experimentalData;
    }

    private static String normalized(String name, String prefix) {
        if (name.startsWith(prefix)) {
            name = name.substring(prefix.length());
        }
        return EscapeUtil.encode(name);
    }

    private void loadArrayDesign(final NetCDFProxy proxy) throws IOException {
        final ArrayDesign arrayDesign = new ArrayDesign(proxy.getArrayDesignAccession());
        proxies.put(arrayDesign, proxy);
        
        final String[] assayAccessions = proxy.getAssayAccessions();
        final String[] sampleAccessions = proxy.getSampleAccessions();

        final Map<String, List<String>> efvs = new HashMap<String, List<String>>();
        
        final String[] factors = proxy.getFactors();
        final String[] factorsAndCharacteristics;
        {
            final String[] tmp = proxy.getFactorsAndCharacteristics();
            // Ensure backwards compatibility
            factorsAndCharacteristics = tmp.length != 0 ? tmp : factors;
        }
        
        for (String ef : factors) {
            ef = normalized(ef, "ba_");
            final List<String> efvList = new ArrayList<String>(assayAccessions.length);
            efvs.put(ef, efvList);
            for (String value : proxy.getFactorValues(ef)) {
                efvList.add(value);
            }
        }
        
        final Map<String, List<String>> scvs = new HashMap<String, List<String>>();
        for (String characteristic : proxy.getCharacteristics()) {
            characteristic = normalized(characteristic, "bs_");
            final List<String> valuesList = new ArrayList<String>(sampleAccessions.length);
            scvs.put(characteristic, valuesList);
            for (String value : proxy.getCharacteristicValues(characteristic)) {
                valuesList.add(value);
            }
        }
        
        final Sample[] samples = new Sample[sampleAccessions.length];
        for (int i = 0; i < sampleAccessions.length; ++i) {
            Map<String, String> scvMap = new HashMap<String, String>();
            for (Map.Entry<String, List<String>> sc : scvs.entrySet()) {
                scvMap.put(sc.getKey(), sc.getValue().get(i));
            }
            samples[i] = addSample(scvMap, sampleAccessions[i]);
        }
        
        final Assay[] assays = new Assay[assayAccessions.length];
        for (int i = 0; i < assayAccessions.length; ++i) {
            final Map<String, String> efvMap = new HashMap<String, String>();
            for (Map.Entry<String, List<String>> ef : efvs.entrySet()) {
                efvMap.put(ef.getKey(), ef.getValue().get(i));
            }
            assays[i] = addAssay(getExperiment().getAssay(assayAccessions[i]), efvMap, i);
        }
        
        final List<String> uvals = proxy.getUniqueValues();
        final int[] uvalIndexes = proxy.getUniqueValueIndexes();
        
        /*
         * Lazy loading of data, matrix is read only for required elements
         */
        if (uvals.size() > 0 && uvalIndexes.length > 0) {
            setExpressionStats(arrayDesign, new ExpressionStats() {
                private final EfvTree<Integer> efvTree = new EfvTree<Integer>();
        
                private EfvTree<Stat> lastData;
                long lastDesignElement = -1;
        
                {
                    int index = 0;
                    int k = 0;
                    for (int propIndex = 0; propIndex < factorsAndCharacteristics.length && index < uvalIndexes.length; ++propIndex) {
                        final String prop = normalized(factorsAndCharacteristics[propIndex], "ba_");
                        int valNum = uvalIndexes[index];
                        for (; valNum > 0 && k < uvals.size(); --valNum) {
                            final String efv = uvals.get(k).replaceAll("^.*" + NetCDFProxy.NCDF_PROP_VAL_SEP_REGEX, "");
                            efvTree.put(prop, efv, k++);
                        }
                    }
                }
        
                public EfvTree<Stat> getExpressionStats(int designElementId) {
                    if (lastData != null && designElementId == lastDesignElement)
                        return lastData;
        
                    try {
                        final float[] pvals = proxy.getPValuesForDesignElement(designElementId);
                        final float[] tstats = proxy.getTStatisticsForDesignElement(designElementId);
                        final EfvTree<Stat> result = new EfvTree<Stat>();
                        for (EfvTree.EfEfv<Integer> efefv : efvTree.getNameSortedList()) {
                            float pvalue = pvals[efefv.getPayload()];
                            float tstat = tstats[efefv.getPayload()];
                            if (tstat > 1e-8 || tstat < -1e-8) {
                                result.put(efefv.getEf(), efefv.getEfv(), new Stat(tstat, pvalue));
                            }
                        }
                        lastDesignElement = designElementId;
                        lastData = result;
                        return result;
                    } catch (IOException e) {
                        throw LogUtil.createUnexpected("Exception during pvalue/tstat load", e);
                    } catch (ArrayIndexOutOfBoundsException e) {
                        throw LogUtil.createUnexpected("Exception during pvalue/tstat load", e);
                    }
                }
            });
        }
        
        final String[] designElementAccessions = proxy.getDesignElementAccessions();
        if (designElementAccessions != null) {
            setDesignElementAccessions(arrayDesign, new DesignElementAccessions() {
                public String getDesignElementAccession(final int designElementIndex) {
                    try {
                        return designElementAccessions[designElementIndex];
                    } catch (ArrayIndexOutOfBoundsException e) {
                        throw LogUtil.createUnexpected("Exception reading design element accessions", e);
                    }
                }
            });
        }
        
        final int[][] samplesToAssays = proxy.getSamplesToAssays();
        for (int sampleI = 0; sampleI < sampleAccessions.length; ++sampleI) {
            for (int assayI = 0; assayI < assayAccessions.length; ++assayI) {
                if (samplesToAssays[sampleI][assayI] > 0) {
                    addSampleAssayMapping(samples[sampleI], assays[assayI]);
                }
            }
        }
    }

    private final Experiment experiment;
    private final List<Sample> samples = new ArrayList<Sample>();
    private final List<Assay> assays = new ArrayList<Assay>();

    private final Map<ArrayDesign, NetCDFProxy> proxies = new HashMap<ArrayDesign, NetCDFProxy>();

    private final Map<ArrayDesign, ExpressionMatrix> expressionMatrix = new HashMap<ArrayDesign, ExpressionMatrix>();
    private final Map<ArrayDesign, DesignElementAccessions> designElementAccessions = new HashMap<ArrayDesign, DesignElementAccessions>();
    private final Map<ArrayDesign, Map<Long, int[]>> geneIdMap = new HashMap<ArrayDesign, Map<Long, int[]>>();

    private Set<ArrayDesign> arrayDesigns = new HashSet<ArrayDesign>();
    private Set<String> experimentalFactors = new HashSet<String>();
    private Set<String> sampleCharacteristics = new HashSet<String>();
    private Map<ArrayDesign, ExpressionStats> expressionStats = new HashMap<ArrayDesign, ExpressionStats>();

    /**
     * Empty class from the start, one should fill it with addXX and setXX methods
     * @param experiment
     */
    public ExperimentalData(Experiment experiment) {
        this.experiment = experiment;
    }

    public void addProxy(NetCDFProxy proxy) throws IOException {
        ResourceWatchdogFilter.register(proxy);
        loadArrayDesign(proxy);
    }

    public void close() {
        for (NetCDFProxy p : proxies.values()) {
            try {
                p.close();
            } catch (IOException e) {
            }
        }
    }

    public Experiment getExperiment() {
        return experiment;
    }

    /**
     * Add sample to experiment
     *
     * @param scvMap      map of sample charactristic values for sample
     * @param accession   sample accession
     * @return created sample reference
     */
    public Sample addSample(Map<String, String> scvMap, String accession) {
        for (Sample s : samples)
            if (accession.equals(s.getAccession()))
                return s;
        sampleCharacteristics.addAll(scvMap.keySet());
        final Sample sample = new Sample(samples.size(), scvMap, accession);
        samples.add(sample);
        return sample;
    }

    /**
     * Add assay to experiment
     *
     *
     *
     *
     *
     * @param efvMap           factor values map for all experimental factors
     * @param positionInMatrix assay's column position in expression matrix
     * @return created assay reference
     */
    public Assay addAssay(uk.ac.ebi.microarray.atlas.model.Assay dbAssay, Map<String, String> efvMap, int positionInMatrix) {
        ArrayDesign arrayDesign = new ArrayDesign(dbAssay.getArrayDesign());
        arrayDesigns.add(arrayDesign);
        experimentalFactors.addAll(efvMap.keySet());

        final Assay assay = new Assay(dbAssay, assays.size(), arrayDesign, positionInMatrix);
        assays.add(assay);
        return assay;
    }

    /**
     * Get expression matrix for array design
     *
     * @param arrayDesign array design, this matrix applies to
     * @param matrix      object, implementing expression matrix interface
     */
    private ExpressionMatrix getExpressionMatrix(ArrayDesign arrayDesign) {
        ExpressionMatrix matrix = expressionMatrix.get(arrayDesign);
        if (matrix == null) {
            final NetCDFProxy proxy = proxies.get(arrayDesign);
            if (proxy == null) {
                return null;
            }

            /*
             * Lazy loading of data, matrix is read only for required elements
             */
            matrix = new ExpressionMatrix() {
                int lastDesignElement = -1;
                float[] lastData = null;
        
                public float getExpression(int designElementIndex, int assayId) {
                    try {
                        if (lastData == null || lastDesignElement != designElementIndex) {
                            lastDesignElement = designElementIndex;
                            lastData = proxy.getExpressionDataForDesignElementAtIndex(designElementIndex);
                        }
                        return lastData[assayId];
                    } catch (IOException e) {
                        throw LogUtil.createUnexpected("Exception during matrix load", e);
                    } catch (ArrayIndexOutOfBoundsException e) {
                        throw LogUtil.createUnexpected("Exception during matrix load", e);
                    }
                }
            };
            expressionMatrix.put(arrayDesign, matrix);
        }
        return matrix;
    }

    /**
     * Set expression statistics object for array design
     *
     * @param arrayDesign array design, this data apply to
     * @param stats       object, implementing expression statistics interface
     */
    public void setExpressionStats(ArrayDesign arrayDesign, ExpressionStats stats) {
        this.expressionStats.put(arrayDesign, stats);
    }

    /**
     * Add mapping between assay and sample
     *
     * @param sample sample to link with specified assay
     * @param assay  assay to link with specified sample
     */
    public void addSampleAssayMapping(Sample sample, Assay assay) {
        assay.addSample(sample);
        sample.addAssay(assay);
    }

    /**
     * Get expression value
     *
     * @param assay              assay, for which show the value
     * @param designElementIndex design element index
     * @return expression value
     */
    public float getExpression(Assay assay, int designElementIndex) {
        final ExpressionMatrix matrix = getExpressionMatrix(assay.getArrayDesign());
        return matrix != null
            ? matrix.getExpression(designElementIndex, assay.getPositionInMatrix()) : Float.NaN;
    }

    /**
     * Get expression statistics map ({@link uk.ac.ebi.gxa.utils.EfvTree}, where payload is {@link ae3.model.ExpressionStats.Stat} structures
     *
     * @param ad            array design
     * @param designElement design element id
     * @return map of statstics
     */
    public EfvTree<ExpressionStats.Stat> getExpressionStats(ArrayDesign ad, int designElement) {
        ExpressionStats stats = expressionStats.get(ad);
        return stats != null ? stats.getExpressionStats(designElement) : new EfvTree<ExpressionStats.Stat>();
    }

    /**
     * Get array of design element id's corresponding to gene on specified array design
     *
     * @param arrayDesign array design
     * @param geneId      gene id (the Atlas/DW one)
     * @return array of design element id's to be used in expression/statistics retrieval functions
     */
    public int[] getDesignElements(ArrayDesign arrayDesign, long geneId) {
        Map<Long, int[]> geneMap = geneIdMap.get(arrayDesign);
        if (geneMap == null) {
            final NetCDFProxy proxy = proxies.get(arrayDesign);
            if (proxy == null) {
                return new int[0];
            }

            final long[] geneIds;
            try {
                geneIds = proxy.getGenes();
            } catch (IOException e) {
                return new int[0];
            }
            geneMap = new HashMap<Long, int[]>();
            for (int i = 0; i < geneIds.length; ++i) {
                int[] olda = geneMap.get(geneIds[i]);
                int[] newa;
                if (olda != null) {
                    newa = new int[olda.length + 1];
                    System.arraycopy(olda, 0, newa, 0, olda.length);
                } else {
                    newa = new int[1];
                }
                newa[newa.length - 1] = i;
                geneMap.put(geneIds[i], newa);
            }
            geneIdMap.put(arrayDesign, geneMap);
        }
        return geneMap.get(geneId);
    }

    /**
     * Get list of experiment's samples
     *
     * @return list of all samples
     */
    @RestOut(name = "samples")
    public List<Sample> getSamples() {
        return samples;
    }

    /**
     * Get list of experiment's assays
     *
     * @return list of assays
     */
    @RestOut(name = "assays")
    public List<Assay> getAssays() {
        return assays;
    }

    /**
     * Get iterable of assays for specified array design only
     *
     * @param arrayDesign array design
     * @return iterable of assays
     */
    public Iterable<Assay> getAssays(final ArrayDesign arrayDesign) {
        return Collections2.filter(
                assays,
                new Predicate<Assay>() {
                    public boolean apply(@Nullable Assay input) {
                        return input != null && arrayDesign.equals(input.getArrayDesign());
                    }
                });
    }

    /**
     * Get set of array designs for this experiment
     *
     * @return set of array designs
     */
    @RestOut(name = "arrayDesigns")
    public Set<ArrayDesign> getArrayDesigns() {
        return arrayDesigns;
    }

    /**
     * Get set of experimental factors for this experiment
     *
     * @return set of experimental factors
     */
    @RestOut(name = "experimentalFactors")
    public Set<String> getExperimentalFactors() {
        return experimentalFactors;
    }

    /**
     * Get set of sample characteristics
     *
     * @return set of sample characteristics
     */
    @RestOut(name = "sampleCharacteristics")
    public Set<String> getSampleCharacteristics() {
        return sampleCharacteristics;
    }

    public String getDesignElementAccession(final ArrayDesign arrayDesign, final int designElementId) {
        return designElementAccessions.get(arrayDesign).getDesignElementAccession(designElementId);
    }

    /**
     * Set design element accessions for array design
     *
     * @param arrayDesign             array design, this matrix applies to
     * @param designElementAccessions object, implementing expression matrix interface
     */
    public void setDesignElementAccessions(ArrayDesign arrayDesign, DesignElementAccessions designElementAccessions) {
        this.designElementAccessions.put(arrayDesign, designElementAccessions);
    }
}
