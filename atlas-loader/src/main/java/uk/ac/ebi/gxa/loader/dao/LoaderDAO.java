package uk.ac.ebi.gxa.loader.dao;

import uk.ac.ebi.gxa.dao.*;
import uk.ac.ebi.microarray.atlas.model.*;

import java.util.Collection;
import java.util.Collections;

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
        // TODO: 4alf: track newly-created values
        Organism organism = organismDAO.getByName(name);
        if (organism == null) {
            organismDAO.save(organism = new Organism(null, name));
        }
        return organism;
    }

    public PropertyValue getOrCreateProperty(String name, String value) {
        // TODO: 4alf: track newly-created values
        Property property = propertyDAO.getByName(name);
        if (property == null) {
            propertyDAO.save(property = new Property(null, name.toLowerCase()));
        }
        PropertyValue propertyValue = propertyValueDAO.find(property, value);
        if (propertyValue == null) {
            propertyValueDAO.save(propertyValue = new PropertyValue(null, property, value));
        }
        return propertyValue;
    }

    public ArrayDesign getArrayDesign(String accession) {
        return arrayDesignDAO.getArrayDesignByAccession(accession);
    }

    public Collection<OntologyTerm> getOrCreateEfoTerms(String efoTerms) {
        // TODO: 4alf: check DAO first
        // TODO: 4alf: track newly-created values
        return Collections.emptyList();
    }

    public void save(Experiment experiment) {
        experimentDAO.save(experiment);
    }

    public Experiment getExperiment(String accession) {
        return experimentDAO.getExperimentByAccession(accession);
    }
}
