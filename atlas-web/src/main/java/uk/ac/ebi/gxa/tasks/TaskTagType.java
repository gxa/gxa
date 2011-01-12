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
 * http://gxa.github.com/gxa
 */

package uk.ac.ebi.gxa.tasks;

/**
 * Task log messages have a conception of "tags" and "task groups". Each scheduled task may trigger a whole
 * cascade of dependent task automatically scheduled upon original task completion. All those task executions
 * are considered related to each other and each of them may be realted to handling of some object like particular
 * experiment or array design accession or load URL. So, the whole group gets a bunch of tags like
 * "experiment=E-AFMX-5", "url=http://ae.uk/e-afmx-5.idf.txt" and "arraydesign=A-AFFY-123" so later one can search
 * for all the log messages somehow related (may be indirectly as arraydesign relates to experiment load, that's the
 * whole point) to particular object.
 *
 * This class represents all existing types of tagged objects
 *
 * @author pashky
 */
public enum TaskTagType {
    EXPERIMENT,
    ARRAYDESIGN,
    URL,
    ANNOTATIONS,
    MAPPING
}
