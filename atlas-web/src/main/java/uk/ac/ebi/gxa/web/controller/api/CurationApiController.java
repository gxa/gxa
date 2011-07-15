package uk.ac.ebi.gxa.web.controller.api;

import com.google.common.collect.Sets;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import uk.ac.ebi.gxa.dao.*;
import uk.ac.ebi.gxa.service.CurationService;
import uk.ac.ebi.gxa.web.controller.AtlasViewController;
import uk.ac.ebi.gxa.web.controller.ResourceNotFoundException;
import uk.ac.ebi.microarray.atlas.api.*;
import uk.ac.ebi.microarray.atlas.model.*;

import javax.servlet.http.HttpServletResponse;
import java.util.Collection;
import java.util.Set;

/**
 * TODO
 *
 * @author Misha Kapushesky
 */
@Controller
public class CurationApiController extends AtlasViewController {
    @Autowired
    private AtlasDAO atlasDAO;

    @Autowired
    private AssayDAO assayDAO;

    @Autowired
    private SampleDAO sampleDAO;

    @Autowired
    private OntologyDAO ontologyDAO;

    @Autowired
    private OntologyTermDAO ontologyTermDAO;

    @Autowired
    private CurationService curationService;

    @RequestMapping(value = "/experiments/{experimentAccession}",
            method = RequestMethod.GET)
    public ApiExperiment getExperiment(@PathVariable("experimentAccession") final String experimentAccession,
                                       HttpServletResponse response) throws ResourceNotFoundException {
        final Experiment experiment = atlasDAO.getExperimentByAccession(experimentAccession);
        if (experiment == null) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            throw new ResourceNotFoundException("No record for experiment " + experimentAccession);
        }

        response.setStatus(HttpServletResponse.SC_FOUND);
        return new ApiExperiment(experiment);
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
    public ApiAssay getAssays(@PathVariable("experimentAccession") final String experimentAccession,
                              @PathVariable("assayAccession") final String assayAccession,
                              HttpServletResponse response) throws ResourceNotFoundException {
        final Experiment experiment = atlasDAO.getExperimentByAccession(experimentAccession);
        checkIfFound(experiment, response, experimentAccession);

        final Assay assay = experiment.getAssay(assayAccession);
        checkIfFound(assay, response, assayAccession);

        response.setStatus(HttpServletResponse.SC_FOUND);
        return new ApiAssay(assay);
    }

    @RequestMapping(value = "/experiments/{experimentAccession}/samples/{sampleAccession}",
            method = RequestMethod.GET)
    public ApiSample getSamples(@PathVariable("experimentAccession") final String experimentAccession,
                                @PathVariable("sampleAccession") final String sampleAccession,
                                HttpServletResponse response) throws ResourceNotFoundException {
        final Experiment experiment = atlasDAO.getExperimentByAccession(experimentAccession);
        checkIfFound(experiment, response, experimentAccession);

        final Sample sample = experiment.getSample(sampleAccession);
        checkIfFound(experiment, response, sampleAccession);

        response.setStatus(HttpServletResponse.SC_FOUND);
        return new ApiSample(sample);
    }

    @RequestMapping(value = "/experiments/{experimentAccession}/assays/{assayAccession}/properties",
            method = RequestMethod.GET)
    public Collection<ApiAssayProperty> getAssayProperties(
            @PathVariable("experimentAccession") final String experimentAccession,
            @PathVariable(value = "assayAccession") final String assayAccession,
            HttpServletResponse response) throws ResourceNotFoundException {
        final Experiment experiment = atlasDAO.getExperimentByAccession(experimentAccession);
        checkIfFound(experiment, response, experimentAccession);

        final Assay assay = experiment.getAssay(assayAccession);
        checkIfFound(assay, response, assayAccession);

        response.setStatus(HttpServletResponse.SC_FOUND);
        return new ApiAssay(assay).getProperties();
    }

    @RequestMapping(value = "/experiments/{experimentAccession}/assays/{assayAccession}/properties",
            method = RequestMethod.PUT)
    public void putAssayProperties(@PathVariable(value = "experimentAccession") String experimentAccession,
                                   @PathVariable(value = "assayAccession") String assayAccession,
                                   @RequestBody ApiAssayProperty[] assayProperties,
                                   HttpServletResponse response) throws ResourceNotFoundException {
        final Experiment experiment = atlasDAO.getExperimentByAccession(experimentAccession);
        checkIfFound(experiment, response, experimentAccession);

        final Assay assay = experiment.getAssay(assayAccession);
        checkIfFound(assay, response, assayAccession);

        for (ApiAssayProperty apiAssayProperty : assayProperties) {
            PropertyValue propertyValue = atlasDAO.getOrCreatePropertyValue(
                    apiAssayProperty.getPropertyValue().getProperty().getName(),
                    apiAssayProperty.getPropertyValue().getValue());

            Set<OntologyTerm> terms = Sets.newHashSet();
            for (ApiOntologyTerm apiOntologyTerm : apiAssayProperty.getTerms()) {
                terms.add(getOrCreateOntologyTerm(apiOntologyTerm));
            }

            assay.addOrUpdateProperty(propertyValue, terms);
        }

        assayDAO.save(assay);
        atlasDAO.flushCurrentSession();
        response.setStatus(HttpServletResponse.SC_CREATED);
    }

