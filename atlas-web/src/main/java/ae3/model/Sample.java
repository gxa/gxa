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

import java.util.*;

import static java.util.Collections.unmodifiableMap;
import static java.util.Collections.unmodifiableSet;

/**
 * A class, representing on experiment sample for use in {@link ae3.model.ExperimentalData}
 * Is used only in NetCDFReader and should be replaced with newer model class.
 *
 * @author pashky
 */
public class Sample {
    private int number;
    private long id;
    private Map<String, String> sampleCharacteristics = new HashMap<String, String>();
    private Set<Assay> assays = new HashSet<Assay>();

    /**
     * Constructor
     *
     * @param number                sample number
     * @param sampleCharacteristics sample characteristics values map
     * @param id                    sample DW id
     */
    Sample(int number, Map<String, String> sampleCharacteristics, long id) {
        this.number = number;
        this.sampleCharacteristics.putAll(sampleCharacteristics);
        this.id = id;
    }

    /**
     * Gets sample characteristics values map
     *
     * @return sample characteristics values map
     */
    @RestOut(name = "sampleCharacteristics")
    public Map<String, String> getSampleCharacteristics() {
        return unmodifiableMap(sampleCharacteristics);
    }

    /**
     * Gets iterable for assay numbers, linked to this sample
     *
     * @return iterable for integer assay numbers
     */
    @RestOut(name = "relatedAssays", xmlItemName = "assayId")
    public Iterator<Integer> getAssayNumbers() {
        return new MappingIterator<Assay, Integer>(getAssays().iterator()) {
            public Integer map(Assay assay) {
                return assay.getNumber();
            }
        };
    }

    /**
     * Gets sample number
     *
     * @return sample number
     */
    @RestOut(name = "id", forRenderer = XmlRestResultRenderer.class)
    public int getNumber() {
        return number;
    }

    /**
     * Gets set of assays, linked to this sample
     *
     * @return set of assays
     */
    public Set<Assay> getAssays() {
        return unmodifiableSet(assays);
    }

    /**
     * Returns DW sample id
     *
     * @return sample id
     */
    long getId() {
        return id;
    }

    /**
     * Links assay to this sample
     *
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
        return number;
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
