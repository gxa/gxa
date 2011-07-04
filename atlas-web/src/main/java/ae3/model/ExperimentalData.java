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

import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.gxa.exceptions.LogUtil;
import uk.ac.ebi.gxa.netcdf.AtlasNetCDFDAO;
import uk.ac.ebi.gxa.netcdf.ExperimentWithData;
import uk.ac.ebi.gxa.netcdf.NetCDFDescriptor;
import uk.ac.ebi.gxa.netcdf.NetCDFProxy;
import uk.ac.ebi.gxa.netcdf.AtlasDataException;
import uk.ac.ebi.gxa.requesthandlers.base.restutil.RestOut;
import uk.ac.ebi.gxa.utils.EfvTree;
import uk.ac.ebi.gxa.web.filter.ResourceWatchdogFilter;
import uk.ac.ebi.microarray.atlas.model.Experiment;
import uk.ac.ebi.microarray.atlas.model.Sample;
import uk.ac.ebi.microarray.atlas.model.ArrayDesign;

import javax.annotation.Nullable;
import java.io.Closeable;
import java.io.IOException;
import java.util.*;

import static java.lang.System.arraycopy;

/**
 * NetCDF experiment data representation class
 *
 * @author pashky
 */
public class ExperimentalData {
    private static final Logger log = LoggerFactory.getLogger(ExperimentalData.class);

    private final ExperimentWithData experiment;
    private final List<SampleDecorator> samples = new ArrayList<SampleDecorator>();
    private final List<AssayDecorator> assays = new ArrayList<AssayDecorator>();

    private final Map<ArrayDesignDecorator, ExpressionMatrix> expressionMatrices = new HashMap<ArrayDesignDecorator, ExpressionMatrix>();
    private final Map<ArrayDesignDecorator, String[]> designElementAccessions = new HashMap<ArrayDesignDecorator, String[]>();
    private final Map<ArrayDesignDecorator, Map<Long, int[]>> geneIdMaps = new HashMap<ArrayDesignDecorator, Map<Long, int[]>>();

    private final Map<ArrayDesignDecorator, ExpressionStats> expressionStats = new HashMap<ArrayDesignDecorator, ExpressionStats>();

    /**
     * Empty class from the start, one should fill it with addXX and setXX methods
     *
     * @param experiment
     */
    public ExperimentalData(AtlasNetCDFDAO atlasNetCDFDAO, Experiment experiment) throws AtlasDataException {
        log.info("loading data for experiment" + experiment.getAccession());
        this.experiment = atlasNetCDFDAO.createExperimentWithData(experiment);

        ResourceWatchdogFilter.register(new Closeable() {
            public void close() {
                ExperimentalData.this.experiment.closeAllDataSources();
            }
        });
        for (NetCDFDescriptor descriptor : this.experiment.getNetCDFDescriptors()) {
            try {
                addProxy(descriptor);
            } catch (IOException e) {
                throw new AtlasDataException(e);
            }
        }
        createAssaySampleMappings();
    }

    public void addProxy(NetCDFDescriptor descriptor) throws AtlasDataException, IOException {
        final NetCDFProxy proxy = descriptor.createProxy();
        try {
            final ArrayDesignDecorator arrayDesign = new ArrayDesignDecorator(proxy.getArrayDesignAccession());
        
            final String[] sampleAccessions = proxy.getSampleAccessions();
            for (int i = 0; i < sampleAccessions.length; ++i) {
                final String accession = sampleAccessions[i];
                SampleDecorator sample = null;
                for (SampleDecorator s : this.samples) {
                    if (accession.equals(s.getAccession())) {
                        sample = s;
                        break;
                    }
                }
                if (sample == null) {
                    this.samples.add(new SampleDecorator(
                        experiment.getExperiment().getSample(accession),
                        this.samples.size()
                    ));
                }
            }
        
            final String[] assayAccessions = proxy.getAssayAccessions();
            for (int i = 0; i < assayAccessions.length; ++i) {
                this.assays.add(new AssayDecorator(
                    experiment.getExperiment().getAssay(assayAccessions[i]),
                    this.assays.size(),
                    arrayDesign,
                    i // position in matrix
                ));
            }
        } finally {
            proxy.close();
        }
    }

    private void createAssaySampleMappings() {
        final Map<Sample, SampleDecorator> sampleMap = new HashMap<Sample, SampleDecorator>();
        for (SampleDecorator sd : samples) {
            sampleMap.put(sd.getSample(), sd);
        }
        for (AssayDecorator ad : assays) {
            for (Sample sample: ad.getAssay().getSamples()) {
                addSampleAssayMapping(sampleMap.get(sample), ad);
            } 
        }
    }

    public ExperimentWithData getExperiment() {
        return experiment;
    }

    private NetCDFProxy getProxy(ArrayDesignDecorator arrayDesign) {
        try {
            return experiment.getProxy(arrayDesign.getArrayDesign());
        } catch (AtlasDataException e) {
            throw LogUtil.createUnexpected("unexpected", e);
        }
    }

    /**
     * Get expression matrix for array design
     *
     * @param arrayDesign array design, this matrix applies to
     */
    private ExpressionMatrix getExpressionMatrix(ArrayDesignDecorator arrayDesign) {
        ExpressionMatrix matrix = expressionMatrices.get(arrayDesign);
        if (matrix == null) {
            matrix = new ExpressionMatrix(getProxy(arrayDesign));
            expressionMatrices.put(arrayDesign, matrix);
        }
        return matrix;
    }

