package uk.ac.ebi.gxa.web.controller.api;

import com.google.common.base.Strings;
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
        return curationService.getPropertyNames();
    }


    @RequestMapping(value = "/properties/{propertyName}.json",
            method = RequestMethod.GET)
    @ResponseStatus(HttpStatus.OK)
    public Collection<ApiPropertyValue> getPropertyValues(
            @PathVariable("v") final ApiVersionType version,
            @PathVariable("propertyName") final String propertyName, HttpServletResponse response)
            throws ResourceNotFoundException {
        return curationService.getPropertyValues(propertyName);
    }

    @RequestMapping(value = "/properties/values/unused.json",
            method = RequestMethod.DELETE)
    @ResponseStatus(HttpStatus.OK)
    public void removeUnusedPropertyValues(@PathVariable("v") final ApiVersionType version,
                                                                HttpServletResponse response) {
        curationService.removeUnusedPropertyValues();
    }

    @RequestMapping(value = "/properties/unused.json",
            method = RequestMethod.DELETE)
    @ResponseStatus(HttpStatus.OK)
    public void removeUnusedProperties(@PathVariable("v") final ApiVersionType version,
                                                           HttpServletResponse response) {
        curationService.removeUnusedPropertyNames();
    }

    @RequestMapping(value = "/properties/{propertyName}",
            method = RequestMethod.DELETE)
    @ResponseStatus(HttpStatus.OK)
    public void removePropertyOrValue(
            @PathVariable("v") final ApiVersionType version,
            @PathVariable("propertyName") final String propertyName,
            @RequestParam(value = "propertyValue", required = false) String propertyValue,
            HttpServletResponse response)
            throws ResourceNotFoundException {
        curationService.deletePropertyOrValue(propertyName, propertyValue);
    }

    @RequestMapping(value = "/properties/{propertyName}",
            method = RequestMethod.PUT)
    @ResponseStatus(HttpStatus.CREATED)
    public void replacePropertyValueInExperiments(
            @PathVariable("v") final ApiVersionType version,
            @PathVariable("propertyName") final String propertyName,
            @RequestParam(value = "oldPropertyValue", required = true) String oldPropertyValue,
            @RequestParam(value = "newPropertyValue", required = true) String newPropertyValue,
            HttpServletResponse response)
            throws ResourceNotFoundException {
        curationService.replacePropertyValueInExperiments(propertyName, oldPropertyValue, newPropertyValue);
    }

    @RequestMapping(value = "/properties/{oldPropertyName}/{newPropertyName}",
            method = RequestMethod.PUT)
    @ResponseStatus(HttpStatus.CREATED)
    public void replacePropertyInExperiments(
            @PathVariable("v") final ApiVersionType version,
            @PathVariable("oldPropertyName") final String oldPropertyName,
            @PathVariable("newPropertyName") final String newPropertyName,
            HttpServletResponse response)
            throws ResourceNotFoundException {
        curationService.replacePropertyInExperiments(oldPropertyName, newPropertyName);
    }

    @RequestMapping(value = "/propertyvaluemappings/exactmatch/{propertyName}.json",
            method = RequestMethod.GET)
    @ResponseStatus(HttpStatus.OK)
    public Collection<ApiShallowProperty> getOntologyMappingsByPropertyValueExactMatch(@PathVariable("v") final ApiVersionType version,
                                                                                       @PathVariable("propertyName") final String propertyName,
                                                                                       @RequestParam(value = "propertyValue", required = false) String propertyValue,
                                                                                       HttpServletResponse response) throws ResourceNotFoundException {
        if (Strings.isNullOrEmpty(propertyValue))
            return curationService.getOntologyMappingsByProperty(propertyName, true);
        return curationService.getOntologyMappingsByPropertyValue(propertyName, propertyValue, true);
    }

    @RequestMapping(value = "/propertyvaluemappings/partialmatch/{propertyName}.json",
            method = RequestMethod.GET)
    @ResponseStatus(HttpStatus.OK)
    public Collection<ApiShallowProperty> getOntologyMappingsByPropertyValuePartialMatch(@PathVariable("v") final ApiVersionType version,
                                                                                         @PathVariable("propertyName") final String propertyName,
                                                                                         @RequestParam(value = "propertyValue", required = false) String propertyValue,
                                                                                         HttpServletResponse response) throws ResourceNotFoundException {
        if (Strings.isNullOrEmpty(propertyValue))
            return curationService.getOntologyMappingsByProperty(propertyName, false);
        return curationService.getOntologyMappingsByPropertyValue(propertyName, propertyValue, false);
    }

    @RequestMapping(value = "/propertyvaluemappings/exactmatch.json",
            method = RequestMethod.GET)
    @ResponseStatus(HttpStatus.OK)
    public Collection<ApiShallowProperty> getOntologyMappingsByPropertyValueExactMatch(@PathVariable("v") final ApiVersionType version,
                                                                                       @RequestParam(value = "propertyValue", required = true) String propertyValue,
                                                                                       HttpServletResponse response) throws ResourceNotFoundException {
        return curationService.getOntologyMappingsByPropertyValue(null, propertyValue, true);
    }

    @RequestMapping(value = "/propertyvaluemappings/{ontologyTerm}.json",
            method = RequestMethod.GET)
    @ResponseStatus(HttpStatus.OK)
    public Collection<ApiShallowProperty> getOntologyMappingsByOntologyTerm(@PathVariable("v") final ApiVersionType version,
                                                                                      @PathVariable("ontologyTerm") final String ontologyTerm,
                                                                                      HttpServletResponse response) throws ResourceNotFoundException {
        return curationService.getOntologyMappingsByOntologyTerm(ontologyTerm);
    }

    @RequestMapping(value = "/propertyvaluemappings/partialmatch.json",
            method = RequestMethod.GET)
    @ResponseStatus(HttpStatus.OK)
    public Collection<ApiShallowProperty> getOntologyMappingsByPropertyValuePartialMatch(@PathVariable("v") final ApiVersionType version,
                                                                                         @RequestParam(value = "propertyValue", required = true) String propertyValue,
                                                                                         HttpServletResponse response) throws ResourceNotFoundException {
        return curationService.getOntologyMappingsByPropertyValue(null, propertyValue, false);
    }

    @RequestMapping(value = "/experiments/{experimentAccession}.json",
            method = RequestMethod.GET)
    @ResponseStatus(HttpStatus.OK)
    public ApiShallowExperiment getExperiment(@PathVariable("v") final ApiVersionType version,
                                              @PathVariable("experimentAccession") final String experimentAccession,
                                              HttpServletResponse response) throws ResourceNotFoundException {
        return curationService.getExperiment(experimentAccession);
    }

    @RequestMapping(value = "/experiments/properties/{propertyName}.json",
            method = RequestMethod.GET)
    @ResponseStatus(HttpStatus.OK)
    public Collection<ApiShallowExperiment> getExperimentsByPropertyValue(@PathVariable("v") final ApiVersionType version,
                                                                          @PathVariable("propertyName") final String propertyName,
                                                                          @RequestParam(value = "propertyValue", required = true) String propertyValue,
                                                                          HttpServletResponse response) throws ResourceNotFoundException {
        return curationService.getExperimentsByPropertyValue(propertyName, propertyValue);
    }

    @RequestMapping(value = "/experiments/ontologyterms/{ontologyTerm}.json",
            method = RequestMethod.GET)
    @ResponseStatus(HttpStatus.OK)
    public Collection<ApiShallowExperiment> getExperimentsByOntologyTerm(@PathVariable("v") final ApiVersionType version,
                                                                         @PathVariable("ontologyTerm") final String ontologyTerm,
                                                                         HttpServletResponse response) throws ResourceNotFoundException {
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

    @RequestMapping(value = "/ontologyterms",
            method = RequestMethod.PUT)
    @ResponseStatus(HttpStatus.CREATED)
    public void putOntologyTerms(@PathVariable("v") final ApiVersionType version,
                                 @RequestBody final ApiOntologyTerm[] apiOntologyTerms,
                                 HttpServletResponse response) {
        curationService.putOntologyTerms(apiOntologyTerms);

    }
}