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
import ae3.model.ListResultRow;
import ae3.service.structuredquery.AtlasStructuredQueryService;
import org.apache.commons.lang.StringUtils;
import org.springframework.web.HttpRequestHandler;
import uk.ac.ebi.gxa.netcdf.reader.NetCDFProxy;
import uk.ac.ebi.gxa.properties.AtlasProperties;
import uk.ac.ebi.gxa.requesthandlers.base.ErrorResponseHelper;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author pashky
 */
public class ExperimentPage_DesignRequestHandler implements HttpRequestHandler {

    private AtlasSolrDAO atlasSolrDAO;
    private AtlasStructuredQueryService queryService;
    private File atlasNetCDFRepo;


    public void setDao(AtlasSolrDAO atlasSolrDAO) {
        this.atlasSolrDAO = atlasSolrDAO;
    }

    public void setQueryService(AtlasStructuredQueryService queryService) {
        this.queryService = queryService;
    }

    public void setAtlasNetCDFRepo(File atlasNetCDFRepo) {
        this.atlasNetCDFRepo = atlasNetCDFRepo;
    }


    public class Assay{
        String name;
        String arrayDesignAccession;
        private List<String> factorValues;
        public List<String> getFactorValues(){
            if(null==this.factorValues){
                this.factorValues=new ArrayList<String>();
            }
            return this.factorValues;
        }
        public String getName(){
            return this.name;
        }
        public void setName(String name){
            this.name=name;
        }
        public String getArrayDesignAccession(){
            return this.arrayDesignAccession;
        }
        public void setArrayDesignAccession(String arrayDesignAccession){
           this.arrayDesignAccession = arrayDesignAccession;
        }
    }

    public class ExperimentFactor{
        private String name;
        public ExperimentFactor(String name){
            this.name = name;
        }
        public String getName(){
            return name;
        }
    }

    public class ExperimentDesign{
        private List<ExperimentFactor> factors;
        private List<Assay> assays;
        public List<ExperimentFactor> getFactors(){
            if(null==factors)
                factors = new ArrayList<ExperimentFactor>();
            return factors;
        }
        public List<Assay> getAssays(){
            if(null==assays)
                assays=new ArrayList<Assay>();
            return assays;
        }
    }


    public void handleRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String expAcc = StringUtils.trimToNull(request.getParameter("eid"));
        //String geneIds = StringUtils.trimToNull(request.getParameter("gid"));
        //String ef = StringUtils.trimToNull(request.getParameter("ef"));

        if (!(expAcc != null && !"".equals(expAcc))) {
            ErrorResponseHelper.errorNotFound(request, response, "There are no records for experiment " + "NULL");
            return;
        }

        final AtlasExperiment exp = atlasSolrDAO.getExperimentByAccession(expAcc);

        if (exp == null) {
            ErrorResponseHelper.errorNotFound(request, response, "There are no records for experiment " + String.valueOf(expAcc));
            return;
        }

        request.setAttribute("exp", exp);
        request.setAttribute("eid", exp.getId());

                    File[] netCDFs = atlasNetCDFRepo.listFiles(new FilenameFilter() {
                        public boolean accept(File file, String name) {
                            return name.matches("^" + exp.getId() + "_[0-9]+(_ratios)?\\.nc$");
                        }
                    });
                    if(netCDFs.length == 0) {
                        ErrorResponseHelper.errorNotFound(request, response, "NetCDF for experiment " + String.valueOf(expAcc) + " is not found");
                        return;
                    }

                    NetCDFProxy netcdf = new NetCDFProxy(netCDFs[0]);

                    /*
                    String[] factors = netcdf.getFactors();
                    for(String factor : factors){
                        netcdf.getFactorValues(factor);
                    }
                    */

        ExperimentDesign experimentDesign = new ExperimentDesign();

        for(String factor :  netcdf.getFactors()){
            experimentDesign.getFactors().add(new ExperimentFactor(factor));
        }

        int iAssay = 0;
        for(long assayId : netcdf.getAssays()){
            Assay assay=new Assay();
            assay.setName(String.format("%05d",assayId));
            for(String factor :  netcdf.getFactors()){
                assay.getFactorValues().add(netcdf.getFactorValues(factor)[iAssay]);
            }
            assay.setArrayDesignAccession(netcdf.getArrayDesignAccession());
            experimentDesign.getAssays().add(assay);
            ++iAssay;

            if(iAssay>100)//do not show more then 100 assays for now
                break;
        }
        netcdf.close();

                //request.setAttribute("genes", genes);

        request.setAttribute("experimentDesign",experimentDesign);

        request.getRequestDispatcher("/WEB-INF/jsp/experimentpage/experiment_design.jsp").forward(request, response);
    }

}
