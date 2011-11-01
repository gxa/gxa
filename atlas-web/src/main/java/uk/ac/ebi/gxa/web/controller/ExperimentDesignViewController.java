package uk.ac.ebi.gxa.web.controller;

import ae3.dao.ExperimentSolrDAO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import uk.ac.ebi.gxa.dao.AtlasDAO;
import uk.ac.ebi.gxa.data.AtlasDataDAO;
import uk.ac.ebi.gxa.data.AtlasDataException;
import uk.ac.ebi.gxa.data.ExperimentWithData;
import uk.ac.ebi.gxa.exceptions.ResourceNotFoundException;
import uk.ac.ebi.microarray.atlas.model.ArrayDesign;
import uk.ac.ebi.microarray.atlas.model.Assay;
import uk.ac.ebi.microarray.atlas.model.Experiment;

import java.util.*;

import static com.google.common.io.Closeables.closeQuietly;

/**
 * @author Olga Melnichuk
 *         Date: 15/03/2011
 */
@Controller
public class ExperimentDesignViewController extends ExperimentViewControllerBase {

    private AtlasDataDAO atlasDataDAO;

    @Autowired
    public ExperimentDesignViewController(ExperimentSolrDAO solrDAO, AtlasDataDAO atlasDataDAO, AtlasDAO atlasDAO) {
        super(solrDAO, atlasDAO);
        this.atlasDataDAO = atlasDataDAO;
    }

    @RequestMapping(value = "/experimentDesign", method = RequestMethod.GET)
    public String getExperimentDesign(
            @RequestParam("eid") String accession,
            Model model) throws ResourceNotFoundException, AtlasDataException {

        ExperimentPage expPage = createExperimentPage(accession);
        expPage.enhance(model);

        model.addAttribute("experimentDesign", constructExperimentDesign(expPage.getExperiment()));
        return "experimentpage/experiment-design";
    }

    private ExperimentDesignUI constructExperimentDesign(Experiment exp) throws ResourceNotFoundException, AtlasDataException {
        final Collection<ArrayDesign> arrayDesigns = exp.getArrayDesigns();
        if (arrayDesigns.isEmpty()) {
            throw new ResourceNotFoundException("ArrayDesign for experiment " + exp.getAccession() + " is not found");
        }
        final ExperimentWithData ewd = atlasDataDAO.createExperimentWithData(exp);
        try {
            final List<ExperimentDesignUI> designs = new ArrayList<ExperimentDesignUI>();

            for (ArrayDesign ad : arrayDesigns) {
                final ExperimentDesignUI experimentDesign = new ExperimentDesignUI();

                final String[] adFactors = ewd.getFactors(ad);
                final Map<String, String[]> factorValues = new HashMap<String, String[]>();
                for (String factor : adFactors) {
                    experimentDesign.addFactor(factor);
                    factorValues.put(factor, ewd.getFactorValues(ad, factor));
                }

                final List<String> sampleCharacteristicsNotFactors = new ArrayList<String>();

                final String[] sampleCharacteristics = ewd.getCharacteristics(ad);
                final Map<String, String[]> characteristicValues = new HashMap<String, String[]>();
                for (String factor : sampleCharacteristics) {
                    characteristicValues.put(factor, ewd.getCharacteristicValues(ad, factor));
                    if (experimentDesign.addFactor(factor)) {
                        sampleCharacteristicsNotFactors.add(factor);
                    }
                }

                int iAssay = 0;

                for (Assay a : ewd.getAssays(ad)) {
                    AssayInfo assay = new AssayInfo();
                    assay.setName(a.getAccession());
                    assay.setArrayDesignAccession(ad.getAccession());

                    for (String factor : adFactors) {
                        experimentDesign.addAssay(factor, assay, factorValues.get(factor)[iAssay]);
                    }

                    for (String factor : sampleCharacteristicsNotFactors) {
                        StringBuilder allValuesOfThisFactor = new StringBuilder();
                        for (int iSample : ewd.getSamplesForAssay(ad,iAssay)) {
                            if (characteristicValues.get(factor).length > 0) {
                                allValuesOfThisFactor.append(characteristicValues.get(factor)[iSample]);
                            }
                        }
                        experimentDesign.addAssay(factor, assay, allValuesOfThisFactor.toString());
                    }

                    ++iAssay;
                }
                designs.add(experimentDesign);
            }

            return mergeExperimentDesigns(designs);
        } finally {
            closeQuietly(ewd);
        }
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
