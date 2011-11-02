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

package uk.ac.ebi.gxa.data;

import com.google.common.primitives.Longs;
import uk.ac.ebi.microarray.atlas.model.ArrayDesign;
import uk.ac.ebi.microarray.atlas.model.ExpressionAnalysis;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * @author Olga Melnichuk
 */
public class ExperimentPart {
    private final ExperimentWithData ewd;
    private final ArrayDesign arrayDesign;

    public ExperimentPart(ExperimentWithData ewd, ArrayDesign arrayDesign) {
        this.ewd = ewd;
        this.arrayDesign = arrayDesign;
    }

    public List<KeyValuePair> getUniqueFactorValues() throws AtlasDataException, StatisticsNotFoundException {
        return ewd.getUniqueFactorValues(arrayDesign);
    }

    public Collection<Long> getGeneIds() throws AtlasDataException {
        return Longs.asList(ewd.getGenes(arrayDesign));
    }

    public Map<Long, Map<String, Map<String, ExpressionAnalysis>>> getExpressionAnalysesForGeneIds(Collection<Long> geneIds)
            throws AtlasDataException, StatisticsNotFoundException {
        return ewd.getExpressionAnalysesForGeneIds(geneIds, arrayDesign);
    }

    public float[] getExpressionDataForDesignElementAtIndex(int deIndex) throws AtlasDataException {
        return ewd.getExpressionDataForDesignElementAtIndex(arrayDesign, deIndex);
    }

    public String[] getFactorValues(String ef) throws AtlasDataException {
        return ewd.getFactorValues(arrayDesign, ef);
    }

    @Override
    public String toString() {
        return "ExperimentPart@{" +
                ewd.getExperiment().getAccession() + "/" +
                arrayDesign.getAccession() + "}";
    }
}
