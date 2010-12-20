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
import uk.ac.ebi.gxa.requesthandlers.base.restutil.RestOut;
import uk.ac.ebi.gxa.utils.EfvTree;

import javax.annotation.Nullable;
import java.util.*;

/**
 * NetCDF experiment data representation class
 *
 * @author pashky
 */
public class ExperimentalData {
    private List<Sample> samples = new ArrayList<Sample>();
    private List<Assay> assays = new ArrayList<Assay>();

    private Map<ArrayDesign, ExpressionMatrix> expressionMatrix = new HashMap<ArrayDesign, ExpressionMatrix>();
    private Map<ArrayDesign, DesignElementAccessions> designElementAccessions = new HashMap<ArrayDesign, DesignElementAccessions>();
    private Map<ArrayDesign, Map<Long, int[]>> geneIdMap = new HashMap<ArrayDesign, Map<Long, int[]>>();

    private Set<ArrayDesign> arrayDesigns = new HashSet<ArrayDesign>();
    private Set<String> experimentalFactors = new HashSet<String>();
    private Set<String> sampleCharacteristics = new HashSet<String>();
    private Map<ArrayDesign, ExpressionStats> expressionStats = new HashMap<ArrayDesign, ExpressionStats>();

    // Map array design accession -> ef -> mapping between experimental factor values and assays for that ef
    private Map<String, Map<String, ExperimentalFactorsCompactData>> adToEfToCompactData = new HashMap<String, Map<String, ExperimentalFactorsCompactData>>();
    // Map array design accession -> sc -> mapping between sample characteristic values and assays for that sc
    private Map<String, Map<String, SampleCharacteristicsCompactData>> adToScToCompactData = new HashMap<String, Map<String, SampleCharacteristicsCompactData>>();

    /**
     * Empty class from the start, one should fill it with addXX and setXX methods
     */
    public ExperimentalData() {
    }

    /**
     * Add sample to experiment
     *
     * @param scvMap map of sample charactristic values for sample
     * @param id     sample id
     * @param adAcc  Accession of array design for which sample is being added
     * @return created sample reference
     */
    public Sample addSample(Map<String, String> scvMap, long id, Integer sampleIndex, Integer numAssays, String adAcc) {
        for (Sample s : samples)
            if (s.getId() == id)
                return s;
        for (Map.Entry<String, String> scv : scvMap.entrySet()) {
            String sc = scv.getKey();
            if (!adToScToCompactData.containsKey(adAcc)) {
                adToScToCompactData.put(adAcc, new HashMap<String, SampleCharacteristicsCompactData>());
            }
            if (!adToScToCompactData.get(adAcc).containsKey(sc)) {
                adToScToCompactData.get(adAcc).put(sc, new SampleCharacteristicsCompactData(sc, numAssays));
            }
            adToScToCompactData.get(adAcc).get(sc).addScv(scv.getValue(), sampleIndex);
        }
        sampleCharacteristics.addAll(scvMap.keySet());
        final Sample sample = new Sample(samples.size(), scvMap, id);
        samples.add(sample);
        return sample;
    }

    /**
     * @return List of sc-scv mappings, served via Atlas API to experiment.js page. Map.Entry was not used to allow
     *         for shorter and more informative field names in JSON (for Map.Entry they would have been key and value).
     *         Indexes in this list are used in SampleCompactData objects returned by getSamplesForPlots()
     */
    public Collection<SampleCharacteristicsCompactData> getSCVsForPlot(String adAcc) {
        return adToScToCompactData.get(adAcc).values();
    }

    /**
     * @return List of ef-efv mappings, served via Atlas API to experiment.js page. Map.Entry was not used to allow
     *         for shorter and more informative field names in JSON (for Map.Entry they would have been key and value).
     *         Indexes in this list are used in AssayCompactData objects returned by getAssaysForPlots()
     */
    public Collection<ExperimentalFactorsCompactData> getEFVsForPlot(String adAcc) {
        return adToEfToCompactData.get(adAcc).values();
    }

    /**
     * Add assay to experiment
     *
     * @param arrayDesign      array design, this assay belongs to
     * @param efvMap           factor values map for all experimental factors
     * @param positionInMatrix assay's column position in expression matrix
     * @param adAcc            Accession of array design for which assay is being added
     * @return created assay reference
     */
    public Assay addAssay(ArrayDesign arrayDesign, Map<String, String> efvMap, int positionInMatrix, Integer numAssays, String adAcc) {
        arrayDesigns.add(arrayDesign);
        experimentalFactors.addAll(efvMap.keySet());

        for (Map.Entry<String, String> efv : efvMap.entrySet()) {
            String ef = efv.getKey();
            if (!adToEfToCompactData.containsKey(adAcc)) {
                adToEfToCompactData.put(adAcc, new HashMap<String, ExperimentalFactorsCompactData>());
            }
            if (!adToEfToCompactData.get(adAcc).containsKey(ef)) {
                adToEfToCompactData.get(adAcc).put(ef, new ExperimentalFactorsCompactData(ef, numAssays));
            }
            adToEfToCompactData.get(adAcc).get(ef).addEfv(efv.getValue(), positionInMatrix);
        }

        final Assay assay = new Assay(assays.size(), efvMap, arrayDesign, positionInMatrix);
        assays.add(assay);
        return assay;
    }

