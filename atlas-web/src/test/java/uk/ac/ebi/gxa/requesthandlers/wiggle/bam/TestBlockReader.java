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

import java.io.File;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.fail;

public class TestBlockReader {
        @Test
    public void test0() {
        runTest("bam/accepted_hits.sorted.bam", "3", 1, 7381, answer0);
    }

        @Test
    public void test1() {
        runTest("bam/accepted_hits.sorted.bam", "1", 983000, 983500, answer1);
    }

    private static String answer0 =
        "65492:19081:35316\n";
    private static String answer1 =
        "65480:3657:12873\n" +
        "65506:0:35319\n" +
        "65498:61706:63410\n" +
        "65475:20022:20886\n" +
        "65492:1657:1801\n";

    public static void runTest(String fileName, String chromosomeName, long start, long end, String answer) {
        try {
            final StringBuilder builder = new StringBuilder();
                final File file = new File(TestBlockReader.class.getClassLoader().getResource(fileName).toURI());
            final BAMReader reader = new BAMReader(file);
            for (BAMBlock b : reader.readBAMBlocks(chromosomeName, start, end)) {
                builder.append(b.buffer.length).append(":").append(b.from).append(":").append(b.to).append("\n");
            }
            assertEquals(answer, builder.toString());
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }
}
