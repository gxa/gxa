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

package ae3.service.experiment;

import com.google.common.base.Supplier;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import org.junit.Test;
import uk.ac.ebi.gxa.properties.AtlasProperties;

import java.util.*;

import static org.junit.Assert.*;

/**
 * @author Olga Melnichuk
 */
public class AtlasExperimentQueryParserTest {
    private static final int DEFAULT_ROWS = 17;
    private static final int MAX_ROWS = 35;
    private static final String[] FACTORS = new String[]{"cell_type", "disease_state", "organism_part"};

    @Test
    public void experimentApiQueryTest1() {
        AtlasExperimentQuery query = parse("experiment=E-AFMX-1");
        assertTrue(query.isValid());
        assertCollectionEqualsTo(query.getExperimentKeywords(), "E-AFMX-1");

        assertFalse(query.isListAll());
        assertTrue(query.getFactors().isEmpty());
        assertTrue(query.getAnyFactorValues().isEmpty());
        assertTrue(query.getFactorValues().isEmpty());
        assertTrue(query.getGeneIdentifiers().isEmpty());
        assertEquals(0, query.getStart());
        assertEquals(DEFAULT_ROWS, query.getRows());
    }

    @Test
    public void experimentApiQueryTest2() {
        AtlasExperimentQuery query = parse("experimentHasOrganism_part=lung");
        assertTrue(query.isValid());
        assertMultimapsEquals(createMultimap("organism_part=lung"), query.getFactorValues());

        assertFalse(query.isListAll());
        assertTrue(query.getExperimentKeywords().isEmpty());
        assertTrue(query.getFactors().isEmpty());
        assertTrue(query.getAnyFactorValues().isEmpty());
        assertTrue(query.getGeneIdentifiers().isEmpty());
        assertEquals(0, query.getStart());
        assertEquals(DEFAULT_ROWS, query.getRows());
    }

    @Test
    public void experimentApiQueryTest3() {
        AtlasExperimentQuery query = parse("experimentHasDisease_state=normal&experiment=cancer&start=10&rows=1");
        assertTrue(query.isValid());
        assertMultimapsEquals(createMultimap("disease_state=normal"), query.getFactorValues());
        assertCollectionEqualsTo(query.getExperimentKeywords(), "cancer");

        assertFalse(query.isListAll());
        assertTrue(query.getFactors().isEmpty());
        assertTrue(query.getAnyFactorValues().isEmpty());
        assertTrue(query.getGeneIdentifiers().isEmpty());
        assertEquals(10, query.getStart());
        assertEquals(1, query.getRows());
    }

    @Test
    public void experimentApiQueryTest4() {
        AtlasExperimentQuery query = parse("experimentHasFactor=cell_type&experiment=cycle");
        assertTrue(query.isValid());
        assertCollectionEqualsTo(query.getExperimentKeywords(), "cycle");
        assertCollectionEqualsTo(query.getFactors(), "cell_type");

        assertFalse(query.isListAll());
        assertTrue(query.getFactorValues().isEmpty());
        assertTrue(query.getAnyFactorValues().isEmpty());
        assertTrue(query.getGeneIdentifiers().isEmpty());
        assertEquals(0, query.getStart());
        assertEquals(DEFAULT_ROWS, query.getRows());
    }

    @Test
    public void experimentApiQueryTest5() {
        AtlasExperimentQuery query = parse("experiment=E-AFMX-5&geneIs=ENSG00000160766&geneIs=ENSG00000166337&format=xml");
        assertTrue(query.isValid());
        assertCollectionEqualsTo(query.getExperimentKeywords(), "E-AFMX-5");
        assertCollectionEqualsTo(query.getGeneIdentifiers(), "ENSG00000160766", "ENSG00000166337");

        assertFalse(query.isListAll());
        assertTrue(query.getFactors().isEmpty());
        assertTrue(query.getFactorValues().isEmpty());
        assertTrue(query.getAnyFactorValues().isEmpty());
        assertEquals(0, query.getStart());
        assertEquals(DEFAULT_ROWS, query.getRows());
    }

    @Test
    public void experimentApiQueryTest6() {
        AtlasExperimentQuery query = parse("experiment=listAll&experimentInfoOnly");
        assertTrue(query.isValid());
        assertTrue(query.isListAll());

        assertTrue(query.getExperimentKeywords().isEmpty());
        assertTrue(query.getFactors().isEmpty());
        assertTrue(query.getFactorValues().isEmpty());
        assertTrue(query.getAnyFactorValues().isEmpty());
        assertTrue(query.getGeneIdentifiers().isEmpty());
        assertEquals(0, query.getStart());
        assertEquals(DEFAULT_ROWS, query.getRows());
    }

    @Test
    public void experimentApiQueryTest7() {
        AtlasExperimentQuery query = parse("experimentHasAnyFactor=factorValue");
        assertTrue(query.isValid());
        assertCollectionEqualsTo(query.getAnyFactorValues(), "factorValue");

        assertFalse(query.isListAll());
        assertTrue(query.getExperimentKeywords().isEmpty());
        assertTrue(query.getFactors().isEmpty());
        assertTrue(query.getGeneIdentifiers().isEmpty());
        assertEquals(0, query.getStart());
        assertEquals(DEFAULT_ROWS, query.getRows());
    }

    private void assertMultimapsEquals(Multimap<String, String> m1, Multimap<String, String> m2) {
        assertEquals(m1.size(), m2.size());
        for (String k : m1.keySet()) {
            assertTrue(m2.containsKey(k));
            List<String> c1 = new ArrayList<String>(m1.get(k));
            List<String> c2 = new ArrayList<String>(m2.get(k));
            assertUnsortedListsEquals(c1, c2);
        }
    }

    private void assertCollectionEqualsTo(Collection<String> co, String... s) {
        List<String> list1 = Arrays.asList(s);
        List<String> list2 = new ArrayList<String>(co);
        assertUnsortedListsEquals(list1, list2);
    }

    private void assertUnsortedListsEquals(List<String> list1, List<String> list2) {
        Collections.sort(list1);
        Collections.sort(list2);
        assertEquals(list1, list2);
    }

    private Multimap<String, String> createMultimap(String... pairs) {
        Multimap<String, String> map = Multimaps.newListMultimap(
                new HashMap<String, Collection<String>>(),
                new Supplier<List<String>>() {
                    @Override
                    public List<String> get() {
                        return new ArrayList<String>();
                    }
                }
        );

        for (String p : pairs) {
            String[] parts = p.split("=");
            map.put(parts[0], parts[1]);
        }
        return map;
    }

    private AtlasExperimentQuery parse(String str) {
        Map<String, String[]> map = new HashMap<String, String[]>();
        String[] parts = str.split("&");
        for (String p : parts) {
            String[] nameValue = p.split("=");
            if (nameValue.length < 2) {
                continue;
            }
            String name = nameValue[0];
            String value = nameValue[1];
            String[] v = map.get(name);
            if (v == null) {
                v = new String[]{value};
            } else {
                String[] tmp = new String[v.length + 1];
                System.arraycopy(v, 0, tmp, 0, v.length);
                tmp[v.length] = value;
                v = tmp;
            }
            map.put(name, v);
        }

        AtlasExperimentQueryParser parser = new AtlasExperimentQueryParser(new AtlasProperties() {
            @Override
            public int getQueryDefaultPageSize() {
                return DEFAULT_ROWS;
            }

            @Override
            public int getAPIQueryMaximumPageSize() {
                return MAX_ROWS;
            }
        }, Arrays.asList(FACTORS));
        return parser.parse(map);
    }
}
