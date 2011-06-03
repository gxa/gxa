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

package ae3.dao;

import ae3.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.ma2.ArrayChar;
import ucar.ma2.IndexIterator;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import uk.ac.ebi.gxa.netcdf.reader.AtlasNetCDFDAO;
import uk.ac.ebi.gxa.netcdf.reader.NetCDFProxy;
import uk.ac.ebi.gxa.utils.EfvTree;
import uk.ac.ebi.gxa.utils.EscapeUtil;
import uk.ac.ebi.gxa.web.filter.ResourceWatchdogFilter;
import uk.ac.ebi.microarray.atlas.model.Experiment;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static uk.ac.ebi.gxa.exceptions.LogUtil.createUnexpected;

/**
 * NetCDF file reader. The first one. Is used only in API data display code and should be replaced with
 * newer one once someone has time to do it.
 *
 * @author pashky
 */
public class NetCDFReader {
    private static final Logger log = LoggerFactory.getLogger(NetCDFReader.class);

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
            loadArrayDesign(file, experimentalData);
        }
        return experimentalData;
    }

    /**
     * Load one array design from file
     *
     * @param file            file to load from
     * @param data            experimental data object, to add data to
     * @throws IOException    if i/o error occurs
     */
    private static void loadArrayDesign(File file, ExperimentalData data) throws IOException {
        log.info("loadArrayDesign from " + file.getAbsolutePath());

        final NetCDFProxy proxy = new NetCDFProxy(file);
        data.addProxy(proxy);

        final NetcdfFile ncfile = NetcdfFile.open(file.getAbsolutePath());
        ResourceWatchdogFilter.register(new Closeable() {
            public void close() throws IOException {
                ncfile.close();
            }
        });
        
        final Variable varEFV = ncfile.findVariable("EFV");
        final Variable varEF = ncfile.findVariable("EF");
        Variable varEFSC = ncfile.findVariable("EFSC");
        if (varEFSC == null) {
            // Ensure backwards compatibility
            varEFSC = varEF;
        }
        
        final ArrayDesign arrayDesign = new ArrayDesign(proxy.getArrayDesignAccession());
        
        final String[] assayAccessions = proxy.getAssayAccessions();
        final String[] sampleAccessions = proxy.getSampleAccessions();

        final Map<String, List<String>> efvs = new HashMap<String, List<String>>();
        
        final ArrayChar efscData = varEFSC != null ? (ArrayChar) varEFSC.read() : new ArrayChar.D2(0, 0);
        final ArrayChar efData = varEF != null ? (ArrayChar) varEF.read() : new ArrayChar.D2(0, 0);
        
        if (varEF != null && varEFV != null) {
            ArrayChar.StringIterator efvi = ((ArrayChar) varEFV.read()).getStringIterator();
            for (ArrayChar.StringIterator i = efData.getStringIterator(); i.hasNext();) {
                String efStr = i.next();
                String ef = efStr.startsWith("ba_") ? efStr.substring("ba_".length()) : efStr;
                ef = EscapeUtil.encode(ef);
                List<String> efvList = new ArrayList<String>(assayAccessions.length);
                efvs.put(ef, efvList);
                for (int j = 0; j < assayAccessions.length; ++j) {
                    efvi.hasNext();
                    efvList.add(efvi.next());
                }
                efvs.put(ef, efvList);
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
        
        Variable varUVAL = ncfile.findVariable("uVAL");
        Variable varUVALNUM = ncfile.findVariable("uVALnum");
        if (varUVAL == null) {
            // Ensure backwards compatibility
            varUVAL = ncfile.findVariable("uEFV");
            varUVALNUM = ncfile.findVariable("uEFVnum");
            log.error("ncdf " + file.getAbsolutePath() + " is out of date - please update it and then recompute its analytics via Atlas administration interface");
        }
        
        final Variable varUValue = varUVAL;
        final Variable varUValueNum = varUVALNUM;
        final Variable varPVAL = ncfile.findVariable("PVAL");
        final Variable varTSTAT = ncfile.findVariable("TSTAT");
        
        /*
         * Lazy loading of data, matrix is read only for required elements
         */
        if (varUValue != null && varUValueNum != null && varPVAL != null && varTSTAT != null) {
            data.setExpressionStats(arrayDesign, new ExpressionStats() {
                private final EfvTree<Integer> efvTree = new EfvTree<Integer>();
        
                private EfvTree<Stat> lastData;
                long lastDesignElement = -1;
        
                {
                    int k = 0;
                    ArrayChar.StringIterator efvi = ((ArrayChar) varUValue.read()).getStringIterator();
                    IndexIterator valNumi = varUValueNum.read().getIndexIterator();
                    for (ArrayChar.StringIterator propi = efscData.getStringIterator(); propi.hasNext() && valNumi.hasNext();) {
                        String propStr = propi.next();
                        String prop = propStr.startsWith("ba_") ? propStr.substring("ba_".length()) : propStr;
                        prop = EscapeUtil.encode(prop);
                        int valNum = valNumi.getIntNext();
                        for (; valNum > 0 && efvi.hasNext(); --valNum) {
                            String efv = efvi.next().replaceAll("^.*" + NetCDFProxy.NCDF_PROP_VAL_SEP_REGEX, "");
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
