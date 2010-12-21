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

abstract class FileTools {
    private static void readArray(InputStream stream, byte[] buffer) throws IOException {
        for (int offset = 0; offset < buffer.length; ) {
            final int len = stream.read(buffer, offset, buffer.length - offset);
            if (len <= 0) {
                throw new IOException("Unexpected end of input stream");
            }
            offset += len;
        }
    }

    static String readString(InputStream stream, int length) throws IOException {
        byte[] buffer = new byte[length];
        readArray(stream, buffer);
        return new String(buffer, "ascii");
    }

    static int readInt32(InputStream stream) throws IOException {
        byte[] buffer = new byte[4];
        readArray(stream, buffer);
        return
            ((0xFF & buffer[3]) << 24) +
            ((0xFF & buffer[2]) << 16) +
            ((0xFF & buffer[1]) << 8) +
            (0xFF & buffer[0]);
    }

    static long readUInt32(InputStream stream) throws IOException {
        byte[] buffer = new byte[4];
        readArray(stream, buffer);
        return
            ((0xFFL & buffer[3]) << 24) +
            ((0xFFL & buffer[2]) << 16) +
            ((0xFFL & buffer[1]) << 8) +
            (0xFFL & buffer[0]);
    }

    static long readUInt64(InputStream stream) throws IOException {
        byte[] buffer = new byte[8];
        readArray(stream, buffer);
        return
            ((0xFFL & buffer[7]) << 56) +
            ((0xFFL & buffer[6]) << 48) +
            ((0xFFL & buffer[5]) << 40) +
            ((0xFFL & buffer[4]) << 32) +
            ((0xFFL & buffer[3]) << 24) +
            ((0xFFL & buffer[2]) << 16) +
            ((0xFFL & buffer[1]) << 8) +
            (0xFFL & buffer[0]);
    }
}
