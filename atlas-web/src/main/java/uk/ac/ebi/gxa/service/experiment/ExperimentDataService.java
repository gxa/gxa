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

package uk.ac.ebi.gxa.service.experiment;

import ae3.dao.ExperimentSolrDAO;
import ae3.dao.GeneSolrDAO;
import ae3.model.AtlasExperiment;
import ae3.service.experiment.AtlasExperimentAnalyticsViewService;
import ae3.service.experiment.BestDesignElementsResult;
import ae3.service.structuredquery.GeneQueryCondition;
import ae3.service.structuredquery.QueryCondition;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ebi.gxa.dao.AtlasDAO;
import uk.ac.ebi.gxa.dao.exceptions.RecordNotFoundException;
import uk.ac.ebi.gxa.data.*;
import uk.ac.ebi.gxa.exceptions.LogUtil;
import uk.ac.ebi.gxa.exceptions.ResourceNotFoundException;
import uk.ac.ebi.gxa.utils.Pair;
import uk.ac.ebi.gxa.web.controller.ExperimentDesignUI;
import uk.ac.ebi.microarray.atlas.model.Asset;
import uk.ac.ebi.microarray.atlas.model.Experiment;
import uk.ac.ebi.microarray.atlas.model.UpDownCondition;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static com.google.common.base.Predicates.*;
import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.common.io.Closeables.closeQuietly;
import static uk.ac.ebi.gxa.data.ExperimentPart.find;
import static uk.ac.ebi.gxa.utils.Pair.create;
import static uk.ac.ebi.microarray.atlas.model.DesignElementStatistics.ANY_KNOWN_GENE;

/**
 * @author Olga Melnichuk
 */
@Service
public class ExperimentDataService {

    private ExperimentSolrDAO experimentSolrDAO;

    private AtlasDAO atlasDAO;

    private AtlasDataDAO atlasDataDAO;

    private GeneSolrDAO geneSolrDAO;

    private AtlasExperimentAnalyticsViewService expAnalyticsService;

    public ExperimentDataService() {
        //default constructor required by AOP
    }

    @Autowired
    public ExperimentDataService(ExperimentSolrDAO experimentSolrDAO,
                                 AtlasDAO atlasDAO,
                                 AtlasDataDAO atlasDataDAO,
                                 GeneSolrDAO geneSolrDAO,
                                 AtlasExperimentAnalyticsViewService expAnalyticsService) {
        this.experimentSolrDAO = experimentSolrDAO;
        this.atlasDAO = atlasDAO;
        this.atlasDataDAO = atlasDataDAO;
        this.geneSolrDAO = geneSolrDAO;
        this.expAnalyticsService = expAnalyticsService;
    }

    public AtlasExperiment getExperimentFromSolr(String expAcc) throws RecordNotFoundException {
        AtlasExperiment exp = experimentSolrDAO.getExperimentByAccession(expAcc);
        if (exp == null) {
            throw new RecordNotFoundException("Solr: No experiment found with expAcc=" + expAcc);
        }
        return exp;
    }

    @Transactional
    public Experiment getExperiment(String expAcc) throws RecordNotFoundException {
        return atlasDAO.getExperimentByAccession(expAcc);
    }

    @Transactional
    public ExperimentWithData getExperimentWithData(String expAcc) throws RecordNotFoundException {
        return getExperimentWithData(getExperiment(expAcc));
    }

    @Transactional
    public ExperimentWithData getExperimentWithData(Experiment experiment) {
        return atlasDataDAO.createExperimentWithData(experiment);
    }

    @Transactional
    public File getAssetFile(String expAcc, String fileName) throws ResourceNotFoundException, RecordNotFoundException {
        Experiment experiment = getExperiment(expAcc);

        Asset asset = experiment.getAsset(fileName);
        if (asset == null) {
            throw assetNotFound(experiment.getAccession(), fileName);
        }

        final File assetFile = new File(new File(atlasDataDAO.getDataDirectory(experiment), "assets"), asset.getFileName());
        if (!assetFile.exists()) {
            throw assetNotFound(experiment.getAccession(), fileName);
        }
        return assetFile;
    }

    private static ResourceNotFoundException assetNotFound(String expAcc, String assetFileName) {
        return new ResourceNotFoundException("Asset: " + assetFileName + " not found for experiment: " + expAcc);
    }

