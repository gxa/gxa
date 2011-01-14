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

import org.apache.solr.common.SolrDocument;
import uk.ac.ebi.gxa.requesthandlers.base.restutil.RestOut;

import java.util.*;

/**
 * View class, wrapping Atlas experiment data stored in SOLR document
 */
@RestOut(xmlItemName = "experiment")
public class AtlasExperiment {
    private HashSet<String> experimentFactors = new HashSet<String>();
    private HashSet<String> sampleCharacteristics = new HashSet<String>();
    private TreeMap<String, Collection<String>> sampleCharacteristicValues = new TreeMap<String, Collection<String>>();
    private TreeMap<String, Collection<String>> factorValues = new TreeMap<String, Collection<String>>();

    private SolrDocument exptSolrDocument;

    private HashMap<String, String> highestRankEF = new HashMap<String, String>();

    public enum DEGStatus {UNKNOWN, EMPTY}

    private DEGStatus exptDEGStatus = DEGStatus.UNKNOWN;


    public static AtlasExperiment createExperiment(SolrDocument exptdoc) {
        // TODO: implement this contition:
        //   a. by arraydesign (?)
        //   b. by special field in database (?)
        return new AtlasExperiment(exptdoc);
    }

    /**
     * Constructor
     *
     * @param exptdoc SOLR document to wrap
     */
    @SuppressWarnings("unchecked")
    protected AtlasExperiment(SolrDocument exptdoc) {
        exptSolrDocument = exptdoc;

        for (String field : exptSolrDocument.getFieldNames()) {
            if (field.startsWith("a_property_")) {
                final String property = field.substring("a_property_".length());
                experimentFactors.add(property);

                TreeSet<String> values = new TreeSet<String>();
                values.addAll((Collection) exptSolrDocument.getFieldValues(field));
                ArrayList<String> sorted_values = new ArrayList<String>(values);
                factorValues.put(property, sorted_values);
            } else if (field.startsWith("s_property_")) {
                String property = field.substring("s_property_".length());
                Collection<String> values = new HashSet<String>();
                values.addAll((Collection) exptSolrDocument.getFieldValues(field));
                sampleCharacteristics.add(property);
                sampleCharacteristicValues.put(property, values);
            }
        }
    }

    public String getTypeString() {
        return getType().toString();
    }

    public Type getType() {
        return Type.getTypeByPlatformName((String) exptSolrDocument.getFieldValue("platform"));
    }

    /**
     * Returns set of sample characteristics
     *
     * @return set of sample characteristics
     */
    public HashSet<String> getSampleCharacteristics() {
        return sampleCharacteristics;
    }

    /**
     * Returns map of sample characteristic values
     *
     * @return map of sample characteristic values
     */
    public TreeMap<String, Collection<String>> getSampleCharacteristicValues() {
        return sampleCharacteristicValues;
    }

    /**
     * Returns map of factor values
     *
     * @return map of factor values
     */
    public TreeMap<String, Collection<String>> getFactorValuesForEF() {
        return factorValues;
    }

    /**
     * Returns experiment internal numeric ID
     *
     * @return experiment internal numeric ID
     */
    public Integer getId() {
        return (Integer) exptSolrDocument.getFieldValue("id");
    }

    /**
     * Return a Collection of top gene ids (i.e. the one with an ef-efv
     * with the lowest pValues across all ef-efvs)
     *
     * @return
     */
    public Collection<String> getTopGeneIds() {
        return getValues("top_gene_ids");
    }

    /**
     * @return Collection of proxyIds (in the same order as getTopGeneIds())
     *         from which best ExpressionAnalyses for each top gene can be retrieved (to be
     *         used in conjunction with getTopDEIndexes())
     */
    public Collection<String> getTopProxyIds() {
        return getValues("top_proxy_ids");
    }

    /**
     * @return Collection of design element indexes (in the same order as getTopGeneIds())
     *         from which best ExpressionAnalyses for each top gene can be retrieved (to be
     *         used in conjunction with getTopProxyIds())
     */
    public Collection<String> getTopDEIndexes() {
        return getValues("top_de_indexes");
    }

    /**
     * Returns experiment accession
     *
     * @return experiment accession
     */
    @RestOut(name = "accession")
    public String getAccession() {
        return (String) exptSolrDocument.getFieldValue("accession");
    }

    /**
     * Returns experiment description
     *
     * @return experiment description
     */
    @RestOut(name = "description")
    public String getDescription() {
        return (String) exptSolrDocument.getFieldValue("description");
    }

