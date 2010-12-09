package uk.ac.ebi.gxa.web.controller;

import ae3.dao.AtlasSolrDAO;
import ae3.model.AtlasExperiment;
import org.apache.commons.lang.ArrayUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import uk.ac.ebi.gxa.dao.AtlasDAO;
import uk.ac.ebi.gxa.netcdf.reader.AtlasNetCDFDAO;
import uk.ac.ebi.gxa.netcdf.reader.NetCDFProxy;
import uk.ac.ebi.microarray.atlas.model.Assay;

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
                experimentDesign.addFactor(new ExperimentFactorUI(factor));
                factorValues.put(factor, netcdf.getFactorValues(factor));
            }

            String[] netCdfSampleCharacteristics = netcdf.getCharacteristics();
            Map<String, String[]> characteristicValues = new HashMap<String, String[]>();
            for (String factor : netCdfSampleCharacteristics) {
                characteristicValues.put(factor, netcdf.getCharacteristicValues(factor));
            }
            int[][] samplesToAssay = netcdf.getSamplesToAssays();

            List<String> sampleCharacteristicsNotFactors = new ArrayList<String>();
            for (String sampleCharacteristic : netCdfSampleCharacteristics) {
                if (!ArrayUtils.contains(netCdfFactors, sampleCharacteristic)) {
                    sampleCharacteristicsNotFactors.add(sampleCharacteristic);
                    experimentDesign.addFactor(new ExperimentFactorUI(sampleCharacteristic));
                }
            }

            int iAssay = 0;

            List<Assay> assays = atlasDAO.getAssaysByExperimentAccession(accession);

            for (long assayId : netcdf.getAssays()) {
                AssayUI assay = new AssayUI();
                assay.setName(findAssayAccession(assayId, assays)); // String.format("%05d",)
                for (String factor : netCdfFactors) {
                    assay.addFactorValue(factorValues.get(factor)[iAssay]);
                }

                for (String factor : sampleCharacteristicsNotFactors) {
                    String allValuesOfThisFactor = "";
                    for (int iSample : getSamplesForAssay(iAssay, samplesToAssay)) {
                        if (characteristicValues.get(factor).length > 0) //it is empty array sometimes
                            allValuesOfThisFactor += characteristicValues.get(factor)[iSample];
                    }
                    assay.addFactorValue(allValuesOfThisFactor);
                }
                assay.setArrayDesignAccession(netcdf.getArrayDesignAccession());
                experimentDesign.addAssay(assay);
                ++iAssay;
                //if(iAssay>100)//do not show more then 100 assays for now
                //break;
            }
            //samplesToAssays[]
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
        //no mashing water in the bucket
        if (designs.size() < 2)
            return designs.iterator().next();

        ExperimentDesignUI result = new ExperimentDesignUI();
        String emptyString = "";

        Map<String, Integer[]> ordinalOfFactorForEachDesign = new HashMap<String, Integer[]>();

        int iDesign = 0;
        for (ExperimentDesignUI design : designs) {
            for (ExperimentFactorUI factor : design.getFactors()) {
                if (!ordinalOfFactorForEachDesign.containsKey(factor.getName())) {
                    ordinalOfFactorForEachDesign.put(factor.getName(), new Integer[designs.size()]); //initialize array with nulls
                }
                ordinalOfFactorForEachDesign.get(factor.getName())[iDesign] = Collections.binarySearch(design.getFactors(), factor, new Comparator<ExperimentFactorUI>() {
                    public int compare(ExperimentFactorUI f1, ExperimentFactorUI f2) {
                        return f1.getName().compareTo(f2.getName());
                    }
                });
            }
            iDesign++;
        }

        for (String factorName : ordinalOfFactorForEachDesign.keySet()) {
            result.addFactor(new ExperimentFactorUI(factorName));
        }

        iDesign = 0;
        for (ExperimentDesignUI design : designs) {
            for (AssayUI assay : design.getAssays()) {
                AssayUI newAssay = new AssayUI();
                newAssay.setName(assay.getName());
                newAssay.setArrayDesignAccession(assay.getArrayDesignAccession());
                for (ExperimentFactorUI factor : result.getFactors()) {
                    Integer ordinalOfFactorForThisDesign = ordinalOfFactorForEachDesign.get(factor.getName())[iDesign];
                    String factorValue = (ordinalOfFactorForThisDesign < 0 ? emptyString : assay.getFactorValues().get(ordinalOfFactorForThisDesign));
                    newAssay.getFactorValues().add(factorValue);
                }
                result.addAssay(newAssay);
            }
            iDesign++;
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

    public static class AssayUI {
        private String name;
        private String arrayDesignAccession;
        private List<String> factorValues = new ArrayList<String>();

        public void addFactorValue(String value) {
            factorValues.add(value);
        }

        public List<String> getFactorValues() {
            return Collections.unmodifiableList(this.factorValues);
        }

        public String getName() {
            return this.name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getArrayDesignAccession() {
            return this.arrayDesignAccession;
        }

        public void setArrayDesignAccession(String arrayDesignAccession) {
            this.arrayDesignAccession = arrayDesignAccession;
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
    }

    public static class ExperimentDesignUI {
        private final List<ExperimentFactorUI> factors = new ArrayList<ExperimentFactorUI>();
        private final List<AssayUI> assays = new ArrayList<AssayUI>();

        public List<ExperimentFactorUI> getFactors() {
            return Collections.unmodifiableList(factors);
        }

        public List<AssayUI> getAssays() {
            return Collections.unmodifiableList(assays);
        }

        public void addFactor(ExperimentFactorUI factor) {
            factors.add(factor);
        }

        public void addAssay(AssayUI assay) {
            assays.add(assay);
        }
    }

}