    @Transactional
    public ExperimentDesignUI getExperimentDesignUI(String expAcc) throws RecordNotFoundException {
        return new ExperimentDesignUI(getExperiment(expAcc)).unlazy();
    }

    @Transactional
    public ExperimentAnalytics getExperimentAnalytics(String expAcc, String adAcc)
            throws AtlasDataException, RecordNotFoundException, StatisticsNotFoundException {
        return getExperimentAnalytics(expAcc, adAcc, null, null, null, UpDownCondition.CONDITION_ANY, 0, -1);
    }

    @Transactional
    public ExpressionDataCursor getExperimentExpressionData(String expAcc, String adAcc)
            throws RecordNotFoundException, ResourceNotFoundException, AtlasDataException {
        return find(getExperimentWithData(expAcc), adAcc).getExpressionData();
    }

    @Transactional
    public ExperimentAnalytics getExperimentAnalytics(
            @Nonnull String expAcc,
            String adAcc,
            String geneConditions,
            String ef,
            String efv,
            UpDownCondition updown,
            int offset,
            int limit) throws RecordNotFoundException, AtlasDataException, StatisticsNotFoundException {

        ExperimentWithData ewd = null;
        try {
            ewd = getExperimentWithData(expAcc);

            List<String> genesConditionsArr = (geneConditions == null ? Collections.<String>emptyList() : Arrays.asList(geneConditions.split(",")));
            List<QueryCondition> geneQueryConditions = Lists.newArrayList();

            if (genesConditionsArr.size() % 2 != 0) {
                throw LogUtil.createUnexpected("Incorrect gene conditions were passed for experimentTable: " + genesConditionsArr);
            }
            for (int i = 0; i < genesConditionsArr.size() - 1; i += 2) {
                QueryCondition geneQueryCondition = new GeneQueryCondition();
                geneQueryCondition.setFactor(genesConditionsArr.get(i));
                geneQueryCondition.setFactorValues(Collections.singletonList(genesConditionsArr.get(i + 1)));
                geneQueryConditions.add(geneQueryCondition);
            }
            final Predicate<Long> geneIdsPredicate = geneIdsPredicate(geneQueryConditions);


            ExperimentPartCriteria criteria = ExperimentPartCriteria.experimentPart();
            if (!isNullOrEmpty(adAcc)) {
                criteria.hasArrayDesignAccession(adAcc);
            } else {
                criteria.containsAtLeastOneGene(geneIdsPredicate);
            }

            BestDesignElementsResult res = expAnalyticsService.findBestGenesForExperiment(
                    criteria.retrieveFrom(ewd),
                    geneIdsPredicate, updown,
                    createFactorCriteria(ef, efv),
                    offset,
                    limit
            );
            return new ExperimentAnalytics(res);
        } finally {
            closeQuietly(ewd);
        }
    }

    private Predicate<Long> geneIdsPredicate(Collection<QueryCondition> geneConditions) {
        if (geneConditions.isEmpty())
            return ANY_KNOWN_GENE;

        return in(findGeneIds(geneConditions));
    }

    private Collection<Long> findGeneIds(Collection<QueryCondition> geneConditions) {
        return Collections2.transform(geneSolrDAO.getGeneIdsByGeneConditions(geneConditions),
                new Function<Integer, Long>() {
                    public Long apply(@Nonnull Integer geneId) {
                        return (long) geneId;
                    }
                });
    }

    private List<Long> findGeneIds(Collection<String> geneQuery) {
        return geneSolrDAO.findGeneIds(geneQuery);
    }

    private static Predicate<Pair<String, String>> createFactorCriteria(final String ef, String efv) {
        if (isNullOrEmpty(ef)) {
            return alwaysTrue();
        } else if (isNullOrEmpty(efv)) {
            return firstEqualTo(ef);
        } else {
            return equalTo(create(ef, efv));
        }
    }

    private static Predicate<Pair<String, String>> firstEqualTo(final String s) {
        return new Predicate<Pair<String, String>>() {
            @Override
            public boolean apply(@Nullable Pair<String, String> input) {
                return input != null && s.equals(input.getFirst());
            }
        };
    }
}
