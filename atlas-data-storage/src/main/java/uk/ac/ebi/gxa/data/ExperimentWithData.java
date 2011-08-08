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

package uk.ac.ebi.gxa.data;

import java.util.*;

import java.io.IOException;

import com.google.common.io.Closeables;

import uk.ac.ebi.microarray.atlas.model.ArrayDesign;
import uk.ac.ebi.microarray.atlas.model.Experiment;
import uk.ac.ebi.microarray.atlas.model.Assay;
import uk.ac.ebi.microarray.atlas.model.Sample;
import uk.ac.ebi.microarray.atlas.model.ExpressionAnalysis;
import uk.ac.ebi.microarray.atlas.model.UpDownCondition;
import uk.ac.ebi.microarray.atlas.model.UpDownExpression;

import javax.annotation.Nullable;

public class ExperimentWithData {
    private final AtlasDataDAO atlasDataDAO;
    private final Experiment experiment;

    private final Map<ArrayDesign,NetCDFProxy> proxies = new HashMap<ArrayDesign,NetCDFProxy>();

    // cached data
    private final Map<ArrayDesign, String[]> designElementAccessions = new HashMap<ArrayDesign, String[]>();

    ExperimentWithData(AtlasDataDAO atlasDataDAO, Experiment experiment) {
        this.atlasDataDAO = atlasDataDAO;
        this.experiment = experiment;
    }

    public Experiment getExperiment() {
        return experiment;
    }

    // TODO: change access rights to private
    public NetCDFProxy getProxy(ArrayDesign arrayDesign) throws AtlasDataException {
        NetCDFProxy p = proxies.get(arrayDesign);
        if (p == null) {
            p = atlasDataDAO.getNetCDFDescriptor(experiment, arrayDesign).createProxy();
            proxies.put(arrayDesign, p);
        }
        return p;
    }

    /*
     * This method returns samples in the order they are stored in netcdf file.
     * While this order is important we have to use this method,
     * in future it would be replaced by Experiment method.
     */
    public List<Sample> getSamples(ArrayDesign arrayDesign) throws AtlasDataException {
        final String[] sampleAccessions;
        try {
            sampleAccessions = getProxy(arrayDesign).getSampleAccessions();
        } catch (IOException e) {
            throw new AtlasDataException(e);
        }
        final ArrayList<Sample> samples = new ArrayList<Sample>(sampleAccessions.length);
        for (String accession : sampleAccessions) {
            samples.add(getExperiment().getSample(accession));
        }
        return samples;
    }

    /*
     * This method returns assays in the order they are stored in netcdf file.
     * While this order is important we have to use this method,
     * in future it would be replaced by Experiment method.
     */
    public List<Assay> getAssays(ArrayDesign arrayDesign) throws AtlasDataException {
        final String[] assayAccessions;
        try {
            assayAccessions = getProxy(arrayDesign).getAssayAccessions();
        } catch (IOException e) {
            throw new AtlasDataException(e);
        }
        final ArrayList<Assay> assays = new ArrayList<Assay>(assayAccessions.length);
        for (String accession : assayAccessions) {
            assays.add(getExperiment().getAssay(accession));
        }
        return assays;
    }

    public int[][] getSamplesToAssays(ArrayDesign arrayDesign) throws AtlasDataException {
        try {
            return getProxy(arrayDesign).getSamplesToAssays();
        } catch (IOException e) {
            throw new AtlasDataException(e);
        }
    }

    public List<Integer> getSamplesForAssay(ArrayDesign arrayDesign, int iAssay) throws AtlasDataException {
        try {
            return getProxy(arrayDesign).getSamplesForAssay(iAssay);
        } catch (IOException e) {
            throw new AtlasDataException(e);
        }
    }

    public String[] getDesignElementAccessions(ArrayDesign arrayDesign) throws AtlasDataException {
        String[] array = designElementAccessions.get(arrayDesign);
        if (array == null) {
            try {
                array = getProxy(arrayDesign).getDesignElementAccessions();
                designElementAccessions.put(arrayDesign, array);
            } catch (IOException e) {
                throw new AtlasDataException(e);
            }
        }
        return array;
    }

    public long[] getGenes(ArrayDesign arrayDesign) throws AtlasDataException {
        try {
            return getProxy(arrayDesign).getGenes(); 
        } catch (IOException e) {
            throw new AtlasDataException(e);
        }
    }

    public List<KeyValuePair> getUniqueFactorValues(ArrayDesign arrayDesign) throws AtlasDataException {
        try {
            return getProxy(arrayDesign).getUniqueFactorValues(); 
        } catch (IOException e) {
            throw new AtlasDataException(e);
        }
    }

