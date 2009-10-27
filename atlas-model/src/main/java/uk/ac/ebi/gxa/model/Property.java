package  uk.ac.ebi.gxa.model;

import java.util.Collection;

/**
 * Property of Sample, Assay and ExperimentFactors.
 * User: Andrey
 * Date: Oct 21, 2009
 * Time: 7:34:54 PM
 * To change this template use File | Settings | File Templates.
 */
public interface Property extends Accessible {

    public String getName();

    public Collection<String> getValues();
}
