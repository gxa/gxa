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
import uk.ac.ebi.gxa.netcdf.AtlasNetCDFDAO;
import uk.ac.ebi.gxa.netcdf.ExperimentWithData;
import uk.ac.ebi.gxa.netcdf.AtlasDataException;
import uk.ac.ebi.gxa.requesthandlers.base.restutil.RestOut;
import uk.ac.ebi.gxa.utils.EfvTree;
import uk.ac.ebi.gxa.web.filter.ResourceWatchdogFilter;
import uk.ac.ebi.microarray.atlas.model.Experiment;
import uk.ac.ebi.microarray.atlas.model.Assay;
import uk.ac.ebi.microarray.atlas.model.Sample;
import uk.ac.ebi.microarray.atlas.model.ArrayDesign;

import javax.annotation.Nullable;
import java.io.Closeable;
import java.util.*;

import static java.lang.System.arraycopy;

/**
 * NetCDF experiment data representation class
 *
 * @author pashky
 */
public class ExperimentalData {
    private static final Logger log = LoggerFactory.getLogger(ExperimentalData.class);

    private final ExperimentWithData experimentWithData;
    private final List<SampleDecorator> sampleDecorators = new ArrayList<SampleDecorator>();
    private final List<AssayDecorator> assayDecorators = new ArrayList<AssayDecorator>();

    private final Map<ArrayDesign, ExpressionMatrix> expressionMatrices = new HashMap<ArrayDesign, ExpressionMatrix>();
    private final Map<ArrayDesign, Map<Long, int[]>> geneIdMaps = new HashMap<ArrayDesign, Map<Long, int[]>>();

    private final Map<ArrayDesign, ExpressionStats> expressionStats = new HashMap<ArrayDesign, ExpressionStats>();

    /**
     * Empty class from the start, one should fill it with addXX and setXX methods
     *
     * @param experiment
     */
    public ExperimentalData(AtlasNetCDFDAO atlasNetCDFDAO, Experiment experiment) throws AtlasDataException {
        log.info("loading data for experiment" + experiment.getAccession());
        experimentWithData = atlasNetCDFDAO.createExperimentWithData(experiment);

        ResourceWatchdogFilter.register(new Closeable() {
            public void close() {
                experimentWithData.closeAllDataSources();
            }
        });

        collectSamples();
        collectAssays();
        createAssaySampleMappings();
    }

    private void collectSamples() throws AtlasDataException {
        for (Sample sample : getExperiment().getSamples()) {
            sampleDecorators.add(new SampleDecorator(
                sample,
                sampleDecorators.size()
            ));
        }
    }

    private void collectAssays() throws AtlasDataException {
        for (ArrayDesign ad : getExperiment().getArrayDesigns()) {
            int index = 0;
            for (Assay assay : experimentWithData.getAssays(ad)) {
                assayDecorators.add(new AssayDecorator(
                    assay,
                    assayDecorators.size(),
                    ad,
                    index++ // position in matrix
                ));
            }
        }
    }

    private void createAssaySampleMappings() {
        final Map<Sample, SampleDecorator> sampleMap = new HashMap<Sample, SampleDecorator>();
        for (SampleDecorator sd : sampleDecorators) {
            sampleMap.put(sd.getSample(), sd);
        }
        for (AssayDecorator ad : assayDecorators) {
            for (Sample sample: ad.getAssay().getSamples()) {
                final SampleDecorator sd = sampleMap.get(sample);
                ad.addSample(sd);
                sd.addAssay(ad);
            } 
        }
    }

    public ExperimentWithData getExperimentWithData() {
        return experimentWithData;
    }

    public Experiment getExperiment() {
        return experimentWithData.getExperiment();
    }

