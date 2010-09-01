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

package ae3.model;

import uk.ac.ebi.gxa.requesthandlers.base.restutil.RestOut;
import org.apache.solr.common.SolrDocument;

import java.util.*;

/**
 * View class, wrapping Atlas experiment data stored in SOLR document
 */
@RestOut(xmlItemName ="experiment")
public class AtlasExperiment implements java.io.Serializable {

    private HashSet<String> experimentFactors = new HashSet<String>();
    private HashSet<String> sampleCharacteristics = new HashSet<String>();
    private TreeMap<String, Collection<String>> sampleCharacterisitcValues = new TreeMap<String, Collection<String>>();
    private TreeMap<String, Collection<String>> factorValues = new TreeMap<String, Collection<String>>();

    private SolrDocument exptSolrDocument;

    private HashMap<String, String> highestRankEF = new HashMap<String, String>();

    public enum DEGStatus {UNKNOWN, EMPTY, NONEMPTY};
    private DEGStatus exptDEGStatus = DEGStatus.UNKNOWN;

    /**
     * Constructor
     * @param exptdoc SOLR document to wrap
     */
    @SuppressWarnings("unchecked")
    public AtlasExperiment(SolrDocument exptdoc) {
        exptSolrDocument = exptdoc;

        for(String field : exptSolrDocument.getFieldNames()) {
            if(field.startsWith("a_property_")) {
                String property = field.substring("a_property_".length());
                Collection<String> values = new HashSet<String>();
                values.addAll((Collection)exptSolrDocument.getFieldValues(field));
                experimentFactors.add(property);
                factorValues.put(property, values);
            } else if(field.startsWith("s_property_")) {
                String property = field.substring("s_property_".length());
                Collection<String> values = new HashSet<String>();
                values.addAll((Collection)exptSolrDocument.getFieldValues(field));
                sampleCharacteristics.add(property);
                sampleCharacterisitcValues.put(property, values);
            }
        }
    }

    /**
     * Returns set of sample characteristics
     * @return set of sample characteristics
     */
    public HashSet<String> getSampleCharacteristics() {
        return sampleCharacteristics;
    }

    /**
     * Returns map of sample characteristic values
     * @return map of sample characteristic values
     */
    public TreeMap<String, Collection<String>> getSampleCharacterisitcValues() {
		return sampleCharacterisitcValues;
	}

    /**
     * Returns map of factor values
     * @return map of factor values
     */
    public TreeMap<String, Collection<String>> getFactorValuesForEF() {
		return factorValues;
	}

    /**
     * Returns experiment internal numeric ID
     * @return experiment internal numeric ID
     */
    public Integer getId()
    {
        return (Integer)exptSolrDocument.getFieldValue("id");
    }

    /**
     * Returns experiment accession
     * @return experiment accession
     */
    @RestOut(name="accession")
    public String getAccession() {
        return (String)exptSolrDocument.getFieldValue("accession");
    }

    /**
     * Returns experiment description
     * @return experiment description
     */
    @RestOut(name="description")
    public String getDescription() {
        return (String)exptSolrDocument.getFieldValue("description");
    }

    /**
     * Returns PubMed ID
     * @return PubMedID
     */
    @RestOut(name="pubmedId")
    public Integer getPubmedId() {
         return (Integer) exptSolrDocument.getFieldValue("pmid");
    }

    /**
     * Returns set of experiment factors
     * @return
     */
    public Set<String> getExperimentFactors() {
        return experimentFactors;
    }

    /**
     * Returns map of highest rank EFs for genes
     * @return map of highest rank EFs for genes
     */
    public HashMap<String, String> getHighestRankEFs() {
        return highestRankEF;
    }

    /**
     * Adds highest rank EF for gene
     * @param geneIdentifier gene identifier
     * @param highestRankEF highest rank EF for gene in this experiment
     */
    public void addHighestRankEF(String geneIdentifier, String highestRankEF) {
        this.highestRankEF.put(geneIdentifier, highestRankEF);
    }

    /**
     * Sets differentially expression status for the experiment
     * @param degStatus differentially expression status for the experiment
     */
    public void setDEGStatus(DEGStatus degStatus) {
        this.exptDEGStatus = degStatus;
    }

    /**
     * Returns one of DEGStatus.EMPTY, DEGStatus.NONEMPTY, DEGStatus.UNKNOWN,
     * if experiment doesn't have any d.e. genes, has some d.e. genes, or if this is unknown
     * @return one of DEGStatus.EMPTY, DEGStatus.NONEMPTY, DEGStatus.UNKNOWN
     */
    public DEGStatus getDEGStatus() {
        return this.exptDEGStatus;
    }

}