    public List<KeyValuePair> getUniqueValues(ArrayDesign arrayDesign) throws AtlasDataException {
        try {
            return getProxy(arrayDesign).getUniqueValues(); 
        } catch (IOException e) {
            throw new AtlasDataException(e);
        }
    }

    public String[] getFactors(ArrayDesign arrayDesign) throws AtlasDataException {
        try {
            return getProxy(arrayDesign).getFactors(); 
        } catch (IOException e) {
            throw new AtlasDataException(e);
        }
    }

    public String[] getCharacteristics(ArrayDesign arrayDesign) throws AtlasDataException {
        try {
            return getProxy(arrayDesign).getCharacteristics(); 
        } catch (IOException e) {
            throw new AtlasDataException(e);
        }
    }

    public String[] getCharacteristicValues(ArrayDesign arrayDesign, String characteristic) throws AtlasDataException {
        try {
            return getProxy(arrayDesign).getCharacteristicValues(characteristic); 
        } catch (IOException e) {
            throw new AtlasDataException(e);
        }
    }

    public String[][] getFactorValues(ArrayDesign arrayDesign) throws AtlasDataException {
        try {
            return getProxy(arrayDesign).getFactorValues(); 
        } catch (IOException e) {
            throw new AtlasDataException(e);
        }
    }

    public String[] getFactorValues(ArrayDesign arrayDesign, String factor) throws AtlasDataException {
        try {
            return getProxy(arrayDesign).getFactorValues(factor); 
        } catch (IOException e) {
            throw new AtlasDataException(e);
        }
    }

    public FloatMatrixProxy getExpressionValues(ArrayDesign arrayDesign, int[] deIndices) throws AtlasDataException {
        try {
            return getProxy(arrayDesign).getExpressionValues(deIndices); 
        } catch (IOException e) {
            throw new AtlasDataException(e);
        }
    }

    public float[] getExpressionDataForDesignElementAtIndex(ArrayDesign arrayDesign, int designElementIndex) throws AtlasDataException {
        try {
            return getProxy(arrayDesign).getExpressionDataForDesignElementAtIndex(designElementIndex); 
        } catch (IOException e) {
            throw new AtlasDataException(e);
        }
    }

    public ExpressionStatistics getExpressionStatistics(ArrayDesign arrayDesign, int[] deIndices) throws AtlasDataException {
        try {
            return getProxy(arrayDesign).getExpressionStatistics(deIndices); 
        } catch (IOException e) {
            throw new AtlasDataException(e);
        }
    }

    public Map<Long, Map<String, Map<String, ExpressionAnalysis>>> getExpressionAnalysesForDesignElementIndexes(ArrayDesign arrayDesign, Map<Long, List<Integer>> geneIdsToDEIndexes) throws AtlasDataException {
        try {
            return getProxy(arrayDesign).getExpressionAnalysesForDesignElementIndexes(geneIdsToDEIndexes); 
        } catch (IOException e) {
            throw new AtlasDataException(e);
        }
    }

    public Map<Long, Map<String, Map<String, ExpressionAnalysis>>> getExpressionAnalysesForDesignElementIndexes(
            ArrayDesign arrayDesign,
            final Map<Long, List<Integer>> geneIdsToDEIndexes,
            @Nullable final String efVal,
            @Nullable final String efvVal,
            final UpDownCondition upDownCondition)
            throws AtlasDataException {
        try {
            return getProxy(arrayDesign).getExpressionAnalysesForDesignElementIndexes(geneIdsToDEIndexes, efVal, efvVal, upDownCondition); 
        } catch (IOException e) {
            throw new AtlasDataException(e);
        }
    }

    public float[] getPValuesForDesignElement(ArrayDesign arrayDesign, int designElementIndex) throws AtlasDataException {
        try {
            return getProxy(arrayDesign).getPValuesForDesignElement(designElementIndex); 
        } catch (IOException e) {
            throw new AtlasDataException(e);
        }
    }

    public float[] getTStatisticsForDesignElement(ArrayDesign arrayDesign, int designElementIndex) throws AtlasDataException {
        try {
            return getProxy(arrayDesign).getTStatisticsForDesignElement(designElementIndex); 
        } catch (IOException e) {
            throw new AtlasDataException(e);
        }
    }

    public TwoDFloatArray getAllExpressionData(ArrayDesign arrayDesign) throws AtlasDataException {
        try {
            return getProxy(arrayDesign).getAllExpressionData(); 
        } catch (IOException e) {
            throw new AtlasDataException(e);
        }
    }

    public void closeAllDataSources() {
        for (NetCDFProxy p : proxies.values()) {
            Closeables.closeQuietly(p);
        }
        proxies.clear();
    }
}