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

package uk.ac.ebi.gxa.data;

import uk.ac.ebi.gxa.utils.PT;

/**
 * @author Robert Petryszak
 * Date: 11/24/11
 *
 * This class is used as an element in Collections sorted by accurate (as stored in ncdfs) pval/tstat, specifically in the experiment page's best design elements table
 */
public class BestDesignElementCandidate extends PT {
    Float pValue;
    Float tStat;
    Integer deIndex;
    Integer uEFVIndex;

    public BestDesignElementCandidate(Float pValue, Float tStat, Integer deIndex, Integer uEFVIndex) {
        this.pValue = pValue;
        this.tStat = tStat;
        this.deIndex = deIndex;
        this.uEFVIndex = uEFVIndex;
    }

    public float getPValue() {
        return pValue;
    }

    public float getTStat() {
        return tStat;
    }

    public Integer getDEIndex() {
        return deIndex;
    }

    public Integer getUEFVIndex() {
        return uEFVIndex;
    }
}
