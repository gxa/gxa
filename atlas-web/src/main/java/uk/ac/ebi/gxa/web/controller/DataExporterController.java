/*
 * Copyright 2008-2012 Microarray Informatics Team, EMBL-European Bioinformatics Institute
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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import uk.ac.ebi.gxa.exceptions.ResourceNotFoundException;
import uk.ac.ebi.gxa.service.export.*;

/**
 * User: nsklyar
 * Date: 02/05/2012
 */
@Controller
public class DataExporterController {

    @Autowired
    private ChEbiXrefExporter chEbiExporter;

    @Autowired
    private CompoundExporter compoundExporter;

    @Autowired
    private EnsemblOrganismExporter organismExporter;

    @Autowired
    private ArrayDesignMappingExporter arrayDesignMappingExporter;

    @Autowired
    private EnsemblIDGeneNameMappingExporter geneNameMappingExporter;

    public static final String COMPOUND = "compound";

    public static final String CHEBI = "chebi";

    public static final String ORGANISMS = "organisms";

    public static final String ARRAY_DESIGN = "arraydesign";

    public static final String GENE_NAMES = "genenames";

    @RequestMapping(value = "/dataExport/{type}")
    public String getAnnotationSourceList(@PathVariable("type") String type,
                                          @RequestParam(value = "accession", required = false) String arrayDesignAccession,
                                          @RequestParam(value = "organism", required = false) String organism,
                                          Model model) throws ResourceNotFoundException {

        if (COMPOUND.equalsIgnoreCase(type)) {
            model.addAttribute("exportText", compoundExporter.generateDataAsString());
        } else if (CHEBI.equalsIgnoreCase(type)) {
            model.addAttribute("exportText", chEbiExporter.generateDataAsString());
        } else if (ORGANISMS.equalsIgnoreCase(type)) {
            model.addAttribute("exportText", organismExporter.generateDataAsString());
        } else if (ARRAY_DESIGN.equalsIgnoreCase(type)) {
            if (arrayDesignAccession != null) {
                model.addAttribute("exportText", arrayDesignMappingExporter.generateDataAsString(arrayDesignAccession));
            } else {
                throw new ResourceNotFoundException("Array design accession is not specified!");
            }
        } else if (GENE_NAMES.equalsIgnoreCase(type)) {
            if (organism != null) {
                model.addAttribute("exportText", geneNameMappingExporter.generateDataAsString(organism));
            } else {
                throw new ResourceNotFoundException("Organism name is not specified!");
            }
        } else {
            throw new ResourceNotFoundException("Cannot export data of type " + type);
        }
        return "dataExport/data-Export";
    }

}

