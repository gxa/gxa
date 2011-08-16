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

    @Autowired
    private PropertyValueDAO propertyValueDAO;

    private static final Function<Property, ApiPropertyName> PROPERTY_NAME =
            new Function<Property, ApiPropertyName>() {
                public ApiPropertyName apply(@Nonnull Property p) {
                    return new ApiPropertyName(p);
                }
            };

    private static final Function<PropertyValue, ApiPropertyValue> PROPERTY_VALUE =
            new Function<PropertyValue, ApiPropertyValue>() {
                public ApiPropertyValue apply(@Nonnull PropertyValue pv) {
                    return new ApiPropertyValue(pv);
                }
            };


    /**
     * @return alphabetically sorted collection of all property names
     */
    public Collection<ApiPropertyName> getPropertyNames() {
        List<Property> property = propertyDAO.getAll();

        List<ApiPropertyName> propertyNames = Lists.newArrayList(transform(property, PROPERTY_NAME));

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
        List<ApiPropertyValue> propertyValues = Lists.newArrayList(transform(property.getValues(), PROPERTY_VALUE));

        Collections.sort(propertyValues, new Comparator<ApiPropertyValue>() {
            public int compare(ApiPropertyValue o1, ApiPropertyValue o2) {
                return o1.getValue().compareToIgnoreCase(o2.getValue());
            }
        });

        return propertyValues;
    }

    /**
     * Remove propertyName:propertyValue from all assays and samples that are mapped to it; then remove propertyValue from
     * the list of values assigned to propertyName
     *
     * @param propertyName
     * @param propertyValue
     * @throws ResourceNotFoundException
     */
    @Transactional
    public void removePropertyValue(final String propertyName,
                                    final String propertyValue) throws ResourceNotFoundException {

        Property property = propertyDAO.getByName(propertyName);
        checkIfFound(property, Property.class, propertyName);
        PropertyValue propValue = propertyValueDAO.find(property, propertyValue);
        checkIfFound(propValue, PropertyValue.class, propertyValue);

        // First delete propertyName:propertyValue from all assays, samples that point to it
        List<Assay> assays = assayDAO.getAssaysByPropertyValue(propertyValue);
        for (Assay assay : assays) {
            assay.deleteProperty(propValue);
            assayDAO.save(assay);
        }
        List<Sample> samples = sampleDAO.getSamplesByPropertyValue(propertyValue);
        for (Sample sample : samples) {
            sample.deleteProperty(propValue);
            sampleDAO.save(sample);
        }

        // Now delete propertyName:propertyValue itself
        propertyDAO.delete(property, propValue);
        propertyDAO.save(property);
    }

    /**
     * Replaces oldValue of property: propertyName with newValue in all assays in which propertyName-oldValue exists.
     * In cases when a given assay contains both oldValue and newValue, the retained newValue gets mapped to the superset of OntologyTerms
     * assigned to oldValue and newValue.
     *
     * @param propertyName
     * @param oldValue
     * @param newValue
     * @throws ResourceNotFoundException if property: propertyName and/or its value: oldValue don't exist
     */
    @Transactional
    public void replacePropertyValueInAssays(
            final String propertyName,
            final String oldValue,
            final String newValue)
            throws ResourceNotFoundException {
        Property property = propertyDAO.getByName(propertyName);
        checkIfFound(property, Property.class, propertyName);
        PropertyValue oldPropertyValue = propertyValueDAO.find(property, oldValue);
        checkIfFound(oldPropertyValue, PropertyValue.class, oldValue);
        PropertyValue newPropertyValue = atlasDAO.getOrCreatePropertyValue(propertyName, newValue);

        List<Assay> assays = assayDAO.getAssaysByPropertyValue(oldValue);
        for (Assay assay : assays) {
            AssayProperty oldAssayProperty = assay.getProperty(oldPropertyValue);
            AssayProperty newAssayProperty = assay.getProperty(newPropertyValue);
            List<OntologyTerm> terms = oldAssayProperty.getTerms();
            assay.deleteProperty(oldPropertyValue);
            if (newAssayProperty != null) {
                terms.addAll(newAssayProperty.getTerms());
            }
            assay.addOrUpdateProperty(newPropertyValue, terms);
            assayDAO.save(assay);
        }
    }

    /**
     * Replaces oldValue of property: propertyName with newValue in all samples in which propertyName-oldValue exists.
     * In cases when a given sample contains both oldValue and newValue, the retained newValue gets mapped to the superset of OntologyTerms
     * assigned to oldValue and newValue.
     *
     * @param propertyName
     * @param oldValue
     * @param newValue
     * @throws ResourceNotFoundException if property: propertyName and/or its value: oldValue don't exist
     */
    @Transactional
    public void replacePropertyValueInSamples(
            final String propertyName,
            final String oldValue,
            final String newValue)
            throws ResourceNotFoundException {
        Property property = propertyDAO.getByName(propertyName);
        checkIfFound(property, Property.class, propertyName);
        PropertyValue oldPropertyValue = propertyValueDAO.find(property, oldValue);
        checkIfFound(oldPropertyValue, PropertyValue.class, oldValue);
        PropertyValue newPropertyValue = atlasDAO.getOrCreatePropertyValue(propertyName, newValue);

        List<Sample> samples = sampleDAO.getSamplesByPropertyValue(oldValue);
        for (Sample sample : samples) {
            SampleProperty oldSampleProperty = sample.getProperty(oldPropertyValue);
            SampleProperty newSampleProperty = sample.getProperty(newPropertyValue);
            List<OntologyTerm> terms = oldSampleProperty.getTerms();
            sample.deleteProperty(oldPropertyValue);
            if (newSampleProperty != null) {
                terms.addAll(newSampleProperty.getTerms());
            }
            sample.addOrUpdateProperty(newPropertyValue, terms);
            sampleDAO.save(sample);
        }
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
     * @throws ResourceNotFoundException if Ontology that at least one of apiOntologyTerms is assigned to doesn't exist -
     *                                   the user needs to explicitly create the new ontology first
     */
    @Transactional
    public void putOntologyTerms(final ApiOntologyTerm[] apiOntologyTerms) throws ResourceNotFoundException {
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
     * @throws ResourceNotFoundException if Ontology that apiOntologyTerm is assigned to doesn't exist - the user needs
     *                                   to explicitly create the new ontology first
     */
    private OntologyTerm getOrCreateOntologyTerm(@Nonnull ApiOntologyTerm apiOntologyTerm)
            throws ResourceNotFoundException {
        Ontology ontology = ontologyDAO.getByName(apiOntologyTerm.getOntology().getName());
        // Note: user needs to create a new ontology first before assigning ontology terms to it
        checkIfFound(ontology, Ontology.class, apiOntologyTerm.getAccession());

        return atlasDAO.getOrCreateOntologyTerm(
                apiOntologyTerm.getAccession(),
                apiOntologyTerm.getTerm(),
                apiOntologyTerm.getDescription(),
                ontology);
    }

    /**
     * @param entity
     * @param accession
     * @param <T>
     * @throws ResourceNotFoundException if entity was not found
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
