package uk.ac.ebi.gxa.service;

import com.google.common.base.Function;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ebi.gxa.dao.*;
import uk.ac.ebi.gxa.dao.exceptions.RecordNotFoundException;
import uk.ac.ebi.gxa.exceptions.ResourceNotFoundException;
import uk.ac.ebi.gxa.utils.Pair;
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
    final private Logger log = LoggerFactory.getLogger(this.getClass());
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
                public ApiPropertyName apply(Property p) {
                    return new ApiPropertyName(p);
                }
            };

    private static final Function<PropertyValue, ApiPropertyValue> PROPERTY_VALUE =
            new Function<PropertyValue, ApiPropertyValue>() {
                public ApiPropertyValue apply(PropertyValue pv) {
                    return new ApiPropertyValue(pv);
                }
            };

    private static final Function<Experiment, ApiShallowExperiment> EXPERIMENT =
            new Function<Experiment, ApiShallowExperiment>() {
                public ApiShallowExperiment apply(Experiment e) {
                    return new ApiShallowExperiment(e);
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
     * @param propertyName - String
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
            throw new ResourceNotFoundException(e);
        }
    }

    /**
     * @return alphabetically sorted collection of property names not used in any assays/samples
     */
    public Collection<ApiPropertyName> getUnusedPropertyNames() {
        List<ApiPropertyName> propertyNames = Lists.newArrayList(transform(propertyDAO.getUnusedProperties(), PROPERTY_NAME));

        Collections.sort(propertyNames, new Comparator<ApiPropertyName>() {
            public int compare(ApiPropertyName o1, ApiPropertyName o2) {
                return o1.getName().compareToIgnoreCase(o2.getName());
            }
        });

        return propertyNames;
    }

    /**
     * @return alphabetically sorted collection of property names not used in any assays/samples
     */
    public Collection<ApiPropertyValue> getUnusedPropertyValues() {
        List<ApiPropertyValue> propertyValues = Lists.newArrayList(transform(propertyValueDAO.getUnusedPropertyValues(), PROPERTY_VALUE));

        Collections.sort(propertyValues, new Comparator<ApiPropertyValue>() {
            public int compare(ApiPropertyValue o1, ApiPropertyValue o2) {
                return o1.getValue().compareToIgnoreCase(o2.getValue());
            }
        });

        return propertyValues;
    }

    /**
     * Remove all property names not used in any assays/samples
     */
    @Transactional
    public void removeUnusedPropertyNames() {
        propertyDAO.removeUnusedProperties();
    }

    /**
     * Remove property values not used in any assays/samples
     */
    @Transactional
    public void removeUnusedPropertyValues() {
        propertyValueDAO.removeUnusedPropertyValues();
    }

    /**
     * @param propertyName  String
     * @param propertyValue String
     * @return Collection of ApiShallowExperiment's containing propertyName-propertyValue pairs
     */
    public Collection<ApiShallowExperiment> getExperimentsByPropertyValue(final String propertyName, final String propertyValue) {
        HashSet<Experiment> experiments = new LinkedHashSet<Experiment>();
        experiments.addAll(experimentDAO.getExperimentsByAssayPropertyValue(propertyName, propertyValue));
        experiments.addAll(experimentDAO.getExperimentsBySamplePropertyValue(propertyName, propertyValue));
        return transform(experiments, EXPERIMENT);

    }

    /**
     * @param propertyName String
     * @param exactMatch   boolean, if true, only experiments with assays/samples containing a property matching propertyName exactly will be considered;
     *                     otherwise all experiments with assays/samples containing a property of which propertyName is a substring will be considered.
     * @return List of ApiShallowProperty's containing propertyName-propertyValue
     */
    public Collection<ApiShallowProperty> getOntologyMappingsByProperty(final String propertyName, boolean exactMatch) {
        boolean caseInsensitive = true;
        ApiPropertyValueMatcher propertyValueMatcher = new ApiPropertyValueMatcher().setExactMatch(exactMatch)
                .setNameMatcher(propertyName);

        List<AssayProperty> assayProperties = assayDAO.getAssayPropertiesByProperty(propertyName, exactMatch);
        for (AssayProperty assayProperty : assayProperties) {
            propertyValueMatcher.add(new ApiProperty(assayProperty.getPropertyValue(), assayProperty.getTerms()));
        }

        List<SampleProperty> sampleProperties = sampleDAO.getSamplePropertiesByProperty(propertyName, exactMatch);
        for (SampleProperty sampleProperty : sampleProperties) {
            propertyValueMatcher.add(new ApiProperty(sampleProperty.getPropertyValue(), sampleProperty.getTerms()));
        }

        return propertyValueMatcher.getMatchingProperties();
    }

    /**
     * @param propertyName  String
     * @param propertyValue String
     * @param exactMatch    boolean, if true, only experiments with assays/samples containing a property value matching propertyValue exactly will be considered;
     *                      otherwise all experiments with assays/samples containing a property value of which propertyValue is a substring will be considered.
     * @return List of ApiShallowProperty's containing propertyName-propertyValue
     */
    public Collection<ApiShallowProperty> getOntologyMappingsByPropertyValue(final String propertyName, @Nonnull final String propertyValue, boolean exactMatch) {
        ApiPropertyValueMatcher propertyValueMatcher = new ApiPropertyValueMatcher().setExactMatch(exactMatch)
                .setValueMatcher(propertyValue)
                .setNameMatcher(propertyName);
        boolean caseInsensitive = true;
        List<AssayProperty> assayProperties = assayDAO.getAssayPropertiesByPropertyValue(propertyName, propertyValue, exactMatch);
        for (AssayProperty assayProperty : assayProperties) {
            propertyValueMatcher.add(new ApiProperty(assayProperty.getPropertyValue(), assayProperty.getTerms()));
        }
        List<SampleProperty> sampleProperties = sampleDAO.getSamplePropertiesByPropertyValue(propertyName, propertyValue, exactMatch);
        for (SampleProperty sampleProperty : sampleProperties) {
            propertyValueMatcher.add(new ApiProperty(sampleProperty.getPropertyValue(), sampleProperty.getTerms()));
        }

        return propertyValueMatcher.getMatchingProperties();
    }


    /**
     * @return List of ApiShallowProperty's containing propertyName-propertyValue
     */
    public Collection<ApiShallowProperty> getOntologyMappingsByOntologyTerm(@Nonnull final String ontologyTerm) {
        ApiPropertyValueMatcher propertyValueMatcher = new ApiPropertyValueMatcher().setExactMatch(false);
        for (AssayProperty assayProperty : assayDAO.getAssayPropertiesByOntologyTerm(ontologyTerm)) {
            propertyValueMatcher.add(new ApiProperty(assayProperty.getPropertyValue(), assayProperty.getTerms()));
        }
        for (SampleProperty sampleProperty : sampleDAO.getSamplePropertiesByOntologyTerm(ontologyTerm)) {
            propertyValueMatcher.add(new ApiProperty(sampleProperty.getPropertyValue(), sampleProperty.getTerms()));
        }

        return propertyValueMatcher.getMatchingProperties();
    }

    /**
     * @param ontologyTerm String
     * @return List of ApiExperiment's containing a property value mapped to  ontologyTerm
     */
    public Collection<ApiShallowExperiment> getExperimentsByOntologyTerm(final String ontologyTerm) {
        HashSet<Experiment> experiments = new LinkedHashSet<Experiment>();
        experiments.addAll(experimentDAO.getExperimentsByAssayPropertyOntologyTerm(ontologyTerm));
        experiments.addAll(experimentDAO.getExperimentsBySamplePropertyOntologyTerm(ontologyTerm));
        return transform(experiments, EXPERIMENT);
    }

    /**
     * Delete property from Property table (thus deleting all its values from PropertyValue? table and all assays/samples they occur in)
     *
     * @param propertyName String
     * @throws ResourceNotFoundException
     */
    @Transactional
    private void deleteProperty(final String propertyName) throws ResourceNotFoundException {
        try {
            Property property = propertyDAO.getByName(propertyName);
            propertyDAO.delete(property);
        } catch (RecordNotFoundException e) {
            throw new ResourceNotFoundException(e);
        }
    }

    /**
     * If propertyValue is not null and non-empty, delete propertyName:propertyValue from PropertyValue table (thus deleting it from all assays/samples it occurs in);
     * otherwise remove propertyName (with all its values)
     *
     * @param propertyName  String
     * @param propertyValue String
     * @throws ResourceNotFoundException
     */
    @Transactional
    public void deletePropertyOrValue(@Nonnull final String propertyName,
                                      @Nullable final String propertyValue) throws ResourceNotFoundException {
        try {

            if (Strings.isNullOrEmpty(propertyValue))
                if (!propertyDAO.isPropertyUsed(propertyName))
                    deleteProperty(propertyName);
                else
                    log.warn("Not removing property: " + propertyName + " as still used in assays or samples");
            else {
                if (!propertyValueDAO.isPropertyValueUsed(propertyName, propertyValue)) {
                    Property property = propertyDAO.getByName(propertyName);
                    PropertyValue propValue = propertyValueDAO.find(property, propertyValue);
                    propertyDAO.delete(property, propValue);
                } else {
                    log.warn("Not removing property: " + propertyName +
                            " and propertyValue: " + propertyValue + " as still used in assays or samples");
                }
            }
        } catch (RecordNotFoundException e) {
            throw new ResourceNotFoundException(e);
        }
    }

    /**
     * Replaces oldValue of property: propertyName with newValue in all assays/samples in which propertyName-oldValue exists.
     * In cases when a given assay/sample contains both oldValue and newValue, the retained newValue gets mapped to the superset of OntologyTerms
     * assigned to oldValue and newValue.
     *
     * @param propertyName String
     * @param oldValue     String
     * @param newValue     String
     * @throws ResourceNotFoundException if property: propertyName and/or its value: oldValue don't exist
     */
    @Transactional
    public void replacePropertyValueInExperiments(
            final String propertyName,
            final String oldValue,
            final String newValue)
            throws ResourceNotFoundException {
        replacePropertyValueInAssays(propertyName, oldValue, newValue);
        replacePropertyValueInSamples(propertyName, oldValue, newValue);
    }

    /**
     * Replaces oldPropertyName with newPropertyName in all assays/samples in which value(s) of oldPropertyName exist.
     * In cases when a given assay/sample contains values for both oldPropertyName and newPropertyName, values corresponding to the retained
     * newPropertyName get mapped to the superset of OntologyTerms assigned to values for newPropertyName and oldPropertyName.
     *
     * @param oldPropertyName String
     * @param newPropertyName String
     */
    @Transactional
    public void replacePropertyInExperiments(
            final String oldPropertyName,
            final String newPropertyName) {
        replacePropertyInAssays(oldPropertyName, newPropertyName);
        replacePropertyInSamples(oldPropertyName, newPropertyName);
    }

    /**
     * Replaces oldValue of property: propertyName with newValue in all assays in which propertyName-oldValue exists.
     * In cases when a given assay contains both oldValue and newValue, oldValue assay property is simply deleted
     *
     * @param propertyName String
     * @param oldValue     String
     * @param newValue     String
     * @throws ResourceNotFoundException if property: propertyName and/or its value: oldValue don't exist
     */
    @Transactional
    protected void replacePropertyValueInAssays(
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
                AssayProperty newAssayProperty = assay.getProperty(newPropertyValue);
                assay.deleteProperty(oldPropertyValue);
                if (newAssayProperty == null) {
                    // Note that since we are eliminating oldValue we don't preserve its old ontology mappings
                    // as they are likely to be either non-existent or simply wrong
                    assay.addOrUpdateProperty(newPropertyValue, Collections.<OntologyTerm>emptyList());
                }
                assayDAO.save(assay);
            }
        } catch (RecordNotFoundException e) {
            throw new ResourceNotFoundException(e);
        }
    }

    /**
     * Replaces oldValue of property: propertyName with newValue in all samples in which propertyName-oldValue exists.
     * In cases when a given assay contains both oldValue and newValue, oldValue sample property is simply deleted
     *
     * @param propertyName String
     * @param oldValue     String
     * @param newValue     String
     * @throws ResourceNotFoundException if property: propertyName and/or its value: oldValue don't exist
     */
    @Transactional
    protected void replacePropertyValueInSamples(
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
                SampleProperty newSampleProperty = sample.getProperty(newPropertyValue);
                sample.deleteProperty(oldPropertyValue);
                if (newSampleProperty == null) {
                    // Note that since we are eliminating oldValue we don't preserve its old ontology mappings
                    // as they are likely to be either non-existent or simply wrong
                    sample.addOrUpdateProperty(newPropertyValue, Collections.<OntologyTerm>emptyList());
                }
                sampleDAO.save(sample);
            }
        } catch (RecordNotFoundException e) {
            throw new ResourceNotFoundException(e);
        }
    }

    /**
     * Replaces oldPropertyName with newPropertyName in all assays in which value(s) of oldPropertyName exist.
     * In cases when a given assay contains values for both oldPropertyName and newPropertyName, values corresponding to the retained
     * newPropertyName get mapped to the superset of OntologyTerms assigned to values for newPropertyName and oldPropertyName.
     *
     * @param oldPropertyName String
     * @param newPropertyName String
     */
    @Transactional
    protected void replacePropertyInAssays(
            final String oldPropertyName,
            final String newPropertyName) {
        Property newProperty = propertyDAO.getOrCreateProperty(newPropertyName);
        List<Assay> assays = assayDAO.getAssaysByProperty(oldPropertyName);
        List<Pair<PropertyValue, List<OntologyTerm>>> pvsToAdd;
        List<PropertyValue> pvsToDelete;

        for (Assay assay : assays) {
            pvsToAdd = new ArrayList<Pair<PropertyValue, List<OntologyTerm>>>();
            pvsToDelete = new ArrayList<PropertyValue>();
            for (AssayProperty oldAssayProperty : assay.getProperties()) {
                if (oldAssayProperty.getName().equals(oldPropertyName)) {

                    PropertyValue newPropertyValue =
                            propertyValueDAO.getOrCreatePropertyValue(newProperty, oldAssayProperty.getPropertyValue().getValue());
                    if (assay.getProperty(newPropertyValue) == null) {
                        // If newPropertyName:newPropertyValue don't exist in this assay, add it together with ontology mappings
                        // assigned to oldAssayProperty.getPropertyValue().getValue()
                        pvsToAdd.add(Pair.create(newPropertyValue, oldAssayProperty.getTerms()));
                    }
                    pvsToDelete.add(oldAssayProperty.getPropertyValue());
                }
            }
            for (PropertyValue pv : pvsToDelete)
                assay.deleteProperty(pv);
            for (Pair<PropertyValue, List<OntologyTerm>> pvToTerms : pvsToAdd)
                assay.addOrUpdateProperty(pvToTerms.getKey(), pvToTerms.getValue());
            assayDAO.save(assay);
        }
    }

    /**
     * Replaces oldPropertyName with newPropertyName in all samples in which value(s) of oldPropertyName exist.
     * In cases when a given sample contains values for both oldPropertyName and newPropertyName, values corresponding to the retained
     * newPropertyName get mapped to the superset of OntologyTerms assigned to values for newPropertyName and oldPropertyName.
     *
     * @param oldPropertyName String
     * @param newPropertyName String
     */
    @Transactional
    protected void replacePropertyInSamples(
            final String oldPropertyName,
            final String newPropertyName) {
        Property newProperty = propertyDAO.getOrCreateProperty(newPropertyName);
        List<Sample> samples = sampleDAO.getSamplesByProperty(oldPropertyName);
        List<Pair<PropertyValue, List<OntologyTerm>>> pvsToAdd;
        List<PropertyValue> pvsToDelete;

        for (Sample sample : samples) {
            pvsToAdd = new ArrayList<Pair<PropertyValue, List<OntologyTerm>>>();
            pvsToDelete = new ArrayList<PropertyValue>();
            for (SampleProperty oldSampleProperty : sample.getProperties()) {
                if (oldSampleProperty.getName().equals(oldPropertyName)) {

                    PropertyValue newPropertyValue =
                            propertyValueDAO.getOrCreatePropertyValue(newProperty, oldSampleProperty.getPropertyValue().getValue());
                    if (sample.getProperty(newPropertyValue) == null) {
                        // If newPropertyName:newPropertyValue don't exist in this sample, add it together with ontology mappings
                        // assigned to oldSampleProperty.getPropertyValue().getValue()
                        pvsToAdd.add(Pair.create(newPropertyValue, oldSampleProperty.getTerms()));
                    }
                    pvsToDelete.add(oldSampleProperty.getPropertyValue());
                }
            }
            for (PropertyValue pv : pvsToDelete)
                sample.deleteProperty(pv);
            for (Pair<PropertyValue, List<OntologyTerm>> pvToTerms : pvsToAdd)
                sample.addOrUpdateProperty(pvToTerms.getKey(), pvToTerms.getValue());
            sampleDAO.save(sample);
        }
    }

    /**
     * @param experimentAccession String
     * @return ApiShallowExperiment corresponding to experimentAccession
     * @throws ResourceNotFoundException if experiment not found
     */
    public ApiShallowExperiment getExperiment(final String experimentAccession)
            throws ResourceNotFoundException {

        try {
            final Experiment experiment = atlasDAO.getExperimentByAccession(experimentAccession);

            return new ApiShallowExperiment(experiment);
        } catch (RecordNotFoundException e) {
            throw new ResourceNotFoundException(e);
        }
    }

    /**
     * @param experimentAccession String
     * @param assayAccession      String
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
     * @param experimentAccession String
     * @param assayAccession      String
     * @param assayProperties     ApiProperty[]
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
     * Adds or updates mapping to efo terms for matching assayProperties to all assays in experiment: experimentAccession
     *
     * @param experimentAccession String
     * @param apiProperties          ApiProperty[]
     * @throws ResourceNotFoundException if experiment: experimentAccession in that experiment are not found
     */
    @Transactional
    public void remapTermsOnMatchingAssayProperties(String experimentAccession, ApiProperty[] apiProperties) throws ResourceNotFoundException {
        try {
            Experiment experiment = atlasDAO.getExperimentByAccession(experimentAccession);
            for (ApiProperty apiProperty : apiProperties) {
                for (Assay assay : experiment.getAssays()) {

                    final Collection<AssayProperty> assayProperties = assay.getProperties(apiProperty.getName(), apiProperty.getValue());
                    remapTermsOnMatchingAssayProperties(assayProperties, apiProperty.getTerms());

                }
            }

        } catch (RecordNotFoundException e) {
            throw new ResourceNotFoundException(e);
        }

    }

    /**
     * Adds or updates mapping to efo terms for matching properties for all assays and all samples in all experiments
     *
     * @param properties ApiProperty[]
     * @throws ResourceNotFoundException if experiment: experimentAccession in that experiment are not found
     */
    @Transactional
    public void remapTermsOnMatchingPropertiesForAllExperiments(ApiProperty[] properties) throws ResourceNotFoundException {

        for (ApiProperty apiProperty : properties) {
              final List<AssayProperty> assayProperties = assayDAO.getAssayPropertiesByPropertyValue(apiProperty.getName(), apiProperty.getValue(), true);
              remapTermsOnMatchingAssayProperties(assayProperties, apiProperty.getTerms());

              final List<SampleProperty> sampleProperties = sampleDAO.getSamplePropertiesByPropertyValue(apiProperty.getName(), apiProperty.getValue(), true);
              remapTermsOnMatchingSampleProperties(sampleProperties, apiProperty.getTerms());
        }

    }


    private void remapTermsOnMatchingAssayProperties(Collection<AssayProperty> assayProperties, Collection<ApiOntologyTerm> apiTerms) {

        for (AssayProperty assayProperty : assayProperties) {

            List<OntologyTerm> ontologyTerms = new ArrayList<OntologyTerm>();

            for (ApiOntologyTerm apiTerm : apiTerms) {
                ontologyTerms.add(getOrCreateOntologyTerm(apiTerm));
            }

            assayProperty.setTerms(ontologyTerms);

            assayDAO.saveAssayProperty(assayProperty);
        }

    }


    /**
     * Removes assayProperties from assay: assayAccession in experiment: experimentAccession
     *
     * @param experimentAccession String
     * @param assayAccession      String
     * @param assayProperties     ApiProperty[]
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
     * Removes properties from all assays and all samples in all experiments
     *
     * @param properties ApiProperty[]
     * @throws ResourceNotFoundException if experiment: experimentAccession is not found
     */
    @Transactional
    public void deleteTermsFromMatchingPropertiesForAllExperiments(ApiProperty[] properties) throws ResourceNotFoundException {
        for (ApiProperty apiProperty : properties) {
            final List<AssayProperty> assayProperties = assayDAO.getAssayPropertiesByPropertyValue(apiProperty.getName(), apiProperty.getValue(), true);
            deleteTermsFromMatchingAssayProperties(assayProperties, apiProperty.getTerms());

            final List<SampleProperty> sampleProperties = sampleDAO.getSamplePropertiesByPropertyValue(apiProperty.getName(), apiProperty.getValue(), true);
            deleteTermsFromMatchingSampleProperties(sampleProperties, apiProperty.getTerms());
        }


    }

    /**
     * Removes terms from matching assayProperties for all assays in experiment: experimentAccession
     *
     * @param experimentAccession String
     * @param properties          ApiProperty[]
     * @throws ResourceNotFoundException if experiment: experimentAccession is not found
     */
    @Transactional
    public void deleteTermsFromMatchingPropertiesForAllAssays(String experimentAccession, ApiProperty[] properties) throws ResourceNotFoundException {
        try {
            Experiment experiment = atlasDAO.getExperimentByAccession(experimentAccession);

            for (final ApiProperty apiProperty : properties) {

                for (Assay assay : experiment.getAssays()) {

                    final Collection<AssayProperty> filteredProperites = assay.getProperties(apiProperty.getName(), apiProperty.getValue());
                    deleteTermsFromMatchingAssayProperties(filteredProperites, apiProperty.getTerms());
                }
            }

        } catch (RecordNotFoundException e) {
            throw new ResourceNotFoundException(e);
        }
    }

    /**
     * Deletes properties from all samples in experiment: experimentAccession
     *
     * @param experimentAccession String
     * @param properties          ApiProperty[]
     * @throws ResourceNotFoundException if experiment: experimentAccession or sample: sampleAccession
     *                                   in that experiment are not found
     */
    @Transactional
    public void deleteTermsFromMatchingPropertiesForAllSamples(String experimentAccession,
                                                               ApiProperty[] properties) throws ResourceNotFoundException {
        try {
            Experiment experiment = atlasDAO.getExperimentByAccession(experimentAccession);

            for (ApiProperty apiProperty : properties) {

                for (Sample sample : experiment.getSamples()) {
                    final Collection<SampleProperty> filteredProperties = sample.getProperties(apiProperty.getName(), apiProperty.getValue());
                    deleteTermsFromMatchingSampleProperties(filteredProperties, apiProperty.getTerms());
                }
            }

        } catch (RecordNotFoundException e) {
            throw new ResourceNotFoundException(e);
        }
    }

    private void deleteTermsFromMatchingSampleProperties(Collection<SampleProperty> sampleProperties, Set<ApiOntologyTerm> apiOntologyTerms) {

        for (SampleProperty sampleProperty : sampleProperties) {
            for (ApiOntologyTerm apiTerm : apiOntologyTerms) {

                OntologyTerm ontologyTerm = apiTerm.toOntologyTerm();

                sampleProperty.removeTerm(ontologyTerm);
                sampleDAO.saveSampleProperty(sampleProperty);
            }
        }

    }


    private void deleteTermsFromMatchingAssayProperties(Collection<AssayProperty> assayProperties, Set<ApiOntologyTerm> apiOntologyTerms) {

        for (AssayProperty assayProperty : assayProperties) {
            for (ApiOntologyTerm apiTerm : apiOntologyTerms) {

                OntologyTerm ontologyTerm = apiTerm.toOntologyTerm();

                assayProperty.removeTerm(ontologyTerm);
                assayDAO.saveAssayProperty(assayProperty);
            }
        }
    }


    /**
     * @param experimentAccession String
     * @param sampleAccession     String
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
     * @param experimentAccession String
     * @param sampleAccession     String
     * @param sampleProperties    ApiProperty[]
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
     * Adds (or updates mapping to efo terms for) properties for all samples in experiment: experimentAccession
     *
     * @param experimentAccession String
     * @param apiProperties          ApiProperty[]
     * @throws ResourceNotFoundException if experiment: experimentAccession or sample: sampleAccession
     *                                   in that experiment are not found
     */
    @Transactional
    public void remapTermsOnMatchingSampleProperties(String experimentAccession, ApiProperty[] apiProperties) throws ResourceNotFoundException {
        try {
            Experiment experiment = atlasDAO.getExperimentByAccession(experimentAccession);
            for (ApiProperty apiProperty : apiProperties) {
                for (Sample sample : experiment.getSamples()) {

                    final Collection<SampleProperty> sampleProperties = sample.getProperties(apiProperty.getName(), apiProperty.getValue());
                    remapTermsOnMatchingSampleProperties(sampleProperties, apiProperty.getTerms());

                }
            }

        } catch (RecordNotFoundException e) {
            throw new ResourceNotFoundException(e);
        }
    }

    private void remapTermsOnMatchingSampleProperties(Collection<SampleProperty> sampleProperties, Collection<ApiOntologyTerm> apiTerms) {

        for (SampleProperty sampleProperty : sampleProperties) {

            List<OntologyTerm> ontologyTerms = new ArrayList<OntologyTerm>();

            for (ApiOntologyTerm apiTerm : apiTerms) {
                ontologyTerms.add(getOrCreateOntologyTerm(apiTerm));
            }

            sampleProperty.setTerms(ontologyTerms);

            sampleDAO.saveSampleProperty(sampleProperty);
        }
    }

    /**
     * Deletes sampleProperties from sample: sampleAccession in experiment: experimentAccession
     *
     * @param experimentAccession String
     * @param sampleAccession     String
     * @param sampleProperties    ApiProperty[]
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


    private void deleteTermsFromMatchingPropertiesForAllSamples(Experiment experiment, ApiProperty[] apiProperties) {

        for (Sample sample : experiment.getSamples()) {

            if (deleteTermsFromMatchingProperties(sample, apiProperties)) {

                sampleDAO.save(sample);

            }

        }

    }


    private boolean deleteTermsFromMatchingProperties(Sample sample, ApiProperty[] apiProperties) {

        boolean hasChanged = false;

        for (ApiProperty apiProperty : apiProperties) {

            PropertyValue propertyValue = getOrCreatePropertyValue(apiProperty.getPropertyValue());

            SampleProperty property = sample.getProperty(propertyValue);

            if (property != null) {

                for (ApiOntologyTerm apiTerm : apiProperty.getTerms()) {
                    OntologyTerm ontologyTerm = getOrCreateOntologyTerm(apiTerm);

                    property.removeTerm(ontologyTerm);

                    hasChanged = true;
                }

            }
        }

        return hasChanged;

    }


    /**
     * Add (or update mappings to Ontology for) apiOntologyTerms
     *
     * @param apiOntologyTerms ApiOntologyTerm[]
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
     * @param apiOntology ApiOntology
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
     * @param apiOntologyTerm ApiOntologyTerm
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
     * @param experimentAccession String
     * @param assayAccession      String
     * @return Assay corresponding to assayAccession in experiment: experimentAccession
     * @throws ResourceNotFoundException if experiment: experimentAccession or assay: assayAccession
     *                                   in that experiment are not found
     */
    private Assay findAssay(final String experimentAccession, final String assayAccession) throws ResourceNotFoundException {
        try {
            final Experiment experiment = atlasDAO.getExperimentByAccession(experimentAccession);
            return experiment.getAssay(assayAccession);
        } catch (RecordNotFoundException e) {
            throw new ResourceNotFoundException(e);
        }
    }

    /**
     * @param experimentAccession String
     * @param sampleAccession     String
     * @return Sample corresponding to sampleAccession in experiment: experimentAccession
     * @throws ResourceNotFoundException if experiment: experimentAccession or sample: sampleAccession
     *                                   in that experiment are not found
     */
    private Sample findSample(final String experimentAccession, final String sampleAccession) throws ResourceNotFoundException {
        try {
            final Experiment experiment = atlasDAO.getExperimentByAccession(experimentAccession);
            return experiment.getSample(sampleAccession);
        } catch (RecordNotFoundException e) {
            throw new ResourceNotFoundException(e);
        }
    }

    private PropertyValue getOrCreatePropertyValue(ApiPropertyValue apv) {
        return propertyValueDAO.getOrCreatePropertyValue(apv.getProperty().getName(), apv.getValue());
    }

}
