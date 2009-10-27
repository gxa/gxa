package  uk.ac.ebi.gxa.model;

import java.util.Collection;

/**
 * Assay
 * User: Andrey
 * Date: Oct 20, 2009
 * Time: 5:44:03 PM
 * To change this template use File | Settings | File Templates.
 */
public interface Assay extends Accessible, Annotated {

        /**
         * Experiment this assay was used in.
         * @return experiment accession
         */
        public String getExperimentAccession();

        /**
         * Samples mixed in assay.
         * @return list of samples accession
        */
        public Collection<String> getSampleAccessions();

        /**
         * Gets column position in expression matrix of the array design, this assay belongs to
         * @return position
         */
        public int getPositionInMatrix();


}