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

import java.util.*;

import static java.util.Collections.singletonList;

public class ArrayDesign {
    private String accession;
    private String name;
    private String provider;
    private String type;
    private long arrayDesignID;
    private long mappingSoftwareId;
    private Map<String, Long> designElements = new HashMap<String, Long>();
    private Map<Long, List<Long>> genes = new HashMap<Long, List<Long>>();

    public String getAccession() {
        return accession;
    }

    public void setAccession(String accession) {
        this.accession = accession;
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

    public long getArrayDesignID() {
        return arrayDesignID;
    }

    public void setArrayDesignID(long arrayDesignID) {
        this.arrayDesignID = arrayDesignID;
    }

    public long getMappingSoftwareId() {
        return mappingSoftwareId;
    }

    public void setMappingSoftwareId(long mappingSoftwareId) {
        this.mappingSoftwareId = mappingSoftwareId;
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
        return this.genes.size() > 0?true:false;
    }
}
