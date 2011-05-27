/*
 * Copyright 2008-2011 Microarray Informatics Team, EMBL-European Bioinformatics Institute
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

package uk.ac.ebi.gxa.efo;

import uk.ac.ebi.gxa.rank.Rank;

/**
 * @author Olga Melnichuk
 */
class EfoNodePrefixRank {
    private final String prefix;

    public EfoNodePrefixRank(String prefix) {
        this.prefix = prefix.toLowerCase();
    }

    public Rank forNode(EfoNode n) {
        double ratio = 0;
        ratio = Math.max(ratio, 0.5 * startsWith(n.term, prefix));
        ratio = Math.max(ratio, 0.5 * startsWith(n.id, prefix));
        if (ratio > 0) {
            ratio += 0.5;
        }
        for (String alt : n.alternativeTerms) {
            ratio = Math.max(ratio, 0.5 * startsWith(alt, prefix));
        }
        return new Rank(ratio);
    }

    private static double startsWith(String str, String prefix) {
        return str.toLowerCase().startsWith(prefix) ? 1.0 * prefix.length() / str.length() : 0;
    }
}
