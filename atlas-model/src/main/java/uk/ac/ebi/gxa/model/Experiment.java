package  uk.ac.ebi.gxa.model;

import java.util.*;

/**
 * Experiment.
 * User: Andrey
 * Date: Oct 20, 2009
 * Time: 5:31:56 PM
 */

public interface Experiment extends Accessible, Annotated {

        public Collection<String> getType();

        public String getDescription();

        public Date getLoadDate();

         /**
         * Returns one of DEGStatus.EMPTY, DEGStatus.NONEMPTY, DEGStatus.UNKNOWN,
         * if experiment doesn't have any d.e. genes, has some d.e. genes, or if this is unknown
         * @return one of DEGStatus.EMPTY, DEGStatus.NONEMPTY, DEGStatus.UNKNOWN
         */
        public enum DEGStatus {UNKNOWN, EMPTY, NONEMPTY};

        public DEGStatus getDEGStatus();

        public String getPerformer();

        public String getLab();

        /**
        * Returns assays used in experiment.
        * @return collection of string.
        */
        public Collection<String> getAssayAccessions();

        /**
        * Returns samples used in experiment.
        * @return collection of string.
        */
        public Collection<String> getSampleAccessions();
}