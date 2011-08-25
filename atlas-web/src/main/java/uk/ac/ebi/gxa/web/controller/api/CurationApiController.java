package uk.ac.ebi.gxa.web.controller.api;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
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
    @ResponseStatus(HttpStatus.OK)
    public Collection<ApiPropertyName> getPropertyNames(
            HttpServletResponse response)
            throws ResourceNotFoundException {
        Collection<ApiPropertyName> propertyNames = curationService.getPropertyNames();
        return propertyNames;
    }


    @RequestMapping(value = "/properties/{propertyName}",
            method = RequestMethod.GET)
    @ResponseStatus(HttpStatus.OK)
    public Collection<ApiPropertyValue> getPropertyValues(
            @PathVariable("propertyName") final String propertyName, HttpServletResponse response)
            throws ResourceNotFoundException {
        Collection<ApiPropertyValue> properties = curationService.getPropertyValues(propertyName);
        return properties;
    }

    @RequestMapping(value = "/properties/{propertyName}/{propertyValue}",
            method = RequestMethod.DELETE)
    @ResponseStatus(HttpStatus.CREATED)
    public void removePropertyValue(
            @PathVariable("propertyName") final String propertyName,
            @PathVariable("propertyValue") final String propertyValue,
            HttpServletResponse response)
            throws ResourceNotFoundException {
        curationService.removePropertyValue(propertyName, propertyValue);
    }

    @RequestMapping(value = "/experiments/assays/properties/{propertyName}/{oldPropertyValue}/{newPropertyValue}",
            method = RequestMethod.PUT)
    @ResponseStatus(HttpStatus.CREATED)
    public void replacePropertyValueInAssays(
            @PathVariable("propertyName") final String propertyName,
            @PathVariable("oldPropertyValue") final String oldPropertyValue,
            @PathVariable("newPropertyValue") final String newPropertyValue,
            HttpServletResponse response)
            throws ResourceNotFoundException {
        curationService.replacePropertyValueInAssays(propertyName, oldPropertyValue, newPropertyValue);
    }

    @RequestMapping(value = "/experiments/samples/properties/{propertyName}/{oldPropertyValue}/{newPropertyValue}",
            method = RequestMethod.PUT)
    @ResponseStatus(HttpStatus.CREATED)
    public void replacePropertyValueInSamples(
            @PathVariable("propertyName") final String propertyName,
            @PathVariable("oldPropertyValue") final String oldPropertyValue,
            @PathVariable("newPropertyValue") final String newPropertyValue,
            HttpServletResponse response)
            throws ResourceNotFoundException {
        curationService.replacePropertyValueInSamples(propertyName, oldPropertyValue, newPropertyValue);
    }


    @RequestMapping(value = "/experiments/{experimentAccession}",
            method = RequestMethod.GET)
    @ResponseStatus(HttpStatus.OK)
    public ApiExperiment getExperiment(@PathVariable("experimentAccession") final String experimentAccession,
                                       HttpServletResponse response) throws ResourceNotFoundException {
        return curationService.getExperiment(experimentAccession);
    }

    @RequestMapping(value = "/experiments/{experimentAccession}/assays/{assayAccession}",
            method = RequestMethod.GET)
    @ResponseStatus(HttpStatus.OK)
    public ApiAssay getAssay(@PathVariable("experimentAccession") final String experimentAccession,
                             @PathVariable("assayAccession") final String assayAccession,
                             HttpServletResponse response) throws ResourceNotFoundException {
        return curationService.getAssay(experimentAccession, assayAccession);
    }

    @RequestMapping(value = "/experiments/{experimentAccession}/samples/{sampleAccession}",
            method = RequestMethod.GET)
    @ResponseStatus(HttpStatus.OK)
    public ApiSample getSample(@PathVariable("experimentAccession") final String experimentAccession,
                               @PathVariable("sampleAccession") final String sampleAccession,
                               HttpServletResponse response) throws ResourceNotFoundException {
        return curationService.getSample(experimentAccession, sampleAccession);
    }

    @RequestMapping(value = "/experiments/{experimentAccession}/assays/{assayAccession}/properties",
            method = RequestMethod.GET)
    @ResponseStatus(HttpStatus.OK)
    public Collection<ApiProperty> getAssayProperties(
            @PathVariable("experimentAccession") final String experimentAccession,
            @PathVariable(value = "assayAccession") final String assayAccession,
            HttpServletResponse response) throws ResourceNotFoundException {
        return curationService.getAssayProperties(experimentAccession, assayAccession);
    }

    @RequestMapping(value = "/experiments/{experimentAccession}/assays/{assayAccession}/properties",
            method = RequestMethod.PUT)
    @ResponseStatus(HttpStatus.CREATED)
    public void putAssayProperties(@PathVariable(value = "experimentAccession") final String experimentAccession,
                                   @PathVariable(value = "assayAccession") final String assayAccession,
                                   @RequestBody final ApiProperty[] assayProperties,
                                   HttpServletResponse response) throws ResourceNotFoundException {
        curationService.putAssayProperties(experimentAccession, assayAccession, assayProperties);
    }

    @RequestMapping(value = "/experiments/{experimentAccession}/assays/{assayAccession}/properties",
            method = RequestMethod.DELETE)
    @ResponseStatus(HttpStatus.CREATED)
    public void deleteAssayProperties(@PathVariable(value = "experimentAccession") final String experimentAccession,
                                      @PathVariable(value = "assayAccession") final String assayAccession,
                                      @RequestBody final ApiProperty[] assayProperties,
                                      HttpServletResponse response) throws ResourceNotFoundException {
        curationService.deleteAssayProperties(experimentAccession, assayAccession, assayProperties);
    }

    @RequestMapping(value = "/experiments/{experimentAccession}/samples/{sampleAccession}/properties",
            method = RequestMethod.GET)
    @ResponseStatus(HttpStatus.OK)
    public Collection<ApiProperty> getSampleProperties(
            @PathVariable("experimentAccession") final String experimentAccession,
            @PathVariable(value = "sampleAccession") final String sampleAccession,
            HttpServletResponse response) throws ResourceNotFoundException {
        return curationService.getSampleProperties(experimentAccession, sampleAccession);
    }

    @RequestMapping(value = "/experiments/{experimentAccession}/samples/{sampleAccession}/properties",
            method = RequestMethod.PUT)
    @ResponseStatus(HttpStatus.CREATED)
    public void putSampleProperties(@PathVariable(value = "experimentAccession") final String experimentAccession,
                                    @PathVariable(value = "sampleAccession") final String sampleAccession,
                                    @RequestBody final ApiProperty[] sampleProperties,
                                    HttpServletResponse response) throws ResourceNotFoundException {
        curationService.putSampleProperties(experimentAccession, sampleAccession, sampleProperties);
    }

    @RequestMapping(value = "/experiments/{experimentAccession}/samples/{sampleAccession}/properties",
            method = RequestMethod.DELETE)
    @ResponseStatus(HttpStatus.CREATED)
    public void deleteSampleProperties(@PathVariable(value = "experimentAccession") String experimentAccession,
                                       @PathVariable(value = "sampleAccession") String sampleAccession,
                                       @RequestBody ApiProperty[] sampleProperties,
                                       HttpServletResponse response) throws ResourceNotFoundException {
        curationService.deleteSampleProperties(experimentAccession, sampleAccession, sampleProperties);
    }

    @RequestMapping(value = "/ontologies/{ontologyName}",
            method = RequestMethod.GET)
    @ResponseStatus(HttpStatus.OK)
    public ApiOntology getOntology(@PathVariable(value = "ontologyName") final String ontologyName,
                                   HttpServletResponse response) throws ResourceNotFoundException {
        return curationService.getOntology(ontologyName);
    }

    @RequestMapping(value = "/ontologies",
            method = RequestMethod.PUT)
    @ResponseStatus(HttpStatus.CREATED)
    public void putOntology(@RequestBody final ApiOntology apiOntology,
                            HttpServletResponse response) {
        curationService.putOntology(apiOntology);
    }

    @RequestMapping(value = "/ontologyterms/{ontologyTerm}",
            method = RequestMethod.GET)
    @ResponseStatus(HttpStatus.OK)
    public ApiOntologyTerm getOntologyTerm(@PathVariable(value = "ontologyTerm") final String ontologyTerm,
                                           HttpServletResponse response) throws ResourceNotFoundException {
        return curationService.getOntologyTerm(ontologyTerm);
    }

    @RequestMapping(value = "/ontologyterms",
            method = RequestMethod.PUT)
    @ResponseStatus(HttpStatus.CREATED)
    public void putOntologyTerms(@RequestBody final ApiOntologyTerm[] apiOntologyTerms,
                                 HttpServletResponse response) {
        curationService.putOntologyTerms(apiOntologyTerms);

    }

}