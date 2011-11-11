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

package uk.ac.ebi.gxa.data;

import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.junit.Test;
import uk.ac.ebi.microarray.atlas.model.ArrayDesign;
import uk.ac.ebi.microarray.atlas.model.Experiment;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.google.common.collect.Maps.newHashMap;
import static java.util.Arrays.asList;
import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;
import static uk.ac.ebi.gxa.data.ExperimentPartCriteria.experimentPart;

/**
 * @author Olga Melnichuk
 */
public class ExperimentPartCriteriaTest {
    private static final ArrayDesign AD1 = new ArrayDesign("AD-1");
    private static final ArrayDesign AD2 = new ArrayDesign("AD-2");

    private static final Map<String, long[]> GENE_IDS = newHashMap();

    static {
        GENE_IDS.put(AD1.getAccession(), new long[]{1L, 2L, 3L});
        GENE_IDS.put(AD2.getAccession(), new long[]{1L, 4L, 5L});
    }

    private static final Map<String, String[]> DE_ACCESSIONS = newHashMap();

    static {
        DE_ACCESSIONS.put(AD1.getAccession(), new String[]{"DE-1", "DE-2"});
        DE_ACCESSIONS.put(AD2.getAccession(), new String[]{"DE-3", "DE-4"});
    }

    private static final Map<String, List<KeyValuePair>> UNIQUE_EFEFVS = newHashMap();

    static {
        UNIQUE_EFEFVS.put(AD1.getAccession(), asList(new KeyValuePair("EF1", "EFV11"), new KeyValuePair("EF1", "EFV12")));
        UNIQUE_EFEFVS.put(AD2.getAccession(), asList(new KeyValuePair("EF2", "EFV21"), new KeyValuePair("EF2", "EFV22")));
    }

    @Test
    public void testEmptyCriteria() throws AtlasDataException, StatisticsNotFoundException {
        ExperimentPart expPart = experimentPart().retrieveFrom(createExperimentWithData());
        assertNotNull(expPart);
        assertEquals(AD1.getAccession(), expPart.getArrayDesign().getAccession());
    }

    @Test
    public void testArrayDesignAccessionCriteria() throws AtlasDataException, StatisticsNotFoundException {
        ExperimentWithData ewd = createExperimentWithData();

        ExperimentPart expPart = experimentPart()
                .hasArrayDesignAccession(AD2.getAccession())
                .retrieveFrom(ewd);
        assertNotNull(expPart);
        assertEquals(AD2.getAccession(), expPart.getArrayDesign().getAccession());

        expPart = experimentPart()
                .hasArrayDesignAccession(AD1.getAccession())
                .hasArrayDesignAccession(AD2.getAccession())
                .retrieveFrom(ewd);
        assertNull(expPart);

        try {
            experimentPart()
                    .hasArrayDesignAccession(null)
                    .retrieveFrom(ewd);
            fail();
        } catch (IllegalArgumentException e) {
            // ok
        }

        try {
            experimentPart()
                    .hasArrayDesignAccession("")
                    .retrieveFrom(ewd);
            fail();
        } catch (IllegalArgumentException e) {
            // ok
        }
    }

    @Test
    public void testContainsGenesCriteria() throws AtlasDataException, StatisticsNotFoundException {
        ExperimentWithData ewd = createExperimentWithData();

        ExperimentPart expPart = experimentPart()
                .containsGenes(asList(1L, 2L))
                .retrieveFrom(ewd);
        assertNotNull(expPart);
        assertEquals(AD1.getAccession(), expPart.getArrayDesign().getAccession());

        try {
            experimentPart()
                    .containsGenes(Collections.<Long>emptyList())
                    .retrieveFrom(ewd);
            fail();
        } catch (IllegalArgumentException e) {
            // ok
        }
    }

