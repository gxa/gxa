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

import uk.ac.ebi.gxa.requesthandlers.base.restutil.RestOut;
import uk.ac.ebi.gxa.requesthandlers.base.restutil.XmlRestResultRenderer;
import uk.ac.ebi.gxa.utils.MappingIterator;

import uk.ac.ebi.gxa.impl.AssayImpl;

import java.util.*;

import static java.util.Collections.unmodifiableMap;

/**
 * A class, representing on experiment assay for use in {@link ae3.model.ExperimentalData}
 * Is used only in NetCDFReader and should be replaced with newer model class.
 *
 * @author pashky
 */
public class Assay extends AssayImpl {
    private int number;
    private Map<String, String> factorValues = new HashMap<String, String>();
    private ArrayDesign arrayDesign;
    private Set<Sample> samples = new HashSet<Sample>();
    private int positionInMatrix;

    /**
     * Constructor
     *
     * @param number           assay number
     * @param factorValues     experimental factors values
     * @param arrayDesign      array design of this assay
     * @param positionInMatrix position in expression matrix (for specified array design)
     */
    Assay(String accession, int number, Map<String, String> factorValues, ArrayDesign arrayDesign, int positionInMatrix) {
        super(accession);
        this.number = number;
        this.factorValues.putAll(factorValues);
        this.arrayDesign = arrayDesign;
        this.positionInMatrix = positionInMatrix;
    }

    /**
     * Gets column position in expression matrix of the array design, this assay belongs to
     *
     * @return position
     */
    int getPositionInMatrix() {
        return positionInMatrix;
    }

    /**
     * Gets experimental factors values map
     *
     * @return efv map
     */
    @RestOut(name = "factorValues")
    public Map<String, String> getFactorValues() {
        return unmodifiableMap(factorValues);
    }

    /**
     * Gets assay number
     *
     * @return assay number
     */
    @RestOut(name = "id", forRenderer = XmlRestResultRenderer.class)
    public int getNumber() {
        return number;
    }

    /**
     * Links sample to this assay
     *
     * @param sample sample to link
     */
    void addSample(Sample sample) {
        samples.add(sample);
    }

    /**
     * Gets array design accession string
     *
     * @return accession string
     */
    @RestOut(name = "arrayDesign")
    public String getArrayDesignAccession() {
        return arrayDesign.getAccession();
    }

    /**
     * Gets array design
     *
     * @return array design
     */
    public ArrayDesign getArrayDesign() {
        return arrayDesign;
    }

    /**
     * Gets iterable of related sample numbers
     *
     * @return iterable of integers
     */
    @RestOut(name = "relatedSamples", xmlItemName = "sampleId")
    public Iterator<Integer> getSampleNumbers() {
        return new MappingIterator<Sample, Integer>(getSamples().iterator()) {
            public Integer map(Sample s) {
                return s.getNumber();
            }
        };
    }

    /**
     * Gets set of related samples
     *
     * @return set of related samples
     */
    public Set<Sample> getSamples() {
        return samples;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Assay assay = (Assay) o;
        return number == assay.number;
    }

    @Override
    public int hashCode() {
        return number;
    }

    @Override
    public String toString() {
        return "Assay{" +
                "number=" + number +
                ", factorValues=" + factorValues +
                ", arrayDesign=" + arrayDesign +
                ", positionInMatrix=" + positionInMatrix +
                '}';
    }
}
