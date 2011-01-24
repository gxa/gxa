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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.zip.GZIPInputStream;

import static com.google.common.io.Closeables.closeQuietly;

class BAMHeaderReader {
    private final File file;
    private HashMap<String, Integer> chromosomeIndex;
    private HashMap<String, Integer> chromosomeLength;

    BAMHeaderReader(File file) {
        this.file = file;
    }

    public int getChromosomeIndex(String name) {
        if (chromosomeIndex == null) {
            try {
                readInfo();
            } catch (IOException e) {
                return -1;
            }
        }
        Integer index = chromosomeIndex.get(name);
        return index != null ? index : -1;
    }

    // TODO: unused declaration?
    public int getChromosomeLength(String name) {
        if (chromosomeLength == null) {
            try {
                readInfo();
            } catch (IOException e) {
                return -1;
            }
        }
        Integer length = chromosomeLength.get(name);
        return length != null ? length : -1;
    }

    private void readInfo() throws IOException {
        chromosomeIndex = new HashMap<String, Integer>();
        chromosomeLength = new HashMap<String, Integer>();

        InputStream stream = null;
        try {
            stream = new GZIPInputStream(new FileInputStream(file));
            if (!"BAM\001".equals(FileTools.readString(stream, 4))) {
                throw new BAMException("Invalid BAM file signature");
            }
            final int l_text = FileTools.readInt32(stream);
            FileTools.readString(stream, l_text);
            final int n_ref = FileTools.readInt32(stream);
            for (int i = 0; i < n_ref; ++i) {
                final int l_name = FileTools.readInt32(stream);
                final String name = l_name > 0 ? FileTools.readString(stream, l_name).substring(0, l_name - 1) : "";
                final int l_ref = FileTools.readInt32(stream);
                chromosomeIndex.put(name, i);
                chromosomeLength.put(name, l_ref);
            }
        } finally {
            closeQuietly(stream);
        }
    }
}