    /**
     * Set expression statistics object for array design
     *
     * @param arrayDesign array design, this data apply to
     */
    private ExpressionStats getExpressionStats(ArrayDesignDecorator arrayDesign) {
        ExpressionStats stats = expressionStats.get(arrayDesign);
        if (stats == null) {
            try {
                stats = new ExpressionStats(getProxy(arrayDesign));
            } catch (AtlasDataException e) {
                return null;
            } catch (IOException e) {
                return null;
            }
            expressionStats.put(arrayDesign, stats);
        }
        return stats;
    }

    /**
     * Add mapping between assay and sample
     *
     * @param sample sample to link with specified assay
     * @param assay  assay to link with specified sample
     */
    public void addSampleAssayMapping(SampleDecorator sample, AssayDecorator assay) {
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
    public float getExpression(AssayDecorator assay, int designElementIndex) {
        final ExpressionMatrix matrix = getExpressionMatrix(assay.getArrayDesign());
        if (matrix == null)
            throw LogUtil.createUnexpected("Cannot find expression matrix for " + assay);
        return matrix.getExpression(designElementIndex, assay.getPositionInMatrix());
    }

    /**
     * Get expression statistics map ({@link uk.ac.ebi.gxa.utils.EfvTree}, where payload is {@link ae3.model.ExpressionStats.Stat} structures
     *
     * @param ad            array design
     * @param designElement design element id
     * @return map of statstics
     */
    public EfvTree<ExpressionStats.Stat> getExpressionStats(ArrayDesignDecorator ad, int designElement) {
        final ExpressionStats stats = getExpressionStats(ad);
        return stats != null ? stats.getExpressionStats(designElement) : new EfvTree<ExpressionStats.Stat>();
    }

    /**
     * Get array of design element id's corresponding to gene on specified array design
     *
     * @param arrayDesign array design
     * @param geneId      gene id (the Atlas/DW one)
     * @return array of design element id's to be used in expression/statistics retrieval functions
     */
    public int[] getDesignElementIndexes(ArrayDesignDecorator arrayDesign, long geneId) {
        Map<Long, int[]> geneMap = geneIdMaps.get(arrayDesign);
        if (geneMap == null) {
            final NetCDFProxy proxy = getProxy(arrayDesign);

            final long[] geneIds;
            try {
                geneIds = proxy.getGenes();
            } catch (IOException e) {
                throw LogUtil.createUnexpected("Error during reading gene ids", e);
            }
            geneMap = new HashMap<Long, int[]>();
            for (int currentIndex = 0; currentIndex < geneIds.length; ++currentIndex) {
                int[] deIndexes = geneMap.get(geneIds[currentIndex]);
                geneMap.put(geneIds[currentIndex], append(deIndexes, currentIndex));
            }
            geneIdMaps.put(arrayDesign, geneMap);
        }
        return geneMap.get(geneId);
    }

    private int[] append(int[] array, int a) {
        if (array == null) {
            return new int[]{a};
        }
        int[] result = new int[array.length + 1];
        arraycopy(array, 0, result, 0, array.length);
        result[result.length - 1] = a;
        return result;
    }

    /**
     * Get list of experiment's samples
     *
     * @return list of all samples
     */
    @RestOut(name = "samples")
    public List<SampleDecorator> getSamples() {
        return samples;
    }

    /**
     * Get list of experiment's assays
     *
     * @return list of assays
     */
    @RestOut(name = "assays")
    public List<AssayDecorator> getAssays() {
        return assays;
    }

    /**
     * Get iterable of assays for specified array design only
     *
     * @param arrayDesign array design
     * @return iterable of assays
     */
    public Iterable<AssayDecorator> getAssays(final ArrayDesignDecorator arrayDesign) {
        return Collections2.filter(
                assays,
                new Predicate<AssayDecorator>() {
                    public boolean apply(@Nullable AssayDecorator input) {
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
    public Set<ArrayDesignDecorator> getArrayDesigns() {
        final HashSet<ArrayDesignDecorator> decorators = new HashSet<ArrayDesignDecorator>();
        for (ArrayDesign ad : experiment.getExperiment().getArrayDesigns()) {
            decorators.add(new ArrayDesignDecorator(ad));
        }
        return decorators;
    }

    /**
     * Get set of experimental factors for this experiment
     *
     * @return set of experimental factors
     */
    @RestOut(name = "experimentalFactors")
    public Set<String> getExperimentalFactors() {
        return experiment.getExperiment().getExperimentFactors();
    }

    /**
     * Get set of sample characteristics
     *
     * @return set of sample characteristics
     */
    @RestOut(name = "sampleCharacteristics")
    public Set<String> getSampleCharacteristics() {
        return experiment.getExperiment().getExperimentCharacteristics();
    }

    public String getDesignElementAccession(ArrayDesignDecorator arrayDesign, int designElementId) {
        String[] array = designElementAccessions.get(arrayDesign);
        if (array == null) {
            try {
                array = getProxy(arrayDesign).getDesignElementAccessions();
            } catch (IOException e) {
                throw LogUtil.createUnexpected("Exception during access to designElements", e);
            }
            designElementAccessions.put(arrayDesign, array);
        }
        return array[designElementId];
    }
}
