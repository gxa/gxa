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

import uk.ac.ebi.gxa.utils.Pair;

/**
 * Statistics together with DE details
 *
 * @author alf
 */
public interface DesignElementStatistics extends BaseStatistics {
    Pair<String, String> getEfv();

    String getDeAccession();

    UpDownExpression getExpression();

    long getBioEntityId();

    /**
     * An old method intended to support the legacy code. DO NOT USE.
     * <p/>
     * If you're adding any functionality relying on this method, you're doing it wrong and putting another nail in
     * the project's coffin.
     *
     * @return the index of DE in the original data file
     * @deprecated use {@link #getDeAccession} instead
     */
    @Deprecated
    int getDeIndex();
}
