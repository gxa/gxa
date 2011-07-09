package uk.ac.ebi.gxa.web.controller.api;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import uk.ac.ebi.gxa.dao.AssayDAO;
import uk.ac.ebi.gxa.dao.AtlasDAO;
import uk.ac.ebi.gxa.dao.ExperimentDAO;
import uk.ac.ebi.gxa.web.controller.AtlasViewController;
import uk.ac.ebi.gxa.web.controller.ResourceNotFoundException;
import uk.ac.ebi.microarray.atlas.api.*;
import uk.ac.ebi.microarray.atlas.model.*;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletResponse;
import java.util.*;

/**
 * TODO
 *
 * @author Misha Kapushesky
 */
@Controller
public class CurationApiController extends AtlasViewController {
    final private AtlasDAO atlasDAO;
    final private AssayDAO assayDAO;
    private ExperimentDAO experimentDAO;

    @Autowired
    public CurationApiController(AtlasDAO atlasDAO, ExperimentDAO experimentDAO, AssayDAO assayDAO) {
        this.atlasDAO = atlasDAO;
        this.experimentDAO = experimentDAO;
        this.assayDAO = assayDAO;
    }

    @RequestMapping(value = "/experiments/{experimentAccession}",
            method = RequestMethod.GET)
    public ApiExperiment getExperiment(@PathVariable("experimentAccession") final String experimentAccession,
                                        HttpServletResponse response) throws ResourceNotFoundException {
        final Experiment experiment = atlasDAO.getExperimentByAccession(experimentAccession);
        if(experiment == null) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            throw new ResourceNotFoundException("No records for experiment " + experimentAccession);
        }

