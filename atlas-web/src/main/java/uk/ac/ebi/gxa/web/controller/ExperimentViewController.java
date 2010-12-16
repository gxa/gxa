package uk.ac.ebi.gxa.web.controller;

import ae3.dao.AtlasSolrDAO;
import ae3.model.AtlasExperiment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import uk.ac.ebi.gxa.dao.AtlasDAO;
import uk.ac.ebi.gxa.netcdf.reader.AtlasNetCDFDAO;
import uk.ac.ebi.gxa.netcdf.reader.NetCDFProxy;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * A code moved from ExperimentPageRequestHandler and ExperimentPage_DesignRequestHandler.
 *
 * @author Olga Melnichuk
 *         Date: Nov 29, 2010
 */
@Controller
public class ExperimentViewController extends AtlasViewController {

    protected final static Logger log = LoggerFactory.getLogger(ExperimentViewController.class);

    private AtlasSolrDAO atlasSolrDAO;
    private AtlasNetCDFDAO atlasNetCDFDAO;
    private AtlasDAO atlasDAO;

    @Autowired
    public ExperimentViewController(AtlasSolrDAO atlasSolrDAO, AtlasNetCDFDAO atlasNetCDFDAO, AtlasDAO atlasDAO) {
        this.atlasSolrDAO = atlasSolrDAO;
        this.atlasNetCDFDAO = atlasNetCDFDAO;
        this.atlasDAO = atlasDAO;
    }

    @RequestMapping(value = "/experiment", method = RequestMethod.GET)
    public String getExperiment(
            @RequestParam("eid") String accession,
            @RequestParam(value = "gid", required = false) String gid,
            @RequestParam(value = "ef", required = false) String ef,
            @RequestParam(value = "ad", required = false) String ad,
            Model model) throws ResourceNotFoundException {

        AtlasExperiment exp = getExperimentByAccession(accession);
        model.addAttribute("exp", exp)
                .addAttribute("eid", exp.getId())
                .addAttribute("gid", gid)
                .addAttribute("ef", ef)
                .addAttribute("arrayDesigns", exp.getPlatform().split(","))
                .addAttribute("arrayDesign", exp.getArrayDesign(ad));

        return "experimentpage/experiment";
    }

    @RequestMapping(value = "/experimentDesign", method = RequestMethod.GET)
    public String getExperimentDesign(
            @RequestParam("eid") String accession,
            @RequestParam(value = "ad", required = false) String ad,
            Model model) throws ResourceNotFoundException, IOException {

        final AtlasExperiment exp = getExperimentByAccession(accession);
        File[] netCDFs = getNetCDFsByAccession(accession);

        List<ExperimentDesignUI> designs = new ArrayList<ExperimentDesignUI>();

        for (File netCdfFile : netCDFs) {
            ExperimentDesignUI experimentDesign = new ExperimentDesignUI();

            NetCDFProxy netcdf = new NetCDFProxy(netCdfFile);

            String[] netCdfFactors = netcdf.getFactors();
            Map<String, String[]> factorValues = new HashMap<String, String[]>();
            for (String factor : netCdfFactors) {
                experimentDesign.addFactor(factor);
                factorValues.put(factor, netcdf.getFactorValues(factor));
            }

            List<String> sampleCharacteristicsNotFactors = new ArrayList<String>();

            String[] netCdfSampleCharacteristics = netcdf.getCharacteristics();
            Map<String, String[]> characteristicValues = new HashMap<String, String[]>();
            for (String factor : netCdfSampleCharacteristics) {
                characteristicValues.put(factor, netcdf.getCharacteristicValues(factor));
                if (experimentDesign.addFactor(factor)) {
                    sampleCharacteristicsNotFactors.add(factor);
                }
            }

            int[][] samplesToAssay = netcdf.getSamplesToAssays();

            int iAssay = 0;

            List<uk.ac.ebi.microarray.atlas.model.Assay> assays = atlasDAO.getAssaysByExperimentAccession(accession);

            for (long assayId : netcdf.getAssays()) {
                AssayInfo assay = new AssayInfo();
                assay.setName(findAssayAccession(assayId, assays));
                assay.setArrayDesignAccession(netcdf.getArrayDesignAccession());

                for (String factor : netCdfFactors) {
                    experimentDesign.addAssay(factor, assay, factorValues.get(factor)[iAssay]);
                }

                for (String factor : sampleCharacteristicsNotFactors) {
                    String allValuesOfThisFactor = "";
                    for (int iSample : getSamplesForAssay(iAssay, samplesToAssay)) {
                        if (characteristicValues.get(factor).length > 0)
                            allValuesOfThisFactor += characteristicValues.get(factor)[iSample];
                    }
                    experimentDesign.addAssay(factor, assay, allValuesOfThisFactor);
                }

                ++iAssay;
            }

            netcdf.close();
            designs.add(experimentDesign);
        }

        model.addAttribute("experimentDesign", mergeExperimentDesigns(designs))
                .addAttribute("arrayDesign", exp.getArrayDesign(ad))
                .addAttribute("arrayDesigns", exp.getPlatform().split(","))
                .addAttribute("exp", exp)
                .addAttribute("eid", exp.getId());

        return "experimentpage/experiment-design";
    }

