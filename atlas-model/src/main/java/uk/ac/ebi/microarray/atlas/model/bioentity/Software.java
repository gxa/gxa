/*
 * Copyright 2008-2011 Microarray Informatics Team, EMBL-European Bioinformatics Institute
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

package uk.ac.ebi.microarray.atlas.model.bioentity;

import javax.persistence.*;

/**
 * User: nsklyar
 * Date: 04/05/2011
 */
@Entity
public class Software {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "swSeq")
    @SequenceGenerator(name = "swSeq", sequenceName = "A2_SOFTWARE_SEQ", allocationSize = 1)
    private Long softwareid;
    private String name;
    private String version;

    Software() {
    }

    public Software(Long softwareid, String name, String version) {
        this.softwareid = softwareid;
        this.name = name;
        this.version = version;
    }

    public Software(String name, String version) {
        this.name = name;
        this.version = version;
    }

    public Long getSoftwareid() {
        return softwareid;
    }

    public String getName() {
        return name;
    }

    public String getVersion() {
        return version;
    }

    public String getFullName(){
        StringBuilder sb = new StringBuilder();
        sb.append(name).append(" v.").append(version);
        return sb.toString();
    }

    @Override
    public String toString() {
        return "Software{" +
                "softwareid=" + softwareid +
                ", name='" + name + '\'' +
                ", version='" + version + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Software software = (Software) o;

        if (name != null ? !name.equals(software.name) : software.name != null) return false;
        if (version != null ? !version.equals(software.version) : software.version != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (version != null ? version.hashCode() : 0);
        return result;
    }
}
