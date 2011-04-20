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

package uk.ac.ebi.gxa.web.controller;

import ae3.dao.ExperimentSolrDAO;
import ae3.model.ExperimentalFactorsCompactData;
import ae3.model.SampleCharacteristicsCompactData;
import com.google.common.collect.Lists;
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
import uk.ac.ebi.gxa.netcdf.reader.NetCDFDescriptor;
import uk.ac.ebi.gxa.netcdf.reader.NetCDFProxy;

import java.io.IOException;
import java.util.List;

import static com.google.common.io.Closeables.closeQuietly;
import static uk.ac.ebi.gxa.netcdf.reader.NetCDFPredicates.hasArrayDesign;

/**
 * @author Olga Melnichuk
 *         Date: Nov 29, 2010
 */
@Controller
public class ExperimentViewController extends ExperimentViewControllerBase {

    protected final static Logger log = LoggerFactory.getLogger(ExperimentViewController.class);

    private final AtlasNetCDFDAO netCDFDAO;

    @Autowired
    public ExperimentViewController(ExperimentSolrDAO solrDAO, AtlasDAO atlasDAO, AtlasNetCDFDAO netCDFDAO) {
        super(solrDAO, atlasDAO);
        this.netCDFDAO = netCDFDAO;
    }

    /**
     * An experiment page handler
     *
     * @param accession an experiment accession to show experiment page for
     * @param model     a model for the view to render
     * @return a view path
     * @throws ResourceNotFoundException if an experiment with the given accession is not found
     */
    @RequestMapping(value = "/experiment", method = RequestMethod.GET)
    public String getExperiment(
            @RequestParam("eid") String accession,
            Model model) throws ResourceNotFoundException {

        ExperimentPage page = createExperimentPage(accession);
        page.enhance(model);

        if (page.isExperimentInCuration()) {
            return "experimentpage/experiment-incuration";
        }

        return "experimentpage/experiment";
    }

    @RequestMapping(value = "/experimentAssayProperties", method = RequestMethod.GET)
    public String getExperimentAnalysis(
            @RequestParam("eid") String accession,
            @RequestParam("ad") String adAcc,
            Model model
    ) throws ResourceNotFoundException, IOException {

        ExperimentPage page = createExperimentPage(accession);
        // TODO: restore this code
        /*
        if (page.getExp().getArrayDesign(adAcc) == null) {
            throw new ResourceNotFoundException("Experiment " + accession + " doesn't have this array design: " + adAcc);
        }
        */
        page.enhance(model);


        List<ExperimentalFactorsCompactData> efcd = Lists.newArrayList();
        List<SampleCharacteristicsCompactData> sccd = Lists.newArrayList();

        NetCDFDescriptor proxyDescr = netCDFDAO.getNetCdfFile(accession, hasArrayDesign(adAcc));
        NetCDFProxy proxy = null;
        try {
            proxy = proxyDescr.createProxy();
            String[] factors = proxy.getFactors();
            String[] sampleCharacteristics = proxy.getCharacteristics();
            int[][] s2a = proxy.getSamplesToAssays();

            for (String f : factors) {
                String[] vals = proxy.getFactorValues(f);
                ExperimentalFactorsCompactData d = new ExperimentalFactorsCompactData(f, vals.length);
                for (int i = 0; i < vals.length; i++) {
                    d.addEfv(vals[i], i);
                }
                efcd.add(d);
            }

            for (String s : sampleCharacteristics) {
                String[] vals = proxy.getCharacteristicValues(s);
                SampleCharacteristicsCompactData d = new SampleCharacteristicsCompactData(s, vals.length);
                for (int i = 0; i < vals.length; i++) {
                    d.addScv(vals[i], i);
                    for (int j = s2a[i].length - 1; j >= 0; j--) {
                        if (s2a[i][j] > 0) {
                            d.addMapping(i, j);
                        }
                    }
                }
                sccd.add(d);
            }

        } finally {
            closeQuietly(proxy);
        }

        model.addAttribute("efs", efcd);
        model.addAttribute("scs", sccd);
        return "experimentpage/experiment-assay-properties";
    }
}
