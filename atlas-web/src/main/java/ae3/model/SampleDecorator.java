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
import uk.ac.ebi.microarray.atlas.model.Sample;
import uk.ac.ebi.microarray.atlas.model.SampleProperty;

import java.util.*;

import static java.util.Collections.unmodifiableSet;

/**
 * A decorator class, representing an experiment sample for use in {@link ae3.model.ExperimentalData}
 *
 * @author pashky
 */
public class SampleDecorator {
    private final Sample sample;
    private final int number;
    private final Set<AssayDecorator> assays = new HashSet<AssayDecorator>();

    /**
     * Constructor
     *
     * @param number                sample number
     */
    SampleDecorator(Sample sample, int number) {
        this.sample = sample;
        this.number = number;
    }

    /**
     * Gets sample characteristics values map
     *
     * @return sample characteristics values map
     */
    @RestOut(name = "sampleCharacteristics")
    public Map<String, String> getSampleCharacteristics() {
        final Map<String, String> result = new HashMap<String, String>();
        for (SampleProperty property : sample.getProperties()) {
            result.put(property.getName(), property.getValue());
        }
        return result;
    }

    /**
     * Gets iterable for assay numbers, linked to this sample
     *
     * @return iterable for integer assay numbers
     */
    @RestOut(name = "relatedAssays", xmlItemName = "assayId")
    public Iterator<Integer> getAssayNumbers() {
        return new MappingIterator<AssayDecorator, Integer>(getAssays().iterator()) {
            public Integer map(AssayDecorator assay) {
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
    public Set<AssayDecorator> getAssays() {
        return unmodifiableSet(assays);
    }

    /**
     * Returns DW sample accession
     *
     * @return sample accession
     */
    String getAccession() {
        return sample.getAccession();
    }

    Sample getSample() {
        return sample;
    }

    /**
     * Links assay to this sample
     *
     * @param assay assay to link
     */
    void addAssay(AssayDecorator assay) {
        assays.add(assay);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SampleDecorator sample = (SampleDecorator) o;

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
                ", accession=" + getAccession() +
                ", sampleCharacteristics=" + getSampleCharacteristics() +
                ", assays=" + assays +
                '}';
    }
}
