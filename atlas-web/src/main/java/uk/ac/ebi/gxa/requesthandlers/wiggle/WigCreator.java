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

package uk.ac.ebi.gxa.requesthandlers.wiggle;

import java.io.PrintWriter;
import java.util.LinkedList;

class WigCreator {
    private final LinkedList<Long> myCountsQueue = new LinkedList<Long>();
    private long myStartNucleotideNumber = -1;

    private final PrintWriter out;
    private final String chromosomeId;
    private final long from;

    WigCreator(PrintWriter out, String chromosomeId, long from, long to) {
        if (from < 0 || from > to) {
            throw new IllegalArgumentException("Incorrect region: " + from + ":" + to);
        }
        this.out = out;
        this.chromosomeId = chromosomeId;
        this.from = from;
    }

    void init(long start) {
        if (myStartNucleotideNumber == -1) {
            myStartNucleotideNumber = Math.max(from, start);
        }
    }

    void fillByZeroes(long end) {
        while (myStartNucleotideNumber + myCountsQueue.size() < end) {
            myCountsQueue.add(0L);
        }
    }

    void removeZeroes(long start) {
        while (myStartNucleotideNumber < start && myCountsQueue.get(0) == 0) {
            myCountsQueue.removeFirst();
            ++myStartNucleotideNumber;
        }
    }

    void addRegion(long start, long end) {
        final long relativeStart = Math.max(0L, start - myStartNucleotideNumber);
        final long relativeEnd = end - myStartNucleotideNumber;
        // TODO: optimize?
        for (int index = (int)relativeStart; index < relativeEnd; ++index) {
            myCountsQueue.set(index, myCountsQueue.get(index) + 1);
        }
    }

    void printRegions(long start) {
        while (true) {
            removeZeroes(start);
            final int maxLength = (int)(start - myStartNucleotideNumber);
            if (maxLength <= 1) {
                return;
            }
            final long firstValue = myCountsQueue.get(0);
            int indexOfCut = -1;
            // TODO: optimize?
            for (int index = 1; index < maxLength; ++index) {
                if (myCountsQueue.get(index) != firstValue) {
                    indexOfCut = index;
                    break;
                }
            }
            if (indexOfCut == -1) {
                break;
            }
            out.println("variableStep chrom=chr" + chromosomeId + " span=" + indexOfCut);
            out.println(myStartNucleotideNumber + " " + firstValue + ".0");
            myStartNucleotideNumber += indexOfCut;
            while (--indexOfCut >= 0) {
                myCountsQueue.removeFirst();
            }
        }
    }
}
