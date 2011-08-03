/*
 * Copyright 2008-2011 Microarray Informatics Team, EMBL-European Bioinformatics Institute
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

package uk.ac.ebi.gxa.web.ui.plot;

import ae3.model.ExperimentalFactorsCompactData;
import ae3.model.SampleCharacteristicsCompactData;
import com.google.common.base.Function;
import com.google.common.collect.Lists;
import uk.ac.ebi.microarray.atlas.model.ArrayDesign;
import uk.ac.ebi.gxa.data.AtlasDataException;
import uk.ac.ebi.gxa.data.ExperimentWithData;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Loads assay properties in order to convert into JSON. Used by assay tooltips on the experiment page.
 * Not appropriate for using in the backend services.
 *
 * @author Olga Melnichuk
 *         Date: 03/05/2011
 */
public class AssayProperties {

    private List<ExperimentalFactorsCompactData> efs = Lists.newArrayList();
    private List<SampleCharacteristicsCompactData> scs = Lists.newArrayList();

    public Collection<ExperimentalFactorsCompactData> getEfs() {
        return Collections.unmodifiableCollection(efs);
    }

    public Collection<SampleCharacteristicsCompactData> getScs() {
        return Collections.unmodifiableCollection(scs);
    }

    private AssayProperties load(ExperimentWithData ewd, ArrayDesign ad, Function<String, String> nameConverter) throws AtlasDataException {
        efs = Lists.newArrayList();
        scs = Lists.newArrayList();

        String[] factors = ewd.getFactors(ad);
        String[] sampleCharacteristics = ewd.getCharacteristics(ad);
        int[][] s2a = ewd.getSamplesToAssays(ad);

        for (String f : factors) {
            String[] vals = ewd.getFactorValues(ad, f);
            ExperimentalFactorsCompactData d = new ExperimentalFactorsCompactData(
                    nameConverter.apply(f), vals.length);
            for (int i = 0; i < vals.length; i++) {
                d.addEfv(vals[i], i);
            }
            efs.add(d);
        }

        for (String s : sampleCharacteristics) {
            String[] vals = ewd.getCharacteristicValues(ad, s);
            SampleCharacteristicsCompactData d = new SampleCharacteristicsCompactData(
                    nameConverter.apply(s), s2a[0].length);
            assert vals.length == s2a.length;
            for (int i = 0; i < s2a.length; i++) {
                d.addScv(vals[i], i);
                for (int j = s2a[i].length - 1; j >= 0; j--) {
                    if (s2a[i][j] > 0) {
                        d.addMapping(i, j);
                    }
                }
            }
            scs.add(d);
        }
        return this;
    }

    public static AssayProperties create(ExperimentWithData ewd, ArrayDesign ad, Function<String, String> nameConverter) throws AtlasDataException {
        try {
            return new AssayProperties().load(ewd, ad, nameConverter);
        } finally {
            ewd.closeAllDataSources();
        }
    }
}
