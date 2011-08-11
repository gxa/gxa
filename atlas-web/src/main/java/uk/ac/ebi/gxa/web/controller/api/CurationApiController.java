package uk.ac.ebi.gxa.web.controller.api;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import uk.ac.ebi.gxa.service.CurationService;
import uk.ac.ebi.gxa.web.controller.AtlasViewController;
import uk.ac.ebi.gxa.exceptions.ResourceNotFoundException;
import uk.ac.ebi.microarray.atlas.api.*;

import javax.servlet.http.HttpServletResponse;
import java.util.Collection;

/**
 * Controller class for mediating Curation API requests - delegates all requests to CurationService
 *
 * @author Misha Kapushesky
 */
@Controller
public class CurationApiController extends AtlasViewController {

    @Autowired
    private CurationService curationService;

    @RequestMapping(value = "/properties",
            method = RequestMethod.GET)
    public Collection<ApiPropertyName> getPropertyNames(
            HttpServletResponse response)
            throws ResourceNotFoundException {
        Collection<ApiPropertyName> propertyNames = curationService.getPropertyNames();
        response.setStatus(HttpServletResponse.SC_FOUND);
        return propertyNames;
    }


    @RequestMapping(value = "/properties/{propertyName}",
            method = RequestMethod.GET)
    public Collection<ApiPropertyValue> getPropertyValues(
            @PathVariable("propertyName") final String propertyName, HttpServletResponse response)
            throws ResourceNotFoundException {
        Collection<ApiPropertyValue> properties = curationService.getPropertyValues(propertyName);
        response.setStatus(HttpServletResponse.SC_FOUND);
        return properties;
    }