    @Test
    public void testContainsAtLeastOneGeneCriteria() throws AtlasDataException, StatisticsNotFoundException {
        ExperimentWithData ewd = createExperimentWithData();

        ExperimentPart expPart = experimentPart()
                .containsAtLeastOneGene(asList(4L, 2L))
                .retrieveFrom(ewd);
        assertNotNull(expPart);
        assertEquals(AD1.getAccession(), expPart.getArrayDesign().getAccession());

        try {
            experimentPart()
                    .containsAtLeastOneGene(Collections.<Long>emptyList())
                    .retrieveFrom(ewd);
            fail();
        } catch (IllegalArgumentException e) {
            // ok
        }
    }

    @Test
    public void testContainsDesignElementAccessionsCriteria() throws AtlasDataException, StatisticsNotFoundException {
        ExperimentWithData ewd = createExperimentWithData();

        ExperimentPart expPart = experimentPart()
                .containsDeAccessions(asList("DE-3"))
                .retrieveFrom(ewd);
        assertNotNull(expPart);
        assertEquals(AD2.getAccession(), expPart.getArrayDesign().getAccession());

        try {
            experimentPart()
                    .containsDeAccessions(Collections.<String>emptyList())
                    .retrieveFrom(ewd);
            fail();
        } catch (IllegalArgumentException e) {
            // ok
        }
    }

    @Test
    public void testContainsEfEfvCriteria() throws AtlasDataException, StatisticsNotFoundException {
        ExperimentWithData ewd = createExperimentWithData();

        ExperimentPart expPart = experimentPart()
                .containsEfEfv("EF1", "EFV11")
                .retrieveFrom(ewd);
        assertNotNull(expPart);
        assertEquals(AD1.getAccession(), expPart.getArrayDesign().getAccession());

        expPart = experimentPart()
                .containsEfEfv("EF2", null)
                .retrieveFrom(ewd);
        assertNotNull(expPart);
        assertEquals(AD2.getAccession(), expPart.getArrayDesign().getAccession());

        try {
            experimentPart()
                    .containsEfEfv("", "anything")
                    .retrieveFrom(ewd);
            fail();
        } catch (IllegalArgumentException e) {
            // ok
        }

        try {
            experimentPart()
                    .containsEfEfv(null, "anything")
                    .retrieveFrom(ewd);
            fail();
        } catch (IllegalArgumentException e) {
            // ok
        }
    }

    private ExperimentWithData createExperimentWithData() throws AtlasDataException, StatisticsNotFoundException {
        final Experiment exp = createExperiment();
        final ExperimentWithData ewd = createMock(ExperimentWithData.class);
        expect(ewd.getExperiment())
                .andReturn(exp)
                .anyTimes();
        expect(ewd.getGenes(EasyMock.<ArrayDesign>anyObject()))
                .andAnswer(new IAnswer<long[]>() {
                    @Override
                    public long[] answer() throws Throwable {
                        ArrayDesign ad = (ArrayDesign) getCurrentArguments()[0];
                        return GENE_IDS.get(ad.getAccession());
                    }
                })
                .anyTimes();
        expect(ewd.getDesignElementAccessions(EasyMock.<ArrayDesign>anyObject()))
                .andAnswer(new IAnswer<String[]>() {
                    @Override
                    public String[] answer() throws Throwable {
                        ArrayDesign ad = (ArrayDesign) getCurrentArguments()[0];
                        return DE_ACCESSIONS.get(ad.getAccession());
                    }
                })
                .anyTimes();

        expect(ewd.getUniqueEFVs(EasyMock.<ArrayDesign>anyObject()))
                .andAnswer(new IAnswer<List<KeyValuePair>>() {
                    @Override
                    public List<KeyValuePair> answer() throws Throwable {
                        ArrayDesign ad = (ArrayDesign) getCurrentArguments()[0];
                        return UNIQUE_EFEFVS.get(ad.getAccession());
                    }
                })
                .anyTimes();
        replay(ewd);
        return ewd;
    }

    private Experiment createExperiment() {
        final Experiment exp = createMock(Experiment.class);
        expect(exp.getArrayDesigns())
                .andReturn(asList(AD1, AD2))
                .anyTimes();
        replay(exp);
        return exp;
    }
}
