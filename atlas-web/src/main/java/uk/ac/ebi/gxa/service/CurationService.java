package uk.ac.ebi.gxa.service;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
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

    private static final Function<Experiment, ApiShallowExperiment> EXPERIMENT =
            new Function<Experiment, ApiShallowExperiment>() {
                public ApiShallowExperiment apply(@Nonnull Experiment e) {
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
     * @param propertyName
     * @param propertyValue
     * @return List of ApiExperiment's containing propertyName-propertyValue
     */
    public Collection<ApiShallowExperiment> getExperimentsByPropertyValue(final String propertyName, final String propertyValue) {
        HashSet<Experiment> experiments = new LinkedHashSet<Experiment>();
        experiments.addAll(experimentDAO.getExperimentsByAssayPropertyValue(propertyName, propertyValue));
        experiments.addAll(experimentDAO.getExperimentsBySamplePropertyValue(propertyName, propertyValue));
        return transform(experiments, EXPERIMENT);

    }

    /**
     * @param propertyName
     * @param propertyValue
     * @param exactValueMatch if true, only experiments with assays/samples containing a property value matching propertyValue exactly will be considered;
     *                        otherwise all experiments with assays/samples containing a property value of which propertyValue is a substring will be considered.
     * @return List of ApiShallowProperty's containing propertyName-propertyValue
     */
    public Collection<ApiShallowProperty> getOntologyMappingsByPropertyValue(final String propertyName, @Nonnull final String propertyValue, boolean exactValueMatch) {
        boolean caseInsensitive = true;
        ApiPropertyValueMappings pvMappings = new ApiPropertyValueMappings(propertyName, propertyValue, caseInsensitive, exactValueMatch);
        for (AssayProperty assayProperty : assayDAO.getAssayPropertiesByPropertyValue(propertyName, propertyValue, exactValueMatch, caseInsensitive)) {
            pvMappings.add(new ApiProperty(assayProperty));
        }
        for (SampleProperty sampleProperty : sampleDAO.getAssayPropertiesByPropertyValue(propertyName, propertyValue, exactValueMatch, caseInsensitive)) {
            pvMappings.add(new ApiProperty(sampleProperty));
        }

        return pvMappings.getAll();
    }

    /**
     * @param ontologyTerm
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
     * @param propertyName
     * @throws ResourceNotFoundException
     */
    @Transactional
    public void deleteProperty(final String propertyName) throws ResourceNotFoundException {
        try {
            Property property = propertyDAO.getByName(propertyName);
            propertyDAO.delete(property);
        } catch (RecordNotFoundException e) {
            throw convert(e);
        }
    }

    /**
     * Delete property value from PropertyValue table (thus deleting it from all assays/samples it occurs in)
     *
     * @param propertyName
     * @param propertyValue
     * @throws ResourceNotFoundException
     */
    @Transactional
    public void deletePropertyValue(final String propertyName,
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
     * Replaces oldValue of property: propertyName with newValue in all assays/samples in which propertyName-oldValue exists.
     * In cases when a given assay/sample contains both oldValue and newValue, the retained newValue gets mapped to the superset of OntologyTerms
     * assigned to oldValue and newValue.
     *
     * @param propertyName
     * @param oldValue
     * @param newValue
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
     * @param oldPropertyName
     * @param newPropertyName
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
     * In cases when a given assay contains both oldValue and newValue, the retained newValue gets mapped to the superset of OntologyTerms
     * assigned to oldValue and newValue.
     *
     * @param propertyName
     * @param oldValue
     * @param newValue
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
     * Replaces oldPropertyName with newPropertyName in all assays in which value(s) of oldPropertyName exist.
     * In cases when a given assay contains values for both oldPropertyName and newPropertyName, values corresponding to the retained
     * newPropertyName get mapped to the superset of OntologyTerms assigned to values for newPropertyName and oldPropertyName.
     *
     * @param oldPropertyName
     * @param newPropertyName
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

                    // Collate ontology terms into newAssayProperty
                    List<OntologyTerm> terms = new ArrayList<OntologyTerm>(oldAssayProperty.getTerms());
                    PropertyValue newPropertyValue =
                            propertyValueDAO.getOrCreatePropertyValue(newProperty, oldAssayProperty.getPropertyValue().getValue());
                    AssayProperty newAssayProperty = assay.getProperty(newPropertyValue);
                    if (newAssayProperty != null)
                        terms.addAll(newAssayProperty.getTerms());

                    pvsToAdd.add(Pair.create(newPropertyValue, terms));
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
     * @param oldPropertyName
     * @param newPropertyName
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

                    // Collate ontology terms into newSampleProperty
                    List<OntologyTerm> terms = new ArrayList<OntologyTerm>(oldSampleProperty.getTerms());
                    PropertyValue newPropertyValue =
                            propertyValueDAO.getOrCreatePropertyValue(newProperty, oldSampleProperty.getPropertyValue().getValue());
                    SampleProperty newSampleProperty = sample.getProperty(newPropertyValue);
                    if (newSampleProperty != null)
                        terms.addAll(newSampleProperty.getTerms());

                    pvsToAdd.add(Pair.create(newPropertyValue, terms));
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
     * @param experimentAccession
     * @return ApiShallowExperiment corresponding to experimentAccession
     * @throws ResourceNotFoundException if experiment not found
     */
    public ApiShallowExperiment getExperiment(final String experimentAccession)
            throws ResourceNotFoundException {

        try {
            final Experiment experiment = atlasDAO.getExperimentByAccession(experimentAccession);

            return new ApiShallowExperiment(experiment);
        } catch (RecordNotFoundException e) {
            throw convert(e);
        }
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
