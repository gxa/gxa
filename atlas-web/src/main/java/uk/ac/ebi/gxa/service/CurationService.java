package uk.ac.ebi.gxa.service;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ebi.gxa.dao.*;
import uk.ac.ebi.gxa.dao.exceptions.RecordNotFoundException;
import uk.ac.ebi.gxa.exceptions.ResourceNotFoundException;
import uk.ac.ebi.microarray.atlas.api.*;
import uk.ac.ebi.microarray.atlas.model.*;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

import static com.google.common.collect.Collections2.transform;

/**
 * This class handles all Curation API requests, delegated from CurationApiController
 *
 * @author Misha Kapushesky
 */
@Service
public class CurationService {
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
    private PropertyDAO propertyDAO;

    @Autowired
    private PropertyValueDAO propertyValueDAO;

    @Autowired
    private ExperimentDAO experimentDAO;

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

    private static final Function<Experiment, ApiExperiment> EXPERIMENT =
            new Function<Experiment, ApiExperiment>() {
                public ApiExperiment apply(@Nonnull Experiment e) {
                    return new ApiExperiment(e);
                }
            };

    private static final Function<Assay, ApiAssay> ASSAY =
            new Function<Assay, ApiAssay>() {
                public ApiAssay apply(@Nonnull Assay e) {
                    return new ApiAssay(e);
                }
            };

    private static final Function<Sample, ApiSample> SAMPLE =
            new Function<Sample, ApiSample>() {
                public ApiSample apply(@Nonnull Sample e) {
                    return new ApiSample(e);
                }
            };

    private static final Function<AssayProperty, ApiProperty> ASSAYPROPERTY_APIPROPERTY =
            new Function<AssayProperty, ApiProperty>() {
                public ApiProperty apply(@Nonnull AssayProperty e) {
                    return new ApiProperty(e);
                }
            };
    private static final Function<SampleProperty, ApiProperty> SAMPLEPROPERTY_APIPROPERTY =
            new Function<SampleProperty, ApiProperty>() {
                public ApiProperty apply(@Nonnull SampleProperty e) {
                    return new ApiProperty(e);
                }
            };

    private static final Function<AssayProperty, PropertyValue> ASSAY_PROPERTY =
            new Function<AssayProperty, PropertyValue>() {
                @Override
                public PropertyValue apply(@Nonnull AssayProperty input) {
                    return input.getPropertyValue();
                }
            };

