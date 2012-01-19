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
 * An interface defining the approach to statistics in Atlas
 * <p/>
 * Every experiment is treated individually as follows:
 * <ol>
 * <li>Data is taken as normalized by original submitters/authors.</li>
 * <li>For every experimental variable ("factor") a set of gene-wise linear models is constructed,
 * coefficients are moderated and one-way multiple comparisons with the mean (MCM) contrasts are computed. [1]</li>
 * <li>Post-hoc tests are used to compute and identify contrasts of interest with respective t-statistics and
 * globally adjusted p-values.</li>
 * </ol>
 * Thus for each experiment-condition pair we have, for each gene in the experiment,
 * a direction and a p-value indicating the strength of differential expression
 * in contrast to groups defined by the experimental variable that the condition belongs to.
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
