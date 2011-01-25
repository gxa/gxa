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
import ucar.ma2.ArrayChar;
import ucar.ma2.IndexIterator;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import uk.ac.ebi.gxa.netcdf.reader.AtlasNetCDFDAO;
import uk.ac.ebi.gxa.utils.EfvTree;
import uk.ac.ebi.gxa.utils.EscapeUtil;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * NetCDF file reader. The first one. Is used only in API experiment display code and should be replaced with
 * newer one once someone has time to do it.
 *
 * @author pashky
 */
public class NetCDFReader {
    /**
     * Load experimental data using default path
     * @param atlasNetCDFDAO
     * @param experimentAccession experiment accession
     * @return either constructed object or null, if no data files was found for this accession
     * @throws IOException if i/o error occurs
     */
    public static ExperimentalData loadExperiment(AtlasNetCDFDAO atlasNetCDFDAO, String experimentAccession) throws IOException {
        ExperimentalData experiment = null;
        for(File file : atlasNetCDFDAO.listNetCDFs(experimentAccession)) {
            if(experiment == null)
                experiment = new ExperimentalData();
            loadArrayDesign(file.getAbsolutePath(), experiment);
        }
        return experiment;
    }

    /**
     * Load one array design from file
     * @param filename file name to load from
     * @param experiment experimental data object, to add data to
     * @throws IOException if i/o error occurs
     */
    private static void loadArrayDesign(String filename, ExperimentalData experiment) throws IOException {
        NetcdfFile ncfile = null;
        try {
            ncfile = NetcdfFile.open(filename);
            final Variable varBDC = ncfile.findVariable("BDC");
            final Variable varGN = ncfile.findVariable("GN");
            final Variable varEFV = ncfile.findVariable("EFV");
            final Variable varEF = ncfile.findVariable("EF");
            final Variable varSC = ncfile.findVariable("SC");
            final Variable varSCV = ncfile.findVariable("SCV");
            final Variable varBS2AS = ncfile.findVariable("BS2AS");
            final Variable varBS = ncfile.findVariable("BS");

            final String arrayDesignAccession = ncfile.findGlobalAttributeIgnoreCase("ADaccession").getStringValue();
            final ArrayDesign arrayDesign = new ArrayDesign(arrayDesignAccession);

            final int numSamples = varBS.getDimension(0).getLength();
            final int numAssays = varEFV != null ? varEFV.getDimension(1).getLength() : 0;

            final Map<String,List<String>> efvs = new HashMap<String,List<String>>();

            final ArrayChar efData = varEF != null ? (ArrayChar) varEF.read() : new ArrayChar.D2(0,0);

            if (varEF != null && varEFV != null) {
                ArrayChar.StringIterator efvi = ((ArrayChar) varEFV.read()).getStringIterator();
                for (ArrayChar.StringIterator i = efData.getStringIterator(); i.hasNext();) {
                    String efStr = i.next();
                    String ef = efStr.startsWith("ba_") ? efStr.substring("ba_".length()) : efStr;
                    ef = EscapeUtil.encode(ef);
                    List<String> efvList = new ArrayList<String>(numAssays);
                    efvs.put(ef, efvList);
                    for (int j = 0; j < numAssays; ++j) {
                        efvi.hasNext();
                        efvList.add(EscapeUtil.encode(efvi.next()));
                    }
                    efvs.put(ef, efvList);
                }
            }

            final Map<String,List<String>> scvs = new HashMap<String,List<String>>();
            if(varSCV != null && varSC != null) {

                ArrayChar.StringIterator scvi = ((ArrayChar)varSCV.read()).getStringIterator();
                for(ArrayChar.StringIterator i = ((ArrayChar)varSC.read()).getStringIterator(); i.hasNext(); ) {
                    String scStr = i.next();
                    String sc = scStr.startsWith("bs_") ? scStr.substring("bs_".length()) : scStr;
                    sc = EscapeUtil.encode(sc);
                    List<String> scvList = new ArrayList<String>(numSamples);
                    scvs.put(sc, scvList);
                    for(int j = 0; j < numSamples; ++j) {
                        scvi.hasNext();
                        scvList.add(EscapeUtil.encode(scvi.next()));
                    }
                }
            }

            Sample[] samples = new Sample[numSamples];

            long[] sampleIds = (long[])varBS.read().get1DJavaArray(long.class);
            for(int i = 0; i < numSamples; ++i) {
                Map<String,String> scvMap = new HashMap<String,String>();
                for(String sc : scvs.keySet())
                    scvMap.put(sc, scvs.get(sc).get(i));
                samples[i] = experiment.addSample(scvMap, sampleIds[i], i, numAssays, arrayDesignAccession);
            }

            Assay[] assays = new Assay[numAssays];

            for(int i = 0; i < numAssays; ++i) {
                Map<String,String> efvMap = new HashMap<String,String>();
                for(String ef : efvs.keySet())
                    efvMap.put(ef, efvs.get(ef).get(i));
                assays[i] = experiment.addAssay(arrayDesign, efvMap, i, numAssays, arrayDesignAccession);
            }

            /*
                * Lazy loading of data, matrix is read only for required elements
                */
            experiment.setExpressionMatrix(arrayDesign, new ExpressionMatrix() {
                int lastDesignElement = -1;
                float[] lastData = null;
                public float getExpression(int designElementIndex, int assayId) {
                    if(lastData != null && designElementIndex == lastDesignElement)
                        return lastData[assayId];

                    int[] shapeBDC = varBDC.getShape();
                    int[] originBDC = new int[varBDC.getRank()];
                    originBDC[0] = designElementIndex;
                    shapeBDC[0] = 1;
                    try {
                        lastData = (float[])varBDC.read(originBDC, shapeBDC).reduce().get1DJavaArray(float.class);
                    } catch(IOException e) {
                        throw new RuntimeException("Exception during matrix load", e);
                    } catch (InvalidRangeException e) {
                        throw new RuntimeException("Exception during matrix load", e);
                    }
                    lastDesignElement = designElementIndex;
                    return lastData[assayId];
                }
            });

            final Variable varUEFV = ncfile.findVariable("uEFV");
            final Variable varUEFVNUM = ncfile.findVariable("uEFVnum");
            final Variable varPVAL = ncfile.findVariable("PVAL");
            final Variable varTSTAT = ncfile.findVariable("TSTAT");

            /*
                * Lazy loading of data, matrix is read only for required elements
                */
            if(varUEFV != null && varUEFVNUM != null && varPVAL != null && varTSTAT != null)
                experiment.setExpressionStats(arrayDesign,  new ExpressionStats() {
                    private final EfvTree<Integer> efvTree = new EfvTree<Integer>();

                    private EfvTree<Stat> lastData;
                    long lastDesignElement = -1;

                    {
                        int k = 0;
                        ArrayChar.StringIterator efvi = ((ArrayChar)varUEFV.read()).getStringIterator();
                        IndexIterator efvNumi = varUEFVNUM.read().getIndexIterator();
                        for(ArrayChar.StringIterator efi = efData.getStringIterator(); efi.hasNext() && efvNumi.hasNext(); ) {
                            String efStr = efi.next();
                            String ef = efStr.startsWith("ba_") ? efStr.substring("ba_".length()) : efStr;
                            ef = EscapeUtil.encode(ef);
                            int efvNum = efvNumi.getIntNext();
                            for(; efvNum > 0 && efvi.hasNext(); --efvNum) {
                                String efv = efvi.next().replaceAll("^.*\\|\\|", "");
                                efvTree.put(ef, EscapeUtil.encode(efv), k++);
                            }
                        }
                    }

                    public EfvTree<Stat> getExpressionStats(int designElementId) {
                        if(lastData != null && designElementId == lastDesignElement)
                            return lastData;

                        try {
                            int[] shapeBDC = varPVAL.getShape();
                            int[] originBDC = new int[varPVAL.getRank()];
                            originBDC[0] = designElementId;
                            shapeBDC[0] = 1;
                            float[] pvals = (float[])varPVAL.read(originBDC, shapeBDC).reduce().get1DJavaArray(float.class);
                            float[] tstats = (float[])varTSTAT.read(originBDC, shapeBDC).reduce().get1DJavaArray(float.class);

                            EfvTree<Stat> result = new EfvTree<Stat>();
                            for(EfvTree.EfEfv<Integer> efefv : efvTree.getNameSortedList()) {
                                float pvalue = pvals[efefv.getPayload()];
                                float tstat = tstats[efefv.getPayload()];
                                if(tstat > 1e-8 || tstat < -1e-8)
                                    result.put(efefv.getEf(), efefv.getEfv(), new Stat(tstat, pvalue));
                            }
                            lastDesignElement = designElementId;
                            lastData = result;
                            return result;
                        } catch(IOException e) {
                            throw new RuntimeException("Exception during pvalue/tstat load", e);
                        } catch (InvalidRangeException e) {
                            throw new RuntimeException("Exception during pvalue/tstat load", e);
                        }
                    }
                });

            final Variable DEAcc = ncfile.findVariable("DEacc");
            if(DEAcc != null) {
                experiment.setDesignElementAccessions(arrayDesign,  new DesignElementAccessions() {
                    public String getDesignElementAccession(final int designElementIndex) {
                        try {
                            return ((ArrayChar.D2) DEAcc.read(String.valueOf(designElementIndex) + ",:")).getString(0);
                        } catch (IOException e) {
                            throw new RuntimeException("Exception reading design element accessions", e);
                        } catch (InvalidRangeException e) {
                            throw new RuntimeException("Exception reading design element accessions", e);
                        }
                    }
                });
            }

            IndexIterator mappingI = varBS2AS.read().getIndexIterator();
            for(int sampleI = 0; sampleI < numSamples; ++sampleI)
                for(int assayI = 0; assayI < numAssays; ++assayI)
                    if(mappingI.hasNext() && mappingI.getIntNext() > 0) {
                        experiment.addSampleAssayMapping(samples[sampleI], assays[assayI]);
                        experiment.addSampleAssayCompactMapping(sampleI, assayI, arrayDesignAccession);
                    }

            final long[] geneIds = (long[])varGN.read().get1DJavaArray(long.class);

            experiment.setGeneIds(arrayDesign, geneIds);
        } finally {
            ncfile.close();
        }
    }
}
