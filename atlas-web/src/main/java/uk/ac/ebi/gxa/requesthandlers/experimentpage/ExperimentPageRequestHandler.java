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

package uk.ac.ebi.gxa.requesthandlers.experimentpage;

import ae3.dao.AtlasSolrDAO;
import ae3.model.AtlasExperiment;
import ae3.model.AtlasGene;
import ae3.service.structuredquery.*;
import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import org.apache.commons.lang.StringUtils;
import org.springframework.web.HttpRequestHandler;
import uk.ac.ebi.gxa.netcdf.reader.AtlasNetCDFDAO;
import uk.ac.ebi.gxa.netcdf.reader.NetCDFProxy;
import uk.ac.ebi.gxa.requesthandlers.base.ErrorResponseHelper;
import uk.ac.ebi.gxa.utils.EscapeUtil;

import javax.annotation.Nullable;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;

/**
 * @author pashky
 */
public class ExperimentPageRequestHandler implements HttpRequestHandler {

    private static final Function<AtlasGene, Long> GENE_TO_IDS = new Function<AtlasGene, Long>() {
        public Long apply(@Nullable AtlasGene gene) {
            return Long.parseLong(gene.getGeneId());
        }
    };

    private AtlasSolrDAO atlasSolrDAO;

    public void setDao(AtlasSolrDAO atlasSolrDAO) {
        this.atlasSolrDAO = atlasSolrDAO;
    }

    private AtlasStructuredQueryService queryService;

    public void setQueryService(AtlasStructuredQueryService queryService) {
        this.queryService = queryService;
    }

    private AtlasNetCDFDAO atlasNetCDFDAO;

    public void setAtlasNetCDFDAO(AtlasNetCDFDAO atlasNetCDFDAO) {
        this.atlasNetCDFDAO = atlasNetCDFDAO;
    }

    public void handleRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        // Get search filters from request
        // Experiment accession
        String expAcc = StringUtils.trimToNull(request.getParameter("eid"));
        // Gene ids
        String geneIdsStr = StringUtils.trimToNull(request.getParameter("gid"));
        // Experimental factor
        String ef = StringUtils.trimToNull(request.getParameter("ef"));
        // Array design id
        String ad = StringUtils.trimToNull(request.getParameter("ad"));

        if (expAcc != null && !"".equals(expAcc)) {
            final AtlasExperiment exp = atlasSolrDAO.getExperimentByAccession(expAcc);
            if (exp != null) {
                request.setAttribute("exp", exp);
                request.setAttribute("eid", exp.getId());
                request.setAttribute("gid", geneIdsStr);
                request.setAttribute("ef", ef);
                request.setAttribute("arrayDesigns", exp.getPlatform().split(","));

                request.setAttribute("arrayDesign", findSuitableArrayDesign(geneIdsStr, ad, exp));
            } else {
                ErrorResponseHelper.errorNotFound(request, response, "No records exist for experiment " + String.valueOf(expAcc));
                return;
            }
        }

        request.getRequestDispatcher("/WEB-INF/jsp/experimentpage/experiment.jsp").forward(request, response);
    }

    private String findSuitableArrayDesign(String geneIdsStr, String ad, AtlasExperiment exp) throws IOException {
        if (geneIdsStr == null) {
            return exp.getArrayDesign(ad);
        }

        Set<Long> geneIds = new HashSet<Long>(Collections2.transform(findGenes(geneIdsStr), GENE_TO_IDS));

        NetCDFProxy proxy = atlasNetCDFDAO.findProxy(exp.getAccession(), null, geneIds);
        if (proxy != null) {
            try {
                return proxy.getArrayDesignAccession();
            } finally {
                proxy.close();
            }
        }

        return exp.getArrayDesign(ad);
    }

    private Collection<AtlasGene> findGenes(String geneIdsStr) {
        AtlasStructuredQuery query = createGeneQuery(geneIdsStr);
        AtlasStructuredQueryResult queryResult = queryService.doStructuredAtlasQuery(query);
        Collection<AtlasGene> genes = new HashSet<AtlasGene>();
        for (StructuredResultRow row : queryResult.getResults()) {
            AtlasGene gene = row.getGene();
            genes.add(gene);
        }
        return genes;
    }

    private AtlasStructuredQuery createGeneQuery(String geneIdsStr) {
        AtlasStructuredQueryBuilder qb = new AtlasStructuredQueryBuilder();
        qb.viewAs(ViewType.HEATMAP);
        qb.andGene("", Collections.singletonList(geneIdsStr));
        qb.expsPerGene(Integer.MAX_VALUE);

        AtlasStructuredQuery query = qb.query();
        query.setFullHeatmap(false);
        query.setConditions(Collections.<ExpFactorQueryCondition>emptyList());
        return query;
    }
}
