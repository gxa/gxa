package uk.ac.ebi.gxa.service;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ebi.gxa.dao.*;
import uk.ac.ebi.gxa.exceptions.ResourceNotFoundException;
import uk.ac.ebi.microarray.atlas.api.*;
import uk.ac.ebi.microarray.atlas.model.*;

import javax.annotation.Nonnull;
import java.util.*;

import static com.google.common.collect.Collections2.transform;

/**
 * This class handles all Curation API requests, delegated from CurationApiController
 *
 * @author Misha Kapushesky
 */
@Service
public class CurationService {
    private final Logger log = LoggerFactory.getLogger(getClass());

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
    private ExperimentDAO experimentDAO;

    @Autowired
    private PropertyDAO propertyDAO;


    /**
     * @return alphabetically sorted collection of all property names
     */
    public Collection<ApiPropertyName> getPropertyNames() {
        List<Property> property = propertyDAO.getAll();

        List<ApiPropertyName> propertyNames = Lists.newArrayList(transform(property,
                new Function<Property, ApiPropertyName>() {
                    public ApiPropertyName apply(@Nonnull Property p) {
                        return new ApiPropertyName(p);
                    }
                }));

        Collections.sort(propertyNames, new Comparator<ApiPropertyName>() {
            public int compare(ApiPropertyName o1, ApiPropertyName o2) {
                return o1.getName().compareToIgnoreCase(o2.getName());
            }
        });

        return propertyNames;
    }

    /**
     * @param propertyName
     * @return alphabetically sorted collection of values for propertyName
     * @throws ResourceNotFoundException
     */
    public Collection<ApiPropertyValue> getPropertyValues(final String propertyName)
            throws ResourceNotFoundException {
        Property property = propertyDAO.getByName(propertyName);
        checkIfFound(property, Property.class, propertyName);
        List<ApiPropertyValue> propertyValues = Lists.newArrayList(transform(property.getValues(),
                new Function<PropertyValue, ApiPropertyValue>() {
                    public ApiPropertyValue apply(@Nonnull PropertyValue pv) {
                        return new ApiPropertyValue(pv);
                    }
                }));

        Collections.sort(propertyValues, new Comparator<ApiPropertyValue>() {
            public int compare(ApiPropertyValue o1, ApiPropertyValue o2) {
                return o1.getValue().compareToIgnoreCase(o2.getValue());
            }
        });

        return propertyValues;
    }