    @RequestMapping(value = "/experiments/{experimentAccession}/assays/{assayAccession}/properties",
            method = RequestMethod.DELETE)
    public void deleteAssayProperties(@PathVariable(value = "experimentAccession") String experimentAccession,
                                      @PathVariable(value = "assayAccession") String assayAccession,
                                      @RequestBody ApiAssayProperty[] assayProperties,
                                      HttpServletResponse response) throws ResourceNotFoundException {
        final Experiment experiment = atlasDAO.getExperimentByAccession(experimentAccession);
        checkIfFound(experiment, response, experimentAccession);

        final Assay assay = experiment.getAssay(assayAccession);
        checkIfFound(assay, response, assayAccession);

        for (ApiAssayProperty apiAssayProperty : assayProperties) {
            PropertyValue propertyValue = atlasDAO.getOrCreatePropertyValue(
                    apiAssayProperty.getPropertyValue().getProperty().getName(),
                    apiAssayProperty.getPropertyValue().getValue());

            assay.deleteProperty(propertyValue);
        }

        assayDAO.save(assay);
        atlasDAO.flushCurrentSession();
        response.setStatus(HttpServletResponse.SC_CREATED);
    }

    @RequestMapping(value = "/experiments/{experimentAccession}/samples/{sampleAccession}/properties",
            method = RequestMethod.GET)
    public Collection<ApiSampleProperty> getSampleProperties(
            @PathVariable("experimentAccession") final String experimentAccession,
            @PathVariable(value = "sampleAccession") final String sampleAccession,
            HttpServletResponse response) throws ResourceNotFoundException {
        final Experiment experiment = atlasDAO.getExperimentByAccession(experimentAccession);
        checkIfFound(experiment, response, experimentAccession);

        final Sample sample = experiment.getSample(sampleAccession);
        checkIfFound(experiment, response, sampleAccession);

        response.setStatus(HttpServletResponse.SC_FOUND);
        return new ApiSample(sample).getProperties();
    }

    @RequestMapping(value = "/experiments/{experimentAccession}/samples/{sampleAccession}/properties",
            method = RequestMethod.PUT)
    public void putSampleProperties(@PathVariable(value = "experimentAccession") String experimentAccession,
                                    @PathVariable(value = "sampleAccession") String sampleAccession,
                                    @RequestBody ApiSampleProperty[] sampleProperties,
                                    HttpServletResponse response) throws ResourceNotFoundException {
        final Experiment experiment = atlasDAO.getExperimentByAccession(experimentAccession);
        checkIfFound(experiment, response, experimentAccession);

        final Sample sample = experiment.getSample(sampleAccession);
        checkIfFound(experiment, response, sampleAccession);

        for (ApiSampleProperty apiSampleProperty : sampleProperties) {
            PropertyValue propertyValue = atlasDAO.getOrCreatePropertyValue(
                    apiSampleProperty.getPropertyValue().getProperty().getName(),
                    apiSampleProperty.getPropertyValue().getValue());

            Set<OntologyTerm> terms = Sets.newHashSet();
            for (ApiOntologyTerm apiOntologyTerm : apiSampleProperty.getTerms()) {
                terms.add(getOrCreateOntologyTerm(apiOntologyTerm));
            }

            sample.addOrUpdateProperty(propertyValue, terms);
        }

        sampleDAO.save(sample);
        atlasDAO.flushCurrentSession();
        response.setStatus(HttpServletResponse.SC_CREATED);
    }

    @RequestMapping(value = "/experiments/{experimentAccession}/samples/{sampleAccession}/properties",
            method = RequestMethod.DELETE)
    public void deleteAssayProperties(@PathVariable(value = "experimentAccession") String experimentAccession,
                                      @PathVariable(value = "sampleAccession") String sampleAccession,
                                      @RequestBody ApiSampleProperty[] sampleProperties,
                                      HttpServletResponse response) throws ResourceNotFoundException {
        final Experiment experiment = atlasDAO.getExperimentByAccession(experimentAccession);
        checkIfFound(experiment, response, experimentAccession);

        final Sample sample = experiment.getSample(sampleAccession);
        checkIfFound(experiment, response, sampleAccession);

        for (ApiSampleProperty apiSampleProperty : sampleProperties) {
            PropertyValue propertyValue = atlasDAO.getOrCreatePropertyValue(
                    apiSampleProperty.getPropertyValue().getProperty().getName(),
                    apiSampleProperty.getPropertyValue().getValue());

            sample.deleteProperty(propertyValue);
        }

        sampleDAO.save(sample);
        atlasDAO.flushCurrentSession();
        response.setStatus(HttpServletResponse.SC_CREATED);
    }

