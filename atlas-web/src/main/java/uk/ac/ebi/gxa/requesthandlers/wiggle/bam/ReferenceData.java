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
import java.io.*;

class ReferenceData {
    final Bin[] bins;
    final long[] intervals;

    ReferenceData(InputStream stream) throws IOException {
        bins = new Bin[FileTools.readInt32(stream)];
        for (int i = 0; i < bins.length; ++i) {
            bins[i] = new Bin(stream);
        }
        intervals = new long[FileTools.readInt32(stream)];
        for (int i = 0; i < intervals.length; ++i) {
            intervals[i] = FileTools.readUInt64(stream);
        }
    }

    List<Bin> getBins(long start, long end) {
        final HashSet<Long> ids = new HashSet<Long>();
        ids.add(0L);
        long s = 0;
        for (int i = 0, j = 26; i < 5; ++i, j -= 3) {
            s += (1 << 3 * i);
            for (long k = s + (start >> j); k <= s + (end >> j); ++k) {
                ids.add(k);
            }
        }
        final ArrayList<Bin> binList = new ArrayList<Bin>();
        for (Bin b : bins) {
            if (ids.contains(b.id)) {
                binList.add(b);    
            }
        }
        return binList;
    }
}
