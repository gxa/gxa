package uk.ac.ebi.gxa.web.controller.api;

import com.google.common.base.Strings;
import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import uk.ac.ebi.gxa.exceptions.ResourceNotFoundException;
import uk.ac.ebi.gxa.service.CurationService;
import uk.ac.ebi.gxa.web.controller.AtlasViewController;
import uk.ac.ebi.microarray.atlas.api.*;

import javax.servlet.http.HttpServletRequest;
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

    final private Logger log = LoggerFactory.getLogger(this.getClass());
    Gson gson = new Gson();

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
                                           HttpServletRequest request,
                                           HttpServletResponse response) {
        curationService.removeUnusedPropertyValues();
        log.info("User: '" + request.getRemoteUser() + "' removed unused property values");
    }

    @RequestMapping(value = "/properties/unused.json",
            method = RequestMethod.DELETE)
    @ResponseStatus(HttpStatus.OK)
    public void removeUnusedProperties(@PathVariable("v") final ApiVersionType version,
                                       HttpServletRequest request,
                                       HttpServletResponse response) {
        curationService.removeUnusedPropertyNames();
        log.info("User: '" + request.getRemoteUser() + "' removed unused properties and their values");
    }

    @RequestMapping(value = "/properties/{propertyName}",
            method = RequestMethod.DELETE)
    @ResponseStatus(HttpStatus.OK)
    public void removePropertyOrValue(
            @PathVariable("v") final ApiVersionType version,
            @PathVariable("propertyName") final String propertyName,
            @RequestParam(value = "propertyValue", required = false) String propertyValue,
            HttpServletRequest request,
            HttpServletResponse response)
            throws ResourceNotFoundException {
        curationService.deletePropertyOrValue(propertyName, propertyValue);
        log.info("User: '" + request.getRemoteUser() +
                "' deleted property: '" + propertyName +
                "'" + (!Strings.isNullOrEmpty(propertyValue) ? " and value: '" + propertyValue + "'" : ""));
    }

    @RequestMapping(value = "/properties/{propertyName}",
            method = RequestMethod.PUT)
    @ResponseStatus(HttpStatus.CREATED)
    public void replacePropertyValueInExperiments(
            @PathVariable("v") final ApiVersionType version,
            @PathVariable("propertyName") final String propertyName,
            @RequestParam(value = "oldPropertyValue", required = true) String oldPropertyValue,
            @RequestParam(value = "newPropertyValue", required = true) String newPropertyValue,
            HttpServletRequest request,
            HttpServletResponse response)
            throws ResourceNotFoundException {
        curationService.replacePropertyValueInExperiments(propertyName, oldPropertyValue, newPropertyValue);
        log.info("User: '" + request.getRemoteUser() +
                "' replaced property value: '" + oldPropertyValue + "' with new value: '" + newPropertyValue +
                "' for property: '" + propertyName + "' in all experiments");
        curationService.deletePropertyOrValue(propertyName, oldPropertyValue);
        log.info("User: '" + request.getRemoteUser() +
                "' deleted property: '" + propertyName +
                "'" + (!Strings.isNullOrEmpty(oldPropertyValue) ? " and value: '" + oldPropertyValue + "'" : ""));
    }

    @RequestMapping(value = "/properties/{oldPropertyName}/{newPropertyName}",
            method = RequestMethod.PUT)
    @ResponseStatus(HttpStatus.CREATED)
    public void replacePropertyInExperiments(
            @PathVariable("v") final ApiVersionType version,
            @PathVariable("oldPropertyName") final String oldPropertyName,
            @PathVariable("newPropertyName") final String newPropertyName,
            HttpServletRequest request,
            HttpServletResponse response)
            throws ResourceNotFoundException {
        curationService.replacePropertyInExperiments(oldPropertyName, newPropertyName);
        log.info("User: '" + request.getRemoteUser() +
                "' replaced property: '" + oldPropertyName + "' with new property: '" + newPropertyName + "' in all experiments");
        curationService.deletePropertyOrValue(oldPropertyName, null);
        log.info("User: '" + request.getRemoteUser() + "' deleted property: '" + oldPropertyName);
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
                                   HttpServletRequest request,
                                   HttpServletResponse response) throws ResourceNotFoundException {
        curationService.putAssayProperties(experimentAccession, assayAccession, assayProperties);
        log.info("User: '" + request.getRemoteUser() +
                "' added/updated the following properties-values in experiment: '" +
                experimentAccession + "' and assay: '" + assayAccession + "' : " + gson.toJson(assayProperties));
    }

    @RequestMapping(value = "/experiments/{experimentAccession}/assays/{assayAccession}/properties",
            method = RequestMethod.DELETE)
    @ResponseStatus(HttpStatus.OK)
    public void deleteAssayProperties(@PathVariable("v") final ApiVersionType version,
                                      @PathVariable(value = "experimentAccession") final String experimentAccession,
                                      @PathVariable(value = "assayAccession") final String assayAccession,
                                      @RequestBody final ApiProperty[] assayProperties,
                                      HttpServletRequest request,
                                      HttpServletResponse response) throws ResourceNotFoundException {
        curationService.deleteAssayProperties(experimentAccession, assayAccession, assayProperties);
        log.info("User: '" + request.getRemoteUser() +
                "' deleted the following properties-values from experiment: '" +
                experimentAccession + "' and assay: '" + assayAccession + "' : " + gson.toJson(assayProperties));
    }

    @RequestMapping(value = "/experiments/{experimentAccession}/samples/{sampleAccession}/properties",
            method = RequestMethod.PUT)
    @ResponseStatus(HttpStatus.CREATED)
    public void putSampleProperties(@PathVariable("v") final ApiVersionType version,
                                    @PathVariable(value = "experimentAccession") final String experimentAccession,
                                    @PathVariable(value = "sampleAccession") final String sampleAccession,
                                    @RequestBody final ApiProperty[] sampleProperties,
                                    HttpServletRequest request,
                                    HttpServletResponse response) throws ResourceNotFoundException {
        curationService.putSampleProperties(experimentAccession, sampleAccession, sampleProperties);
        log.info("User: '" + request.getRemoteUser() +
                "' added/updated the following properties-values in experiment: '" +
                experimentAccession + "' and sample: '" + sampleAccession + "' : " + gson.toJson(sampleProperties));
    }

    @RequestMapping(value = "/experiments/{experimentAccession}/samples/{sampleAccession}/properties",
            method = RequestMethod.DELETE)
    @ResponseStatus(HttpStatus.OK)
    public void deleteSampleProperties(@PathVariable("v") final ApiVersionType version,
                                       @PathVariable(value = "experimentAccession") String experimentAccession,
                                       @PathVariable(value = "sampleAccession") String sampleAccession,
                                       @RequestBody ApiProperty[] sampleProperties,
                                       HttpServletRequest request,
                                       HttpServletResponse response) throws ResourceNotFoundException {
        curationService.deleteSampleProperties(experimentAccession, sampleAccession, sampleProperties);
        log.info("User: '" + request.getRemoteUser() +
                "' deleted the following properties-values from experiment: '" +
                experimentAccession + "' and sample: '" + sampleAccession + "' : " + gson.toJson(sampleProperties));
    }

    @RequestMapping(value = "/ontologyterms",
            method = RequestMethod.PUT)
    @ResponseStatus(HttpStatus.CREATED)
    public void putOntologyTerms(@PathVariable("v") final ApiVersionType version,
                                 @RequestBody final ApiOntologyTerm[] apiOntologyTerms,
                                 HttpServletRequest request,
                                 HttpServletResponse response) {
        curationService.putOntologyTerms(apiOntologyTerms);
        log.info("User: '" + request.getRemoteUser() + "' created the following ontology terms: " + gson.toJson(apiOntologyTerms));
    }
}