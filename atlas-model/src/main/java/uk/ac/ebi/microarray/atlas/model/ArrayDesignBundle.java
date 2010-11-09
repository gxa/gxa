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
//    private List<String> designElementNames = new ArrayList<String>(300000);
    private Map<String, Map<String, List<String>>> designElementDBEs = new HashMap<String, Map<String, List<String>>>(300000);

    private Collection<String> geneIdentifierNames;

    private Logger log = LoggerFactory.getLogger(this.getClass());

    public String getAccession() {
        return accession;
    }

    public void setAccession(String accession) {
        this.accession = accession;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
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

    public Collection<String> getDesignElementNames() {
        return designElementDBEs.keySet();
    }

    public void addDesignElementName(String designElementName) {
        designElementDBEs.put(designElementName, new HashMap<String, List<String>>());
    }

    public Map<String, List<String>> getDatabaseEntriesForDesignElement(String designElementName) {
        if (designElementDBEs != null && designElementDBEs.containsKey(designElementName)) {
            return designElementDBEs.get(designElementName);
        }
        else {
            return new HashMap<String, List<String>>();
        }
    }

    public void addDatabaseEntryForDesignElement1(String designElement, String type, String... values) {
        // lazy instantiate
        if (designElementDBEs == null) {
            designElementDBEs = new HashMap<String, Map<String, List<String>>>();
        }
        // if there is no key for this design element, add it with a new map
        if (!designElementDBEs.containsKey(designElement)) {
            designElementDBEs.put(designElement, new HashMap<String, List<String>>());
        }
        // if there is no previous type, add it with a new list
        if (!designElementDBEs.get(designElement).containsKey(type)) {
            designElementDBEs.get(designElement).put(type, new ArrayList<String>());
        }
        // now put the values into the list
        designElementDBEs.get(designElement).get(type).addAll(Arrays.asList(values));
    }

    public void addDatabaseEntryForDesignElement(String designElement, String type, String... values) {

        Map<String, List<String>> entries = designElementDBEs.get(designElement);
        if (entries == null) {
            entries = new HashMap<String, List<String>>(30);
            designElementDBEs.put(designElement, entries);
        }

        List<String> entryValues = entries.get(type);
        if (entryValues == null) {
            entryValues = new ArrayList<String>();
            entries.put(type, entryValues);
        }

        entryValues.addAll(Arrays.asList(values));

    }

    public void addDesignElementWithEntries(String designElement, Map<String, List<String>> entries) {

        designElementDBEs.put(designElement, entries);
    }

    public Collection<String> getGeneIdentifierNames() {
        return geneIdentifierNames;
    }

    public void setGeneIdentifierNamesInPriorityOrder(Collection<String> geneIdentifierNames) {
        this.geneIdentifierNames = geneIdentifierNames;
    }

    @Override
    public String toString() {
        return "ArrayDesignBundle{" +
                "accession='" + accession + '\'' +
                ", name='" + name + '\'' +
                ", provider='" + provider + '\'' +
                ", type='" + type + '\'' +
                ", designElementDBEs=" + designElementDBEs +
                ", geneIdentifierNames=" + geneIdentifierNames +
                '}';
    }
}