    /**
     * Get expression matrix for array design
     *
     * @param arrayDesign array design, this matrix applies to
     */
    private ExpressionMatrix getExpressionMatrix(ArrayDesign arrayDesign) throws AtlasDataException {
        ExpressionMatrix matrix = expressionMatrices.get(arrayDesign);
        if (matrix == null) {
            matrix = new ExpressionMatrix(experimentWithData, arrayDesign);
            expressionMatrices.put(arrayDesign, matrix);
        }
        return matrix;
    }

    /**
     * Set expression statistics object for array design
     *
     * @param arrayDesign array design, this data apply to
     */
    private ExpressionStats getExpressionStats(ArrayDesign arrayDesign) {
        ExpressionStats stats = expressionStats.get(arrayDesign);
        if (stats == null) {
            try {
                stats = new ExpressionStats(experimentWithData, arrayDesign);
            } catch (AtlasDataException e) {
                return null;
            }
            expressionStats.put(arrayDesign, stats);
        }
        return stats;
    }

    /**
     * Get expression value
     *
     * @param assay              assay, for which show the value
     * @param designElementIndex design element index
     * @return expression value
     */
    public float getExpression(AssayDecorator assay, int designElementIndex) throws AtlasDataException {
        final ExpressionMatrix matrix = getExpressionMatrix(assay.getArrayDesign());
        if (matrix == null) {
            throw new AtlasDataException("Cannot find expression matrix for " + assay);
        }
        return matrix.getExpression(designElementIndex, assay.getPositionInMatrix());
    }

    /**
     * Get expression statistics map ({@link uk.ac.ebi.gxa.utils.EfvTree}, where payload is {@link ae3.model.ExpressionStats.Stat} structures
     *
     * @param ad            array design
     * @param designElement design element id
     * @return map of statstics
     */
    public EfvTree<ExpressionStats.Stat> getExpressionStats(ArrayDesign ad, int designElement) {
        final ExpressionStats stats = getExpressionStats(ad);
        
        if (stats != null) {
            try {
                return stats.getExpressionStats(designElement);
            } catch (AtlasDataException e) {
            }
        }
        return new EfvTree<ExpressionStats.Stat>();
    }

    /**
     * Get array of design element id's corresponding to gene on specified array design
     *
     * @param arrayDesign array design
     * @param geneId      gene id (the Atlas/DW one)
     * @return array of design element id's to be used in expression/statistics retrieval functions
     */
    public int[] getDesignElementIndexes(ArrayDesign arrayDesign, long geneId) throws AtlasDataException {
        Map<Long, int[]> geneMap = geneIdMaps.get(arrayDesign);
        if (geneMap == null) {
            final long[] geneIds = experimentWithData.getGenes(arrayDesign);
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
    public List<SampleDecorator> getSampleDecorators() {
        return sampleDecorators;
    }

    /**
     * Get list of experiment's assays
     *
     * @return list of assays
     */
    @RestOut(name = "assays")
    public List<AssayDecorator> getAssayDecorators() {
        return assayDecorators;
    }

    /**
     * Get iterable of assays for specified array design only
     *
     * @param arrayDesign array design
     * @return iterable of assays
     */
    public Iterable<AssayDecorator> getAssays(final ArrayDesign arrayDesign) {
        return Collections2.filter(
                assayDecorators,
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
    public Set<ArrayDesignDecorator> getArrayDesignDecorators() {
        final HashSet<ArrayDesignDecorator> decorators = new HashSet<ArrayDesignDecorator>();
        for (ArrayDesign ad : getExperiment().getArrayDesigns()) {
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
        return getExperiment().getExperimentFactors();
    }

    /**
     * Get set of sample characteristics
     *
     * @return set of sample characteristics
     */
    @RestOut(name = "sampleCharacteristics")
    public Set<String> getSampleCharacteristics() {
        return getExperiment().getExperimentCharacteristics();
    }

    public String getDesignElementAccession(ArrayDesign arrayDesign, int designElementId) throws AtlasDataException {
        return experimentWithData.getDesignElementAccessions(arrayDesign)[designElementId];
    }
}