    /**
     * Set expression matrix for array design
     *
     * @param arrayDesign array design, this matrix applies to
     * @param matrix      object, implementing expression matrix interface
     */
    public void setExpressionMatrix(ArrayDesign arrayDesign, ExpressionMatrix matrix) {
        this.expressionMatrix.put(arrayDesign, matrix);
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
     * @param sampleIndex index of Sample corresponding to assayIndex
     * @param assayIndex  index of Assay corresponding to sampleIndex
     */
    public void addSampleAssayCompactMapping(Integer sampleIndex, Integer assayIndex, String adAcc) {
        final Map<String, SampleCharacteristicsCompactData> map = adToScToCompactData.get(adAcc);
        if (map != null) {
            for (SampleCharacteristicsCompactData sc : map.values()) {
                sc.addMapping(sampleIndex, assayIndex);
            }
        }
    }

    /**
     * Get expression value
     *
     * @param ad                 array design
     * @param assayPosition      assay's position in matrix
     * @param designElementIndex design element index
     * @return expression value
     */
    private float getExpression(ArrayDesign ad, int assayPosition, int designElementIndex) {
        return expressionMatrix.get(ad).getExpression(designElementIndex, assayPosition);
    }

    /**
     * Get expression value
     *
     * @param assay              assay, for which show the value
     * @param designElementIndex design element index
     * @return expression value
     */
    public float getExpression(Assay assay, int designElementIndex) {
        return getExpression(assay.getArrayDesign(), assay.getPositionInMatrix(), designElementIndex);
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
        return geneIdMap.get(arrayDesign).get(geneId);
    }

    public Integer[] getAllDesignElementsForArrayDesign(ArrayDesign arrayDesign) {
        Map<Long, int[]> geneToDEMap = geneIdMap.get(arrayDesign);

        Set<Integer> designElementIDs = new HashSet<Integer>();
        for (int[] deArray : geneToDEMap.values()) {
            for (int deID : deArray) {
                designElementIDs.add(deID);
            }
        }

        // copy set to array - can't do toArray() due to unboxing
//        long[] result = new long[designElementIDs.size()];
//        System.arraycopy(designElementIDs.toArray(new Long[1]), 0, result, 0, designElementIDs.size());
        return designElementIDs.toArray(new Integer[1]);
    }

    public Integer[] getAllDesignElements() {
        Set<Integer[]> parts = new HashSet<Integer[]>();

        // get each individual set of design elements for each array design
        for (ArrayDesign ad : getArrayDesigns()) {
            parts.add(getAllDesignElementsForArrayDesign(ad));
        }

        // total number of design elements?
        int size = 0;
        for (Integer[] part : parts) {
            size = size + part.length;
        }

        // copy into a single array
        Integer[] result = new Integer[size];
        int counter = 0;
        for (Integer[] part : parts) {
            System.arraycopy(part, 0, result, counter, part.length);
            counter = counter + part.length;
        }

        return result;
    }

    /**
     * Do not use this, as it doesn't handle multiple design elements for gene case
     *
     * @param geneId gene id
     * @return map of assays to expression values
     */
    @Deprecated
    public Map<Assay, Float> getExpressionsForGene(long geneId) {
        Map<Assay, Float> result = new HashMap<Assay, Float>();
        for (Assay ass : assays) {
            final ArrayDesign ad = ass.getArrayDesign();
            int[] deIds = geneIdMap.get(ad).get(geneId);
            if (deIds != null)
                for (int designElement : deIds) {
                    float expression = getExpression(ass, designElement);
                    if (expression > -1000000.0f)
                        result.put(ass, expression);
                }
        }
        return result;
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

    /**
     * Initializes gene to design element mapping
     *
     * @param arrayDesign array design, this map applies to
     * @param geneIds     array of gene ids corresponding to rows of expression matrix (and thus, design elements)
     */
    public void setGeneIds(ArrayDesign arrayDesign, long[] geneIds) {
        Map<Long, int[]> geneMap = new HashMap<Long, int[]>();
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
