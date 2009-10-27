package  uk.ac.ebi.gxa.model;

import java.util.Collection;

/**
 * Tissue sample; there can be more then one sample in assay, and more then one assay per sample.
 * User: Andrey
 * Date: Oct 20, 2009
 * Time: 5:48:14 PM
 * To change this template use File | Settings | File Templates.
 */
public interface Sample extends Accessible, Annotated {

    public Collection<String> getAssayAccessions();

    public Collection<String> getExperimentAccessions();
}