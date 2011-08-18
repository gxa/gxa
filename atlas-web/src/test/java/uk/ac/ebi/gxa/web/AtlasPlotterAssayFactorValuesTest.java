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

package uk.ac.ebi.gxa.web;

import org.junit.Test;
import uk.ac.ebi.gxa.web.AtlasPlotter.AssayFactorValues;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Olga Melnichuk
 */
public class AtlasPlotterAssayFactorValuesTest {

    @Test
    public void testAssayFactorValues() {
        String[] factorValues = new String[]{"value1", "value2", "value1", "value2", "value3", "(empty)"};
        List<Float> expressions = Arrays.asList(1.0f, 2.0f, 1.0f, 2.0f, 3.0f, 0.0f);
        assertEquals(factorValues.length, expressions.size());

        AssayFactorValues afv = new AssayFactorValues(factorValues);
        assertContainsAll(afv.getUniqueValues(), "value1", "value2", "value3");
        assertContainsAll(afv.getAssayExpressionsFor("value1", expressions), 1.0f, 1.0f);
        assertContainsAll(afv.getAssayExpressionsFor("value2", expressions), 2.0f, 2.0f);
        assertContainsAll(afv.getAssayExpressionsFor("value3", expressions), 3.0f);

        try{
            afv.getAssayExpressionsFor("value1", Collections.<Float>emptyList());
            fail();
        }catch(IllegalArgumentException e) {
            //OK
        }
    }

    private <T> void assertContainsAll(Collection<T> collection, T... array) {
        assertEquals(collection.size(), array.length);
        for(T s : array) {
            assertTrue(collection.contains(s));
        }
    }
}
