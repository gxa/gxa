package ae3.model;

import uk.ac.ebi.gxa.requesthandlers.base.restutil.RestOut;
import uk.ac.ebi.gxa.requesthandlers.base.restutil.XmlRestResultRenderer;
import uk.ac.ebi.gxa.utils.MappingIterator;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * A class, representing on experiment assay for use in {@link ae3.model.ExperimentalData}
 * @author pashky
 */
public class Assay {
    private int number;
    private Map<String,String> factorValues;
    private ExperimentalData experiment;
    private ArrayDesign arrayDesign;
    private Set<Sample> samples = new HashSet<Sample>();
    private int positionInMatrix;

    /**
     * Constructor
     * @param experiment experimental data object, this assay belongs to
     * @param number assay number
     * @param factorValues experimental factors values
     * @param arrayDesign array design of this assay
     * @param positionInMatrix position in expression matrix (for specified array design)
     */
    Assay(ExperimentalData experiment, int number, Map<String, String> factorValues, ArrayDesign arrayDesign, int positionInMatrix) {
        this.number = number;
        this.factorValues = factorValues;
        this.experiment = experiment;
        this.arrayDesign = arrayDesign;
        this.positionInMatrix = positionInMatrix;
    }

    /**
     * Gets column position in expression matrix of the array design, this assay belongs to
     * @return position
     */
    int getPositionInMatrix() {
        return positionInMatrix;
    }

    /**
     * Gets experimental factors values map
     * @return efv map
     */
    @RestOut(name="factorValues")
    public Map<String, String> getFactorValues() {
        return factorValues;
    }

    /**
     * Gets assay number
     * @return assay number
     */
    @RestOut(name="id", forRenderer = XmlRestResultRenderer.class)
    public int getNumber() {
        return number;
    }

    /**
     * Links sample to this assay
     * @param sample sample to link
     */
    void addSample(Sample sample) {
        samples.add(sample);
    }

    /**
     * Gets array design accession string
     * @return accession string
     */
    @RestOut(name="arrayDesign")
    public String getArrayDesignAccession() {
        return arrayDesign.getAccession();
    }

    /**
     * Gets array design
     * @return array design
     */
    public ArrayDesign getArrayDesign() {
        return arrayDesign;
    }

    /**
     * Gets iterable of related sample numbers
     * @return iterable of integers
     */
    @RestOut(name="relatedSamples", xmlItemName ="sampleId")
    public Iterator<Integer> getSampleNumbers() {
        return new MappingIterator<Sample,Integer>(getSamples().iterator()) {
            public Integer map(Sample s) { return s.getNumber(); }
        };
    }

    /**
     * Gets set of related samples
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

        if (number != assay.number) return false;

        return true;
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
