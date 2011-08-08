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

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.gxa.exceptions.LogUtil;
import uk.ac.ebi.gxa.utils.FileUtil;
import uk.ac.ebi.microarray.atlas.model.ArrayDesign;
import uk.ac.ebi.microarray.atlas.model.Experiment;
import uk.ac.ebi.microarray.atlas.model.ExpressionAnalysis;
import uk.ac.ebi.microarray.atlas.model.UpDownCondition;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.File;
import java.util.*;

import static com.google.common.io.Closeables.closeQuietly;
import static com.google.common.primitives.Floats.asList;
import static java.util.Collections.singleton;

/**
 * This class wraps the functionality of retrieving values across multiple instances of NetCDFProxy
 *
 * @author Alexey Filippov
 * @author Rober Petryszak
 * @author Nikolay Pultsin
 */
public class AtlasDataDAO {
    private final Logger log = LoggerFactory.getLogger(getClass());

    // Location of the experiment data files
    private File atlasDataRepo;

    private static String getFilename(Experiment experiment, ArrayDesign arrayDesign) {
        return experiment.getAccession() + "_" + arrayDesign.getAccession() + ".nc";
    }

    File getNetCDFLocation(Experiment experiment, ArrayDesign arrayDesign) {
        return new File(getDataDirectory(experiment), getFilename(experiment, arrayDesign));
    }

    public void setAtlasDataRepo(File atlasDataRepo) {
        this.atlasDataRepo = atlasDataRepo;
    }

    public ExperimentWithData createExperimentWithData(Experiment experiment) {
        return new ExperimentWithData(this, experiment);
    }

    /**
     * @param experiment experiment to plot
     * @param geneIds    ids of genes to plot
     * @param criteria   other criteria to choose NetCDF to plot
     * @return geneId -> ef -> efv -> ea of best pValue for this geneid-ef-efv combination
     *         Note that ea contains arrayDesign and designElement index from which it came, so that
     *         the actual expression values can be easily retrieved later
     * @throws AtlasDataException in case of I/O errors
     */
    public Map<Long, Map<String, Map<String, ExpressionAnalysis>>> getExpressionAnalysesForGeneIds(
            @Nonnull ExperimentWithData ewd, @Nonnull Collection<Long> geneIds, @Nonnull Predicate<DataPredicates.Pair> criteria) throws AtlasDataException {
        final ArrayDesign arrayDesign = findArrayDesign(ewd, Predicates.<DataPredicates.Pair>and(new DataPredicates().containsGenes(geneIds), criteria));
        if (arrayDesign == null) {
            return null;
        }

        final Map<Long, List<Integer>> geneIdToDEIndexes =
            getGeneIdToDesignElementIndexes(ewd, arrayDesign, geneIds);
        return ewd.getExpressionAnalysesForDesignElementIndexes(arrayDesign, geneIdToDEIndexes);
    }

    public NetCDFCreator getNetCDFCreator(Experiment experiment, ArrayDesign arrayDesign) {
        return new NetCDFCreator(this, experiment, arrayDesign);
    }

    public String getPathForR(Experiment experiment, ArrayDesign arrayDesign) {
        return getNetCDFLocation(experiment, arrayDesign).getAbsolutePath();
    }

    public NetCDFDescriptor getNetCDFDescriptor(Experiment experiment, ArrayDesign arrayDesign) {
        return new NetCDFDescriptor(this, experiment, arrayDesign);
    }

    private NetCDFProxy getNetCDFProxy(Experiment experiment, ArrayDesign arrayDesign) throws AtlasDataException {
        return getNetCDFDescriptor(experiment, arrayDesign).createProxy();
    }

    /**
     * @param experiment the experiment to find arrayDesign for
     * @param criteria   the criteria to choose arrayDesign
     * @return first arrayDesign used in experiment, that matches criteria;
     *         or null if no arrayDesign has been found
     */
    public ArrayDesign findArrayDesign(ExperimentWithData ewd, Predicate<DataPredicates.Pair> criteria) throws AtlasDataException {
        for (ArrayDesign ad : ewd.getExperiment().getArrayDesigns()) {
            if (criteria.apply(new DataPredicates.Pair(ewd, ad))) {
                return ad;
            }
        }
        return null;
    }

    public File getDataDirectory(Experiment experiment) {
        final String[] parts = experiment.getAccession().split("-");
        if (parts.length != 3 || !"E".equals(parts[0])) {
            throw LogUtil.createUnexpected("Invalid experiment accession: " + experiment.getAccession());
        }
        final String num = (parts[2].length() > 2) ?
                parts[2].substring(0, parts[2].length() - 2) + "00" : "00";
        return new File(new File(new File(atlasDataRepo, parts[1]), num), experiment.getAccession());
    }

