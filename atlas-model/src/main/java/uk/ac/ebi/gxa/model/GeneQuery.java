package  uk.ac.ebi.gxa.model;

import java.util.List;
import java.util.ArrayList;

/**
 * Simple search object, used to retrieve gene.
 * User: Andrey
 * Date: Oct 14, 2009
 * Time: 5:07:41 PM
 * To change this template use File | Settings | File Templates.
 */

public class GeneQuery extends AccessionQuery<GeneQuery> {
    private String species = null;

    private List<ExperimentQuery> experimentQueries = new ArrayList<ExperimentQuery>();
    private List<PropertyQuery> propertyQueries = new ArrayList<PropertyQuery>();
    private List<PropertyQuery> propertyNotQueries = new ArrayList<PropertyQuery>();

    public GeneQuery(){};

    public GeneQuery(AccessionQuery accessionQuery){
        super(accessionQuery);
    } 

    public GeneQuery usedInExperiments(ExperimentQuery experimentQuery) {
        experimentQueries.add(experimentQuery);
        return this;
    }

    public GeneQuery hasProperty(PropertyQuery propertyQuery) {
        propertyQueries.add(propertyQuery);
        return this;
    }

    public GeneQuery hasNotProperty(PropertyQuery propertyQuery) {
        propertyNotQueries.add(propertyQuery);
        return this;
    }

    public List<ExperimentQuery> getExperimentQueries() {
        return experimentQueries;
    }

    public List<PropertyQuery> getPropertyQueries() {
        return propertyQueries;
    }

    public GeneQuery hasSpecies(String species){
        this.species = species;
        return this;
    }

    public String getSpecies(){
        return this.species;
    }

}