    @RequestMapping(value = "/experiments/{experimentAccession}",
            method = RequestMethod.GET)
    public ApiExperiment getExperiment(@PathVariable("experimentAccession") final String experimentAccession,
                                       HttpServletResponse response) throws ResourceNotFoundException {
        try {
            ApiExperiment apiExperiment = curationService.getExperiment(experimentAccession);
            response.setStatus(HttpServletResponse.SC_FOUND);
            return apiExperiment;
        } catch (ResourceNotFoundException e) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            throw e;
        }
    }

    @RequestMapping(value = "/experiments/{experimentAccession}",
            method = RequestMethod.PUT)
    public void putExperiment(@RequestBody final ApiExperiment apiExperiment,
                              HttpServletResponse response) throws ResourceNotFoundException {
        curationService.saveExperiment(apiExperiment);
        response.setStatus(HttpServletResponse.SC_CREATED);
    }

    @RequestMapping(value = "/experiments/{experimentAccession}/assays/{assayAccession}",
            method = RequestMethod.GET)
    public ApiAssay getAssay(@PathVariable("experimentAccession") final String experimentAccession,
                             @PathVariable("assayAccession") final String assayAccession,
                             HttpServletResponse response) throws ResourceNotFoundException {
        try {
            ApiAssay assay = curationService.getAssay(experimentAccession, assayAccession);
            response.setStatus(HttpServletResponse.SC_FOUND);
            return assay;
        } catch (ResourceNotFoundException e) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            throw e;
        }
    }

    @RequestMapping(value = "/experiments/{experimentAccession}/samples/{sampleAccession}",
            method = RequestMethod.GET)
    public ApiSample getSample(@PathVariable("experimentAccession") final String experimentAccession,
                               @PathVariable("sampleAccession") final String sampleAccession,
                               HttpServletResponse response) throws ResourceNotFoundException {
        try {
            ApiSample sample = curationService.getSample(experimentAccession, sampleAccession);
            response.setStatus(HttpServletResponse.SC_FOUND);
            return sample;
        } catch (ResourceNotFoundException e) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            throw e;
        }
    }

    @RequestMapping(value = "/experiments/{experimentAccession}/assays/{assayAccession}/properties",
            method = RequestMethod.GET)
    public Collection<ApiProperty> getAssayProperties(
            @PathVariable("experimentAccession") final String experimentAccession,
            @PathVariable(value = "assayAccession") final String assayAccession,
            HttpServletResponse response) throws ResourceNotFoundException {
        try {
            Collection<ApiProperty> assayProperties = curationService.getAssayProperties(experimentAccession, assayAccession);
            response.setStatus(HttpServletResponse.SC_FOUND);
            return assayProperties;
        } catch (ResourceNotFoundException e) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            throw e;
        }
    }

    @RequestMapping(value = "/experiments/{experimentAccession}/assays/{assayAccession}/properties",
            method = RequestMethod.PUT)
    public void putAssayProperties(@PathVariable(value = "experimentAccession") final String experimentAccession,
                                   @PathVariable(value = "assayAccession") final String assayAccession,
                                   @RequestBody final ApiProperty[] assayProperties,
                                   HttpServletResponse response) throws ResourceNotFoundException {
        try {
            curationService.putAssayProperties(experimentAccession, assayAccession, assayProperties);
            response.setStatus(HttpServletResponse.SC_CREATED);
        } catch (ResourceNotFoundException e) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            throw e;
        }
    }

    @RequestMapping(value = "/experiments/{experimentAccession}/assays/{assayAccession}/properties",
            method = RequestMethod.DELETE)
    public void deleteAssayProperties(@PathVariable(value = "experimentAccession") final String experimentAccession,
                                      @PathVariable(value = "assayAccession") final String assayAccession,
                                      @RequestBody final ApiProperty[] assayProperties,
                                      HttpServletResponse response) throws ResourceNotFoundException {
        try {
            curationService.deleteAssayProperties(experimentAccession, assayAccession, assayProperties);
            response.setStatus(HttpServletResponse.SC_CREATED);
        } catch (ResourceNotFoundException e) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            throw e;
        }
    }

    @RequestMapping(value = "/experiments/{experimentAccession}/samples/{sampleAccession}/properties",
            method = RequestMethod.GET)
    public Collection<ApiProperty> getSampleProperties(
            @PathVariable("experimentAccession") final String experimentAccession,
            @PathVariable(value = "sampleAccession") final String sampleAccession,
            HttpServletResponse response) throws ResourceNotFoundException {
        try {
            Collection<ApiProperty> sampleProperties = curationService.getSampleProperties(experimentAccession, sampleAccession);
            response.setStatus(HttpServletResponse.SC_FOUND);
            return sampleProperties;
        } catch (ResourceNotFoundException e) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            throw e;
        }
    }

    @RequestMapping(value = "/experiments/{experimentAccession}/samples/{sampleAccession}/properties",
            method = RequestMethod.PUT)
    public void putSampleProperties(@PathVariable(value = "experimentAccession") final String experimentAccession,
                                    @PathVariable(value = "sampleAccession") final String sampleAccession,
                                    @RequestBody final ApiProperty[] sampleProperties,
                                    HttpServletResponse response) throws ResourceNotFoundException {
        try {
            curationService.putSampleProperties(experimentAccession, sampleAccession, sampleProperties);
            response.setStatus(HttpServletResponse.SC_CREATED);
        } catch (ResourceNotFoundException e) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            throw e;
        }

    }

    @RequestMapping(value = "/experiments/{experimentAccession}/samples/{sampleAccession}/properties",
            method = RequestMethod.DELETE)
    public void deleteSampleProperties(@PathVariable(value = "experimentAccession") String experimentAccession,
                                       @PathVariable(value = "sampleAccession") String sampleAccession,
                                       @RequestBody ApiProperty[] sampleProperties,
                                       HttpServletResponse response) throws ResourceNotFoundException {
        try {
            curationService.deleteSampleProperties(experimentAccession, sampleAccession, sampleProperties);
            response.setStatus(HttpServletResponse.SC_CREATED);
        } catch (ResourceNotFoundException e) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            throw e;
        }

    }

    @RequestMapping(value = "/ontologies/{ontologyName}",
            method = RequestMethod.GET)
    public ApiOntology getOntology(@PathVariable(value = "ontologyName") final String ontologyName,
                                   HttpServletResponse response) throws ResourceNotFoundException {
        try {
            ApiOntology ontology = curationService.getOntology(ontologyName);
            response.setStatus(HttpServletResponse.SC_FOUND);
            return ontology;
        } catch (ResourceNotFoundException e) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            throw e;
        }

    }

    @RequestMapping(value = "/ontologies",
            method = RequestMethod.PUT)
    public void putOntology(@RequestBody final ApiOntology apiOntology,
                            HttpServletResponse response) {
        curationService.putOntology(apiOntology);
        response.setStatus(HttpServletResponse.SC_CREATED);
    }

    @RequestMapping(value = "/ontologyterms/{ontologyTerm}",
            method = RequestMethod.GET)
    public ApiOntologyTerm getOntologyTerm(@PathVariable(value = "ontologyTerm") final String ontologyTerm,
                                           HttpServletResponse response) throws ResourceNotFoundException {
        try {
            ApiOntologyTerm apiOntologyTerm = curationService.getOntologyTerm(ontologyTerm);
            response.setStatus(HttpServletResponse.SC_FOUND);
            return apiOntologyTerm;
        } catch (ResourceNotFoundException e) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            throw e;
        }

    }

    @RequestMapping(value = "/ontologyterms",
            method = RequestMethod.PUT)
    public void putOntologyTerms(@RequestBody final ApiOntologyTerm[] apiOntologyTerms,
                                 HttpServletResponse response) {
        curationService.putOntologyTerms(apiOntologyTerms);
        response.setStatus(HttpServletResponse.SC_CREATED);
    }

}