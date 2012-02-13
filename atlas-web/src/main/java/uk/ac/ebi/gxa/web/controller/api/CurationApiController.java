package uk.ac.ebi.gxa.web.controller.api;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import uk.ac.ebi.gxa.exceptions.ResourceNotFoundException;
import uk.ac.ebi.gxa.service.CurationService;
import uk.ac.ebi.gxa.web.controller.AtlasViewController;
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

    @RequestMapping(value = "/properties.json",
            method = RequestMethod.GET)
    @ResponseStatus(HttpStatus.OK)
    public Collection<ApiPropertyName> getPropertyNames(
            @PathVariable("v") final ApiVersionType version,
            HttpServletResponse response)
            throws ResourceNotFoundException {
        crossOriginHack(response);
        return curationService.getPropertyNames();
    }


    @RequestMapping(value = "/properties/{propertyName}.json",
            method = RequestMethod.GET)
    @ResponseStatus(HttpStatus.OK)
    public Collection<ApiPropertyValue> getPropertyValues(
            @PathVariable("v") final ApiVersionType version,
            @PathVariable("propertyName") final String propertyName, HttpServletResponse response)
            throws ResourceNotFoundException {
        crossOriginHack(response);
        return curationService.getPropertyValues(propertyName);
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
        crossOriginHack(response);
        curationService.removePropertyValue(propertyName, propertyValue);
    }

    @RequestMapping(value = "/experiments/properties/{propertyName}/{oldPropertyValue}/{newPropertyValue}",
            method = RequestMethod.PUT)
    @ResponseStatus(HttpStatus.CREATED)
    public void replacePropertyValueInExperiments(
            @PathVariable("v") final ApiVersionType version,
            @PathVariable("propertyName") final String propertyName,
            @PathVariable("oldPropertyValue") final String oldPropertyValue,
            @PathVariable("newPropertyValue") final String newPropertyValue,
            HttpServletResponse response)
            throws ResourceNotFoundException {
        crossOriginHack(response);
        curationService.replacePropertyValueInExperiments(propertyName, oldPropertyValue, newPropertyValue);
    }

    @RequestMapping(value = "/propertyvaluemappings/exactmatch/{propertyName}/{propertyValue}.json",
            method = RequestMethod.GET)
    @ResponseStatus(HttpStatus.OK)
    public Collection<ApiShallowProperty> getOntologyMappingsByPropertyValueExactMatch(@PathVariable("v") final ApiVersionType version,
                                                                                       @PathVariable("propertyName") final String propertyName,
                                                                                       @PathVariable("propertyValue") final String propertyValue,
                                                                                       HttpServletResponse response) throws ResourceNotFoundException {
        crossOriginHack(response);
        return curationService.getOntologyMappingsByPropertyValue(propertyName, propertyValue, true);
    }

    @RequestMapping(value = "/propertyvaluemappings/partialmatch/{propertyName}/{propertyValue}.json",
            method = RequestMethod.GET)
    @ResponseStatus(HttpStatus.OK)
    public Collection<ApiShallowProperty> getOntologyMappingsByPropertyValuePartialMatch(@PathVariable("v") final ApiVersionType version,
                                                                                         @PathVariable("propertyName") final String propertyName,
                                                                                         @PathVariable("propertyValue") final String propertyValue,
                                                                                         HttpServletResponse response) throws ResourceNotFoundException {
        crossOriginHack(response);
        return curationService.getOntologyMappingsByPropertyValue(propertyName, propertyValue, false);
    }

    @RequestMapping(value = "/propertyvaluemappings/exactmatch/{propertyValue}.json",
            method = RequestMethod.GET)
    @ResponseStatus(HttpStatus.OK)
    public Collection<ApiShallowProperty> getOntologyMappingsByPropertyValueExactMatch(@PathVariable("v") final ApiVersionType version,
                                                                                       @PathVariable("propertyValue") final String propertyValue,
                                                                                       HttpServletResponse response) throws ResourceNotFoundException {
        crossOriginHack(response);
        return curationService.getOntologyMappingsByPropertyValue(null, propertyValue, true);
    }

    @RequestMapping(value = "/propertyvaluemappings/partialmatch/{propertyValue}.json",
            method = RequestMethod.GET)
    @ResponseStatus(HttpStatus.OK)
    public Collection<ApiShallowProperty> getOntologyMappingsByPropertyValuePartialMatch(@PathVariable("v") final ApiVersionType version,
                                                                                         @PathVariable("propertyValue") final String propertyValue,
                                                                                         HttpServletResponse response) throws ResourceNotFoundException {
        crossOriginHack(response);
        return curationService.getOntologyMappingsByPropertyValue(null, propertyValue, false);
    }

    @RequestMapping(value = "/experiments/{experimentAccession}.json",
            method = RequestMethod.GET)
    @ResponseStatus(HttpStatus.OK)
    public ApiShallowExperiment getExperiment(@PathVariable("v") final ApiVersionType version,
                                              @PathVariable("experimentAccession") final String experimentAccession,
                                              HttpServletResponse response) throws ResourceNotFoundException {
        crossOriginHack(response);
        return curationService.getExperiment(experimentAccession);
    }

    @RequestMapping(value = "/assays/properties/{propertyName}",
            method = RequestMethod.DELETE)
    @ResponseStatus(HttpStatus.OK)
    public void removePropertyFromAssays(@PathVariable("v") final ApiVersionType version,
                                         @PathVariable("propertyName") final String propertyName,
                                         HttpServletResponse response) {
        crossOriginHack(response);
        curationService.removePropertyFromAssays(propertyName);
    }

    @RequestMapping(value = "/samples/properties/{propertyName}",
            method = RequestMethod.DELETE)
    @ResponseStatus(HttpStatus.OK)
    public void removePropertyFromSamples(@PathVariable("v") final ApiVersionType version,
                                          @PathVariable("propertyName") final String propertyName,
                                          HttpServletResponse response) {
        crossOriginHack(response);
        curationService.removePropertyFromSamples(propertyName);
    }

    @RequestMapping(value = "/experiments/properties/{propertyName}/{propertyValue}.json",
            method = RequestMethod.GET)
    @ResponseStatus(HttpStatus.OK)
    public Collection<ApiShallowExperiment> getExperimentsByPropertyValue(@PathVariable("v") final ApiVersionType version,
                                                                          @PathVariable("propertyName") final String propertyName,
                                                                          @PathVariable("propertyValue") final String propertyValue,
                                                                          HttpServletResponse response) throws ResourceNotFoundException {
        crossOriginHack(response);
        return curationService.getExperimentsByPropertyValue(propertyName, propertyValue);
    }

    @RequestMapping(value = "/experiments/ontologyterms/{ontologyTerm}.json",
            method = RequestMethod.GET)
    @ResponseStatus(HttpStatus.OK)
    public Collection<ApiShallowExperiment> getExperimentsByOntologyTerm(@PathVariable("v") final ApiVersionType version,
                                                                         @PathVariable("ontologyTerm") final String ontologyTerm,
                                                                         HttpServletResponse response) throws ResourceNotFoundException {
        crossOriginHack(response);
        return curationService.getExperimentsByOntologyTerm(ontologyTerm);
    }

    @RequestMapping(value = "/experiments/{experimentAccession}/assays/{assayAccession}/properties",
            method = RequestMethod.PUT)
    @ResponseStatus(HttpStatus.CREATED)
    public void putAssayProperties(@PathVariable("v") final ApiVersionType version,
                                   @PathVariable(value = "experimentAccession") final String experimentAccession,
                                   @PathVariable(value = "assayAccession") final String assayAccession,
                                   @RequestBody final ApiProperty[] assayProperties,
                                   HttpServletResponse response) throws ResourceNotFoundException {
        crossOriginHack(response);
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
        crossOriginHack(response);
        curationService.deleteAssayProperties(experimentAccession, assayAccession, assayProperties);
    }

    @RequestMapping(value = "/experiments/{experimentAccession}/samples/{sampleAccession}/properties",
            method = RequestMethod.PUT)
    @ResponseStatus(HttpStatus.CREATED)
    public void putSampleProperties(@PathVariable("v") final ApiVersionType version,
                                    @PathVariable(value = "experimentAccession") final String experimentAccession,
                                    @PathVariable(value = "sampleAccession") final String sampleAccession,
                                    @RequestBody final ApiProperty[] sampleProperties,
                                    HttpServletResponse response) throws ResourceNotFoundException {
        crossOriginHack(response);
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
        crossOriginHack(response);
        curationService.deleteSampleProperties(experimentAccession, sampleAccession, sampleProperties);
    }

    @RequestMapping(value = "/ontologyterms",
            method = RequestMethod.PUT)
    @ResponseStatus(HttpStatus.CREATED)
    public void putOntologyTerms(@PathVariable("v") final ApiVersionType version,
                                 @RequestBody final ApiOntologyTerm[] apiOntologyTerms,
                                 HttpServletResponse response) {
        crossOriginHack(response);
        curationService.putOntologyTerms(apiOntologyTerms);

    }

    // TODO 4rpetry remove the following - temporary hack to aid development (prevent cross origin resource sharing issues)
    private static void crossOriginHack(HttpServletResponse response) {
        response.setHeader("Access-Control-Allow-Origin", "*");
    }
}