    private static final Function<SampleProperty, PropertyValue> SAMPLE_PROPERTY =
            new Function<SampleProperty, PropertyValue>() {
                @Override
                public PropertyValue apply(@Nonnull SampleProperty input) {
                    return input.getPropertyValue();
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
        try {
            Property property = propertyDAO.getByName(propertyName);
            List<ApiPropertyValue> propertyValues = Lists.newArrayList(transform(property.getValues(), PROPERTY_VALUE));

            Collections.sort(propertyValues, new Comparator<ApiPropertyValue>() {
                public int compare(ApiPropertyValue o1, ApiPropertyValue o2) {
                    return o1.getValue().compareToIgnoreCase(o2.getValue());
                }
            });

            return propertyValues;
        } catch (RecordNotFoundException e) {
            throw convert(e);
        }
    }

    /**
     * @param propertyName
     * @return List of ApiAssay's containing propertyName-propertyValue
     */
    public Collection<ApiProperty> getAssayPropertiesByProperty(final String propertyName) {
        return transform(assayDAO.getAssayPropertiesByProperty(propertyName), ASSAYPROPERTY_APIPROPERTY);

    }

    /**
     * @param propertyName
     * @return List of ApiSample's containing propertyName-propertyValue
     */
    public Collection<ApiProperty> getSamplePropertiesByProperty(final String propertyName) {
        return transform(sampleDAO.getSamplePropertiesByProperty(propertyName), SAMPLEPROPERTY_APIPROPERTY);

    }

    /**
     * @param propertyName
     * @param propertyValue
     * @return List of ApiAssay's containing propertyName-propertyValue
     */
    public Collection<ApiAssay> getAssaysByPropertyValue(final String propertyName, final String propertyValue) {
        return transform(assayDAO.getAssaysByPropertyValue(propertyName, propertyValue), ASSAY);

    }

    /**
     * @param propertyName
     * @param propertyValue
     * @return List of ApiSample's containing propertyName-propertyValue
     */
    public Collection<ApiSample> getSamplesByPropertyValue(final String propertyName, final String propertyValue) {
        return transform(sampleDAO.getSamplesByPropertyValue(propertyName, propertyValue), SAMPLE);

    }

    /**
     * @param ontologyTerm
     * @return List of ApiAssay's containing a property value mapped to  ontologyTerm
     */
    public Collection<ApiAssay> getAssaysByOntologyTerm(final String ontologyTerm) {
        return transform(assayDAO.getAssaysByOntologyTerm(ontologyTerm), ASSAY);

    }

    /**
     * @param ontologyTerm
     * @return List of ApiSample's containing  a property value mapped to  ontologyTerm
     */
    public Collection<ApiSample> getSamplesByOntologyTerm(final String ontologyTerm) {
        return transform(sampleDAO.getSamplesByOntologyTerm(ontologyTerm), SAMPLE);

    }

    /**
     * @param assayAccession
     * @return List of ApiExperiments's containing assay assayAccession
     */
    public Collection<ApiExperiment> getExperimentsByAssay(final String assayAccession) {
        return transform(experimentDAO.getExperimentsByAssay(assayAccession), EXPERIMENT);

    }

    /**
     * @param sampleAccession
     * @return List of ApiExperiments's containing sample sampleAccession
     */
    public Collection<ApiExperiment> getExperimentsBySample(final String sampleAccession) {
        return transform(experimentDAO.getExperimentsBySample(sampleAccession), EXPERIMENT);

    }

    /**
     * Remove propertyName:propertyValue from all assays and samples that are mapped to it (via FK cascading in Oracle) and remove propertyValue from
     * the list of values assigned to propertyName
     *
     * @param propertyName
     * @param propertyValue
     * @throws ResourceNotFoundException
     */
    @Transactional
    public void removePropertyValue(final String propertyName,
                                    final String propertyValue) throws ResourceNotFoundException {
        try {
            Property property = propertyDAO.getByName(propertyName);
            PropertyValue propValue = propertyValueDAO.find(property, propertyValue);
            propertyDAO.delete(property, propValue);
        } catch (RecordNotFoundException e) {
            throw convert(e);
        }
    }

    /**
     * Remove all AssayProperties from assays which have property values for property: propertyName; if propertyValue is not null,
     * remove only those AssayProperties for propertyName that match propertyValue.
     *
     * @param propertyName
     * @param propertyValue
     */
    @Transactional
    public void removePropertyFromAssays(
            @Nonnull final String propertyName, @Nullable final String propertyValue) {
        for (Assay assay : assayDAO.getAssaysByProperty(propertyName)) {
            for (PropertyValue propValue : Lists.newArrayList(transform(assay.getProperties(propertyName, propertyValue), ASSAY_PROPERTY)))
                assay.deleteProperty(propValue);
            assayDAO.save(assay);
        }
    }

    /**
     * Remove all SampleProperties from assays which have property values for property: propertyName; if propertyValue is not null,
     * remove only those SampleProperties for propertyName that match propertyValue.
     *
     * @param propertyName
     * @param propertyValue
     */
    @Transactional
    public void removePropertyFromSamples(
            @Nonnull final String propertyName, @Nullable final String propertyValue) {
        for (Sample sample : sampleDAO.getSamplesByProperty(propertyName)) {
            for (PropertyValue propValue : Lists.newArrayList(transform(sample.getProperties(propertyName, propertyValue), SAMPLE_PROPERTY)))
                sample.deleteProperty(propValue);
            sampleDAO.save(sample);
        }
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
        try {
            Property property = propertyDAO.getByName(propertyName);
            PropertyValue oldPropertyValue = propertyValueDAO.find(property, oldValue);
            PropertyValue newPropertyValue = propertyValueDAO.getOrCreatePropertyValue(property, newValue);

            List<Assay> assays = assayDAO.getAssaysByPropertyValue(propertyName, oldValue);
            for (Assay assay : assays) {
                AssayProperty oldAssayProperty = assay.getProperty(oldPropertyValue);
                AssayProperty newAssayProperty = assay.getProperty(newPropertyValue);
                List<OntologyTerm> terms = new ArrayList<OntologyTerm>(oldAssayProperty.getTerms());
                assay.deleteProperty(oldPropertyValue);
                if (newAssayProperty != null) {
                    terms.addAll(newAssayProperty.getTerms());
                }
                assay.addOrUpdateProperty(newPropertyValue, terms);
                assayDAO.save(assay);
            }
        } catch (RecordNotFoundException e) {
            throw convert(e);
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
        try {
            Property property = propertyDAO.getByName(propertyName);
            PropertyValue oldPropertyValue = propertyValueDAO.find(property, oldValue);
            PropertyValue newPropertyValue = propertyValueDAO.getOrCreatePropertyValue(property, newValue);

            List<Sample> samples = sampleDAO.getSamplesByPropertyValue(propertyName, oldValue);
            for (Sample sample : samples) {
                SampleProperty oldSampleProperty = sample.getProperty(oldPropertyValue);
                SampleProperty newSampleProperty = sample.getProperty(newPropertyValue);
                List<OntologyTerm> terms = new ArrayList<OntologyTerm>(oldSampleProperty.getTerms());
                sample.deleteProperty(oldPropertyValue);
                if (newSampleProperty != null) {
                    terms.addAll(newSampleProperty.getTerms());
                }
                sample.addOrUpdateProperty(newPropertyValue, terms);
                sampleDAO.save(sample);
            }
        } catch (RecordNotFoundException e) {
            throw convert(e);
        }
    }

    /**
     * @param experimentAccession
     * @return ApiExperiment corresponding to experimentAccession
     * @throws ResourceNotFoundException if experiment not found
     */
    public ApiExperiment getExperiment(final String experimentAccession)
            throws ResourceNotFoundException {

        try {
            final Experiment experiment = atlasDAO.getExperimentByAccession(experimentAccession);

            return new ApiExperiment(experiment);
        } catch (RecordNotFoundException e) {
            throw convert(e);
        }
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
            PropertyValue propertyValue = getOrCreatePropertyValue(apiAssayProperty.getPropertyValue());

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
            PropertyValue propertyValue = getOrCreatePropertyValue(apiProperty.getPropertyValue());

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
            PropertyValue propertyValue = getOrCreatePropertyValue(apiSampleProperty.getPropertyValue());

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
            PropertyValue propertyValue = getOrCreatePropertyValue(apiSampleProperty.getPropertyValue());

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
        try {
            Ontology ontology = ontologyDAO.getByName(ontologyName);
            return new ApiOntology(ontology);
        } catch (RecordNotFoundException e) {
            throw convert(e);
        }
    }

    /**
     * Adds or updates details for Ontology corresponding to apiOntology
     *
     * @param apiOntology
     */
    @Transactional
    public void putOntology(@Nonnull final ApiOntology apiOntology) {
        try {
            Ontology ontology = ontologyDAO.getByName(apiOntology.getName());
            ontology.setDescription(apiOntology.getDescription());
            ontology.setName(apiOntology.getName());
            ontology.setVersion(apiOntology.getVersion());
            ontology.setSourceUri(apiOntology.getSourceUri());
            ontologyDAO.save(ontology);
        } catch (RecordNotFoundException e) { // ontology not found - create a new one
            getOrCreateOntology(apiOntology);
        }
    }

    /**
     * @param ontologyTermAcc
     * @return ApiOntologyTerm corresponding to ontologyTerm
     * @throws ResourceNotFoundException if ontology term: ontologyTerm was not found
     */
    public ApiOntologyTerm getOntologyTerm(final String ontologyTermAcc) throws ResourceNotFoundException {

        try {
            OntologyTerm ontologyTerm = ontologyTermDAO.getByName(ontologyTermAcc);
            return new ApiOntologyTerm(ontologyTerm);
        } catch (RecordNotFoundException e) {
            throw convert(e);
        }
    }

    /**
     * Add (or update mappings to Ontology for) apiOntologyTerms
     *
     * @param apiOntologyTerms
     */
    @Transactional
    public void putOntologyTerms(final ApiOntologyTerm[] apiOntologyTerms) {
        for (ApiOntologyTerm apiOntologyTerm : apiOntologyTerms) {
            try {
                OntologyTerm ontologyTerm = ontologyTermDAO.getByName(apiOntologyTerm.getAccession());
                ontologyTerm.setAccession(apiOntologyTerm.getAccession());
                ontologyTerm.setDescription(apiOntologyTerm.getDescription());
                ontologyTerm.setOntology(getOrCreateOntology(apiOntologyTerm.getOntology()));
                ontologyTerm.setTerm(apiOntologyTerm.getTerm());
                ontologyTermDAO.save(ontologyTerm);
            } catch (RecordNotFoundException e) {
                // ontology term not found - create a new one
                getOrCreateOntologyTerm(apiOntologyTerm);
            }
        }
    }


    /**
     * @param apiOntology
     * @return existing Ontology corresponding to apiOntology.getName(); otherwise a new Ontology corresponding to apiOntology
     */
    private Ontology getOrCreateOntology(@Nonnull ApiOntology apiOntology) {
        return ontologyDAO.getOrCreateOntology(
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

        Ontology ontology = getOrCreateOntology(apiOntologyTerm.getOntology());

        return ontologyTermDAO.getOrCreateOntologyTerm(
                apiOntologyTerm.getAccession(),
                apiOntologyTerm.getTerm(),
                apiOntologyTerm.getDescription(),
                ontology);

    }


    /**
     * @param experimentAccession
     * @param assayAccession
     * @return Assay corresponding to assayAccession in experiment: experimentAccession
     * @throws ResourceNotFoundException if experiment: experimentAccession or assay: assayAccession
     *                                   in that experiment are not found
     */
    private Assay findAssay(final String experimentAccession, final String assayAccession) throws ResourceNotFoundException {
        try {
            final Experiment experiment = atlasDAO.getExperimentByAccession(experimentAccession);
            return experiment.getAssay(assayAccession);
        } catch (RecordNotFoundException e) {
            throw convert(e);
        }
    }

    /**
     * @param experimentAccession
     * @param sampleAccession
     * @return Sample corresponding to sampleAccession in experiment: experimentAccession
     * @throws ResourceNotFoundException if experiment: experimentAccession or sample: sampleAccession
     *                                   in that experiment are not found
     */
    private Sample findSample(final String experimentAccession, final String sampleAccession) throws ResourceNotFoundException {
        try {
            final Experiment experiment = atlasDAO.getExperimentByAccession(experimentAccession);
            return experiment.getSample(sampleAccession);
        } catch (RecordNotFoundException e) {
            throw convert(e);
        }
    }

    private PropertyValue getOrCreatePropertyValue(ApiPropertyValue apv) {
        return propertyValueDAO.getOrCreatePropertyValue(apv.getProperty().getName(), apv.getValue());
    }

    private static ResourceNotFoundException convert(RecordNotFoundException e) {
        return new ResourceNotFoundException(e.getMessage(), e);
    }
}
