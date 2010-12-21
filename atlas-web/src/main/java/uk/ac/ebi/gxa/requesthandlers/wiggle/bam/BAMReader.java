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

import java.util.*;
import java.io.File;
import java.io.IOException;

public class BAMReader {
    private final File file;
    private List<BAMBlock> blocks;
    private ArrayList<Chunk> chunks = new ArrayList<Chunk>();
    private boolean upToDate;

    public BAMReader(File file) {
        this.file = file;
    }

    public List<BAMBlock> readBAMBlocks(String chromosomeName, long start, long end) throws IOException {
        if (!upToDate) {
            upToDate = true;

            final BAMHeaderReader headerReader = new BAMHeaderReader(file);
            final int chromosomeIndex = headerReader.getChromosomeIndex(chromosomeName);
            if (chromosomeIndex == -1) {
                throw new BAMException("Chromosome " + chromosomeName + " is not found in the file " + file);
            }
                
            final BAIReader baiReader = new BAIReader(new File(file.getPath() + ".bai"));
            final ReferenceData data = baiReader.getReferenceData(chromosomeIndex);
            for (Bin b : data.getBins(start, end)) {
                for (Chunk c : b.chunks) {
                    chunks.add(c);
                    upToDate = false;
                }
            }

            // we update our chunk collections with sorting them and
            // replacing all the intersected chunk pairs with their sum
            Collections.sort(chunks);
            final ArrayList<Chunk> updated = new ArrayList<Chunk>();
            Chunk previous = null;
            for (Chunk c : chunks) {
                if (previous == null) {
                    previous = c;
                } else if (previous.intersects(c)) {
                    previous = previous.sum(c);
                } else {
                    updated.add(previous);
                    previous = c;
                }
            }
            if (previous != null) {
                updated.add(previous);
            }
            chunks = updated;

            final BAMBlockReader reader = new BAMBlockReader(file);
            blocks = reader.readBlocks(chunks);
        }
        return blocks;
    }
}
