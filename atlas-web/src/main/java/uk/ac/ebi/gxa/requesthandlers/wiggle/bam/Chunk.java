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

import java.io.IOException;
import java.io.InputStream;

class Chunk implements Comparable<Chunk> {
    private final long start;
    private final long end;

    private Chunk(long start, long end) {
        this.start = start;
        this.end = end;
    }

    Chunk(InputStream stream) throws IOException {
        this(FileTools.readUInt64(stream), FileTools.readUInt64(stream));
    }

    long getStartFileOffset() {
        return start >> 16;
    }

    int getStartBlockOffset() {
        return (int) (start & 0xFFFF);
    }

    long getEndFileOffset() {
        return end >> 16;
    }

    int getEndBlockOffset() {
        return (int) (end & 0xFFFF);
    }

    boolean intersects(Chunk other) {
        return getStartFileOffset() <= other.getEndFileOffset() &&
                other.getStartFileOffset() <= getEndFileOffset();
    }

    Chunk sum(Chunk other) {
        return new Chunk(Math.min(start, other.start), Math.max(end, other.end));
    }

    public int compareTo(Chunk other) {
        final long result = start - other.start;
        if (result < 0) return -1;
        if (result == 0) return 0;
        return 1;
    }

    public String toString() {
        return getStartFileOffset() + "(" + getStartBlockOffset() + ") : " +
                getEndFileOffset() + "(" + getEndBlockOffset() + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Chunk chunk = (Chunk) o;

        if (start != chunk.start) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return (int) (start ^ (start >>> 32));
    }
}
