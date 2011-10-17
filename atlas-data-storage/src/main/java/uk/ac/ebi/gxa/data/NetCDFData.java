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

package uk.ac.ebi.gxa.data;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.gxa.data.DataMatrixStorage;
import uk.ac.ebi.gxa.data.KeyValuePair;
import uk.ac.ebi.gxa.utils.*;
import uk.ac.ebi.microarray.atlas.model.Assay;
import uk.ac.ebi.microarray.atlas.model.AssayProperty;
import uk.ac.ebi.microarray.atlas.model.Sample;
import uk.ac.ebi.microarray.atlas.model.SampleProperty;

import java.util.*;

class NetCDFData {
    final private Logger log = LoggerFactory.getLogger(this.getClass());

    private DataMatrixStorage storage;
    private List<String> uniqueValues; // scvs/efvs
    private final Map<Assay, List<Sample>> assayToSamples = new LinkedHashMap<Assay, List<Sample>>();


    public void setStorage(DataMatrixStorage storage) {
        this.storage = storage;
    }

    public void addToStorage(String designElement, Iterator<Float> values) {
        storage.add(designElement, values);
    }

    public void setUniqueValues(List<KeyValuePair> uniqueValues) {
        // TODO: change this.uniqueValues to List of KeyValuePairs
        this.uniqueValues = new ArrayList<String>(uniqueValues.size());
        for (KeyValuePair pair : uniqueValues) {
            this.uniqueValues.add(pair.key + "||" + pair.value);
        }
    }

    public List<Assay> getAssays() {
        return new ArrayList<Assay>(assayToSamples.keySet());
    }

    public void addAssay(Assay assay) {
        assayToSamples.put(assay, assay.getSamples());
    }

    int getWidth() {
        return assayToSamples.keySet().size();
    }

    Map<String, DataMatrixStorage.ColumnRef> getAssayDataMap() {
        Map<String, DataMatrixStorage.ColumnRef> result = new HashMap<String, DataMatrixStorage.ColumnRef>();
        int i = 0;
        for (Assay assay : assayToSamples.keySet()) {
            result.put(assay.getAccession(), new DataMatrixStorage.ColumnRef(storage, i));
            i++;
        }
        return result;
    }

    // TODO: there was the pattern-matching logic,
    // see rev. 48f0df44ce1fbaea42dff50167827d0138bd4eb1 for an attempt to fix it
    // and rev. 05be531ebb5a93df06d6045f982d0b25e4008a11 for nearly-original version
    //

    EfvTree<CBitSet> getValuePatterns() {
        Set<String> properties = new HashSet<String>();

        // First store assay patterns
        final Set<Assay> assays = assayToSamples.keySet();
        for (Assay assay : assays)
            for (AssayProperty property : assay.getProperties())
                properties.add(property.getName());

        EfvTree<CBitSet> efvTree = new EfvTree<CBitSet>();
        int i = 0;
        for (Assay assay : assays) {
            for (final String propName : properties) {
                String value = assay.getPropertySummary(propName);
                efvTree.getOrCreateCaseSensitive(propName, value, new Maker<CBitSet>() {
                    public CBitSet make() {
                        return new CBitSet(assays.size());
                    }
                }).set(i, true);
            }
            ++i;
        }

        // Now add to efvTree sample patterns
        properties = new HashSet<String>();
        for (Map.Entry<Assay, List<Sample>> entry : assayToSamples.entrySet()) {
            for (Sample sample : entry.getValue()) {
                for (SampleProperty property : sample.getProperties())
                    properties.add(property.getName());
            }
        }

        i = 0;
        for (Map.Entry<Assay, List<Sample>> entry : assayToSamples.entrySet()) {
            final List<Sample> samples = entry.getValue();
            for (Sample sample : samples) {
                for (final String propName : properties) {
                    String value = sample.getPropertySummary(propName);
                    efvTree.getOrCreateCaseSensitive(propName, value, new Maker<CBitSet>() {
                        public CBitSet make() {
                            return new CBitSet(samples.size());
                        }
                    }).set(i, true);
                }
                ++i;
            }
        }

        return efvTree;
    }
}
