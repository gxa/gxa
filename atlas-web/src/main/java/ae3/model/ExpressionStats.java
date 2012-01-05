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


import uk.ac.ebi.gxa.data.AtlasDataException;
import uk.ac.ebi.gxa.data.DesignElementStatistics;
import uk.ac.ebi.gxa.data.ExperimentWithData;
import uk.ac.ebi.gxa.data.StatisticsNotFoundException;
import uk.ac.ebi.gxa.utils.EfvTree;
import uk.ac.ebi.gxa.utils.Pair;
import uk.ac.ebi.microarray.atlas.model.ArrayDesign;

/**
 * Lazy expression statistics class
 *
 * @author pashky
 */
public class ExpressionStats {
    private final ExperimentWithData experiment;
    private final ArrayDesign arrayDesign;
    private final EfvTree<Integer> efvTree = new EfvTree<Integer>();

    private EfvTree<DesignElementStatistics> lastData;
    private long lastDesignElement = -1;

    ExpressionStats(ExperimentWithData experiment, ArrayDesign arrayDesign) throws AtlasDataException {
        this.experiment = experiment;
        this.arrayDesign = arrayDesign;

        int valueIndex = 0;
        try {
            for (Pair<String, String> uefv : experiment.getUniqueEFVs(arrayDesign)) {
                efvTree.put(uefv.getKey(), uefv.getValue(), valueIndex);
                ++valueIndex;
            }
        } catch (StatisticsNotFoundException e) {
            // TODO: ignore
        }
    }

    /**
     * Gets {@link uk.ac.ebi.gxa.utils.EfvTree} of expression statistics structures
     *
     * @param designElementId design element id
     * @return efv tree of stats
     * @throws AtlasDataException whenever it wants to
     */
    EfvTree<DesignElementStatistics> getExpressionStats(int designElementId) throws AtlasDataException {
        if (lastData != null && designElementId == lastDesignElement) {
            return lastData;
        }

        final EfvTree<DesignElementStatistics> result = new EfvTree<DesignElementStatistics>();
        try {
            for (EfvTree.EfEfv<Integer> efefv : efvTree.getNameSortedList()) {
                result.put(efefv.getEf(), efefv.getEfv(), experiment.getStatistics(efefv.getPayload(), designElementId, arrayDesign));
            }
        } catch (StatisticsNotFoundException e) {
            // TODO: throw this exception outside?
        }
        lastDesignElement = designElementId;
        lastData = result;
        return result;
    }
}