        response.setStatus(HttpServletResponse.SC_FOUND);
        return new ApiExperiment(experiment);
    }

    @RequestMapping(value = "/experiments/{experimentAccession}",
            method = RequestMethod.PUT)
    public void putExperiment(@PathVariable("experimentAccession") final String experimentAccession,
                                       @RequestBody final ApiExperiment apiExperiment,
                                        HttpServletResponse response) throws ResourceNotFoundException {
        if(atlasDAO.getExperimentByAccession(experimentAccession) != null) {
            response.setStatus(HttpServletResponse.SC_CONFLICT);
        }

        Experiment experiment = new Experiment(apiExperiment.getAccession());

        experiment.setAbstract(apiExperiment.getArticleAbstract());
        experiment.setCurated(apiExperiment.isCurated());
        experiment.setDescription(apiExperiment.getDescription());
        experiment.setLab(apiExperiment.getLab());
        experiment.setLoadDate(apiExperiment.getLoadDate());
        experiment.setPerformer(apiExperiment.getPerformer());
        experiment.setPrivate(apiExperiment.isPrivate());
        experiment.setPubmedId(apiExperiment.getPubmedId());
        experiment.setReleaseDate(apiExperiment.getReleaseDate());

        Map<String,Assay> assays = Maps.newHashMap();
        for (ApiAssay apiAssay : apiExperiment.getAssays()) {
            Assay assay = new Assay(apiAssay.getAccession());
            // TODO: create ArrayDesign
            assay.setArrayDesign(atlasDAO.getArrayDesignShallowByAccession(apiAssay.getArrayDesign().getAccession()));

            for (ApiAssayProperty apiAssayProperty : apiAssay.getProperties()) {
                PropertyValue propertyValue =  atlasDAO.getOrCreatePropertyValue(
                        apiAssayProperty.getPropertyValue().getProperty().getName(),
                        apiAssayProperty.getPropertyValue().getValue());

                Set<OntologyTerm> terms = Sets.newHashSet();
                for (ApiOntologyTerm apiOntologyTerm : apiAssayProperty.getTerms()) {
                    terms.add(atlasDAO.getOrCreateOntologyTerm(
                            apiOntologyTerm.getAccession(),
                            apiOntologyTerm.getTerm(),
                            apiOntologyTerm.getDescription(),
                            apiOntologyTerm.getOntology().getName(),
                            apiOntologyTerm.getOntology().getDescription(),
                            apiOntologyTerm.getOntology().getSourceUri(),
                            apiOntologyTerm.getOntology().getVersion()));
                }

                assay.addOrUpdateProperty(propertyValue, terms);
            }

            assays.put(apiAssay.getAccession(), assay);
        }

        List<Sample> samples = Lists.newArrayList();
        for (ApiSample apiSample : apiExperiment.getSamples()) {
            Sample sample = new Sample(apiSample.getAccession());
            sample.setChannel(apiSample.getChannel());

            if(apiSample.getOrganism() != null){
                sample.setOrganism(atlasDAO.getOrganismByName(apiSample.getOrganism().getName()));
            }

            for (ApiSampleProperty apiSampleProperty : apiSample.getProperties()) {
                PropertyValue propertyValue =  atlasDAO.getOrCreatePropertyValue(
                        apiSampleProperty.getPropertyValue().getProperty().getName(),
                        apiSampleProperty.getPropertyValue().getValue());

                Set<OntologyTerm> terms = Sets.newHashSet();
                for (ApiOntologyTerm apiOntologyTerm : apiSampleProperty.getTerms()) {
                    terms.add(atlasDAO.getOrCreateOntologyTerm(
                            apiOntologyTerm.getAccession(),
                            apiOntologyTerm.getTerm(),
                            apiOntologyTerm.getDescription(),
                            apiOntologyTerm.getOntology().getName(),
                            apiOntologyTerm.getOntology().getDescription(),
                            apiOntologyTerm.getOntology().getSourceUri(),
                            apiOntologyTerm.getOntology().getVersion()));
                }

                sample.addOrUpdateProperty(propertyValue, terms);
            }

            for (ApiAssay apiAssay : apiSample.getAssays()) {
                sample.addAssay(assays.get(apiAssay.getAccession()));
            }

            samples.add(sample);
        }

        experiment.setAssays(new ArrayList<Assay>(assays.values()));
        experiment.setSamples(samples);

        experimentDAO.save(experiment);
        atlasDAO.flushCurrentSession();
        response.setStatus(HttpServletResponse.SC_CREATED);
    }

    @RequestMapping(value = "/experiments/{experimentAccession}/assays/{assayAccession}",
            method = RequestMethod.GET)
    public ApiAssay getAssays(@PathVariable("experimentAccession") final String experimentAccession,
                                   @PathVariable("assayAccession") final String assayAccession,
                                        HttpServletResponse response) throws ResourceNotFoundException {
        final Experiment experiment = atlasDAO.getExperimentByAccession(experimentAccession);
        if(experiment == null) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            throw new ResourceNotFoundException("No records for experiment " + experimentAccession);
        }

        final Assay assay = experiment.getAssay(assayAccession);
        if(assay == null) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            throw new ResourceNotFoundException("No records for assay " + assayAccession);
        }

        response.setStatus(HttpServletResponse.SC_FOUND);
        return new ApiAssay(assay);
    }
    @RequestMapping(value = "/experiments/{experimentAccession}/samples/{sampleAccession}",
            method = RequestMethod.GET)
    public ApiSample getSamples(@PathVariable("experimentAccession") final String experimentAccession,
                                   @PathVariable("sampleAccession") final String sampleAccession,
                                        HttpServletResponse response) throws ResourceNotFoundException {
        final Experiment experiment = atlasDAO.getExperimentByAccession(experimentAccession);
        if(experiment == null) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            throw new ResourceNotFoundException("No records for experiment " + experimentAccession);
        }

        final Sample sample = experiment.getSample(sampleAccession);
        if(sample == null) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            throw new ResourceNotFoundException("No records for assay " + sampleAccession);
        }

        response.setStatus(HttpServletResponse.SC_FOUND);
        return new ApiSample(sample);
    }

    @RequestMapping(value = "/experiments/{experimentAccession}/assays/{assayAccession}/properties",
            method = RequestMethod.GET)
    public Collection<ApiAssayProperty> getAssayProperties(
            @PathVariable("experimentAccession") final String experimentAccession,
            @PathVariable(value="assayAccession") final String assayAccession,
            HttpServletResponse response) throws ResourceNotFoundException {
        final Experiment experiment = atlasDAO.getExperimentByAccession(experimentAccession);
        if(experiment == null) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            throw new ResourceNotFoundException("No records for experiment " + experimentAccession);
        }

        final Assay assay = experiment.getAssay(assayAccession);
        if(assay == null) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            throw new ResourceNotFoundException("No records for assay " + assayAccession);
        }

        response.setStatus(HttpServletResponse.SC_FOUND);
        return new ApiAssay(assay).getProperties();
    }

    @RequestMapping(value = "/experiments/{experimentAccession}/assays/{assayAccession}/properties",
            method = RequestMethod.PUT)
    public void putAssayProperties(@PathVariable(value="experimentAccession") String experimentAccession,
                                   @PathVariable(value="assayAccession") String assayAccession,
                                   @RequestBody ApiAssayProperty[] assayProperties,
            HttpServletResponse response) throws ResourceNotFoundException {
        final Experiment experiment = atlasDAO.getExperimentByAccession(experimentAccession);
        if(experiment == null) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            throw new ResourceNotFoundException("No records for experiment " + experimentAccession);
        }

        final Assay assay = experiment.getAssay(assayAccession);
        if(assay == null) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            throw new ResourceNotFoundException("No records for assay " + assayAccession);
        }

        for (ApiAssayProperty apiAssayProperty : assayProperties) {
            PropertyValue propertyValue =  atlasDAO.getOrCreatePropertyValue(
                    apiAssayProperty.getPropertyValue().getProperty().getName(),
                    apiAssayProperty.getPropertyValue().getValue());

            Set<OntologyTerm> terms = Sets.newHashSet();
            for (ApiOntologyTerm apiOntologyTerm : apiAssayProperty.getTerms()) {
                terms.add(atlasDAO.getOrCreateOntologyTerm(
                        apiOntologyTerm.getAccession(),
                        apiOntologyTerm.getTerm(),
                        apiOntologyTerm.getDescription(),
                        apiOntologyTerm.getOntology().getName(),
                        apiOntologyTerm.getOntology().getDescription(),
                        apiOntologyTerm.getOntology().getSourceUri(),
                        apiOntologyTerm.getOntology().getVersion()));
            }

            assay.addOrUpdateProperty(propertyValue, terms);
        }

        assayDAO.save(assay);
        atlasDAO.flushCurrentSession();
        response.setStatus(HttpServletResponse.SC_CREATED);
    }

    @RequestMapping(value = "/ontologies/{ontologyName}",
            method = RequestMethod.GET)
    public ApiOntology getOntologies(@PathVariable(value="ontologyName") final String ontologyName,
            HttpServletResponse response) {
        response.setStatus(HttpServletResponse.SC_FOUND);
        return new ApiOntology(atlasDAO.getOntologyByName(ontologyName));
    }
}