    /**
     * Returns PubMed ID
     *
     * @return PubMedID
     */
    @RestOut(name = "pubmedId")
    public Integer getPubmedId() {
        return (Integer) exptSolrDocument.getFieldValue("pmid");
    }

    /**
     * Returns set of experiment factors
     *
     * @return
     */
    public Set<String> getExperimentFactors() {
        return experimentFactors;
    }

    /**
     * Returns map of highest rank EFs for genes
     *
     * @return map of highest rank EFs for genes
     */
    public HashMap<String, String> getHighestRankEFs() {
        return highestRankEF;
    }

    /**
     * Adds highest rank EF for gene
     *
     * @param geneIdentifier gene identifier
     * @param highestRankEF  highest rank EF for gene in this experiment
     */
    public void addHighestRankEF(String geneIdentifier, String highestRankEF) {
        this.highestRankEF.put(geneIdentifier, highestRankEF);
    }

    /**
     * Sets differentially expression status for the experiment
     *
     * @param degStatus differentially expression status for the experiment
     */
    public void setDEGStatus(DEGStatus degStatus) {
        this.exptDEGStatus = degStatus;
    }

    /**
     * Returns one of DEGStatus.EMPTY, DEGStatus.NONEMPTY, DEGStatus.UNKNOWN,
     * if experiment doesn't have any d.e. genes, has some d.e. genes, or if this is unknown
     *
     * @return one of DEGStatus.EMPTY, DEGStatus.NONEMPTY, DEGStatus.UNKNOWN
     */
    public DEGStatus getDEGStatus() {
        return this.exptDEGStatus;
    }

    public boolean isDEGStatusEmpty() {
        return this.exptDEGStatus == DEGStatus.EMPTY;
    }

    /**
     * Safely gets collection of field values
     *
     * @param name field name
     * @return collection (maybe empty but never null)
     */
    @SuppressWarnings("unchecked")
    private Collection<String> getValues(String name) {
        Collection<Object> r = exptSolrDocument.getFieldValues(name);
        return r == null ? Collections.EMPTY_LIST : r;
    }

    public String getPlatform() {
        return (String) exptSolrDocument.getFieldValue("platform");
    }

    //try to find requested array design, or return first one if not found
    //best if this function checked if ncdf file is avaliable,
    //also it may accept geneID as a parameter, and skip ArrayDesigns where no such gene
    public String getArrayDesign(String arrayDesign) {
        String[] arrayDesigns = getPlatform().split(",");
        if (null != arrayDesign) {
            for (int i = 0; i != arrayDesigns.length; i++) {
                if (arrayDesign.equalsIgnoreCase(arrayDesigns[i])) {
                    return arrayDesigns[i];
                }
            }
        }
        return arrayDesigns[0];
    }

    public String getOrganism() {
        return (String) exptSolrDocument.getFieldValue("organism");
    }

    public String getNumSamples() {
        return (String) exptSolrDocument.getFieldValue("numSamples");
    }

    public String getNumIndividuals() {
        return (String) exptSolrDocument.getFieldValue("numIndividuals");
    }

    public String getStudyType() {
        return (String) exptSolrDocument.getFieldValue("studyType");
    }

    public List<Asset> getAssets() {
        Collection<Object> assetCaption = exptSolrDocument.getFieldValues("assetCaption");
        if (null == assetCaption) {
            return Collections.emptyList();
        }

        ArrayList<Asset> result = new ArrayList<Asset>();
        String[] fileInfo = ((String) exptSolrDocument.getFieldValue("assetFileInfo")).split(",");
        int i = 0;
        for (Object o : assetCaption) {
            String description = (null == exptSolrDocument.getFieldValues("assetDescription") ? null : (String) exptSolrDocument.getFieldValues("assetDescription").toArray()[i]);
            result.add(new Asset((String) o, fileInfo[i], description));
            i++;
        }
        return result;
    }

    @RestOut(name = "abstract")
    public String getAbstract() {
        return (String) exptSolrDocument.getFieldValue("abstract");
    }

    //any local resource associated with experiment
    //for example, pictures from published articles
    public static class Asset {
        private String name;
        private String fileName;
        private String description;

        public Asset(String name, String fileName, String description) {
            this.name = name;
            this.fileName = fileName;
            this.description = description;
        }

        public String getName() {
            return name;
        }

        public String getFileName() {
            return fileName;
        }

        public String getDescription() {
            return this.description;
        }

        public String toString() {
            return this.name;
        }
    }
}

