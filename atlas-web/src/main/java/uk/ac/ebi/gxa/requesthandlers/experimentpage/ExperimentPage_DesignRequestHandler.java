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
import ae3.service.structuredquery.AtlasStructuredQueryService;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.web.HttpRequestHandler;
import uk.ac.ebi.gxa.dao.AtlasDAO;
import uk.ac.ebi.gxa.netcdf.reader.NetCDFProxy;
import uk.ac.ebi.gxa.requesthandlers.base.ErrorResponseHelper;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.*;

/**
 * @author pashky
 */
public class ExperimentPage_DesignRequestHandler implements HttpRequestHandler {

    private AtlasSolrDAO atlasSolrDAO;
    private AtlasStructuredQueryService queryService;
    private File atlasNetCDFRepo;
    private AtlasDAO atlasDAO;

    public void setDao(AtlasSolrDAO atlasSolrDAO) {
        this.atlasSolrDAO = atlasSolrDAO;
    }

    public void setQueryService(AtlasStructuredQueryService queryService) {
        this.queryService = queryService;
    }

    public void setAtlasNetCDFRepo(File atlasNetCDFRepo) {
        this.atlasNetCDFRepo = atlasNetCDFRepo;
    }

    public void setAtlasDAO(AtlasDAO atlasDAO) {
        this.atlasDAO = atlasDAO;
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

    //merge experimental factors from all designs, and create assays with factor values either blank (if factor
    // not found in this design, or actual)
    public ExperimentDesign mergeExperimentDesigns(Collection<ExperimentDesign> designs){
        //no mashing water in the bucket
        if (designs.size()<2)
            return designs.iterator().next();
        
        ExperimentDesign result = new ExperimentDesign();
        String emptyString = "";

        Map<String,Integer[]> ordinalOfFactorForEachDesign = new HashMap<String,Integer[]>();

        int iDesign = 0;
        for(ExperimentDesign design : designs){
            for(ExperimentFactor factor : design.getFactors()){
                if(!ordinalOfFactorForEachDesign.containsKey(factor.getName())){
                    ordinalOfFactorForEachDesign.put(factor.getName(), new Integer[designs.size()]); //initialize array with nulls
                }
                ordinalOfFactorForEachDesign.get(factor.getName())[iDesign] = Collections.binarySearch(design.getFactors(),factor,new Comparator<ExperimentFactor>(){
                    public int compare(ExperimentFactor f1, ExperimentFactor f2){
                        return f1.getName().compareTo(f2.getName());
                    }
                });
            }
            iDesign++;
        }

        for(String factorName : ordinalOfFactorForEachDesign.keySet()){
            result.getFactors().add(new ExperimentFactor(factorName));
        }

        iDesign = 0;
        for(ExperimentDesign design : designs){
            for(Assay assay : design.getAssays()){
                Assay newAssay = new Assay();
                newAssay.setName(assay.getName());
                newAssay.setArrayDesignAccession(assay.getArrayDesignAccession());
                for(ExperimentFactor factor : result.getFactors()){
                    Integer ordinalOfFactorForThisDesign = ordinalOfFactorForEachDesign.get(factor.getName())[iDesign];
                    String factorValue = (ordinalOfFactorForThisDesign < 0 ? emptyString : assay.getFactorValues().get(ordinalOfFactorForThisDesign));
                    newAssay.getFactorValues().add(factorValue);
                }
                result.getAssays().add(newAssay);
            }
            iDesign++;
        }

        return result;
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

        request.setAttribute("arrayDesigns", exp.getPlatform().split(","));
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

        List<ExperimentDesign> designs = new ArrayList<ExperimentDesign>();

        for(File netCdfFile : netCDFs){
        ExperimentDesign experimentDesign = new ExperimentDesign();

        NetCDFProxy netcdf = new NetCDFProxy(netCdfFile);

        String[] netCdfFactors = netcdf.getFactors();
        Map<String,String[]> factorValues = new HashMap<String,String[]>();
        for(String factor : netCdfFactors){
            experimentDesign.getFactors().add(new ExperimentFactor(factor));
            factorValues.put(factor, netcdf.getFactorValues(factor));
        }

        String[] netCdfSampleCharacteristics = netcdf.getCharacteristics();
        Map<String,String[]> characteristicValues = new HashMap<String,String[]>();
        for(String factor : netCdfSampleCharacteristics){
            characteristicValues.put(factor, netcdf.getCharacteristicValues(factor));
        }
        int[][] samplesToAssay = netcdf.getSamplesToAssays();

        List<String> sampleCharacteristicsNotFactors = new ArrayList<String>();
        for(String sampleCharacteristic : netCdfSampleCharacteristics){
            if(!ArrayUtils.contains(netCdfFactors, sampleCharacteristic)){
                sampleCharacteristicsNotFactors.add(sampleCharacteristic);
                experimentDesign.getFactors().add(new ExperimentFactor(sampleCharacteristic));
            }
        }

        int iAssay = 0;

        List<uk.ac.ebi.microarray.atlas.model.Assay> assays = atlasDAO.getAssaysByExperimentAccession(exp.getAccession());
            
        for(long assayId : netcdf.getAssays()){
            Assay assay=new Assay();
            assay.setName(findAssayAccession(assayId, assays)); // String.format("%05d",)
            for(String factor :  netCdfFactors){
                assay.getFactorValues().add(factorValues.get(factor)[iAssay]);
            }

            for(String factor : sampleCharacteristicsNotFactors){
                String allValuesOfThisFactor = "";
                for(int iSample : getSamplesForAssay(iAssay,samplesToAssay)){
                    if(characteristicValues.get(factor).length>0) //it is empty array sometimes
                        allValuesOfThisFactor += characteristicValues.get(factor)[iSample];
                }
                assay.getFactorValues().add(allValuesOfThisFactor);
            }
            assay.setArrayDesignAccession(netcdf.getArrayDesignAccession());
            experimentDesign.getAssays().add(assay);
            ++iAssay;
            //if(iAssay>100)//do not show more then 100 assays for now
                //break;
        }
        //samplesToAssays[]
        netcdf.close();
            designs.add(experimentDesign);
        }

        request.setAttribute("experimentDesign",mergeExperimentDesigns(designs));

        String ad = StringUtils.trimToNull(request.getParameter("ad"));
        request.setAttribute("arrayDesign", exp.getArrayDesign(ad));

        request.getRequestDispatcher("/WEB-INF/jsp/experimentpage/experiment-design.jsp").forward(request, response);
    }

    private List<Integer> getSamplesForAssay(int iAssay,int[][] samplesToAssayMap){
        ArrayList<Integer> result = new ArrayList<Integer>();
        for(int iSample = 0; iSample!= samplesToAssayMap.length; iSample++){
            if(1 == samplesToAssayMap[iSample][iAssay]){
                result.add(iSample);
            }
        }
        return result;
    }


    private String findAssayAccession(long AssayID, List<uk.ac.ebi.microarray.atlas.model.Assay> assays){
        for(uk.ac.ebi.microarray.atlas.model.Assay a : assays){
            if(AssayID==a.getAssayID()){
                return a.getAccession();
            }
        }
        return String.format("%d",AssayID);
    }
}
