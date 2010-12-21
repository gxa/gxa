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

import java.util.List;
import java.util.Map;

/**
 * Javadocs go here!
 *
 * @author Tony Burdett
 * @date 28-Sep-2009
 */

public class ArrayDesign {
    private String accession;
    private String name;
    private String provider;
    private long arrayDesignID;
    private Map<String, Long> designElements;
    private Map<Long, List<Long>> genes;

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

    public long getArrayDesignID() {
        return arrayDesignID;
    }

    public void setArrayDesignID(long arrayDesignID) {
        this.arrayDesignID = arrayDesignID;
    }

    public Map<String, Long> getDesignElements() {
        return designElements;
    }

    public void setDesignElements(Map<String, Long> designElements) {
        this.designElements = designElements;
    }

    public Map<Long, List<Long>> getGenes() {
        return genes;
    }

    public void setGenes(Map<Long, List<Long>> genes) {
        this.genes = genes;
    }
}
