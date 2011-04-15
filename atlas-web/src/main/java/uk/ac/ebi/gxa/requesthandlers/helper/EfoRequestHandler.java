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

package uk.ac.ebi.gxa.requesthandlers.helper;

import ae3.service.AtlasStatisticsQueryService;
import ae3.service.structuredquery.AtlasEfoService;
import uk.ac.ebi.gxa.requesthandlers.base.AbstractRestRequestHandler;
import uk.ac.ebi.gxa.requesthandlers.base.restutil.RestOut;
import uk.ac.ebi.gxa.statistics.Attribute;
import uk.ac.ebi.gxa.statistics.EfoAttribute;
import uk.ac.ebi.gxa.statistics.StatisticsType;
import uk.ac.ebi.gxa.utils.EscapeUtil;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author pashky
 */
public class EfoRequestHandler extends AbstractRestRequestHandler {
    public static class Result {
        private Collection hls;
        private Collection res;

        public Result(Collection hls, Collection res) {
            this.hls = hls;
            this.res = res;
        }

        public @RestOut Collection hl() {
            return hls;
        }

        public @RestOut(xmlItemName = "efo") Collection tree() {
            return res;
        }

    }

    private AtlasEfoService efoService;
    private AtlasStatisticsQueryService atlasStatisticsQueryService;

    public void setEfoService(AtlasEfoService efoService) {
        this.efoService = efoService;
    }

    public void setAtlasStatisticsQueryService(AtlasStatisticsQueryService atlasStatisticsQueryService) {
        this.atlasStatisticsQueryService = atlasStatisticsQueryService;
    }

    public Object process(HttpServletRequest request) {

        Collection<AtlasEfoService.EfoTermCount> result = null;
        String id = request.getParameter("childrenOf");

        if (id != null) {
            if (id.length() == 0) {
                id = null;
            }
            result = efoService.getTermChildren(id);
            for (AtlasEfoService.EfoTermCount efoTermCount : result) {
                // For each efoTermCount, check if any of its children in turn have non-zero experiment counts;
                // if not, set efoTermCount to non-expandable
                int geneCount = 0;
                Collection<AtlasEfoService.EfoTermCount> children = efoService.getTermChildren(efoTermCount.getId());
                for (AtlasEfoService.EfoTermCount efoChildTermCount : children) {
                    Attribute attr = new EfoAttribute(efoChildTermCount.getId(), StatisticsType.UP_DOWN);
                    geneCount = atlasStatisticsQueryService.getGeneCountForEfoAttribute(attr, StatisticsType.UP_DOWN);
                    if (geneCount > 0)
                        break;
                    attr.setStatType(StatisticsType.NON_D_E);
                    geneCount = atlasStatisticsQueryService.getGeneCountForEfoAttribute(attr, StatisticsType.NON_D_E);
                    if (geneCount > 0)
                        break;
                }
                if (geneCount == 0) {
                    efoTermCount.setNonExpandable();
                }
            }
            log.info("EFO request for children of " + id);
        } else {
            id = request.getParameter("downTo");
            if (id != null && id.length() != 0) {
                result = efoService.getTreeDownToTerm(id);
                log.info("EFO request for tree down to " + id);
            } else if (id != null && id.length() == 0) {
                // just show roots if nothing is down to
                log.info("EFO request for tree root");
                result = efoService.getTermChildren(null);
            } else {
                id = request.getParameter("parentsOf");
                if (id != null && id.length() != 0) {
                    log.info("EFO request for parents of " + id);
                    result = new ArrayList<AtlasEfoService.EfoTermCount>();
                    for (List<AtlasEfoService.EfoTermCount> i : efoService.getTermParentPaths(id)) {
                        result.addAll(i);
                    }
                }

            }
        }


        final String highlights = request.getParameter("hl");
        final Collection hls =
                highlights != null && highlights.length() != 0
                        ? efoService.searchTerms(EscapeUtil.parseQuotedList(highlights)) : null;

        return new Result(hls, result);
    }
}
