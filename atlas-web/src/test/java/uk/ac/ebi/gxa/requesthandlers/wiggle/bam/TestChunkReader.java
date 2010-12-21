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

package uk.ac.ebi.gxa.requesthandlers.wiggle.bam;

import org.junit.Test;
import static org.junit.Assert.fail;

import java.util.*;
import java.io.*;
import java.net.*;

import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.assertEquals;

public class TestChunkReader {
        @Test
    public void test0() {
        runTest("bam/accepted_hits.sorted.bam", "3", 1, 7381, answer0);
    }

        @Test
    public void test1() {
        runTest("bam/accepted_hits.sorted.bam", "1", 983000, 983500, answer1);
    }

    private static final String answer0 =
        "73630121(35172) : 73630121(35316)\n" +
        "73630121(19081) : 73630121(19201)\n";
    private static final String answer1 =
        "86476272(1657) : 86476272(1801)\n" +
        "85770301(61706) : 85770301(63410)\n" +
        "85955980(20022) : 85955980(20886)\n" +
        "85630479(3657) : 85630479(3776)\n" +
        "85643352(35199) : 85643352(35319)\n" +
        "85630479(54943) : 85643352(1319)\n" +
        "85643352(1319) : 85643352(11520)\n";

    public static void runTest(String fileName, String chromosomeName, long start, long end, String answer) {
        try {
                   final File bamFile = new File(TestChunkReader.class.getClassLoader().getResource(fileName).toURI());
                   final File baiFile = new File(TestChunkReader.class.getClassLoader().getResource(fileName + ".bai").toURI());
            final BAMHeaderReader headerReader = new BAMHeaderReader(bamFile);
            final int chromosomeIndex = headerReader.getChromosomeIndex(chromosomeName);
            assertTrue("Chromosome " + chromosomeName + " not found", chromosomeIndex != -1);
        
            final BAIReader baiReader = new BAIReader(baiFile);
            final ReferenceData data = baiReader.getReferenceData(chromosomeIndex);
            final StringBuilder builder = new StringBuilder();
            for (Bin b : data.getBins(start, end)) {
                for (Chunk c : b.chunks) {
                    builder.append(c);
                    builder.append("\n");
                }
            }
            assertEquals(answer, builder.toString());
        } catch (URISyntaxException e) {
            fail(e.getMessage());
        }
    }
}
