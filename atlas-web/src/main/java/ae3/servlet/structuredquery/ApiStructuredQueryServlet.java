package ae3.servlet.structuredquery;

import ae3.dao.AtlasDao;
import ae3.model.AtlasExperiment;
import ae3.model.AtlasGene;
import ae3.service.ArrayExpressSearchService;
import ae3.service.structuredquery.*;
import ae3.servlet.structuredquery.result.*;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Collection;

/**
 * REST APIU structured query servlet. Handles all gene and experiment API queries according to HTTP request parameters
 * @author pashky
 */
public class ApiStructuredQueryServlet extends RestServlet {

    @Override
    public Object process(HttpServletRequest request) {
        final String experimentId = request.getParameter("experiment");
        if(experimentId != null) {
            AtlasDao dao = ArrayExpressSearchService.instance().getAtlasDao();
            AtlasExperiment exp = dao.getExperimentByAccession(experimentId);
            if(exp == null)
                exp = dao.getExperimentById(experimentId);
            if(exp == null) {
                Collection<AtlasExperiment> exps = dao.getExperimentsByQuery(experimentId, 0, 5000);
                if(exps.isEmpty())
                    return new ErrorResult("No such experiment found for: " + experimentId);
                else
                    return exps;
            } else {
                List<AtlasGene> genes = new ArrayList<AtlasGene>();
                final String[] geneIds = request.getParameterValues("gene");
                if(geneIds != null) {
                    if(geneIds.length == 1 && geneIds[0].startsWith("top")) {
                        int nTop = 10;
                        try {
                            nTop = Integer.valueOf(geneIds[0].substring(3));
                            if(nTop > 100)
                                nTop = 100;
                        } catch(Exception e) {/**/}
                        for(StructuredResultRow r : ArrayExpressSearchService.instance().getStructQueryService().findGenesForExperiment("", experimentId, 0, nTop).getResults()) {
                            genes.add(r.getGene());
                        }
                    } else
                        for(String geneId : geneIds) {
                            AtlasDao.AtlasGeneResult agr = dao.getGeneByIdentifier(geneId);
                            if(agr.isFound() && !genes.contains(agr.getGene()))
                                genes.add(agr.getGene());
                        }
                }
                setRestProfile(ExperimentRestProfile.class);
                return new ExperimentResultAdapter(exp, genes);
            }
        } else {
            final AtlasStructuredQueryService asqs = ArrayExpressSearchService.instance().getStructQueryService();

            AtlasStructuredQuery atlasQuery = AtlasStructuredQueryParser.parseRestRequest(request,
                    GeneProperties.allPropertyIds(),
                    asqs.getExperimentalFactors());

            if(!atlasQuery.isNone()) {
                atlasQuery.setFullHeatmap(true);
                atlasQuery.setViewType(ViewType.HEATMAP);
                AtlasStructuredQueryResult atlasResult = asqs.doStructuredAtlasQuery(atlasQuery);
                return new HeatmapResultAdapter(atlasResult);
            } else {
                return new ErrorResult("Empty query specified");
            }
        }
    }

}
