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

class BAIReader {
    private final File file;

    BAIReader(File file) {
        this.file = file;
    }

    ReferenceData getReferenceData(int chromosomeIndex) {
        try {
            final InputStream stream = new BufferedInputStream(new FileInputStream(file));
            if (!"BAI\001".equals(FileTools.readString(stream, 4))) {
                throw new BAMException("Invalid BAI file signature");
            }
            final int n_ref = FileTools.readInt32(stream);
            for (int i = 0; i < chromosomeIndex; ++i) {
                new ReferenceData(stream);
            }
                
            return new ReferenceData(stream);
        } catch (IOException e) {
            return null;
        }
    }
}
