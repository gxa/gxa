/*
 * Copyright 2008-2012 Microarray Informatics Team, EMBL-European Bioinformatics Institute
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

import org.junit.Before;
import org.junit.Test;
import uk.ac.ebi.gxa.utils.Pair;

import java.util.List;
import java.util.Random;

import static com.google.common.collect.Lists.newArrayList;
import static org.easymock.EasyMock.*;
import static org.junit.Assert.assertEquals;
import static uk.ac.ebi.microarray.atlas.model.DesignElementStatistics.ANY_EFV;
import static uk.ac.ebi.microarray.atlas.model.DesignElementStatistics.ANY_KNOWN_GENE;

/**
 * @author alf
 */
public class StatisticsCursorTest {
    private static final int DE_COUNT = 42;
    private Random r;

    @Before
    public void init() {
        r = new Random(12345L);
    }

    @Test
    public void walkByEfvFirst() throws AtlasDataException, StatisticsNotFoundException {
        final List<Pair<String, String>> efvs = efvs();
        final long[] genes = longs(DE_COUNT);
        final DataProxy proxy = dataProxy(efvs, genes);

        StatisticsCursor cursor = new StatisticsCursor(proxy, ANY_KNOWN_GENE, ANY_EFV);
        int c = 0;
        while (cursor.nextEFV()) {
            while (cursor.nextBioEntity()) {
                c++;
            }
        }
        assertEquals("Invalid number of cells", countOfNonZeros(genes) * efvs.size(), c);
    }

    @Test
    public void walkByGeneFirst() throws AtlasDataException, StatisticsNotFoundException {
        final List<Pair<String, String>> efvs = efvs();
        final long[] genes = longs(DE_COUNT);
        final DataProxy proxy = dataProxy(efvs, genes);

        StatisticsCursor cursor = new StatisticsCursor(proxy, ANY_KNOWN_GENE, ANY_EFV);
        int c = 0;
        while (cursor.nextBioEntity()) {
            while (cursor.nextEFV()) {
                c++;
            }
        }
        assertEquals("Invalid number of cells", countOfNonZeros(genes) * efvs.size(), c);
    }

    private DataProxy dataProxy(List<Pair<String, String>> efvs, long[] genes) throws AtlasDataException, StatisticsNotFoundException {
        final DataProxy proxy = createMock(DataProxy.class);
        expect(proxy.getDesignElementAccessions()).andReturn(new String[DE_COUNT]).anyTimes();
        expect(proxy.getUniqueEFVs()).andReturn(efvs).anyTimes();
        expect(proxy.getTStatistics()).andReturn(floatMatrix(DE_COUNT, efvs.size())).once();
        expect(proxy.getPValues()).andReturn(floatMatrix(DE_COUNT, efvs.size())).once();
        expect(proxy.getGenes()).andReturn(genes).once();
        replay(proxy);
        return proxy;
    }

    private int countOfNonZeros(long[] genes) {
        int result = 0;
        for (long gene : genes) {
            if (gene != 0) result++;
        }
        return result;
    }


    private long[] longs(int count) {
        final long[] longs = new long[count];
        for (int i = 0; i < longs.length; i++) {
            longs[i] = r.nextLong();
        }
        return longs;
    }

    private FloatMatrixProxy floatMatrix(int a, int b) {
        return new FloatMatrixProxy(new float[a][b], missVal());
    }

    private NetCDFMissingVal missVal() {
        final NetCDFMissingVal missingVal = createMock(NetCDFMissingVal.class);
        expect(missingVal.isMissVal(anyFloat())).andReturn(false).anyTimes();
        replay(missingVal);
        return missingVal;
    }

    private List<Pair<String, String>> efvs() {
        final List<Pair<String, String>> pairs = newArrayList();
        pairs.add(Pair.create("EF1", "EFV11"));
        pairs.add(Pair.create("EF1", "EFV12"));
        pairs.add(Pair.create("EF2", "EFV21"));
        pairs.add(Pair.create("EF2", "EFV22"));
        pairs.add(Pair.create("EF2", "EFV23"));
        return pairs;
    }
}
