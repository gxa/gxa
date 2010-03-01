/*
 * Copyright 2008-2010 Microarray Informatics Team, EMBL-European Bioinformatics Institute
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *
 * For further details of the Gene Expression Atlas project, including source code,
 * downloads and documentation, please see:
 *
 * http://ostolop.github.com/gxa/
 */

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