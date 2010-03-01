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

package uk.ac.ebi.microarray.atlas.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Javadocs go here!
 *
 * @author Tony Burdett
 * @date 18-Feb-2010
 */
public class ArrayDesignBundle {
    private String accession;
    private String name;
    private String provider;
    private String type;
    private List<String> designElementNames;
    private Map<String, Map<String, List<String>>> designElementDBEs;

    private List<String> geneIdentifierNames;

    private Logger log = LoggerFactory.getLogger(this.getClass());

    public synchronized String getAccession() {
        return accession;
    }

    public synchronized void setAccession(String accession) {
        this.accession = accession;
    }

    public synchronized String getType() {
        return type;
    }

    public synchronized void setType(String type) {
        this.type = type;
    }

    public synchronized String getName() {
        return name;
    }

    public synchronized void setName(String name) {
        this.name = name;
    }

    public synchronized String getProvider() {
        return provider;
    }

    public synchronized void setProvider(String provider) {
        this.provider = provider;
    }

    public synchronized List<String> getDesignElementNames() {
        if (designElementNames == null) {
            designElementNames = new ArrayList<String>();
        }
        return designElementNames;
    }

    public synchronized void addDesignElementName(String designElementName) {
        if (designElementNames == null) {
            designElementNames = new ArrayList<String>();
        }
        designElementNames.add(designElementName);
    }

    public synchronized Map<String, List<String>> getDatabaseEntriesForDesignElement(String designElementName) {
        if (designElementDBEs != null && designElementDBEs.containsKey(designElementName)) {
            return designElementDBEs.get(designElementName);
        }
        else {
            return new HashMap<String, List<String>>();
        }
    }

    public synchronized void addDatabaseEntryForDesignElement(String designElement, String type, String... values) {
        // lazy instantiate
        if (designElementDBEs == null) {
            designElementDBEs = new HashMap<String, Map<String, List<String>>>();
        }
        // if there is no key for this design element, add it with a new map
        if (!designElementDBEs.containsKey(designElement)) {
            if (designElementNames.contains(designElement)) {
                designElementDBEs.put(designElement, new HashMap<String, List<String>>());
            }
            else {
                throw new NullPointerException("No design element with name '" + designElement + "'");
            }
        }
        // if there is no previous type, add it with a new list
        if (!designElementDBEs.get(designElement).containsKey(type)) {
            designElementDBEs.get(designElement).put(type, new ArrayList<String>());
        }
        // now put the values into the list
        designElementDBEs.get(designElement).get(type).addAll(Arrays.asList(values));
    }

    public synchronized List<String> getGeneIdentifierNames() {
        return geneIdentifierNames;
    }

    public synchronized void setGeneIdentifierNamesInPriorityOrder(List<String> geneIdentifierNames) {
        this.geneIdentifierNames = geneIdentifierNames;
    }
}