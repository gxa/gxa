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
import com.google.common.base.Strings;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.HttpRequestHandler;
import uk.ac.ebi.gxa.dao.AtlasDAO;
import uk.ac.ebi.gxa.netcdf.reader.AtlasNetCDFDAO;
import uk.ac.ebi.gxa.netcdf.reader.NetCDFProxy;
import uk.ac.ebi.gxa.requesthandlers.base.ErrorResponseHelper;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * @author pashky
 */
@Deprecated
public class ExperimentPage_DesignRequestHandler implements HttpRequestHandler {

    protected final static Logger log = LoggerFactory.getLogger(ExperimentPage_DesignRequestHandler.class);

    private AtlasSolrDAO atlasSolrDAO;
    private AtlasNetCDFDAO atlasNetCDFDAO;
    private AtlasDAO atlasDAO;

    public void setDao(AtlasSolrDAO atlasSolrDAO) {
        this.atlasSolrDAO = atlasSolrDAO;
    }

    public void setAtlasNetCDFDAO(AtlasNetCDFDAO atlasNetCDFDAO) {
        this.atlasNetCDFDAO = atlasNetCDFDAO;
    }

    public void setAtlasDAO(AtlasDAO atlasDAO) {
        this.atlasDAO = atlasDAO;
    }

    static class AssayInfo {
        private String name;
        private String arrayDesignAccession;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getArrayDesignAccession() {
            return arrayDesignAccession;
        }

        public void setArrayDesignAccession(String arrayDesignAccession) {
            this.arrayDesignAccession = arrayDesignAccession;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            AssayInfo assayInfo = (AssayInfo) o;

            if (arrayDesignAccession != null ? !arrayDesignAccession.equals(assayInfo.arrayDesignAccession) : assayInfo.arrayDesignAccession != null)
                return false;
            if (name != null ? !name.equals(assayInfo.name) : assayInfo.name != null) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = name != null ? name.hashCode() : 0;
            result = 31 * result + (arrayDesignAccession != null ? arrayDesignAccession.hashCode() : 0);
            return result;
        }
    }

    public static class Assay{
        private AssayInfo info;
        private List<String> factorValues;

        public Assay(AssayInfo info) {
            this.info = info;
        }

        public List<String> getFactorValues(){
            if(null==this.factorValues){
                this.factorValues=new ArrayList<String>();
            }
            return this.factorValues;
        }

        public String getName(){
            return this.info.getName();
        }

        public String getArrayDesignAccession(){
            return this.info.getArrayDesignAccession();
        }
        }

    public static class ExperimentFactor{
        private String name;
        public ExperimentFactor(String name){
            this.name = name;
        }
        public String getName(){
            return name;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            ExperimentFactor that = (ExperimentFactor) o;

            if (name != null ? !name.equals(that.name) : that.name != null) return false;

            return true;
    }

        @Override
        public int hashCode() {
            return name != null ? name.hashCode() : 0;
        }
    }

    public static class ExperimentDesign{
        private Map<ExperimentFactor, Map<AssayInfo, String>> factors = new LinkedHashMap<ExperimentFactor, Map<AssayInfo, String>>();
        private Set<AssayInfo> assays = new LinkedHashSet<AssayInfo>();

        boolean addFactor(String factorName) {
            ExperimentFactor factor = new ExperimentFactor(factorName);
            if (!factors.containsKey(factor)) {
                factors.put(factor, new HashMap<AssayInfo, String>());
                return true;
            }
            return false;
        }

        void addAssay(String factorName, AssayInfo assay, String value) {
            ExperimentFactor factor = new ExperimentFactor(factorName);
            if (factors.get(factor).put(assay, value) != null) {
                log.error("One more value for factor {} and assay {}", factorName, assay.name);
            }
            assays.add(assay);
        }

        void addDesign(ExperimentDesign design) {
            for(ExperimentFactor factor : design.factors.keySet()) {
                if (factors.containsKey(factor)) {
                    factors.get(factor).putAll(design.factors.get(factor));
                } else {
                    factors.put(factor, design.factors.get(factor));
                }
            }

            assays.addAll(design.assays);
        }

        public List<ExperimentFactor> getFactors(){
            List<ExperimentFactor> list = new ArrayList<ExperimentFactor>();
            list.addAll(factors.keySet());
            return list;
        }

        public List<Assay> getAssays(){
            List<Assay> list = new ArrayList<Assay>();
            for(AssayInfo info : assays) {
               Assay assay = new Assay(info);
               for(Map<AssayInfo, String> factorValues : factors.values()) {
                   String value = factorValues.get(info);
                   assay.getFactorValues().add(value == null ? "" : value);
        }
               list.add(assay);
    }
            return list;
        }
    }

    //merge experimental factors from all designs, and create assays with factor values either blank (if factor
    // not found in this design, or actual)
    public ExperimentDesign mergeExperimentDesigns(Collection<ExperimentDesign> designs){
        //no mashing water in the bucket
        if (designs.size()<2)
            return designs.iterator().next();
        
        ExperimentDesign result = new ExperimentDesign();
        for(ExperimentDesign design : designs){
            result.addDesign(design);
                }

        return result;
    }

    public void handleRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String expAcc = StringUtils.trimToNull(request.getParameter("eid"));

        if (Strings.isNullOrEmpty(expAcc)) {
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

        File[] netCDFs = atlasNetCDFDAO.listNetCDFs(expAcc);
        if (netCDFs.length == 0) {
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
                experimentDesign.addFactor(factor);
            factorValues.put(factor, netcdf.getFactorValues(factor));
        }

            List<String> sampleCharacteristicsNotFactors = new ArrayList<String>();

        String[] netCdfSampleCharacteristics = netcdf.getCharacteristics();
        Map<String,String[]> characteristicValues = new HashMap<String,String[]>();
        for(String factor : netCdfSampleCharacteristics){
            characteristicValues.put(factor, netcdf.getCharacteristicValues(factor));
                if (experimentDesign.addFactor(factor)) {
                    sampleCharacteristicsNotFactors.add(factor);
        }
            }

        int[][] samplesToAssay = netcdf.getSamplesToAssays();

        int iAssay = 0;

        List<uk.ac.ebi.microarray.atlas.model.Assay> assays = atlasDAO.getAssaysByExperimentAccession(expAcc);
            
        for(long assayId : netcdf.getAssays()){
                AssayInfo assay = new AssayInfo();
                assay.setName(findAssayAccession(assayId, assays));
                assay.setArrayDesignAccession(netcdf.getArrayDesignAccession());

            for(String factor :  netCdfFactors){
                    experimentDesign.addAssay(factor, assay, factorValues.get(factor)[iAssay]);
            }

            for (String factor : sampleCharacteristicsNotFactors) {
                StringBuilder allValuesOfThisFactor = new StringBuilder();
                for (int iSample : getSamplesForAssay(iAssay, samplesToAssay)) {
                    if (characteristicValues.get(factor).length > 0)
                        allValuesOfThisFactor.append(characteristicValues.get(factor)[iSample]);
                }
                experimentDesign.addAssay(factor, assay, allValuesOfThisFactor.toString());
            }

            ++iAssay;
        }

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
