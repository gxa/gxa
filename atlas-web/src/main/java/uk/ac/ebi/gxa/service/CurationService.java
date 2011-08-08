package uk.ac.ebi.gxa.service;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ebi.gxa.dao.AtlasDAO;
import uk.ac.ebi.gxa.dao.ExperimentDAO;
import uk.ac.ebi.microarray.atlas.api.*;
import uk.ac.ebi.microarray.atlas.model.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Misha Kapushesky
 */
@Service
public class CurationService {
    private final Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    private AtlasDAO atlasDAO;

    @Autowired
    private ExperimentDAO experimentDAO;

    @Transactional
    public void saveExperiment(final ApiExperiment apiExperiment) {
        Experiment experiment = atlasDAO.getExperimentByAccession(apiExperiment.getAccession());

        if(experiment != null) {
//            log.info("Deleting experiment " + experiment.getAccession() + " in order to update");
//            experimentDAO.delete(experiment);
        }

        experiment = new Experiment(apiExperiment.getAccession());

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

                List<OntologyTerm> terms = Lists.newArrayList();
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

                List<OntologyTerm> terms = Lists.newArrayList();
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
    }
}
