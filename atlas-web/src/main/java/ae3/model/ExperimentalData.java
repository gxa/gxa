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
import uk.ac.ebi.gxa.netcdf.reader.AtlasNetCDFDAO;
import uk.ac.ebi.gxa.netcdf.reader.NetCDFDescriptor;
import uk.ac.ebi.gxa.netcdf.reader.NetCDFProxy;
import uk.ac.ebi.gxa.requesthandlers.base.restutil.RestOut;
import uk.ac.ebi.gxa.utils.EfvTree;
import uk.ac.ebi.gxa.utils.EscapeUtil;
import uk.ac.ebi.gxa.web.filter.ResourceWatchdogFilter;
import uk.ac.ebi.microarray.atlas.model.Experiment;

import javax.annotation.Nullable;
import java.io.Closeable;
import java.io.IOException;
import java.util.*;

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
     * @param atlasNetCDFDAO netCDF DAO
     * @param experiment     data accession
     * @return either constructed object or null, if no data files was found for this accession
     * @throws IOException if i/o error occurs
     */
    public static ExperimentalData loadExperiment(AtlasNetCDFDAO atlasNetCDFDAO, Experiment experiment) throws IOException {
        log.info("loading data for experiment" + experiment.getAccession());

        ExperimentalData experimentalData = null;
        for (NetCDFDescriptor descriptor : atlasNetCDFDAO.getNetCDFDescriptors(experiment)) {
            if (experimentalData == null) {
                experimentalData = new ExperimentalData(experiment);
            }
            experimentalData.addProxy(descriptor.createProxy());
        }
        return experimentalData;
    }

    private final Experiment experiment;
    private final List<SampleDecorator> samples = new ArrayList<SampleDecorator>();
    private final List<AssayDecorator> assays = new ArrayList<AssayDecorator>();

    private final Map<ArrayDesign, NetCDFProxy> proxies = new HashMap<ArrayDesign, NetCDFProxy>();

    private final Map<ArrayDesign, ExpressionMatrix> expressionMatrix = new HashMap<ArrayDesign, ExpressionMatrix>();
    private final Map<ArrayDesign, String[]> designElementAccessions = new HashMap<ArrayDesign, String[]>();
    private final Map<ArrayDesign, Map<Long, int[]>> geneIdMap = new HashMap<ArrayDesign, Map<Long, int[]>>();

    private final Map<ArrayDesign, ExpressionStats> expressionStats = new HashMap<ArrayDesign, ExpressionStats>();

    /**
     * Empty class from the start, one should fill it with addXX and setXX methods
     *
     * @param experiment
     */
    public ExperimentalData(Experiment experiment) {
        this.experiment = experiment;
    }

    static String normalized(String name, String prefix) {
        if (name.startsWith(prefix)) {
            name = name.substring(prefix.length());
        }
        return EscapeUtil.encode(name);
    }

    public void addProxy(NetCDFProxy proxy) throws IOException {
        ResourceWatchdogFilter.register(proxy);

        final ArrayDesign arrayDesign = new ArrayDesign(proxy.getArrayDesignAccession());
        proxies.put(arrayDesign, proxy);

        final String[] sampleAccessions = proxy.getSampleAccessions();
        final SampleDecorator[] sampleDecorators = new SampleDecorator[sampleAccessions.length];
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
                sample = new SampleDecorator(
                        getExperiment().getSample(accession),
                        this.samples.size()
                );
                this.samples.add(sample);
            }
            sampleDecorators[i] = sample;
        }

        final String[] assayAccessions = proxy.getAssayAccessions();
        final AssayDecorator[] assayDecorators = new AssayDecorator[assayAccessions.length];
        for (int i = 0; i < assayAccessions.length; ++i) {
            assayDecorators[i] = new AssayDecorator(
                    getExperiment().getAssay(assayAccessions[i]),
                    this.assays.size(),
                    arrayDesign,
                    i // position in matrix
            );
            this.assays.add(assayDecorators[i]);
        }

        final int[][] samplesToAssays = proxy.getSamplesToAssays();
        for (int sampleI = 0; sampleI < sampleDecorators.length; ++sampleI) {
            for (int assayI = 0; assayI < assayDecorators.length; ++assayI) {
                if (samplesToAssays[sampleI][assayI] > 0) {
                    addSampleAssayMapping(sampleDecorators[sampleI], assayDecorators[assayI]);
                }
            }
        }
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

    private NetCDFProxy getProxy(ArrayDesign arrayDesign) {
        final NetCDFProxy proxy = proxies.get(arrayDesign);
        if (proxy == null) {
            throw LogUtil.createUnexpected("NetCDF for " + experiment.getAccession() + "/" + arrayDesign.getAccession() + "is not found");
        }
        return proxy;
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
            matrix = new ExpressionMatrix(getProxy(arrayDesign));
            expressionMatrix.put(arrayDesign, matrix);
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
                stats = new ExpressionStats(getProxy(arrayDesign));
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
    public EfvTree<ExpressionStats.Stat> getExpressionStats(ArrayDesign ad, int designElement) {
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
    public int[] getDesignElements(ArrayDesign arrayDesign, long geneId) {
        Map<Long, int[]> geneMap = geneIdMap.get(arrayDesign);
        if (geneMap == null) {
            final NetCDFProxy proxy = getProxy(arrayDesign);

            final long[] geneIds;
            try {
                geneIds = proxy.getGenes();
            } catch (IOException e) {
                throw LogUtil.createUnexpected("Error during reading gene ids", e);
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
    public Iterable<AssayDecorator> getAssays(final ArrayDesign arrayDesign) {
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
    public Set<ArrayDesign> getArrayDesigns() {
        return proxies.keySet();
    }

    /**
     * Get set of experimental factors for this experiment
     *
     * @return set of experimental factors
     */
    @RestOut(name = "experimentalFactors")
    public Set<String> getExperimentalFactors() {
        return experiment.getExperimentFactors();
    }

    /**
     * Get set of sample characteristics
     *
     * @return set of sample characteristics
     */
    @RestOut(name = "sampleCharacteristics")
    public Set<String> getSampleCharacteristics() {
        return experiment.getExperimentCharacteristics();
    }

    public String getDesignElementAccession(ArrayDesign arrayDesign, int designElementId) {
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
