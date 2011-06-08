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

package ae3.service.structuredquery;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.*;

/**
 * @author Olga Melnichuk
 */
public class GeneAutoCompleteItemRankingTest {

    @Test
    public void rankTest() {
        AtlasGenePropertyService.GeneAutoCompleteItemRanking geneRanking =
                new AtlasGenePropertyService.GeneAutoCompleteItemRanking(Arrays.asList("human", "homo", "mus", "rattus"), Arrays.asList("property"));

        Rank r1 = geneRanking.getRank(newGeneItem("property", "gene1", null));
        Rank r2 = geneRanking.getRank(newGeneItem("property", "gene2", null));
        assertEquals(r1, r2);
        assertTrue(r1.isMin());

        Rank r3 = geneRanking.getRank(newGeneItem("property", "gene1", "human"));
        Rank r4 = geneRanking.getRank(newGeneItem("property", "gene1", "mus rattus"));
        assertGreater(r3, r4);
        assertTrue(r3.isMax());

        assertGreater(r4, r1);

        try {
            geneRanking.getRank(newGeneItem("property 1", "gene", null));
            fail();
        } catch (IllegalArgumentException e) {
            //OK
        }
    }

    private static void assertGreater(Rank r1, Rank r2) {
        assertTrue(r1.compareTo(r2) > 0);
    }

    private static GeneAutoCompleteItem newGeneItem(String property, String value, String species) {
        return new GeneAutoCompleteItem(property, value, 0L, species, value, Collections.<String>emptyList());
    }
}
