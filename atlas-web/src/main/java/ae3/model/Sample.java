package ae3.model;

import uk.ac.ebi.gxa.requesthandlers.base.restutil.RestOut;
import uk.ac.ebi.gxa.requesthandlers.base.restutil.XmlRestResultRenderer;
import uk.ac.ebi.gxa.utils.MappingIterator;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * A class, representing on experiment sample for use in {@link ae3.model.ExperimentalData}
 * @author pashky
 */
public class Sample {
    private int number;
    private int id;
    private Map<String,String> sampleCharacteristics;
    private ExperimentalData experiment;
    private Set<Assay> assays = new HashSet<Assay>();

    /**
     * Constructor
     * @param experiment experimental data object, this sample belongs to
     * @param number sample number
     * @param sampleCharacteristics sample characteristics values map
     * @param id sample DW id
     */
    Sample(ExperimentalData experiment, int number, Map<String, String> sampleCharacteristics, int id) {
        this.number = number;
        this.sampleCharacteristics = sampleCharacteristics;
        this.experiment = experiment;
        this.id = id;
    }

    /**
     * Gets sample characteristics values map
     * @return sample characteristics values map
     */
    @RestOut(name="sampleCharacteristics")
    public Map<String, String> getSampleCharacteristics() {
        return sampleCharacteristics;
    }

    /**
     * Gets iterable for assay numbers, linked to this sample
     * @return iterable for integer assay numbers
     */
    @RestOut(name="relatedAssays", xmlItemName ="assayId")
    public Iterator<Integer> getAssayNumbers() {
        return new MappingIterator<Assay,Integer>(getAssays().iterator()) {
            public Integer map(Assay assay) { return assay.getNumber(); }
        };
    }

    /**
     * Gets sample number
     * @return sample number
     */
    @RestOut(name="id", forRenderer = XmlRestResultRenderer.class)
    public int getNumber() {
        return number;
    }

    /**
     * Gets set of assays, linked to this sample
     * @return set of assays
     */
    public Set<Assay> getAssays() {
        return assays;
    }

    /**
     * Returns DW sample id
     * @return sample id
     */
    int getId() {
        return id;
    }

    /**
     * Links assay to this sample
     * @param assay assay to link
     */
    void addAssay(Assay assay) {
        assays.add(assay);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Sample sample = (Sample) o;

        if (number != sample.number) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = number;
        return result;
    }

    @Override
    public String toString() {
        return "Sample{" +
                "number=" + number +
                ", id=" + id +
                ", sampleCharacteristics=" + sampleCharacteristics +
                ", assays=" + assays +
                '}';
    }
}
