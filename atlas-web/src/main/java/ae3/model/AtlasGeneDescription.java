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

import ae3.service.AtlasStatisticsQueryService;
import ae3.service.structuredquery.UpdownCounter;
import uk.ac.ebi.gxa.properties.AtlasProperties;
import uk.ac.ebi.gxa.utils.EfvTree;
import uk.ac.ebi.gxa.utils.StringUtil;

import java.util.*;

//BRCA2 is differentially expressed in 89 experiments [60up/76dn]: active in 10 organism parts:bone marrow[2up/1dn],skin[1up/1dn],...;
// 22 disease states: normal[1up/6dn],glioblastoma [1up/0dn],...; 23 cell lines, 14 cell types, 10 compaund treatments and 123 other conditions.

public class AtlasGeneDescription {
    final public static int MAX_EFV = 2;
    final public static int MAX_LONG_EF = 2;
    final public static int MAX_EF = 5;
    private static final String PIPE = "|";

    // This constant is used to prevent empty efvs from being displayed in EB-eye dumps (cf. SDRFWritingUtils)
    private static final String EMTPY_EFV = "(empty)";

    private String text;
    // Stores mapping ef name -> descriptive text
    // LinkedHashMap because the order of efs is significant
    private Map<String, String> efToEbeyeDumpText = new LinkedHashMap<String, String>();
    private String experimentCountText;
    private Integer totalExperiments;
    private final AtlasProperties atlasProperties;

    private static int sortOrder(String val) {
        if (val.equalsIgnoreCase("gene")) {
            return 1;
        } else if (val.equalsIgnoreCase("organism part")) {
            return 2;
        } else if (val.equalsIgnoreCase("disease state")) {
            return 3;
        } else if (val.equalsIgnoreCase("cell type")) {
            return 4;
        } else if (val.equalsIgnoreCase("cell line")) {
            return 5;
        } else if (val.equalsIgnoreCase("compound treatment")) {
            return 6;
        } else if (val.equalsIgnoreCase("experiment")) {
            return 8;
        } else {
            return 7;
        }
    }

    class Ef {
        class Efv {
            public Efv(String name) {
                this.name = name;
            }

            public String name;

            public String toText() {
                return StringUtil.quoteComma(name);
            }
        }

        List<Efv> efv = new ArrayList<Efv>();
        String name = null;

        public void addEfv(String name) {
            efv.add(new Efv(name));
        }

        public String toLongText() {
            StringBuilder result = new StringBuilder();
            result.append(toShortText()).append(": ").append(getEfvsText(MAX_EFV)).append(";");
            return result.toString();
        }

        /**
         * Add short descriptive text for  this ef to efToEbeyeDumpText map
         */
        public void addefToEbeyeDumpText() {
            assert (efToEbeyeDumpText != null);
            if (atlasProperties.getDasFactors().contains(this.name.toLowerCase())) {
                efToEbeyeDumpText.put(this.name, getEfvsText(efv.size(), PIPE));
            }
        }

        public String toShortText() {
            return efv.size() + " " + StringUtil.pluralize(StringUtil.decapitalise(atlasProperties.getCuratedEf(this.name)));
        }

        /**
         * @return descriptive text for all the efvs associated with this ef
         */
        private String getEfvsText(int maxNumber) {
            return getEfvsText(maxNumber, ", ");
        }

        /**
         * @return descriptive text for all the efvs associated with this ef
         */
        private String getEfvsText(int maxNumber, String efvSeparator) {
            StringBuilder efvsText = new StringBuilder();
            int i = 0;
            for (Efv v : efv) {
                if (!EMTPY_EFV.equals(v.name)) { // Exclude empty efvs
                    if (i == 0) {
                        efvsText.append(v.toText());
                    } else if (i < maxNumber) {
                        efvsText.append(efvSeparator).append(v.toText());
                    } else if (i == maxNumber) {
                        String more = (efv.size() - MAX_EFV > 0) ? " (" + (efv.size() - MAX_EFV) + " more)" : "";
                        efvsText.append(efvSeparator).append(" ...").append(more); //semicolon replaces comma after ...
                    } else {
                        break;
                    }
                    ++i;
                }
            }
            return efvsText.toString();
        }
    }

    class EfWriter {
        List<Ef> efs = new ArrayList<Ef>();

        public String getCurrentEfName() {
            return 0 == efs.size() ? null : getCurrentEf().name;
        }

        public Ef getCurrentEf() {
            if (0 == efs.size())
                return null;

            return efs.get(efs.size() - 1);
        }

        public void addEfv(EfvTree.EfEfv<UpdownCounter> r) {
            getCurrentEf().addEfv(r.getEfv());
        }

        public void addEf(String name) {
            Ef ef = new Ef();
            ef.name = name;
            efs.add(ef);
        }

