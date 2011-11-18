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
@RequestMapping("/api/curators/{v}")
public class CurationApiController extends AtlasViewController {

    @Autowired
    private CurationService curationService;

    @RequestMapping(value = "/properties",
            method = RequestMethod.GET)
    @ResponseStatus(HttpStatus.OK)
    public Collection<ApiPropertyName> getPropertyNames(
            @PathVariable("v") final ApiVersionType version,
            HttpServletResponse response)
            throws ResourceNotFoundException {
        Collection<ApiPropertyName> propertyNames = curationService.getPropertyNames();
        return propertyNames;
    }


    @RequestMapping(value = "/properties/{propertyName}",
            method = RequestMethod.GET)
    @ResponseStatus(HttpStatus.OK)
    public Collection<ApiPropertyValue> getPropertyValues(
            @PathVariable("v") final ApiVersionType version,
            @PathVariable("propertyName") final String propertyName, HttpServletResponse response)
            throws ResourceNotFoundException {
        Collection<ApiPropertyValue> properties = curationService.getPropertyValues(propertyName);
        return properties;
    }

    @RequestMapping(value = "/properties/{propertyName}/{propertyValue}",
            method = RequestMethod.DELETE)
    @ResponseStatus(HttpStatus.CREATED)
    public void removePropertyValue(
            @PathVariable("v") final ApiVersionType version,
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
            @PathVariable("v") final ApiVersionType version,
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
            @PathVariable("v") final ApiVersionType version,
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
    public ApiExperiment getExperiment(@PathVariable("v") final ApiVersionType version,
                                       @PathVariable("experimentAccession") final String experimentAccession,
                                       HttpServletResponse response) throws ResourceNotFoundException {
        return curationService.getExperiment(experimentAccession);
    }

    @RequestMapping(value = "/experiments/assays/{assayAccession}",
            method = RequestMethod.GET)
    @ResponseStatus(HttpStatus.OK)
    public Collection<ApiExperiment> getExperimentsByAssay(@PathVariable("v") final ApiVersionType version,
                                                           @PathVariable("assayAccession") final String assayAccession,
                                                           HttpServletResponse response) throws ResourceNotFoundException {
        return curationService.getExperimentsByAssay(assayAccession);
    }

    @RequestMapping(value = "/experiments/samples/{sampleAccession}",
            method = RequestMethod.GET)
    @ResponseStatus(HttpStatus.OK)
    public Collection<ApiExperiment> getExperimentsBySample(@PathVariable("v") final ApiVersionType version,
                                                            @PathVariable("sampleAccession") final String sampleAccession,
                                                            HttpServletResponse response) throws ResourceNotFoundException {
        return curationService.getExperimentsBySample(sampleAccession);
    }

    @RequestMapping(value = "/assays/properties/{propertyName}",
            method = RequestMethod.GET)
    @ResponseStatus(HttpStatus.OK)
    public Collection<ApiProperty> getAssayPropertiesByPropertyValue(@PathVariable("v") final ApiVersionType version,
                                                         @PathVariable("propertyName") final String propertyName,
                                                         HttpServletResponse response) throws ResourceNotFoundException {
        return curationService.getAssayPropertiesByProperty(propertyName);
    }

    @RequestMapping(value = "/samples/properties/{propertyName}",
            method = RequestMethod.GET)
    @ResponseStatus(HttpStatus.OK)
    public Collection<ApiProperty> getSamplePropertiesByPropertyValue(@PathVariable("v") final ApiVersionType version,
                                                           @PathVariable("propertyName") final String propertyName,
                                                           HttpServletResponse response) throws ResourceNotFoundException {
        return curationService.getSamplePropertiesByProperty(propertyName);
    }

    @RequestMapping(value = "/assays/properties/{propertyName}",
            method = RequestMethod.DELETE)
    @ResponseStatus(HttpStatus.OK)
    public void removePropertyFromAssays(@PathVariable("v") final ApiVersionType version,
                                         @PathVariable("propertyName") final String propertyName,
                                         HttpServletResponse response) {
        curationService.removePropertyFromAssays(propertyName, null);
    }

    @RequestMapping(value = "/samples/properties/{propertyName}",
            method = RequestMethod.DELETE)
    @ResponseStatus(HttpStatus.OK)
    public void removePropertyFromSamples(@PathVariable("v") final ApiVersionType version,
                                          @PathVariable("propertyName") final String propertyName,
                                          HttpServletResponse response) {
        curationService.removePropertyFromSamples(propertyName, null);
    }

    @RequestMapping(value = "/assays/properties/{propertyName}/{propertyValue}",
            method = RequestMethod.DELETE)
    @ResponseStatus(HttpStatus.OK)
    public void removePropertyValueFromAssays(@PathVariable("v") final ApiVersionType version,
                                              @PathVariable("propertyName") final String propertyName,
                                              @PathVariable("propertyValue") final String propertyValue,
                                              HttpServletResponse response) {
        curationService.removePropertyFromAssays(propertyName, propertyValue);
    }

    @RequestMapping(value = "/samples/properties/{propertyName}/{propertyValue}",
            method = RequestMethod.DELETE)
    @ResponseStatus(HttpStatus.OK)
    public void removePropertyValueFromSamples(@PathVariable("v") final ApiVersionType version,
                                               @PathVariable("propertyName") final String propertyName,
                                               @PathVariable("propertyValue") final String propertyValue,
                                               HttpServletResponse response) {
        curationService.removePropertyFromSamples(propertyName, propertyValue);
    }

    @RequestMapping(value = "/assays/properties/{propertyName}/{propertyValue}",
            method = RequestMethod.GET)
    @ResponseStatus(HttpStatus.OK)
    public Collection<ApiProperty> getAssayPropertiesByPropertyValue(@PathVariable("v") final ApiVersionType version,
                                                         @PathVariable("propertyName") final String propertyName,
                                                         @PathVariable("propertyValue") final String propertyValue,
                                                         HttpServletResponse response) throws ResourceNotFoundException {
        return curationService.getAssayPropertiesByPropertyValue(propertyName, propertyValue);
    }

    @RequestMapping(value = "/samples/properties/{propertyName}/{propertyValue}",
            method = RequestMethod.GET)
    @ResponseStatus(HttpStatus.OK)
    public Collection<ApiProperty> getSamplePropertiesByPropertyValue(@PathVariable("v") final ApiVersionType version,
                                                           @PathVariable("propertyName") final String propertyName,
                                                           @PathVariable("propertyValue") final String propertyValue,
                                                           HttpServletResponse response) throws ResourceNotFoundException {
        return curationService.getSamplePropertiesByPropertyValue(propertyName, propertyValue);
    }

    @RequestMapping(value = "/assays/ontologyterms/{ontologyTerm}",
            method = RequestMethod.GET)
    @ResponseStatus(HttpStatus.OK)
    public Collection<ApiAssay> getAssaysByOntologyTerm(@PathVariable("v") final ApiVersionType version,
                                                        @PathVariable("ontologyTerm") final String ontologyTerm,
                                                        HttpServletResponse response) throws ResourceNotFoundException {
        return curationService.getAssaysByOntologyTerm(ontologyTerm);
    }

    @RequestMapping(value = "/samples/ontologyterms/{ontologyTerm}",
            method = RequestMethod.GET)
    @ResponseStatus(HttpStatus.OK)
    public Collection<ApiSample> getSamplesByOntologyTerm(@PathVariable("v") final ApiVersionType version,
                                                          @PathVariable("ontologyTerm") final String ontologyTerm,
                                                          HttpServletResponse response) throws ResourceNotFoundException {
        return curationService.getSamplesByOntologyTerm(ontologyTerm);
    }

    @RequestMapping(value = "/experiments/{experimentAccession}/assays/{assayAccession}",
            method = RequestMethod.GET)
    @ResponseStatus(HttpStatus.OK)
    public ApiAssay getAssay(@PathVariable("v") final ApiVersionType version,
                             @PathVariable("experimentAccession") final String experimentAccession,
                             @PathVariable("assayAccession") final String assayAccession,
                             HttpServletResponse response) throws ResourceNotFoundException {
        return curationService.getAssay(experimentAccession, assayAccession);
    }

    @RequestMapping(value = "/experiments/{experimentAccession}/samples/{sampleAccession}",
            method = RequestMethod.GET)
    @ResponseStatus(HttpStatus.OK)
    public ApiSample getSample(@PathVariable("v") final ApiVersionType version,
                               @PathVariable("experimentAccession") final String experimentAccession,
                               @PathVariable("sampleAccession") final String sampleAccession,
                               HttpServletResponse response) throws ResourceNotFoundException {
        return curationService.getSample(experimentAccession, sampleAccession);
    }

    @RequestMapping(value = "/experiments/{experimentAccession}/assays/{assayAccession}/properties",
            method = RequestMethod.GET)
    @ResponseStatus(HttpStatus.OK)
    public Collection<ApiProperty> getAssayProperties(
            @PathVariable("v") final ApiVersionType version,
            @PathVariable("experimentAccession") final String experimentAccession,
            @PathVariable(value = "assayAccession") final String assayAccession,
            HttpServletResponse response) throws ResourceNotFoundException {
        return curationService.getAssayProperties(experimentAccession, assayAccession);
    }

    @RequestMapping(value = "/experiments/{experimentAccession}/assays/{assayAccession}/properties",
            method = RequestMethod.PUT)
    @ResponseStatus(HttpStatus.CREATED)
    public void putAssayProperties(@PathVariable("v") final ApiVersionType version,
                                   @PathVariable(value = "experimentAccession") final String experimentAccession,
                                   @PathVariable(value = "assayAccession") final String assayAccession,
                                   @RequestBody final ApiProperty[] assayProperties,
                                   HttpServletResponse response) throws ResourceNotFoundException {
        curationService.putAssayProperties(experimentAccession, assayAccession, assayProperties);
    }

    @RequestMapping(value = "/experiments/{experimentAccession}/assays/{assayAccession}/properties",
            method = RequestMethod.DELETE)
    @ResponseStatus(HttpStatus.CREATED)
    public void deleteAssayProperties(@PathVariable("v") final ApiVersionType version,
                                      @PathVariable(value = "experimentAccession") final String experimentAccession,
                                      @PathVariable(value = "assayAccession") final String assayAccession,
                                      @RequestBody final ApiProperty[] assayProperties,
                                      HttpServletResponse response) throws ResourceNotFoundException {
        curationService.deleteAssayProperties(experimentAccession, assayAccession, assayProperties);
    }

    @RequestMapping(value = "/experiments/{experimentAccession}/samples/{sampleAccession}/properties",
            method = RequestMethod.GET)
    @ResponseStatus(HttpStatus.OK)
    public Collection<ApiProperty> getSampleProperties(
            @PathVariable("v") final ApiVersionType version,
            @PathVariable("experimentAccession") final String experimentAccession,
            @PathVariable(value = "sampleAccession") final String sampleAccession,
            HttpServletResponse response) throws ResourceNotFoundException {
        return curationService.getSampleProperties(experimentAccession, sampleAccession);
    }

    @RequestMapping(value = "/experiments/{experimentAccession}/samples/{sampleAccession}/properties",
            method = RequestMethod.PUT)
    @ResponseStatus(HttpStatus.CREATED)
    public void putSampleProperties(@PathVariable("v") final ApiVersionType version,
                                    @PathVariable(value = "experimentAccession") final String experimentAccession,
                                    @PathVariable(value = "sampleAccession") final String sampleAccession,
                                    @RequestBody final ApiProperty[] sampleProperties,
                                    HttpServletResponse response) throws ResourceNotFoundException {
        curationService.putSampleProperties(experimentAccession, sampleAccession, sampleProperties);
    }

    @RequestMapping(value = "/experiments/{experimentAccession}/samples/{sampleAccession}/properties",
            method = RequestMethod.DELETE)
    @ResponseStatus(HttpStatus.CREATED)
    public void deleteSampleProperties(@PathVariable("v") final ApiVersionType version,
                                       @PathVariable(value = "experimentAccession") String experimentAccession,
                                       @PathVariable(value = "sampleAccession") String sampleAccession,
                                       @RequestBody ApiProperty[] sampleProperties,
                                       HttpServletResponse response) throws ResourceNotFoundException {
        curationService.deleteSampleProperties(experimentAccession, sampleAccession, sampleProperties);
    }

    @RequestMapping(value = "/ontologies/{ontologyName}",
            method = RequestMethod.GET)
    @ResponseStatus(HttpStatus.OK)
    public ApiOntology getOntology(@PathVariable("v") final ApiVersionType version,
                                   @PathVariable(value = "ontologyName") final String ontologyName,
                                   HttpServletResponse response) throws ResourceNotFoundException {
        return curationService.getOntology(ontologyName);
    }

    @RequestMapping(value = "/ontologies",
            method = RequestMethod.PUT)
    @ResponseStatus(HttpStatus.CREATED)
    public void putOntology(@PathVariable("v") final ApiVersionType version,
                            @RequestBody final ApiOntology apiOntology,
                            HttpServletResponse response) {
        curationService.putOntology(apiOntology);
    }

    @RequestMapping(value = "/ontologyterms/{ontologyTerm}",
            method = RequestMethod.GET)
    @ResponseStatus(HttpStatus.OK)
    public ApiOntologyTerm getOntologyTerm(@PathVariable("v") final ApiVersionType version,
                                           @PathVariable(value = "ontologyTerm") final String ontologyTerm,
                                           HttpServletResponse response) throws ResourceNotFoundException {
        return curationService.getOntologyTerm(ontologyTerm);
    }

    @RequestMapping(value = "/ontologyterms",
            method = RequestMethod.PUT)
    @ResponseStatus(HttpStatus.CREATED)
    public void putOntologyTerms(@PathVariable("v") final ApiVersionType version,
                                 @RequestBody final ApiOntologyTerm[] apiOntologyTerms,
                                 HttpServletResponse response) {
        curationService.putOntologyTerms(apiOntologyTerms);

    }

}