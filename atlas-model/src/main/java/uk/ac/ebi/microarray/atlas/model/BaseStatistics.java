/*
 * Copyright 2008-2012 Microarray Informatics Team, EMBL-European Bioinformatics Institute
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

package uk.ac.ebi.microarray.atlas.model;

import java.util.Comparator;

import static java.lang.Math.abs;

/**
 * An interface defining what's meant by "Statistics" in Atlas
 * <p/>
 * Basically, we carry P and T values all around, sort basing on those (in different ways),
 * rank the results basing on those, etc. This entity encapsulates what we know about statistics, so that
 * if we decide to move from P and T to P and, say, F, we'd know how to find the loose ends.
 * <p/>
 * Additionally, it's a temporary tool for making sure there are no loose ends right now.
 * Whether we actually need this after the refactoring
 * (see <a href="http://bar.ebi.ac.uk:8080/trac/ticket/3150">ticket:3150</a>) is an open question,
 * and the easiest way to answer is to finish with the change and see whether it's the next candidate
 * to be removed.
 *
 * @author alf
 */
public interface BaseStatistics {
    /**
     * Defines natural order descending by absolute value of T first, then ascending by P
     */
    Comparator<BaseStatistics> ORDER = new Comparator<BaseStatistics>() {
        @Override
        public int compare(BaseStatistics a, BaseStatistics b) {
            int result = -Float.compare(abs(a.getT()), abs(b.getT()));
            return result != 0 ? result : Float.compare(a.getP(), b.getP());
        }
    };

    /**
     * @return t-statistic
     */
    float getT();

    /**
     * @return p-value
     */
    float getP();
}
