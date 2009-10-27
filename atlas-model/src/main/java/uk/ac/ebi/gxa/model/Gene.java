package  uk.ac.ebi.gxa.model;

/**
 * Primary object of the research.
 * User: Andrey
 * Date: Oct 20, 2009
 * Time: 4:34:46 PM
 * To change this template use File | Settings | File Templates.
 */
public interface Gene extends Accessible, Annotated {

    /**
    * What is singular form of Species?
    */
    public String getSpecies();
}