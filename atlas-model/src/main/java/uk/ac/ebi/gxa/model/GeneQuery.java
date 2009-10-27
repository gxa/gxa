package  uk.ac.ebi.gxa.model;

/**
 * Simple search object, used to retrieve {@link Gene}.
 * User: Andrey
 * Date: Oct 14, 2009
 * Time: 5:07:41 PM
 * To change this template use File | Settings | File Templates.
 */

public class GeneQuery extends AccessionQuery<GeneQuery> {
    public GeneQuery usedInExperiments(ExperimentQuery experimentQuery){
        return this;
    }

    /**
     * how it will be - Species in singular form - again?
     * @param organism
     * @return
     */
    public GeneQuery isSpecies(String organism){
        return this;
    }

    public GeneQuery hasProperty(PropertyQuery propertyQuery){
        return this;
    }


}