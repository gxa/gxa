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
    private List<String> species = new ArrayList<String>();

    private List<ExperimentQuery> experimentQueries = new ArrayList<ExperimentQuery>();
    private List<GenePropertyQuery> propertyQueries = new ArrayList<GenePropertyQuery>();
    private List<GenePropertyQuery> propertyNotQueries = new ArrayList<GenePropertyQuery>();

    public GeneQuery(){};

    public GeneQuery(AccessionQuery accessionQuery){
        super(accessionQuery);
    } 

    public GeneQuery usedInExperiments(ExperimentQuery experimentQuery) {
        experimentQueries.add(experimentQuery);
        return this;
    }

    public GeneQuery hasProperty(GenePropertyQuery propertyQuery) {
        propertyQueries.add(propertyQuery);
        return this;
    }

    public GeneQuery hasNotProperty(GenePropertyQuery propertyQuery) {
        propertyNotQueries.add(propertyQuery);
        return this;
    }

    public List<ExperimentQuery> getExperimentQueries() {
        return experimentQueries;
    }

    public List<GenePropertyQuery> getPropertyQueries() {
        return propertyQueries;
    }

    public GeneQuery hasSpecies(String species){
        this.species.add(species);
        return this;
    }

    public List<String> getSpecies(){
        return this.species;
    }


    public List<GenePropertyQuery> getPropertyNotQueries() {
        return propertyNotQueries;
    }
}