    //merge experimental factors from all designs, and create assays with factor values either blank (if factor
    // not found in this design, or actual)
    public ExperimentDesignUI mergeExperimentDesigns(Collection<ExperimentDesignUI> designs) {
        if (designs.size() == 1)
            return designs.iterator().next();

        ExperimentDesignUI result = new ExperimentDesignUI();
        for (ExperimentDesignUI design : designs) {
            result.addDesign(design);
        }

        return result;
    }

    private List<Integer> getSamplesForAssay(int iAssay, int[][] samplesToAssayMap) {
        ArrayList<Integer> result = new ArrayList<Integer>();
        for (int i = 0; i != samplesToAssayMap.length; i++) {
            if (1 == samplesToAssayMap[i][iAssay]) {
                result.add(i);
            }
        }
        return result;
    }

    private String findAssayAccession(long AssayID, List<uk.ac.ebi.microarray.atlas.model.Assay> assays) {
        for (uk.ac.ebi.microarray.atlas.model.Assay a : assays) {
            if (AssayID == a.getAssayID()) {
                return a.getAccession();
            }
        }
        return String.format("%d", AssayID);
    }

    private File[] getNetCDFsByAccession(String accession) throws ResourceNotFoundException {
        File[] netCDFs = atlasNetCDFDAO.listNetCDFs(accession);
        if (netCDFs.length == 0) {
            throw new ResourceNotFoundException("NetCDF for experiment " + accession + " is not found");
        }
        return netCDFs;
    }

    private AtlasExperiment getExperimentByAccession(String accession) throws ResourceNotFoundException {
        final AtlasExperiment exp = atlasSolrDAO.getExperimentByAccession(accession);

        if (exp == null) {
            throw new ResourceNotFoundException("There are no records for experiment " + accession);
        }
        return exp;
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

    public static class AssayUI {
        private AssayInfo info;
        private List<String> factorValues;

        public AssayUI(AssayInfo info) {
            this.info = info;
        }

        public List<String> getFactorValues() {
            if (null == this.factorValues) {
                this.factorValues = new ArrayList<String>();
            }
            return this.factorValues;
        }

        public String getName() {
            return this.info.getName();
        }

        public String getArrayDesignAccession() {
            return this.info.getArrayDesignAccession();
        }
    }

    public static class ExperimentFactorUI {
        private String name;

        public ExperimentFactorUI(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            ExperimentFactorUI that = (ExperimentFactorUI) o;

            if (name != null ? !name.equals(that.name) : that.name != null) return false;

            return true;
        }

        @Override
        public int hashCode() {
            return name != null ? name.hashCode() : 0;
        }
    }

    public static class ExperimentDesignUI {
        private Map<ExperimentFactorUI, Map<AssayInfo, String>> factors = new LinkedHashMap<ExperimentFactorUI, Map<AssayInfo, String>>();
        private Set<AssayInfo> assays = new LinkedHashSet<AssayInfo>();

        boolean addFactor(String factorName) {
            ExperimentFactorUI factor = new ExperimentFactorUI(factorName);
            if (!factors.containsKey(factor)) {
                factors.put(factor, new HashMap<AssayInfo, String>());
                return true;
            }
            return false;
        }

        void addAssay(String factorName, AssayInfo assay, String value) {
            ExperimentFactorUI factor = new ExperimentFactorUI(factorName);
            if (factors.get(factor).put(assay, value) != null) {
                log.error("One more value for factor {} and assay {}", factorName, assay.name);
            }
            assays.add(assay);
        }

        void addDesign(ExperimentDesignUI design) {
            for (ExperimentFactorUI factor : design.factors.keySet()) {
                if (factors.containsKey(factor)) {
                    factors.get(factor).putAll(design.factors.get(factor));
                } else {
                    factors.put(factor, design.factors.get(factor));
                }
            }

            assays.addAll(design.assays);
        }

        public List<ExperimentFactorUI> getFactors() {
            List<ExperimentFactorUI> list = new ArrayList<ExperimentFactorUI>();
            list.addAll(factors.keySet());
            return list;
        }

        public List<AssayUI> getAssays() {
            List<AssayUI> list = new ArrayList<AssayUI>();
            for (AssayInfo info : assays) {
                AssayUI assay = new AssayUI(info);
                for (Map<AssayInfo, String> factorValues : factors.values()) {
                    String value = factorValues.get(info);
                    assay.getFactorValues().add(value == null ? "" : value);
                }
                list.add(assay);
            }
            return list;
        }
    }

}
