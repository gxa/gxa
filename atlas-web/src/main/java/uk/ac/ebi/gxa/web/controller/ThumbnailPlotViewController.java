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

package uk.ac.ebi.gxa.web.controller;

import ae3.dao.GeneSolrDAO;
import ae3.model.AtlasGene;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import uk.ac.ebi.gxa.dao.ExperimentDAO;
import uk.ac.ebi.gxa.dao.exceptions.RecordNotFoundException;
import uk.ac.ebi.gxa.data.*;
import uk.ac.ebi.microarray.atlas.model.Experiment;
import uk.ac.ebi.microarray.atlas.model.ExpressionAnalysis;

import java.util.*;
import java.util.regex.Pattern;

import static com.google.common.io.Closeables.closeQuietly;
import static uk.ac.ebi.gxa.utils.CollectionUtil.makeMap;

/**
 * @author Olga Melnichuk
 */
@Controller
public class ThumbnailPlotViewController extends AtlasViewController {
    private static final Logger log = LoggerFactory.getLogger(ThumbnailPlotViewController.class);

    private static final Pattern startsOrEndsWithDigits = java.util.regex.Pattern.compile("^\\d+|\\d+$");

    private final ExperimentDAO experimentDAO;
    private final GeneSolrDAO geneSolrDAO;
    private final AtlasDataDAO atlasDataDAO;

    @Autowired
    public ThumbnailPlotViewController(ExperimentDAO experimentDAO,
                                       GeneSolrDAO geneSolrDAO,
                                       AtlasDataDAO atlasDataDAO) {
        this.experimentDAO = experimentDAO;
        this.geneSolrDAO = geneSolrDAO;
        this.atlasDataDAO = atlasDataDAO;
    }

    @RequestMapping(value = "/thumbnailPlot", method = RequestMethod.GET)
    public String getExperimentPlot(
            @RequestParam("eacc") String expAccession,
            @RequestParam("gid") Long geneId,
            @RequestParam("ef") String ef,
            @RequestParam("efv") String efv,
            @RequestParam(value = "width", required = false, defaultValue = "90") String width,
            @RequestParam(value = "height", required = false, defaultValue = "45") String height,
            Model model
    ) throws RecordNotFoundException, AtlasDataException, StatisticsNotFoundException {
        final Experiment exp = experimentDAO.getByName(expAccession);

        GeneSolrDAO.AtlasGeneResult geneResult = geneSolrDAO.getGeneById(geneId);
        AtlasGene gene = (geneResult.isFound()) ? geneResult.getGene() : null;
        if (gene == null) {
            throw new RecordNotFoundException("No gene found for id = " + geneId);
        }
        ExperimentWithData ewd = null;
        try {
            ewd = atlasDataDAO.createExperimentWithData(exp);
            Map<Long, Map<String, Map<String, ExpressionAnalysis>>> geneExpressionAnalyses;
            List<Long> geneIds = Arrays.asList(geneId);
            ExperimentPart expPart = new ArrayDesignAmbiguity()
                    .containsGenes(geneIds)
                    .containsEfEfv(ef, efv)
                    .resolve(ewd);

            ExpressionAnalysis ea = extractExpressionAnalysesFor(
                    expPart.getExpressionAnalysesForGeneIds(geneIds),
                    geneId,
                    ef,
                    efv
            );


            model.addAttribute("plot",
                    createThumbnailPlot(ef, efv, ea, expPart));
            return UNSUPPORTED_HTML_VIEW;
        } finally {
            closeQuietly(ewd);
        }
    }

    private ExpressionAnalysis extractExpressionAnalysesFor(
            Map<Long, Map<String, Map<String, ExpressionAnalysis>>> geneExpressionAnalyses,
            Long geneId,
            String ef,
            String efv) {
        if (geneExpressionAnalyses == null) {
            return null;
        }
        Map<String, Map<String, ExpressionAnalysis>> eaByEfEfv = geneExpressionAnalyses.get(geneId);
        if (eaByEfEfv == null) {
            return null;
        }
        Map<String, ExpressionAnalysis> eaByEf = eaByEfEfv.get(ef);
        if (eaByEf == null) {
            return null;
        }
        return eaByEf.get(efv);
    }

