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

package uk.ac.ebi.microarray.atlas.model;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import javax.persistence.*;
import java.util.*;

import static java.util.Collections.singletonList;

@Entity
@Cache(usage = CacheConcurrencyStrategy.TRANSACTIONAL)
public class ArrayDesign {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "arrayDesignSeq")
    @SequenceGenerator(name = "arrayDesignSeq", sequenceName = "A2_ARRAYDESIGN_SEQ", allocationSize = 1)
    private Long arrayDesignID;
    private String accession;
    @Column(name = "ACCESSION_MASTER")
    private String accessionMaster;
    private String name;
    private String provider;
    private String type;

    @Transient
    private Map<String, Long> designElements = new HashMap<String, Long>();
    @Transient
    private Map<Long, List<Long>> genes = new HashMap<Long, List<Long>>();

    // this constructor can be used in hibernate only
    ArrayDesign() {
    }

    public ArrayDesign(String accession) {
        this.accession = accession;
    }

    public String getAccession() {
        return accession;
    }

    public String getAccessionMaster() {
        return accessionMaster;
    }

    public void setAccessionMaster(String accessionMaster) {
        this.accessionMaster = accessionMaster;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Long getArrayDesignID() {
        return arrayDesignID;
    }

    public void setArrayDesignID(Long arrayDesignID) {
        this.arrayDesignID = arrayDesignID;
    }

    public Set<Long> getAllGenes() {
        Set<Long> result = new HashSet<Long>();
        for (List<Long> genes : this.genes.values()) {
            result.addAll(genes);
        }
        return result;
    }

    public void addDesignElement(String name, long id) {
        designElements.put(name, id);
    }

    public Long getDesignElement(String de) {
        return designElements.get(de);
    }

    public void addGene(long id, long geneId) {
        genes.put(id, singletonList(geneId)); // TODO: as of today, we have one gene per de
    }

    public List<Long> getGeneId(Long deId) {
        return genes.get(deId);
    }

    public boolean hasGenes() {
        return this.genes.size() > 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ArrayDesign that = (ArrayDesign) o;

        if (accession != null ? !accession.equals(that.accession) : that.accession != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = 17;
        result = 31 * result + (accession != null ? accession.hashCode() : 0);
        return result;
    }
}