    @RequestMapping(value = "/ontologies/{ontologyName}",
            method = RequestMethod.GET)
    public ApiOntology getOntologies(@PathVariable(value = "ontologyName") final String ontologyName,
                                     HttpServletResponse response) {
        response.setStatus(HttpServletResponse.SC_FOUND);
        return new ApiOntology(atlasDAO.getOntologyByName(ontologyName));
    }

    @RequestMapping(value = "/ontologies",
            method = RequestMethod.PUT)
    public void putOntology(@RequestBody ApiOntology apiOntology,
                            HttpServletResponse response) {
        Ontology ontology = getOrCreateOntology(apiOntology);
        ontologyDAO.save(ontology);
        atlasDAO.flushCurrentSession();
        response.setStatus(HttpServletResponse.SC_CREATED);
    }

    @RequestMapping(value = "/ontologies/{ontologyName}",
            method = RequestMethod.PUT)
    public void putOntology(@PathVariable(value = "ontologyName") final String ontologyName,
                            @RequestBody ApiOntology apiOntology,
                            HttpServletResponse response) {

        Ontology ontology = atlasDAO.getOntologyByName(ontologyName);
        if (ontology == null) {
            ontology = getOrCreateOntology(apiOntology);
        } else {
            ontology.setDescription(apiOntology.getDescription());
            ontology.setName(apiOntology.getName());
            ontology.setVersion(apiOntology.getVersion());
            ontology.setSourceUri(apiOntology.getSourceUri());
        }
        ontologyDAO.save(ontology);
        atlasDAO.flushCurrentSession();
        response.setStatus(HttpServletResponse.SC_CREATED);
    }

    @RequestMapping(value = "/ontologyterms/{ontologyTerm}",
            method = RequestMethod.GET)
    public ApiOntologyTerm getOntologyTerm(@PathVariable(value = "ontologyTerm") final String ontologyTerm,
                                           HttpServletResponse response) {
        response.setStatus(HttpServletResponse.SC_FOUND);
        return new ApiOntologyTerm(atlasDAO.getOntologyTermByAccession(ontologyTerm));
    }

    @RequestMapping(value = "/ontologyterms",
            method = RequestMethod.PUT)
    public void putOntologyTerm(@RequestBody ApiOntologyTerm[] apiOntologyTerms,
                                HttpServletResponse response) {
        for (ApiOntologyTerm apiOntologyTerm : apiOntologyTerms) {
            OntologyTerm ontologyTerm = getOrCreateOntologyTerm(apiOntologyTerm);
            ontologyTermDAO.save(ontologyTerm);
        }
        atlasDAO.flushCurrentSession();
        response.setStatus(HttpServletResponse.SC_CREATED);
    }

    @RequestMapping(value = "/ontologyterms/{ontologyTerm}",
            method = RequestMethod.PUT)
    public void putOntologyTerm(@PathVariable(value = "ontologyTerm") final String ontologyTerm,
                                @RequestBody ApiOntologyTerm apiOntologyTerm,
                                HttpServletResponse response) {
        OntologyTerm ontoTerm = atlasDAO.getOntologyTermByAccession(ontologyTerm);
        if (ontoTerm == null) {
            ontoTerm = getOrCreateOntologyTerm(apiOntologyTerm);
        } else {
            ontoTerm.setAccession(apiOntologyTerm.getAccession());
            ontoTerm.setDescription(apiOntologyTerm.getDescription());
            Ontology ontology = getOrCreateOntology(apiOntologyTerm.getOntology());
            ontoTerm.setOntology(ontology);
            ontoTerm.setTerm(apiOntologyTerm.getTerm());
        }

        ontologyTermDAO.save(ontoTerm);

        atlasDAO.flushCurrentSession();
        response.setStatus(HttpServletResponse.SC_CREATED);
    }


    /**
     *
     * @param apiOntology
     * @return Ontology
     */
    private Ontology getOrCreateOntology(ApiOntology apiOntology) {
        return atlasDAO.getOrCreateOntology(
                apiOntology.getName(),
                apiOntology.getDescription(),
                apiOntology.getSourceUri(),
                apiOntology.getVersion());
    }

    /**
     *
     * @param apiOntologyTerm
     * @return OntologyTerm
     */
    private OntologyTerm getOrCreateOntologyTerm(ApiOntologyTerm apiOntologyTerm) {
        return atlasDAO.getOrCreateOntologyTerm(
                apiOntologyTerm.getAccession(),
                apiOntologyTerm.getTerm(),
                apiOntologyTerm.getDescription(),
                apiOntologyTerm.getOntology().getName(),
                apiOntologyTerm.getOntology().getDescription(),
                apiOntologyTerm.getOntology().getSourceUri(),
                apiOntologyTerm.getOntology().getVersion());
    }

    /**
     * If entity == null, this method sets appropriate response status and then throws ResourceNotFoundException
     *
     * @param entity
     * @param response
     * @param accession
     * @param <T>
     * @throws ResourceNotFoundException
     */
    private <T> void checkIfFound(T entity, HttpServletResponse response, String accession) throws ResourceNotFoundException {
        if (entity == null) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            throw new ResourceNotFoundException("No records for " + entity.getClass().getName() + accession);
        }
    }
}