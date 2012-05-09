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
import com.google.common.base.Predicate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ebi.gxa.dao.AtlasDAO;
import uk.ac.ebi.gxa.dao.PropertyDAO;
import uk.ac.ebi.gxa.dao.exceptions.RecordNotFoundException;
import uk.ac.ebi.gxa.data.*;
import uk.ac.ebi.gxa.exceptions.ResourceNotFoundException;
import uk.ac.ebi.gxa.properties.AtlasProperties;
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
import java.util.List;

import static com.google.common.base.Predicates.alwaysTrue;
import static com.google.common.base.Predicates.equalTo;
import static com.google.common.base.Predicates.in;
import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.common.io.Closeables.closeQuietly;
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

    public AtlasExperiment getExperimentFromSolr(String accession) throws RecordNotFoundException {
        AtlasExperiment exp = experimentSolrDAO.getExperimentByAccession(accession);
        if (exp == null) {
            throw new RecordNotFoundException("Solr: No experiment found with accession=" + accession);
        }
        return exp;
    }

    @Transactional
    public Experiment getExperiment(String accession) throws RecordNotFoundException {
        return atlasDAO.getExperimentByAccession(accession);
    }

    @Transactional
    public ExperimentWithData getExperimentWithData(String accession) throws RecordNotFoundException {
        return getExperimentWithData(getExperiment(accession));
    }

    @Transactional
    public ExperimentWithData getExperimentWithData(Experiment experiment) {
        return atlasDataDAO.createExperimentWithData(experiment);
    }

    @Transactional
    public File getAssetFile(String accession, String fileName) throws ResourceNotFoundException, RecordNotFoundException {
        Experiment experiment = getExperiment(accession);

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

    private static ResourceNotFoundException assetNotFound(String accession, String assetFileName) {
        return new ResourceNotFoundException("Asset: " + assetFileName + " not found for experiment: " + accession);
    }

    @Transactional
    public ExperimentDesignUI getExperimentDesignUI(String expAccession) throws RecordNotFoundException {
        return new ExperimentDesignUI(getExperiment(expAccession)).unlazy();
    }

    @Transactional
    public ExperimentAnalytics getExperimentAnalytics(String expAccession, String adAccession)
            throws AtlasDataException, RecordNotFoundException, StatisticsNotFoundException {
        return getExperimentAnalytics(expAccession, adAccession, null, null, null, UpDownCondition.CONDITION_ANY, 0, -1);
    }

    @Transactional
    public ExperimentAnalytics getExperimentAnalytics(
            @Nonnull String accession,
            String adAcc,
            String gid,
            String ef,
            String efv,
            UpDownCondition updown,
            int offset,
            int limit) throws RecordNotFoundException, AtlasDataException, StatisticsNotFoundException {

        ExperimentWithData ewd = null;
        try {
            ewd = getExperimentWithData(accession);

            final Predicate<Long> geneIdPredicate = genePredicate(gid);

            ExperimentPartCriteria criteria = ExperimentPartCriteria.experimentPart();
            if (!isNullOrEmpty(adAcc)) {
                criteria.hasArrayDesignAccession(adAcc);
            } else {
                criteria.containsAtLeastOneGene(geneIdPredicate);
            }

            BestDesignElementsResult res = expAnalyticsService.findBestGenesForExperiment(
                    criteria.retrieveFrom(ewd),
                    geneIdPredicate, updown,
                    createFactorCriteria(ef, efv),
                    offset,
                    limit
            );
            return new ExperimentAnalytics(res);
        } finally {
            closeQuietly(ewd);
        }
    }

    private Predicate<Long> genePredicate(String gid) {
        if (isNullOrEmpty(gid))
            return ANY_KNOWN_GENE;

        return in(findGeneIds(Arrays.asList(gid.trim())));
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
