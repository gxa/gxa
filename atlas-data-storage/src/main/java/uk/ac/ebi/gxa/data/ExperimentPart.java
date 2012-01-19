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

import com.google.common.base.Predicate;
import com.google.common.primitives.Longs;
import uk.ac.ebi.gxa.utils.Pair;
import uk.ac.ebi.microarray.atlas.model.ArrayDesign;
import uk.ac.ebi.microarray.atlas.model.ExpressionAnalysis;

import java.util.*;

import static com.google.common.base.Strings.isNullOrEmpty;
import static uk.ac.ebi.gxa.exceptions.LogUtil.createUnexpected;

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

    public StatisticsCursor getStatisticsIterator(Predicate<Long> bePredicate,
                                                  Predicate<Pair<String, String>> efvPredicate)
            throws AtlasDataException, StatisticsNotFoundException {
        return ewd.getStatistics(arrayDesign, bePredicate, efvPredicate);
    }

    public List<Long> getGeneIds() throws AtlasDataException {
        return Longs.asList(ewd.getGenes(arrayDesign));
    }

    public Map<Long, Map<String, Map<String, ExpressionAnalysis>>> getExpressionAnalysesForGeneIds(Collection<Long> geneIds)
            throws AtlasDataException, StatisticsNotFoundException {
        return ewd.getExpressionAnalysesForGeneIds(geneIds, arrayDesign);
    }

    public List<ExpressionValue> getBestGeneExpressionValues(Long geneId, String ef, String efv) throws AtlasDataException, StatisticsNotFoundException {
        Map<Long, Map<String, Map<String, ExpressionAnalysis>>> bestEAValues =
                ewd.getExpressionAnalysesForGeneIds(Arrays.asList(geneId), arrayDesign);

        if (bestEAValues == null) {
            return Collections.emptyList();
        }
        Map<String, Map<String, ExpressionAnalysis>> eaByEfEfv = bestEAValues.get(geneId);
        if (eaByEfEfv == null) {
            return Collections.emptyList();
        }
        Map<String, ExpressionAnalysis> eaByEf = eaByEfEfv.get(ef);
        if (eaByEf == null) {
            return Collections.emptyList();
        }
        ExpressionAnalysis bestEA = eaByEf.get(efv);
        return getDeExpressionValues(bestEA.getDeIndex(), ef);
    }

    public List<ExpressionValue> getDeExpressionValues(String deAccession, String ef) throws AtlasDataException {
        String[] deAccessions = ewd.getDesignElementAccessions(arrayDesign);
        for (int i = 0; i < deAccessions.length; i++) {
            if (deAccessions[i].equals(deAccession)) {
                return getDeExpressionValues(i + 1, ef);
            }
        }
        return Collections.emptyList();
    }

    private List<ExpressionValue> getDeExpressionValues(int deIndex, String ef) throws AtlasDataException {
        List<ExpressionValue> res = new ArrayList<ExpressionValue>();
        float[] expressions = ewd.getExpressionDataForDesignElementAtIndex(arrayDesign, deIndex);
        String[] assayEfvs = ewd.getFactorValues(arrayDesign, ef);
        if (expressions.length != assayEfvs.length) {
            throw createUnexpected("Inconsistent parallel arrays in " + toString() + ": " + expressions.length + " != " + assayEfvs.length);
        }
        for (int i = 0; i < expressions.length; i++) {
            res.add(new ExpressionValue(assayEfvs[i], expressions[i]));
        }
        return res;
    }

    @Override
    public String toString() {
        return "ExperimentPart@{" +
                ewd.getExperiment().getAccession() + "/" +
                arrayDesign.getAccession() + "}";
    }

    public boolean containsDeAccessions(Collection<String> list) throws AtlasDataException {
        String[] deAccessions = ewd.getDesignElementAccessions(arrayDesign);
        return Arrays.asList(deAccessions).containsAll(list);
    }

    public ArrayDesign getArrayDesign() {
        return arrayDesign;
    }

    boolean hasEfEfv(String ef, String efv) throws AtlasDataException, StatisticsNotFoundException {
        for (Pair<String, String> efEfv : ewd.getUniqueEFVs(arrayDesign)) {
            if (efEfv.getKey().equals(ef) &&
                    (isNullOrEmpty(efv) || efEfv.getValue().equals(efv))) {
                return true;
            }
        }
        return false;
    }
}
