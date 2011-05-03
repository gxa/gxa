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
import uk.ac.ebi.gxa.Asset;
import uk.ac.ebi.gxa.Experiment;
import uk.ac.ebi.gxa.exceptions.LogUtil;
import uk.ac.ebi.gxa.requesthandlers.base.restutil.RestOut;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 * View class, wrapping Atlas experiment data stored in SOLR document
 */
@RestOut(xmlItemName = "experiment")
public class AtlasExperimentImpl extends uk.ac.ebi.gxa.impl.ExperimentImpl {
    private HashSet<String> experimentFactors = new HashSet<String>();
    private HashSet<String> sampleCharacteristics = new HashSet<String>();
    private TreeMap<String, Collection<String>> sampleCharacteristicValues = new TreeMap<String, Collection<String>>();
    private TreeMap<String, Collection<String>> factorValues = new TreeMap<String, Collection<String>>();

    private final SolrDocument exptSolrDocument;

    // Stores the highest ranking ef when this experiment has been found in a list of pVal/tStatRank-sorted experiments
    // for a given gene (and no ef had been specified in the user's request)
    private String highestRankEF;

    public enum DEGStatus {UNKNOWN, EMPTY}

    private DEGStatus exptDEGStatus = DEGStatus.UNKNOWN;


    public static Experiment createExperiment(SolrDocument exptdoc) {
        final Experiment experiment = new AtlasExperimentImpl(exptdoc);

        experiment.setDescription((String) exptdoc.getFieldValue("description"));
        experiment.setAbstract((String) exptdoc.getFieldValue("abstract"));
        // TODO: setPerformer
        // TODO: setLab

        experiment.setLoadDate((Date) exptdoc.getFieldValue("loaddate"));
        experiment.setReleaseDate((Date) exptdoc.getFieldValue("releasedate"));
        experiment.setPubmedId((Long) exptdoc.getFieldValue("pmid"));

        final Collection<Object> assetCaption = exptdoc.getFieldValues("assetCaption");
        if (assetCaption != null) {
            final Collection<Object> descriptions = exptdoc.getFieldValues("assetDescription");
            // TODO: are we sure order is always the same?!?
            final Object[] descriptionsArray =
                    descriptions != null ? descriptions.toArray() : null;
            // TODO: 4geometer: descriptionsArray can be null here, thus NPE
            if (assetCaption.size() != descriptionsArray.length) {
                throw LogUtil.createUnexpected(
                        "Asset caption & description array sizes are different :" +
                                assetCaption.size() + " != " + descriptionsArray.length
                );
            }

            final String[] fileInfo =
                    ((String) exptdoc.getFieldValue("assetFileInfo")).split(",");
            if (assetCaption.size() != fileInfo.length) {
                throw LogUtil.createUnexpected(
                        "Asset caption & file info array sizes are different :" +
                                assetCaption.size() + " != " + fileInfo.length
                );
            }

            final ArrayList<Asset> assets = new ArrayList<Asset>();
            int i = 0;
            for (Object o : assetCaption) {
                // TODO: 4geometer: descriptionsArray was dereferenced in line 78, thus the check is redundant
                final String description =
                        descriptionsArray != null ? (String) descriptionsArray[i] : null;
                assets.add(new Asset((String) o, fileInfo[i], description));
                ++i;
            }
            experiment.addAssets(assets);
        }

        return experiment;
    }

    /**
     * Constructor
     *
     * @param exptdoc SOLR document to wrap
     */
    @SuppressWarnings("unchecked")
    private AtlasExperimentImpl(SolrDocument exptdoc) {
        super(
                (String) exptdoc.getFieldValue("accession"),
                (Long) exptdoc.getFieldValue("id")
        );

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
        return Type.getTypeByPlatformName(getPlatform());
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
        return super.getAccession();
    }

    /**
     * Returns experiment description
     *
     * @return experiment description
     */
    @RestOut(name = "description")
    public String getDescription() {
        return super.getDescription();
    }

    /**
     * Returns PubMed ID
     *
     * @return PubMedID
     */
    @RestOut(name = "pubmedId")
    public Long getPubmedId() {
        return super.getPubmedId();
    }

    /**
     * Returns set of experiment factors
     *
     * @return
     */
    public Set<String> getExperimentFactors() {
        return experimentFactors;
    }

    public String getHighestRankEF() {
        return highestRankEF;
    }

    public void setHighestRankEF(String highestRankEF) {
        this.highestRankEF = highestRankEF;
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

    @RestOut(name = "abstract")
    public String getAbstract() {
        return super.getAbstract();
    }

    public Collection<String> getArrayDesigns() {
        return new TreeSet<String>(Arrays.asList(getPlatform().split(",")));
    }

    public Integer getNumSamples() {
        return (Integer) exptSolrDocument.getFieldValue("numSamples");
    }

    @RestOut(name = "archiveUrl")
    public String getArchiveUrl() {
        return "/data/" + this.getAccession() + ".zip";
    }

    private static String dateToString(Date date) {
        return date == null ? null : (new SimpleDateFormat("dd-MM-yyyy").format(date));
    }

    @RestOut(name = "loaddate")
    public String getLoadDateString() {
        return dateToString(getLoadDate());
    }

    @RestOut(name = "releasedate")
    public String getReleaseDateString() {
        return dateToString(getReleaseDate());
    }

    /**
     * Not yet implemented, always new
     *
     * @return "new"
     */
    @RestOut(name = "status")
    public String getStatus() {
        return "new";
    }
}

