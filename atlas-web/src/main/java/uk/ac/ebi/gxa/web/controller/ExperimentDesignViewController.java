package uk.ac.ebi.gxa.web.controller;

import ae3.dao.ExperimentSolrDAO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import uk.ac.ebi.gxa.dao.AtlasDAO;
import uk.ac.ebi.gxa.netcdf.reader.AtlasNetCDFDAO;
import uk.ac.ebi.gxa.netcdf.reader.NetCDFProxy;
import uk.ac.ebi.microarray.atlas.model.Experiment;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static com.google.common.io.Closeables.closeQuietly;

/**
 * @author Olga Melnichuk
 *         Date: 15/03/2011
 */
@Controller
public class ExperimentDesignViewController extends ExperimentViewControllerBase {

    private AtlasNetCDFDAO atlasNetCDFDAO;

    @Autowired
    public ExperimentDesignViewController(ExperimentSolrDAO solrDAO, AtlasNetCDFDAO atlasNetCDFDAO, AtlasDAO atlasDAO) {
        super(solrDAO, atlasDAO);
        this.atlasNetCDFDAO = atlasNetCDFDAO;
    }

    @RequestMapping(value = "/experimentDesign", method = RequestMethod.GET)
    public String getExperimentDesign(
            @RequestParam("eid") String accession,
            Model model) throws ResourceNotFoundException, IOException {

        ExperimentPage expPage = createExperimentPage(accession);
        expPage.enhance(model);

        model.addAttribute("experimentDesign", constructExperimentDesign(expPage.getExp().getExperiment()));
        return "experimentpage/experiment-design";
    }

    private ExperimentDesignUI constructExperimentDesign(Experiment exp) throws ResourceNotFoundException, IOException {
        File[] netCDFs = getNetCDFs(exp);

        List<ExperimentDesignUI> designs = new ArrayList<ExperimentDesignUI>();

        for (File netCdfFile : netCDFs) {
            ExperimentDesignUI experimentDesign = new ExperimentDesignUI();

            NetCDFProxy netcdf = null;
            try {
                netcdf = new NetCDFProxy(netCdfFile);

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

                int iAssay = 0;

                for (String assayAccession : netcdf.getAssayAccessions()) {
                    AssayInfo assay = new AssayInfo();
                    assay.setName(assayAccession);
                    assay.setArrayDesignAccession(netcdf.getArrayDesignAccession());

                    for (String factor : netCdfFactors) {
                        experimentDesign.addAssay(factor, assay, factorValues.get(factor)[iAssay]);
                    }

                    for (String factor : sampleCharacteristicsNotFactors) {
                        StringBuilder allValuesOfThisFactor = new StringBuilder();
                        for (int iSample : netcdf.getSamplesForAssay(iAssay)) {
                            if (characteristicValues.get(factor).length > 0)
                                allValuesOfThisFactor.append(characteristicValues.get(factor)[iSample]);
                        }
                        experimentDesign.addAssay(factor, assay, allValuesOfThisFactor.toString());
                    }

                    ++iAssay;
                }
            } finally {
                closeQuietly(netcdf);
            }
            designs.add(experimentDesign);
        }

        return mergeExperimentDesigns(designs);
    }

    private File[] getNetCDFs(Experiment exp) throws ResourceNotFoundException {
        File[] netCDFs = atlasNetCDFDAO.listNetCDFs(exp.getAccession());
        if (netCDFs.length == 0) {
            throw new ResourceNotFoundException("NetCDF for experiment " + exp.getAccession() + " is not found");
        }
        return netCDFs;
    }

    //merge experimental factors from all designs, and create assays with factor values either blank (if factor
    // not found in this design, or actual)
    private ExperimentDesignUI mergeExperimentDesigns(Collection<ExperimentDesignUI> designs) {
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

            return name == null ? that.name == null : name.equals(that.name);
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