    /**
     * Saves apiExperiment
     *
     * @param apiExperiment
     */
    @Transactional
    public void saveExperiment(@Nonnull final ApiExperiment apiExperiment) {
        Experiment experiment = atlasDAO.getExperimentByAccession(apiExperiment.getAccession());

        if (experiment != null) {
            // TODO: 4ostolop: should we or should we not keep it?

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

        Map<String, Assay> assays = Maps.newHashMap();
        for (ApiAssay apiAssay : apiExperiment.getAssays()) {
            Assay assay = new Assay(apiAssay.getAccession());
            // TODO: create ArrayDesign
            assay.setArrayDesign(atlasDAO.getArrayDesignShallowByAccession(apiAssay.getArrayDesign().getAccession()));

            for (ApiProperty apiAssayProperty : apiAssay.getProperties()) {
                PropertyValue propertyValue = atlasDAO.getOrCreatePropertyValue(
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

            if (apiSample.getOrganism() != null) {
                sample.setOrganism(atlasDAO.getOrganismByName(apiSample.getOrganism().getName()));
            }

            for (ApiProperty apiSampleProperty : apiSample.getProperties()) {
                PropertyValue propertyValue = atlasDAO.getOrCreatePropertyValue(
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

    /**
     * @param experimentAccession
     * @return ApiExperiment corresponding to experimentAccession
     * @throws ResourceNotFoundException if experiment not found
     */
    public ApiExperiment getExperiment(final String experimentAccession)
            throws ResourceNotFoundException {

        final Experiment experiment = atlasDAO.getExperimentByAccession(experimentAccession);
        checkIfFound(experiment, Experiment.class, experimentAccession);

        return new ApiExperiment(experiment);
    }

    /**
     * @param experimentAccession
     * @param assayAccession
     * @return ApiAssay corresponding to assayAccession in experiment: experimentAccession
     * @throws ResourceNotFoundException if experiment: experimentAccession or assay: assayAccession in that experiment are not found
     */
    public ApiAssay getAssay(final String experimentAccession, final String assayAccession)
            throws ResourceNotFoundException {

        Assay assay = findAssay(experimentAccession, assayAccession);
        return new ApiAssay(assay);
    }

    /**
     * @param experimentAccession
     * @param sampleAccession
     * @return ApiSample corresponding to sampleAccession in experiment: experimentAccession
     * @throws ResourceNotFoundException if experiment: experimentAccession or sample: sampleAccession in that experiment are not found
     */
    public ApiSample getSample(final String experimentAccession, final String sampleAccession)
            throws ResourceNotFoundException {
        Sample sample = findSample(experimentAccession, sampleAccession);
        return new ApiSample(sample);
    }

    /**
     * @param experimentAccession
     * @param assayAccession
     * @return Collection of ApiAssayProperty for assay: assayAccession in experiment: experimentAccession
     * @throws ResourceNotFoundException if experiment: experimentAccession or assay: assayAccession in that experiment are not found
     */
    public Collection<ApiProperty> getAssayProperties(
            final String experimentAccession,
            final String assayAccession)
            throws ResourceNotFoundException {
        Assay assay = findAssay(experimentAccession, assayAccession);
        return new ApiAssay(assay).getProperties();
    }

    /**
     * Adds (or updates mapping to efo terms for) assayProperties to assay: assayAccession in experiment: experimentAccession
     *
     * @param experimentAccession
     * @param assayAccession
     * @param assayProperties
     * @throws ResourceNotFoundException if experiment: experimentAccession or assay: assayAccession in that experiment are not found
     */
    @Transactional
    public void putAssayProperties(final String experimentAccession,
                                   final String assayAccession,
                                   final ApiProperty[] assayProperties) throws ResourceNotFoundException {
        Assay assay = findAssay(experimentAccession, assayAccession);

        for (ApiProperty apiAssayProperty : assayProperties) {
            PropertyValue propertyValue = atlasDAO.getOrCreatePropertyValue(
                    apiAssayProperty.getPropertyValue().getProperty().getName(),
                    apiAssayProperty.getPropertyValue().getValue());

            List<OntologyTerm> terms = Lists.newArrayList();
            for (ApiOntologyTerm apiOntologyTerm : apiAssayProperty.getTerms()) {
                terms.add(getOrCreateOntologyTerm(apiOntologyTerm));
            }

            assay.addOrUpdateProperty(propertyValue, terms);
        }

        assayDAO.save(assay);
    }

    /**
     * Removes assayProperties from assay: assayAccession in experiment: experimentAccession
     *
     * @param experimentAccession
     * @param assayAccession
     * @param assayProperties
     * @throws ResourceNotFoundException if experiment: experimentAccession or assay: assayAccession in that experiment are not found
     */
    @Transactional
    public void deleteAssayProperties(final String experimentAccession,
                                      final String assayAccession,
                                      final ApiProperty[] assayProperties) throws ResourceNotFoundException {
        Assay assay = findAssay(experimentAccession, assayAccession);

        for (ApiProperty apiProperty : assayProperties) {
            PropertyValue propertyValue = atlasDAO.getOrCreatePropertyValue(
                    apiProperty.getPropertyValue().getProperty().getName(),
                    apiProperty.getPropertyValue().getValue());

            assay.deleteProperty(propertyValue);
        }

        assayDAO.save(assay);
    }

    /**
     * @param experimentAccession
     * @param sampleAccession
     * @return Collection of ApiSampleProperty from sample: sampleAccession in experiment: experimentAccession
     * @throws ResourceNotFoundException if experiment: experimentAccession or sample: sampleAccession
     *                                   in that experiment are not found
     */
    public Collection<ApiProperty> getSampleProperties(
            final String experimentAccession,
            final String sampleAccession)
            throws ResourceNotFoundException {
        Sample sample = findSample(experimentAccession, sampleAccession);
        return new ApiSample(sample).getProperties();
    }


    /**
     * Adds (or updates mapping to efo terms for) sampleProperties to sample: sampleAccession in experiment: experimentAccession
     *
     * @param experimentAccession
     * @param sampleAccession
     * @param sampleProperties
     * @throws ResourceNotFoundException if experiment: experimentAccession or sample: sampleAccession
     *                                   in that experiment are not found
     */
    @Transactional
    public void putSampleProperties(final String experimentAccession,
                                    final String sampleAccession,
                                    final ApiProperty[] sampleProperties) throws ResourceNotFoundException {
        Sample sample = findSample(experimentAccession, sampleAccession);

        for (ApiProperty apiSampleProperty : sampleProperties) {
            PropertyValue propertyValue = atlasDAO.getOrCreatePropertyValue(
                    apiSampleProperty.getPropertyValue().getProperty().getName(),
                    apiSampleProperty.getPropertyValue().getValue());

            List<OntologyTerm> terms = Lists.newArrayList();
            for (ApiOntologyTerm apiOntologyTerm : apiSampleProperty.getTerms()) {
                terms.add(getOrCreateOntologyTerm(apiOntologyTerm));
            }

            sample.addOrUpdateProperty(propertyValue, terms);
        }

        sampleDAO.save(sample);
    }

    /**
     * Deletes sampleProperties from sample: sampleAccession in experiment: experimentAccession
     *
     * @param experimentAccession
     * @param sampleAccession
     * @param sampleProperties
     * @throws ResourceNotFoundException if experiment: experimentAccession or sample: sampleAccession
     *                                   in that experiment are not found
     */
    @Transactional
    public void deleteSampleProperties(final String experimentAccession,
                                       final String sampleAccession,
                                       final ApiProperty[] sampleProperties) throws ResourceNotFoundException {
        Sample sample = findSample(experimentAccession, sampleAccession);

        for (ApiProperty apiSampleProperty : sampleProperties) {
            PropertyValue propertyValue = atlasDAO.getOrCreatePropertyValue(
                    apiSampleProperty.getPropertyValue().getProperty().getName(),
                    apiSampleProperty.getPropertyValue().getValue());

            sample.deleteProperty(propertyValue);
        }

        sampleDAO.save(sample);
    }

    /**
     * @param ontologyName
     * @return ApiOntology corresponding to ontologyName
     * @throws ResourceNotFoundException if ontology: ontologyName was not found
     */
    public ApiOntology getOntology(final String ontologyName) throws ResourceNotFoundException {
        Ontology ontology = atlasDAO.getOntologyByName(ontologyName);
        checkIfFound(ontology, Ontology.class, ontologyName);
        return new ApiOntology(ontology);
    }


    /**
     * Adds or updates details for Ontology corresponding to apiOntology
     *
     * @param apiOntology
     */
    @Transactional
    public void putOntology(@Nonnull final ApiOntology apiOntology) {

        Ontology ontology = atlasDAO.getOntologyByName(apiOntology.getName());
        if (ontology == null) {
            ontology = getOrCreateOntology(apiOntology);
        } else {
            ontology.setDescription(apiOntology.getDescription());
            ontology.setName(apiOntology.getName());
            ontology.setVersion(apiOntology.getVersion());
            ontology.setSourceUri(apiOntology.getSourceUri());
        }
        ontologyDAO.save(ontology);
    }

    /**
     * @param ontologyTermAcc
     * @return ApiOntologyTerm corresponding to ontologyTerm
     * @throws ResourceNotFoundException if ontology term: ontologyTerm was not found
     */
    public ApiOntologyTerm getOntologyTerm(final String ontologyTermAcc) throws ResourceNotFoundException {

        OntologyTerm ontologyTerm = atlasDAO.getOntologyTermByAccession(ontologyTermAcc);
        checkIfFound(ontologyTerm, OntologyTerm.class, ontologyTermAcc);
        return new ApiOntologyTerm(ontologyTerm);
    }

    /**
     * Add (or update mappings to Ontology for) apiOntologyTerms
     *
     * @param apiOntologyTerms
     */
    @Transactional
    public void putOntologyTerms(final ApiOntologyTerm[] apiOntologyTerms) {
        for (ApiOntologyTerm apiOntologyTerm : apiOntologyTerms) {
            OntologyTerm ontologyTerm = atlasDAO.getOntologyTermByAccession(apiOntologyTerm.getAccession());
            if (ontologyTerm == null) {
                ontologyTerm = getOrCreateOntologyTerm(apiOntologyTerm);
            } else {
                ontologyTerm.setAccession(apiOntologyTerm.getAccession());
                ontologyTerm.setDescription(apiOntologyTerm.getDescription());
                Ontology ontology = getOrCreateOntology(apiOntologyTerm.getOntology());
                ontologyTerm.setOntology(ontology);
                ontologyTerm.setTerm(apiOntologyTerm.getTerm());
            }
            ontologyTermDAO.save(ontologyTerm);
        }
    }


    /**
     * @param apiOntology
     * @return existing Ontology corresponding to apiOntology.getName(); otherwise a new Ontology corresponding to apiOntology
     */
    private Ontology getOrCreateOntology(@Nonnull ApiOntology apiOntology) {
        return atlasDAO.getOrCreateOntology(
                apiOntology.getName(),
                apiOntology.getDescription(),
                apiOntology.getSourceUri(),
                apiOntology.getVersion());
    }

    /**
     * @param apiOntologyTerm
     * @return existing OntologyTerm corresponding to apiOntologyTerm.getAccession(); otherwise a new OntologyTerm
     *         corresponding to apiOntologyTerm
     */
    private OntologyTerm getOrCreateOntologyTerm(@Nonnull ApiOntologyTerm apiOntologyTerm) {
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
     * @param accession
     * @param <T>
     * @throws ResourceNotFoundException
     */
    private <T> void checkIfFound(T entity, Class clazz, String accession) throws ResourceNotFoundException {
        if (entity == null) {
            throw new ResourceNotFoundException("No records for " + clazz.getName() + ": " + accession);
        }
    }


    /**
     * @param experimentAccession
     * @param assayAccession
     * @return Assay corresponding to assayAccession in experiment: experimentAccession
     * @throws ResourceNotFoundException if experiment: experimentAccession or assay: assayAccession
     *                                   in that experiment are not found
     */
    private Assay findAssay(final String experimentAccession, final String assayAccession) throws ResourceNotFoundException {
        final Experiment experiment = atlasDAO.getExperimentByAccession(experimentAccession);
        checkIfFound(experiment, Experiment.class, experimentAccession);

        final Assay assay = experiment.getAssay(assayAccession);
        checkIfFound(assay, Assay.class, assayAccession);
        return assay;
    }

    /**
     * @param experimentAccession
     * @param sampleAccession
     * @return Sample corresponding to sampleAccession in experiment: experimentAccession
     * @throws ResourceNotFoundException if experiment: experimentAccession or sample: sampleAccession
     *                                   in that experiment are not found
     */
    private Sample findSample(final String experimentAccession, final String sampleAccession) throws ResourceNotFoundException {
        final Experiment experiment = atlasDAO.getExperimentByAccession(experimentAccession);
        checkIfFound(experiment, Experiment.class, experimentAccession);

        final Sample sample = experiment.getSample(sampleAccession);
        checkIfFound(sample, Sample.class, sampleAccession);
        return sample;
    }
}