    private Map<String, Object> createThumbnailPlot(String ef, String efv, ExpressionAnalysis ea, ExperimentPart expPart) throws AtlasDataException {
        log.debug("Creating thumbnail plot... EF: {}, Top FVs: {}, GeneId: {}",
                new Object[]{ef, efv, ea});

        // Get assayFVs from the proxy from which ea came
        //final ArrayDesign arrayDesign = new ArrayDesign(ea.getArrayDesignAccession());

        final String[] assayFVs = expPart.getFactorValues(ef);
        final List<String> uniqueFVs = sortUniqueFVs(assayFVs);
        final float[] expressions = expPart.getExpressionDataForDesignElementAtIndex(ea.getDesignElementIndex());

        List<Object> seriesData = new ArrayList<Object>();
        int startMark = 0;
        int endMark = 0;
        // iterate over each factor value (in sorted order)
        for (String factorValue : uniqueFVs) {
            // mark start position, in list of all samples, of the factor value we're after
            if (factorValue.equals(efv)) {
                startMark = seriesData.size() + 1;
            }

            for (int assayIndex = 0; assayIndex < assayFVs.length; assayIndex++)
                if (assayFVs[assayIndex].equals(factorValue)) {
                    float value = expressions[assayIndex];
                    seriesData.add(Arrays.<Number>asList(seriesData.size() + 1, value <= -1000000 ? null : value));
                }

            // mark end position, in list of all samples, of the factor value we're after
            if (factorValue.equals(efv)) {
                endMark = seriesData.size();
            }
        }

        return makeMap(
                "series", Collections.singletonList(makeMap(
                "data", seriesData,
                "lines", makeMap("show", true, "lineWidth", 2, "fill", false),
                "legend", makeMap("show", false))),
                "options", makeMap(
                "xaxis", makeMap("ticks", 0),
                "yaxis", makeMap("ticks", 0),
                "legend", makeMap("show", false),
                "colors", Collections.singletonList("#edc240"),
                "grid", makeMap(
                "backgroundColor", "#f0ffff",
                "autoHighlight", false,
                "hoverable", true,
                "clickable", true,
                "borderWidth", 1,
                "markings", Collections.singletonList(
                makeMap("xaxis", makeMap("from", startMark, "to", endMark),
                        "color", "#F5F5DC"))
        ),
                "selection", makeMap("mode", "x")
        )
        );
    }

    private static List<String> sortUniqueFVs(String[] assayFVs) {
        Set<String> uniqueSet = new HashSet<String>(Arrays.asList(assayFVs));
        List<String> uniqueFVs = new ArrayList<String>(uniqueSet);
        Collections.sort(uniqueFVs, new Comparator<String>() {
            public int compare(String s1, String s2) {
                // want to make sure that empty strings are pushed to the back
                boolean isEmptyS1 = (s1.length() == 0);
                boolean isEmptyS2 = (s2.length() == 0);

                if (isEmptyS1 && isEmptyS2) {
                    return 0;
                }
                if (isEmptyS1) {
                    return 1;
                }
                if (isEmptyS2) {
                    return -1;
                }

                java.util.regex.Matcher m1 = startsOrEndsWithDigits.matcher(s1);
                java.util.regex.Matcher m2 = startsOrEndsWithDigits.matcher(s2);

                if (m1.find() && m2.find()) {
                    Long i1 = new Long(s1.substring(m1.start(), m1.end()));
                    Long i2 = new Long(s2.substring(m2.start(), m2.end()));

                    int compareRes = i1.compareTo(i2);
                    return (compareRes == 0) ? s1.compareToIgnoreCase(s2) : compareRes;
                }

                return s1.compareToIgnoreCase(s2);
            }
        });
        return uniqueFVs;
    }
}
