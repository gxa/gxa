package uk.ac.ebi.gxa.loader.dao;

import uk.ac.ebi.gxa.dao.*;
import uk.ac.ebi.gxa.dao.exceptions.RecordNotFoundException;
import uk.ac.ebi.microarray.atlas.model.*;

/**
 * The bridge between loader and the rest of the application - encapsulates external services
 * used during data loading
 */
public class LoaderDAO {
    private final ExperimentDAO experimentDAO;
    private final PropertyDAO propertyDAO;
    private final PropertyValueDAO propertyValueDAO;
    private final OrganismDAO organismDAO;
    private final ArrayDesignDAO arrayDesignDAO;

    public LoaderDAO(ExperimentDAO experimentDAO, PropertyDAO propertyDAO, PropertyValueDAO propertyValueDAO, OrganismDAO organismDAO, ArrayDesignDAO arrayDesignDAO) {
        this.experimentDAO = experimentDAO;
        this.propertyDAO = propertyDAO;
        this.propertyValueDAO = propertyValueDAO;
        this.organismDAO = organismDAO;
        this.arrayDesignDAO = arrayDesignDAO;
    }

    public Organism getOrCreateOrganism(String name) {
        try {
            return organismDAO.getByName(name);
        } catch (RecordNotFoundException e) {
            // organism not found - create a new one
            Organism organism = new Organism(null, name);
            organismDAO.save(organism);
            return organism;
        }
    }

    public PropertyValue getOrCreatePropertyValue(String name, String value) {
        Property property = getOrCreateProperty(name);
        try {
            return propertyValueDAO.find(property, value);
        } catch (RecordNotFoundException e) {
            // property value not found - create a new one
            PropertyValue propertyValue = new PropertyValue(null, property, value);
            propertyValueDAO.save(propertyValue);
            return propertyValue;
        }
    }

    private Property getOrCreateProperty(String name) {
        try {
            return propertyDAO.getByName(name);
        } catch (RecordNotFoundException e) {
            // property not found - create a new one
            Property property = new Property(null, name);
            propertyDAO.save(property);
            return property;
        }
    }

    public ArrayDesign getArrayDesign(String accession) {
        return arrayDesignDAO.getArrayDesignShallowByAccession(accession);
    }

    public void save(Experiment experiment) {
        experimentDAO.save(experiment);
    }

    public Experiment getExperiment(String accession) throws RecordNotFoundException {
        return experimentDAO.getByName(accession);
    }
}
