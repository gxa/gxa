/*
 * Copyright 2008-2012 Microarray Informatics Team, EMBL-European Bioinformatics Institute
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

package uk.ac.ebi.microarray.atlas.export;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * User: nsklyar
 * Date: 01/05/2012
 */
public class ChEbiEntry {
    private String chebiAcc;

    private Set<ExperimentInfo> experimentInfos = new HashSet<ExperimentInfo>();

    public ChEbiEntry(String chebiAcc, String accession, String description) {
        this.chebiAcc = chebiAcc;
        this.experimentInfos.add(new ExperimentInfo(accession, description));
    }

    public boolean addExperimentInfo(String accession, String description) {
        return  this.experimentInfos.add(new ExperimentInfo(accession, description));
    }

    public String getChebiAcc() {
        return chebiAcc;
    }

    public Set<ExperimentInfo> getExperimentInfos() {
        return Collections.unmodifiableSet(experimentInfos);
    }

    @Override
    public String toString() {
        return "ChEbiEntry{" +
                "chebiAcc='" + chebiAcc + '\'' +
                ", experimentInfos=" + experimentInfos +
                '}';
    }

    public class ExperimentInfo {
        private String accession;
        private String description;

        private ExperimentInfo(String accession, String description) {
            this.accession = accession;
            this.description = description;
        }

        public String getAccession() {
            return accession;
        }

        public String getDescription() {
            return description;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            ExperimentInfo that = (ExperimentInfo) o;

            if (accession != null ? !accession.equals(that.accession) : that.accession != null) return false;
            if (description != null ? !description.equals(that.description) : that.description != null) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = accession != null ? accession.hashCode() : 0;
            result = 31 * result + (description != null ? description.hashCode() : 0);
            return result;
        }

        @Override
        public String toString() {
            return "ExperimentInfo{" +
                    "accession='" + accession + '\'' +
                    ", description='" + description + '\'' +
                    '}';
        }
    }
}
