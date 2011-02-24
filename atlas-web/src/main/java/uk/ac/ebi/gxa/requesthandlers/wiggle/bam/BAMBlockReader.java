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

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;

class BAMBlockReader {
    private static int readUint16LE(RandomAccessFile raf) throws IOException {
        return raf.readUnsignedByte() + (raf.readUnsignedByte() << 8);
    }

    private static long readUint32LE(RandomAccessFile raf) throws IOException {
        return
            raf.readUnsignedByte() +
            (raf.readUnsignedByte() << 8) +
            (raf.readUnsignedByte() << 16) +
            (raf.readUnsignedByte() << 24L);
    }

    private final RandomAccessFile raf;

    BAMBlockReader(File file) throws IOException {
        raf = new RandomAccessFile(file, "r");
    }

    List<BAMBlock> readBlocks(List<Chunk> chunks) throws IOException {
        final ArrayList<BAMBlock> blocks = new ArrayList<BAMBlock>();
        for (Chunk c : chunks) {
            readBlocksForChunk(c, blocks);
        }
        return blocks;
    }

    private void readBlocksForChunk(Chunk c, List<BAMBlock> blocks) throws IOException {
        for (long offset = c.getStartFileOffset(); offset <= c.getEndFileOffset(); ) {
            raf.seek(offset + 10);
            final int eXtraLENgth = readUint16LE(raf);
            int blockLength = -1;
            for (int len = 0; len < eXtraLENgth; ) {
                raf.seek(offset + 12 + len);    
                final int id = readUint16LE(raf);
                final int fieldLen = readUint16LE(raf);
                if (id != 17218) {
                    len += fieldLen + 4;
                } else {
                    blockLength = readUint16LE(raf) + 1;
                    break;
                }
            }
            if (blockLength == -1) {
                throw new BAMException("BGZF block size information not found");
            }
            raf.seek(offset + blockLength - 4);
            final long uncompressedSize = readUint32LE(raf);
            if (uncompressedSize > 65536) {
                throw new BAMException("Too large uncompressed block size (" + uncompressedSize + "in BGZF");
            }
            final byte[] byteArray = new byte[blockLength];
            BAMBlock block = new BAMBlock(
                (int)uncompressedSize,
                offset == c.getStartFileOffset() ? c.getStartBlockOffset() : 0,
                offset == c.getEndFileOffset() ? c.getEndBlockOffset() : (int)uncompressedSize
            );
            blocks.add(block);
            raf.seek(offset);
            raf.readFully(byteArray);
            final InputStream gzipStream = new GZIPInputStream(new ByteArrayInputStream(byteArray));
            int len = 0;
            while (true) {
                final int part = gzipStream.read(block.buffer, len, block.buffer.length - len);
                if (part <= 0) {
                    break;
                }
                len += part;
            }
            if (len != block.buffer.length) {
                throw new BAMException("Incorrect uncompressed block size: " + len + " != " + block.buffer.length);
            }
            offset += blockLength;
        }
    }
}
