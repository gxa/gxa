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

import uk.ac.ebi.gxa.netcdf.reader.NetCDFProxy;
import uk.ac.ebi.gxa.utils.EfvTree;
import uk.ac.ebi.gxa.utils.EscapeUtil;
import uk.ac.ebi.microarray.atlas.model.Experiment;

import java.io.IOException;
import java.util.*;

import static uk.ac.ebi.gxa.exceptions.LogUtil.createUnexpected;

/**
 * NetCDF file reader. Now it is rewritten in terms of NetCDFProxy
 * the code should be moved into ExperimentalData class
 */
class NetCDFReader {
    /**
     * Load one array design from file
     *
     * @param file            file to load from
     * @param data            experimental data object, to add data to
     * @throws IOException    if i/o error occurs
     */
    static void loadArrayDesign(final NetCDFProxy proxy, ExperimentalData data) throws IOException {
        final ArrayDesign arrayDesign = new ArrayDesign(proxy.getArrayDesignAccession());
        
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
            if (ef.startsWith("ba_")) {
                ef = ef.substring("ba_".length());
            }
            ef = EscapeUtil.encode(ef);
            final List<String> efvList = new ArrayList<String>(assayAccessions.length);
            efvs.put(ef, efvList);
            for (String value : proxy.getFactorValues(ef)) {
                efvList.add(value);
            }
        }
        
        final Map<String, List<String>> scvs = new HashMap<String, List<String>>();
        for (String characteristic : proxy.getCharacteristics()) {
            if (characteristic.startsWith("bs_")) {
                characteristic = characteristic.substring("bs_".length());
            }
            characteristic = EscapeUtil.encode(characteristic);
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
            samples[i] = data.addSample(scvMap, sampleAccessions[i]);
        }
        
        final Assay[] assays = new Assay[assayAccessions.length];
        for (int i = 0; i < assayAccessions.length; ++i) {
            final Map<String, String> efvMap = new HashMap<String, String>();
            for (Map.Entry<String, List<String>> ef : efvs.entrySet()) {
                efvMap.put(ef.getKey(), ef.getValue().get(i));
            }
            assays[i] = data.addAssay(data.getExperiment().getAssay(assayAccessions[i]), efvMap, i);
        }
        
        /*
         * Lazy loading of data, matrix is read only for required elements
         */
        data.setExpressionMatrix(arrayDesign, new ExpressionMatrix() {
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
                    throw createUnexpected("Exception during matrix load", e);
                } catch (ArrayIndexOutOfBoundsException e) {
                    throw createUnexpected("Exception during matrix load", e);
                }
            }
        });
        
        final List<String> uvals = proxy.getUniqueValues();
        final int[] uvalIndexes = proxy.getUniqueValueIndexes();
        
        /*
         * Lazy loading of data, matrix is read only for required elements
         */
        if (uvals.size() > 0 && uvalIndexes.length > 0) {
            data.setExpressionStats(arrayDesign, new ExpressionStats() {
                private final EfvTree<Integer> efvTree = new EfvTree<Integer>();
        
                private EfvTree<Stat> lastData;
                long lastDesignElement = -1;
        
                {
                    int index = 0;
                    int k = 0;
                    for (int propIndex = 0; propIndex < factorsAndCharacteristics.length && index < uvalIndexes.length; ++propIndex) {
                        String propStr = factorsAndCharacteristics[propIndex];
                        String prop = propStr.startsWith("ba_") ? propStr.substring("ba_".length()) : propStr;
                        prop = EscapeUtil.encode(prop);
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
                        throw createUnexpected("Exception during pvalue/tstat load", e);
                    } catch (ArrayIndexOutOfBoundsException e) {
                        throw createUnexpected("Exception during pvalue/tstat load", e);
                    }
                }
            });
        }
        
        final String[] designElementAccessions = proxy.getDesignElementAccessions();
        if (designElementAccessions != null) {
            data.setDesignElementAccessions(arrayDesign, new DesignElementAccessions() {
                public String getDesignElementAccession(final int designElementIndex) {
                    try {
                        return designElementAccessions[designElementIndex];
                    } catch (ArrayIndexOutOfBoundsException e) {
                        throw createUnexpected("Exception reading design element accessions", e);
                    }
                }
            });
        }
        
        final int[][] samplesToAssays = proxy.getSamplesToAssays();
        for (int sampleI = 0; sampleI < sampleAccessions.length; ++sampleI) {
            for (int assayI = 0; assayI < assayAccessions.length; ++assayI) {
                if (samplesToAssays[sampleI][assayI] > 0) {
                    data.addSampleAssayMapping(samples[sampleI], assays[assayI]);
                }
            }
        }
        
        final long[] geneIds = proxy.getGenes();
        
        data.setGeneIds(arrayDesign, geneIds);
    }
}
