package  uk.ac.ebi.gxa.model;

/**
 * Object has unique id and accession, which can be retrieved by getId() getAccession().
 * User: Andrey
 * Date: Oct 21, 2009
 * Time: 5:22:07 PM
 * To change this template use File | Settings | File Templates.
 */
public interface Accessible {
    public int getId();
    public String getAccession();
}
