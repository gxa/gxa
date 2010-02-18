package uk.ac.ebi.gxa.requesthandlers.experimentpage;

import ae3.dao.AtlasDao;
import ae3.model.AtlasExperiment;
import ae3.model.AtlasGene;
import ae3.model.ListResultRow;
import ae3.service.structuredquery.AtlasStructuredQueryService;
import org.apache.commons.lang.StringUtils;
import org.springframework.web.HttpRequestHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import uk.ac.ebi.gxa.netcdf.reader.NetCDFProxy;

/**
 * @author pashky
 */
public class ExperimentPageRequestHandler implements HttpRequestHandler {

    private AtlasDao dao;
    private AtlasStructuredQueryService queryService;
    private File atlasNetCDFRepo;

    public void setDao(AtlasDao dao) {
        this.dao = dao;
    }

    public void setQueryService(AtlasStructuredQueryService queryService) {
        this.queryService = queryService;
    }

    public void setAtlasNetCDFRepo(File atlasNetCDFRepo) {
        this.atlasNetCDFRepo = atlasNetCDFRepo;
    }


    public void handleRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String expAcc = request.getParameter("eid");
        String geneIds = request.getParameter("gid");
        String ef = request.getParameter("ef");

        if (expAcc != null && !"".equals(expAcc)) {
            final AtlasExperiment exp = dao.getExperimentByAccession(expAcc);
            if (exp != null) {
                request.setAttribute("exp", exp);
                request.setAttribute("eid", exp.getId());

                List<ListResultRow> topGenes = queryService.findGenesForExperiment("", exp.getId(), 0, 10);
                request.setAttribute("geneList", topGenes);

                Collection<AtlasGene> genes = new ArrayList<AtlasGene>();
                if(geneIds != null) {
                    for(String geneQuery : StringUtils.split(geneIds, ",")) {
                        AtlasDao.AtlasGeneResult result = dao.getGeneByIdentifier(geneQuery);
                        if (result.isFound()) {
                            genes.add(result.getGene());
                        }
                    }
                } else {
                    for(ListResultRow lrr : topGenes.size() > 5 ? topGenes.subList(0, 5) : topGenes)
                        genes.add(lrr.getGene());
                }

                if(genes.isEmpty()) {
                    File[] netCDFs = atlasNetCDFRepo.listFiles(new FilenameFilter() {
                        public boolean accept(File file, String name) {
                            return name.matches("^" + exp.getId() + "_[0-9]+(_ratios)?\\.nc$");
                        }
                    });
                    if(netCDFs.length == 0) {
                        error(request, response, "NetCDF for experiment " + String.valueOf(expAcc) + " is not found");
                        return;
                    }
                    NetCDFProxy netcdf = new NetCDFProxy(netCDFs[0]);
                    for(int geneId : netcdf.getGenes()) {
                        AtlasDao.AtlasGeneResult result = dao.getGeneById(String.valueOf(geneId));
                        if (result.isFound()) {
                            genes.add(result.getGene());
                            if(genes.size() >= 5)
                                break;
                        }
                    }
                    String[] factors = netcdf.getFactors();
                    if(factors.length > 0)
                        ef = factors[0];
                    if(genes.isEmpty() || ef == null) {
                        error(request, response, "No genes to display for experiment " + String.valueOf(expAcc));
                        return;
                    }
                }

                request.setAttribute("genes", genes);
            } else {
                error(request, response, "There are no records for experiment " + String.valueOf(expAcc));
                return;
            }
        }

        request.setAttribute("ef", ef);
        request.getRequestDispatcher("/experiment.jsp").forward(request, response);
    }

    private void error(HttpServletRequest request, HttpServletResponse response, String message) throws ServletException, IOException {
        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        request.setAttribute("errorMessage", message);
        request.getRequestDispatcher("/error.jsp").forward(request,response);
    }
}
