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
import uk.ac.ebi.gxa.efo.EfoTerm;

import java.util.Arrays;

import static org.junit.Assert.*;

/**
 * @author Olga Melnichuk
 */
public class EfoTermPrefixRankTest {

    @Test
    public void rankTest() {
        EfoTermPrefixRank efoRank = new EfoTermPrefixRank("test");

        Rank noPrefix = efoRank.getRank(newEfoTerm("a test", "a test", "a test"));
        assertTrue(noPrefix.isMin());

        Rank idPrefix = efoRank.getRank(newEfoTerm("test", "a test"));
        assertTrue(idPrefix.isMax());

        Rank idPrefix2 = efoRank.getRank(newEfoTerm("test one", "a test"));
        assertFalse(idPrefix2.isMax());
        assertGreater(idPrefix, idPrefix2);

        Rank termPrefix = efoRank.getRank(newEfoTerm("a test", "test"));
        assertTrue(termPrefix.isMax());
        assertEquals(termPrefix, idPrefix);
        assertGreater(termPrefix, idPrefix2);

        Rank termPrefix2 = efoRank.getRank(newEfoTerm("a test", "test one"));
        assertEquals(termPrefix2, idPrefix2);

        Rank altTermPrefix = efoRank.getRank(newEfoTerm("some other id", "some other term", "test"));
        assertGreater(termPrefix2, altTermPrefix);

        Rank altTermPrefix2 = efoRank.getRank(newEfoTerm("some other id", "some other term", "test one"));
        assertGreater(altTermPrefix, altTermPrefix2);
    }

    private static void assertGreater(Rank r1, Rank r2) {
        assertTrue(r1.compareTo(r2) > 0);
    }

    private static EfoTerm newEfoTerm(String id, String term, String... alternativeTerms) {
        return new EfoTerm(id, term, Arrays.asList(alternativeTerms), false, false, false, 0);
    }
}