    /**
     * @param experiment@return List of NetCDF proxies corresponding to experimentAccession
     */
    private List<NetCDFDescriptor> getNetCDFDescriptors(final Experiment experiment) {
        final Collection<ArrayDesign> arrayDesigns = experiment.getArrayDesigns();
        final List<NetCDFDescriptor> ncdfs = new ArrayList<NetCDFDescriptor>(arrayDesigns.size());
        for (ArrayDesign ad : arrayDesigns) {
            ncdfs.add(getNetCDFDescriptor(experiment, ad));
        }
        return ncdfs;
    }

    /**
     * @param proxy
     * @param geneIds
     * @return Map: geneId -> List of design element indexes in proxy
     * @throws IOException
     */
    private Map<Long, List<Integer>> getGeneIdToDesignElementIndexes(
            final ExperimentWithData ewd, ArrayDesign ad,
            final Collection<Long> geneIds) throws AtlasDataException {
        // Note that in a given NetCDF proxy more than one geneIndex (==designElementIndex) may correspond to one geneId
        // (i.e. proxy.getGenes() may contain duplicates, whilst proxy.getDesignElements() will not; and
        // proxy.getGenes().size() == proxy.getDesignElements().size())
        Map<Long, List<Integer>> geneIdToDEIndexes = new HashMap<Long, List<Integer>>();

        int deIndex = 0;
        for (Long geneId : ewd.getGenes(ad)) {
            if (geneIds.contains(geneId)) {
                List<Integer> deIndexes = geneIdToDEIndexes.get(geneId);
                if (deIndexes == null) {
                    deIndexes = new ArrayList<Integer>();
                }
                deIndexes.add(deIndex);
                geneIdToDEIndexes.put(geneId, deIndexes);
            }
            deIndex++;
        }
        return geneIdToDEIndexes;
    }

    /**
     * @param experiment
     * @param geneId
     * @param ef
     * @param efv
     * @param upDownCondition
     * @return best (according to expression) ExpressionAnalysis for geneId-ef-efv in experimentAccession's
     *         first proxy in which expression data for that combination exists
     */
    public ExpressionAnalysis getBestEAForGeneEfEfvInExperiment(final Experiment experiment,
                                                                final Long geneId,
                                                                final String ef,
                                                                final String efv,
                                                                final UpDownCondition upDownCondition) {
        ExpressionAnalysis ea = null;
        final ExperimentWithData ewd = createExperimentWithData(experiment);
        try {
            final Collection<ArrayDesign> ads = experiment.getArrayDesigns();
            for (ArrayDesign ad : ads) {
                if (ea == null) {
                    Map<Long, List<Integer>> geneIdToDEIndexes = getGeneIdToDesignElementIndexes(ewd, ad, singleton(geneId));
                    Map<Long, Map<String, Map<String, ExpressionAnalysis>>> geneIdsToEfToEfvToEA =
                            ewd.getExpressionAnalysesForDesignElementIndexes(ad, geneIdToDEIndexes, ef, efv, upDownCondition);
                    if (geneIdsToEfToEfvToEA.containsKey(geneId) &&
                            geneIdsToEfToEfvToEA.get(geneId).containsKey(ef) &&
                            geneIdsToEfToEfvToEA.get(geneId).get(ef).containsKey(efv) &&

                            geneIdsToEfToEfvToEA.get(geneId).get(ef).get(efv) != null) {
                        ea = geneIdsToEfToEfvToEA.get(geneId).get(ef).get(efv);
                    }

                }
            }
        } catch (AtlasDataException e) {
            log.error("Failed to ExpressionAnalysis for gene id: " + geneId + "; ef: " + ef + " ; efv: " + efv + " in experiment: " + experiment);
        } finally {
            ewd.closeAllDataSources();
        }
        return ea;
    }

    /**
     * @param arrayDesign
     * @param geneId
     * @param ef
     * @return Map: efv -> best ExpressionAnalysis for geneid-ef in this proxy
     * @throws AtlasDataException
     */
    public Map<String, ExpressionAnalysis> getBestEAsPerEfvInProxy(
            final Experiment experiment,
            final ArrayDesign arrayDesign,
            final Long geneId,
            final String ef)
            throws AtlasDataException {
        final ExperimentWithData ewd = createExperimentWithData(experiment);
        try {
            Map<Long, List<Integer>> geneIdToDEIndexes = getGeneIdToDesignElementIndexes(ewd, arrayDesign, singleton(geneId));
            Map<Long, Map<String, Map<String, ExpressionAnalysis>>> geneIdsToEfToEfvToEA =
                    ewd.getExpressionAnalysesForDesignElementIndexes(arrayDesign, geneIdToDEIndexes);
            return geneIdsToEfToEfvToEA.get(geneId).get(ef);
        } finally {
            ewd.closeAllDataSources();
        }
    }

    public void deleteExperiment(Experiment experiment) {
        FileUtil.deleteDirectory(getDataDirectory(experiment));
    }
}
