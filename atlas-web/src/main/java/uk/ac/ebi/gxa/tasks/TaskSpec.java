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

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

/**
 * Task "specification". Consists of task type and object accession string.
 *
 * @author pashky
 */
public class TaskSpec {
    private final String type;
    private final String accession;
    private final Multimap<String, String> userData;

    /**
     * Constructor
     *
     * @param type      type
     * @param accession accession
     */
    public TaskSpec(String type, String accession) {
        this(type, accession, HashMultimap.<String, String>create());
    }

    /**
     * Constructor
     *
     * @param type      type
     * @param accession accession
     * @param userData
     */
    public TaskSpec(String type, String accession, Multimap<String, String> userData) {
        this.type = type;
        this.accession = accession;
        this.userData = userData;
    }

    /**
     * Returns type
     *
     * @return type
     */
    public String getType() {
        return type;
    }

    /**
     * Returns accession
     *
     * @return accession
     */
    public String getAccession() {
        return accession;
    }

    public Multimap<String, String> getUserData() {
        return userData;
    }

    @Override
    public String toString() {
        return type + "Task[" + accession + "]";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TaskSpec taskSpec = (TaskSpec) o;

        if (accession != null ? !accession.equals(taskSpec.accession) : taskSpec.accession != null) return false;
        if (type != null ? !type.equals(taskSpec.type) : taskSpec.type != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = type != null ? type.hashCode() : 0;
        result = 31 * result + (accession != null ? accession.hashCode() : 0);
        return result;
    }
}
