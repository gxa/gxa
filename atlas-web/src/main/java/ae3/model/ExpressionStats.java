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

package ae3.model;


import uk.ac.ebi.gxa.data.*;
import uk.ac.ebi.gxa.utils.EfvTree;
import uk.ac.ebi.gxa.utils.Pair;
import uk.ac.ebi.microarray.atlas.model.ArrayDesign;

import static uk.ac.ebi.microarray.atlas.model.DesignElementStatistics.ANY_EFV;

/**
 * Lazy expression statistics class
 *
 * @deprecated to be replaced with {@link StatisticsCursor} ASAP
 */
@Deprecated
public class ExpressionStats {
    private final ExperimentWithData experiment;
    private final ArrayDesign arrayDesign;

    private EfvTree<StatisticsSnapshot> cachedResult;
    private int cachedDe = -1;

    ExpressionStats(ExperimentWithData experiment, ArrayDesign arrayDesign) throws AtlasDataException {
        this.experiment = experiment;
        this.arrayDesign = arrayDesign;
    }

    /**
     * Gets {@link uk.ac.ebi.gxa.utils.EfvTree} of expression statistics structures
     *
     * @param deIndex design element index
     * @return efv tree of stats
     * @throws AtlasDataException whenever it wants to
     */
    EfvTree<StatisticsSnapshot> getExpressionStats(int deIndex) throws AtlasDataException {
        if (cachedResult == null || deIndex != cachedDe) {
            cachedDe = deIndex;
            cachedResult = retrieveExpressionStats(deIndex);
        }
        return cachedResult;
    }

    private EfvTree<StatisticsSnapshot> retrieveExpressionStats(int deIndex) throws AtlasDataException {
        final EfvTree<StatisticsSnapshot> result = new EfvTree<StatisticsSnapshot>();
        try {
            final StatisticsCursor statistics = experiment.getStatistics(arrayDesign, deIndex, ANY_EFV);
            while (statistics.nextBioEntity()) {
                while (statistics.nextEFV()) {
                    final Pair<String, String> efv = statistics.getEfv();
                    result.put(efv.getFirst(), efv.getSecond(), statistics.getSnapshot());
                }
            }    
        } catch (StatisticsNotFoundException e) {
            // TODO: throw this exception outside?
        }
        return result;
    }
}
