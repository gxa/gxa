/*
 * Copyright 2008-2011 Microarray Informatics Team, EMBL-European Bioinformatics Institute
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
import com.google.common.base.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import ucar.ma2.InvalidRangeException;
import uk.ac.ebi.gxa.dao.AtlasDAO;
import uk.ac.ebi.gxa.netcdf.reader.AtlasNetCDFDAO;
import uk.ac.ebi.gxa.netcdf.reader.NetCDFDescriptor;
import uk.ac.ebi.gxa.plot.AssayProperties;
import uk.ac.ebi.gxa.plot.ExperimentPlot;
import uk.ac.ebi.gxa.properties.AtlasProperties;

import javax.annotation.Nullable;
import java.io.IOException;

import static uk.ac.ebi.gxa.netcdf.reader.NetCDFPredicates.hasArrayDesign;

/**
 * @author Olga Melnichuk
 */
@Controller
public class ExperimentViewController extends ExperimentViewControllerBase {

    protected final static Logger log = LoggerFactory.getLogger(ExperimentViewController.class);

    private final AtlasNetCDFDAO netCDFDAO;

    private final AtlasProperties atlasProperties;

    private final Function<String, String> curatedStringConverter = new Function<String, String>() {
        @Override
        public String apply(@Nullable String input) {
            String s = atlasProperties.getCuratedEf(input);
            return (s == null) ? input : s;
        }
    };

    @Autowired
    public ExperimentViewController(ExperimentSolrDAO solrDAO,
                                    AtlasDAO atlasDAO,
                                    AtlasNetCDFDAO netCDFDAO,
                                    AtlasProperties atlasProperties) {
        super(solrDAO, atlasDAO);
        this.netCDFDAO = netCDFDAO;
        this.atlasProperties = atlasProperties;
    }

    /**
     * An experiment page handler. If the experiment with the given id/accession exists it fills the model with the
     * appropriate values and returns the corresponding view. Parameters like gid (geneId) and ef (experiment factor)
     * are optional; they could be specified manually or by the url re-writer
     * (when url like /experiment/E-MTAB-62/ENSG00000136487/organism_part is used).
     *
     * @param accession an experiment accession to show experiment page for
     * @param gid       a gene identifier to fill in initial search fields
     * @param ef        an experiment factor name to fill in initial search fields
     * @param model     a model for the view to render
     * @return path of the view
     * @throws ResourceNotFoundException if an experiment with the given accession is not found
     */
    @RequestMapping(value = "/experiment", method = RequestMethod.GET)
    public String getExperiment(
            @RequestParam("eid") String accession,
            @RequestParam(value = "gid", required = false) String gid,
            @RequestParam(value = "ef", required = false) String ef,
            Model model) throws ResourceNotFoundException {

        JsMapModel jsMapModel = JsMapModel.wrap(model);

        ExperimentPage page = createExperimentPage(accession);
        page.enhance(jsMapModel);

        (jsMapModel)
                .addJsAttribute("eid", page.getExp().getAccession())
                .addJsAttribute("gid", gid)
                .addJsAttribute("ef", ef);

        if (page.isExperimentInCuration()) {
            return "experimentpage/experiment-incuration";
        }

        return "experimentpage/experiment";
    }

    /**
     * An experiment assay properties handler.
     *
     * @param accession an experiment accession to get assay properties for
     * @param adAcc     a design array accession to get assay properties for
     * @param model     a model for the view to render
     * @return the view path
     * @throws ResourceNotFoundException if an experiment or array design is not found
     * @throws IOException               if any netCDF file reading error happened
     */
    @RequestMapping(value = "/experimentAssayProperties", method = RequestMethod.GET)
    public String getExperimentAnalysis(
            @RequestParam("eid") String accession,
            @RequestParam("ad") String adAcc,
            Model model
    ) throws ResourceNotFoundException, IOException {

        ExperimentPage page = createExperimentPage(accession);
        if (page.getExp().getArrayDesign(adAcc) == null) {
            throw new ResourceNotFoundException("Improper array design accession: " + adAcc + " (in " + accession + " experiment)");
        }
        page.enhance(model);

        NetCDFDescriptor proxyDescr = netCDFDAO.getNetCdfFile(accession, hasArrayDesign(adAcc));
        model.addAttribute("assayProperties", AssayProperties.create(proxyDescr, curatedStringConverter));

        return "experimentpage/experiment-assay-properties";
    }

    /**
     * Returns experiment plots for given set of design elements.
     * (JSON view only supported)
     *
     * @param accession               an experiment accession to find out the required netCDF
     * @param adAcc                   an array design accession to find out the required netCDF
     * @param des                     an array of design element indexes to get plot data for
     * @param assayPropertiesRequired a boolean value to specify if assay properties ard needed
     * @param model                   a model for the view to render
     * @return the view path
     * @throws ResourceNotFoundException      if an experiment or array design is not found
     * @throws IOException                    if any netCDF file reading error happened
     * @throws ucar.ma2.InvalidRangeException if given design element indexes are out of range
     */
    @RequestMapping(value = "/experimentPlot", method = RequestMethod.GET)
    public String getExperimentPlot(
            @RequestParam("eid") String accession,
            @RequestParam("ad") String adAcc,
            @RequestParam("de") int[] des,
            @RequestParam(value = "assayPropertiesRequired", required = false, defaultValue = "false") Boolean assayPropertiesRequired,
            Model model
    ) throws ResourceNotFoundException, IOException, InvalidRangeException {

        ExperimentPage page = createExperimentPage(accession);
        if (page.getExp().getArrayDesign(adAcc) == null) {
            throw new ResourceNotFoundException("Improper array design accession: " + adAcc + " (in " + accession + " experiment)");
        }

        NetCDFDescriptor proxyDescr = netCDFDAO.getNetCdfFile(accession, hasArrayDesign(adAcc));
        model.addAttribute("plot", ExperimentPlot.create(des, proxyDescr, curatedStringConverter));
        if (assayPropertiesRequired) {
            model.addAttribute("assayProperties", AssayProperties.create(proxyDescr, curatedStringConverter));
        }
        return "unsupported-html-view";
    }
}
