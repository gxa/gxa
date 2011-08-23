package uk.ac.ebi.gxa.service;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ebi.gxa.dao.*;
import uk.ac.ebi.gxa.dao.hibernate.DAOException;
import uk.ac.ebi.gxa.exceptions.ResourceNotFoundException;
import uk.ac.ebi.microarray.atlas.api.*;
import uk.ac.ebi.microarray.atlas.model.*;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

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
        try {
            Property property = propertyDAO.getByName(propertyName);
            List<ApiPropertyValue> propertyValues = Lists.newArrayList(transform(property.getValues(), PROPERTY_VALUE));

            Collections.sort(propertyValues, new Comparator<ApiPropertyValue>() {
                public int compare(ApiPropertyValue o1, ApiPropertyValue o2) {
                    return o1.getValue().compareToIgnoreCase(o2.getValue());
                }
            });

            return propertyValues;
        } catch (DAOException e) {
            throw new ResourceNotFoundException(e.getMessage(), e);
        }
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
        } catch (DAOException e) {
            throw new ResourceNotFoundException(e.getMessage(), e);
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
        } catch (DAOException e) {
            throw new ResourceNotFoundException(e.getMessage(), e);
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
        } catch (DAOException e) {
            throw new ResourceNotFoundException(e.getMessage(), e);
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
        } catch (DAOException e) {
            throw new ResourceNotFoundException(e.getMessage(), e);
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
            Ontology ontology = atlasDAO.getOntologyByName(ontologyName);
            return new ApiOntology(ontology);
        } catch (DAOException e) {
            throw new ResourceNotFoundException(e.getMessage(), e);
        }
    }

    /**
     * Adds or updates details for Ontology corresponding to apiOntology
     *
     * @param apiOntology
     */
    @Transactional
    public void putOntology(@Nonnull final ApiOntology apiOntology) {

        Ontology ontology = null;
        try {
            ontology = atlasDAO.getOntologyByName(apiOntology.getName());
        } catch (DAOException e) {
            // Do nothing - valid situation
        }

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

        try {
            OntologyTerm ontologyTerm = atlasDAO.getOntologyTermByAccession(ontologyTermAcc);
            return new ApiOntologyTerm(ontologyTerm);
        } catch (DAOException e) {
            throw new ResourceNotFoundException(e.getMessage(), e);
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
            OntologyTerm ontologyTerm = null;
            try {
                ontologyTerm = atlasDAO.getOntologyTermByAccession(apiOntologyTerm.getAccession());
            } catch (DAOException e) {
                // do nothing - valid situation
            }

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

        Ontology ontology = getOrCreateOntology(apiOntologyTerm.getOntology());

        return atlasDAO.getOrCreateOntologyTerm(
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
        } catch (DAOException e) {
            throw new ResourceNotFoundException(e.getMessage(), e);
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
        } catch (DAOException e) {
            throw new ResourceNotFoundException(e.getMessage(), e);
        }
    }

    private PropertyValue getOrCreatePropertyValue(ApiPropertyValue apv) {
        return atlasDAO.getOrCreatePropertyValue(apv.getProperty().getName(), apv.getValue());
    }
}