        public String toText() {
            StringBuilder result = new StringBuilder();
            int iEfs = 0;
            for (Ef ef : efs) {
                // Store ef for inclusion in the EB-ebeye dump file
                // All gene's ef's should be indexed by EB-eye
                ef.addefToEbeyeDumpText();
                // Now store info for inclusion on the gene page
                if (iEfs < MAX_LONG_EF) { //2 long EFs
                    appendCommaIfNecessary(result);
                    result.append(ef.toLongText());
                } else if (iEfs < MAX_EF) { //5 efs total
                    appendCommaIfNecessary(result);
                    result.append(ef.toShortText());
                }
                ++iEfs;
            }
            if (efs.size() > MAX_EF) {
                result.append(" and ").append(efs.size() - MAX_EF).append(" other conditions.");
                // Generic text informing the EB-eye viewer that some
                // other factors that are not shown are present
                efToEbeyeDumpText.put("otherconditions", String.valueOf(efs.size() - MAX_EF));
            }
            return result.toString();
        }

        private void appendCommaIfNecessary(StringBuilder result) {
            if (result.length() == 0)
                return;

            if (",;:".indexOf(lastChar(result)) >= 0)
                result.append(",");

            if (lastChar(result) != ' ')
                result.append(" ");
        }

        private char lastChar(CharSequence result) {
            return result.charAt(result.length() - 1);
        }

        private int totalExperiments;

        public void setTotalExperiments(int totalExperiments) {
            this.totalExperiments = totalExperiments;
        }

        public int getTotalExperiments() {
            return totalExperiments;
        }
    }

    /**
     * constructor
     *
     * @param gene <STRONG>must</STRONG> be initialized
     */
    public AtlasGeneDescription(AtlasProperties atlasProp, AtlasGene gene, AtlasStatisticsQueryService atlasStatisticsQueryService) {

        this.atlasProperties = atlasProp;

        // TODO Replace null with atlasStatisticsQueryService
        List<EfvTree.EfEfv<UpdownCounter>> efs = gene.getHeatMap(atlasProp.getGeneHeatmapIgnoredEfs(), atlasStatisticsQueryService).getNameSortedList();

        Collections.sort(efs, new Comparator<EfvTree.EfEfv<UpdownCounter>>() {
            public int compare(EfvTree.EfEfv<UpdownCounter> o1, EfvTree.EfEfv<UpdownCounter> o2) {
                int result;

                String ef1 = atlasProperties.getCuratedEf(o1.getEf());
                String ef2 = atlasProperties.getCuratedEf(o2.getEf());

                result = sortOrder(ef1) - sortOrder(ef2);

                if (0 == result)
                    result = ef1.compareTo(ef2);

                if (0 == result)
                    result = o2.getPayload().getNoStudies() - o1.getPayload().getNoStudies();

                return result;
            }
        });


        EfWriter writer = new EfWriter();

        writer.setTotalExperiments(gene.getNumberOfExperiments());

        for (EfvTree.EfEfv<UpdownCounter> r : efs) {
            if (r.getEf().equals(writer.getCurrentEfName())) {
                writer.addEfv(r);
            } else {
                writer.addEf(r.getEf());
                writer.addEfv(r);
            }
        }

        text = writer.toText();

        //sometimes ", ...;"  appears at the end of the description
        text = StringUtil.replaceLast(text, "...;", "...");
        text = StringUtil.replaceLast(text, " ...", "...");

        totalExperiments = writer.getTotalExperiments();
        experimentCountText = gene.getGeneName() + " is differentially expressed in " + totalExperiments + " experiment" +
                (totalExperiments > 1 ? "s" : "");
        //&lt;a href="http://www.ebi.ac.uk/gxa">expressed&lt;/a> - was a test for ensemble portal
        text = experimentCountText + ": " + text;
        //text += "<a href=\"http://www.ebi.ac.uk/gxa\">.</a>";
    }


    /**
     * @return human readable gene description, like "BRCA2 is differentially expressed in 91 experiments [93 up/91 dn]:  15 organism parts: kidney [0 up/2 dn], bone marrow [2 up/0 dn], ...; 21 disease states: normal [1 up/6 dn], glioblastoma [1 up/0 dn], ...; 21 cell types, 28 cell lines, 19 compound treatments and 17 other conditions."
     */
    @Override
    public String toString() {
        return text;
    }

    /**
     * @return efToText Map: Experimental factor name -> descriptive text, for the purpose of
     *         Lucene indexing in EB-eye
     */
    public Map<String, String> getEfToEbeyeDumpText() {
        return efToEbeyeDumpText;
    }


    /**
     * @return totalExperiments the total number of experiments in which
     *         this gene was differentially expressed in.
     */
    public Integer getTotalExperiments() {
        return totalExperiments;
    }

    public String toStringExperimentCount() {
        return experimentCountText;
    }
}