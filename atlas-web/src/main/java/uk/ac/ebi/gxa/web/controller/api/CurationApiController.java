package uk.ac.ebi.gxa.web.controller.api;

import com.google.common.collect.Sets;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import uk.ac.ebi.gxa.dao.AssayDAO;
import uk.ac.ebi.gxa.dao.AtlasDAO;
import uk.ac.ebi.gxa.web.controller.AtlasViewController;
import uk.ac.ebi.gxa.web.controller.ResourceNotFoundException;
import uk.ac.ebi.microarray.atlas.api.ApiAssay;
import uk.ac.ebi.microarray.atlas.api.ApiAssayProperty;
import uk.ac.ebi.microarray.atlas.api.ApiExperiment;
import uk.ac.ebi.microarray.atlas.api.ApiOntologyTerm;
import uk.ac.ebi.microarray.atlas.model.Assay;
import uk.ac.ebi.microarray.atlas.model.Experiment;
import uk.ac.ebi.microarray.atlas.model.OntologyTerm;
import uk.ac.ebi.microarray.atlas.model.PropertyValue;

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
    final private AtlasDAO atlasDAO;
    final private AssayDAO assayDAO;

    @Autowired
    public CurationApiController(AtlasDAO atlasDAO, AssayDAO assayDAO) {
        this.atlasDAO = atlasDAO;
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
        atlasDAO.commit();
        response.setStatus(HttpServletResponse.SC_CREATED);
    }
}