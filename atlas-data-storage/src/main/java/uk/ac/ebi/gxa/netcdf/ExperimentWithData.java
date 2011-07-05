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

package uk.ac.ebi.gxa.netcdf;

import java.util.*;

import java.io.IOException;

import com.google.common.io.Closeables;

import uk.ac.ebi.microarray.atlas.model.ArrayDesign;
import uk.ac.ebi.microarray.atlas.model.Experiment;
import uk.ac.ebi.microarray.atlas.model.Assay;
import uk.ac.ebi.microarray.atlas.model.Sample;

public class ExperimentWithData {
    private final AtlasNetCDFDAO netCDFDao;
    private final Experiment experiment;

    private final Map<ArrayDesign,NetCDFProxy> proxies = new HashMap<ArrayDesign,NetCDFProxy>();

    // cached data
    private final Map<ArrayDesign, String[]> designElementAccessions = new HashMap<ArrayDesign, String[]>();

    ExperimentWithData(AtlasNetCDFDAO netCDFDao, Experiment experiment) {
        this.netCDFDao = netCDFDao;
        this.experiment = experiment;
    }

    public Experiment getExperiment() {
        return experiment;
    }

    // TODO: remove this temporary method
    public List<NetCDFDescriptor> getNetCDFDescriptors() {
        return netCDFDao.getNetCDFDescriptors(experiment);
    }

    private NetCDFProxy getProxy(ArrayDesign arrayDesign) throws AtlasDataException {
        NetCDFProxy p = proxies.get(arrayDesign);
        if (p == null) {
            p = netCDFDao.getNetCDFDescriptor(experiment, arrayDesign).createProxy();
            proxies.put(arrayDesign, p);
        }
        return p;
    }

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

    public List<KeyValuePair> getUniqueValues(ArrayDesign arrayDesign) throws AtlasDataException {
        try {
            return getProxy(arrayDesign).getUniqueValues(); 
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
