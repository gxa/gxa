package uk.ac.ebi.gxa.loader.dao;

import uk.ac.ebi.gxa.dao.ArrayDesignDAO;
import uk.ac.ebi.gxa.dao.ExperimentDAO;
import uk.ac.ebi.gxa.dao.OrganismDAO;
import uk.ac.ebi.gxa.dao.PropertyValueDAO;
import uk.ac.ebi.gxa.dao.exceptions.RecordNotFoundException;
import uk.ac.ebi.microarray.atlas.model.ArrayDesign;
import uk.ac.ebi.microarray.atlas.model.Experiment;
import uk.ac.ebi.microarray.atlas.model.Organism;
import uk.ac.ebi.microarray.atlas.model.PropertyValue;

/**
 * The bridge between loader and the rest of the application - encapsulates external services
 * used during data loading
 */
public class LoaderDAO {
    private final ExperimentDAO experimentDAO;
    private final PropertyValueDAO propertyValueDAO;
    private final OrganismDAO organismDAO;
    private final ArrayDesignDAO arrayDesignDAO;

    public LoaderDAO(ExperimentDAO experimentDAO, PropertyValueDAO propertyValueDAO, OrganismDAO organismDAO, ArrayDesignDAO arrayDesignDAO) {
        this.experimentDAO = experimentDAO;
        this.propertyValueDAO = propertyValueDAO;
        this.organismDAO = organismDAO;
        this.arrayDesignDAO = arrayDesignDAO;
    }

    public Organism getOrCreateOrganism(String name) {
        return organismDAO.getOrCreateOrganism(name);
    }

    public PropertyValue getOrCreatePropertyValue(String name, String value) {
        return propertyValueDAO.getOrCreatePropertyValue(name, value);
    }

    public ArrayDesign getArrayDesignShallow(String accession) {
        return arrayDesignDAO.getArrayDesignShallowByAccession(accession);
    }

    public ArrayDesign getArrayDesign(String accession) {
        return arrayDesignDAO.getArrayDesignByAccession(accession);
    }

    public void save(Experiment experiment) {
        experimentDAO.save(experiment);
    }

    public Experiment getExperiment(String accession) throws RecordNotFoundException {
        return experimentDAO.getByName(accession);
    }
}
