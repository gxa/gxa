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

import ae3.service.GxaS4DasDataSource;
import ae3.service.structuredquery.UpdownCounter;
import uk.ac.ebi.gxa.utils.StringUtil;
import uk.ac.ebi.gxa.utils.EfvTree;
import uk.ac.ebi.gxa.properties.AtlasProperties;

import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: Andrey
 * Date: Aug 28, 2009
 * Time: 9:38:22 AM
 * To change this template use File | Settings | File Templates.
 */

//BRCA2 is differentially expressed in 89 experiments [60up/76dn]: active in 10 organism parts:bone marrow[2up/1dn],skin[1up/1dn],...;
// 22 disease states: normal[1up/6dn],glioblastoma [1up/0dn],...; 23 cell lines, 14 cell types, 10 compaund treatments and 123 other conditions.
public class AtlasGeneDescription {
    final public static int MAX_EFV = 2;
    final public static int MAX_LONG_EF = 2;
    final public static int MAX_EF = 5;

    // Constants used in experimental factor field names in the EB-eye
    // dump to indicate whether a given field should be indexed
    // or displayed.
    private static final String DISPLAYED = "_displayed";
    private static final String INDEXED = "_indexed";

    private String text;
    // Stores mapping ef name -> descriptive text
    // LinkedHashMap because the order of efs is significant
    private Map<String, String> efToDisplayedText = new LinkedHashMap<String, String>();
    private Map<String, String> efToIndexedText = new LinkedHashMap<String, String>();
    private String experimentCountText;
    private Integer totalExperiments;
    private final AtlasProperties atlasProperties;
    private List<EfvTree.EfEfv<UpdownCounter>> efs;

    class Ef {
        class Efv{
            public Efv(String name){
                   this.name = name;
            }
            public String name;
            public String toText(){
                return StringUtil.quoteComma(name);
            }
        }
        List<Efv> efv = new ArrayList<Efv>();
        String Name = null;
        public void addEfv(String name){
            efv.add(new Efv(name));
        }
        public String toLongText(){
            StringBuilder result = new StringBuilder();
            result.append(toShortText()).append(": ").append(getEfvsText(MAX_EFV)).append(";");
            return result.toString();
        }

        /**
         * Add long descriptive text for  this ef to efToDisplayedText map
         */
        public void addEfToDisplayedText() {
            assert (efToDisplayedText != null);
            if (atlasProperties.getDasFactors().contains(this.Name.toLowerCase())) {
                efToDisplayedText.put(this.Name + DISPLAYED, getEfvsText(MAX_EFV));
            }
        }

        /**
         * Add short descriptive text for  this ef to efToIndexedText map
         */
        public void addEfToIndexedText() {
            assert (efToIndexedText != null);
            if (atlasProperties.getDasFactors().contains(this.Name.toLowerCase())) {
                efToIndexedText.put(this.Name + INDEXED, getEfvsText(efv.size()));
            }
        }

        public String toShortText(){
            return efv.size()+" "+ StringUtil.pluralize(StringUtil.decapitalise(atlasProperties.getCuratedEf(this.Name)));
        }

        /**
         *
         * @return descriptive text for all the efvs associated with this ef
         */
        private String getEfvsText(int maxNumber) {
            StringBuilder efvsText = new StringBuilder();
            int i = 0;
            for (Efv v : efv) {
                if (i == 0) {
                    efvsText.append(v.toText());
                } else if (i < maxNumber) {
                    efvsText.append(", ").append(v.toText());
                } else if (i == maxNumber) {
                    String more = (efv.size() - MAX_EFV > 0) ? " ("+(efv.size() - MAX_EFV)+" more)" : "";
                    efvsText.append(", ...").append(more); //semicolon replaces comma after ...
                } else
                    break;
                ++i;
            }
            return efvsText.toString();
        }
    }

    class EfWriter{
        List<Ef> Efs = new ArrayList<Ef>();
        int iEFs = 0;

        public String getCurrentEfName(){
            if(0==Efs.size())
                return null;
            return getCurrentEf().Name;
        }
        public Ef getCurrentEf(){
            if(0==Efs.size())
                return null;

            return Efs.get(Efs.size()-1);
        }
        public void addEfv(EfvTree.EfEfv<UpdownCounter> r){
            getCurrentEf().addEfv(r.getEfv());
        }
        public void addEf(String name){
           Ef ef = new Ef();
           ef.Name = name;
           Efs.add(ef);
        }
        public String toText(){
            String result = "";
            int iEfs = 0;
            int otherFactors = 0; //not shown
            for(Ef ef:Efs){
                // Store ef for inclusion in the EB-ebeye dump file
                // All gene's ef's should be indexed by EB-eye
                ef.addEfToIndexedText();
                if (iEfs < MAX_EF) {
                    // EB-eye search result screen should display max MAX_EF
                    // ef's per gene entry
                    ef.addEfToDisplayedText();
                }
                // Now store info for inclusion on the gene page
                if(iEfs<MAX_LONG_EF){ //2 long EFs
                    if((!result.endsWith(","))&&(!(result.endsWith(";")))&&(!(result.endsWith(":"))&&(result.length()>0)))
                              result+=",";

                    if(!result.endsWith(" "))
                        result += " ";

                    result += ef.toLongText();
                }
                else if(iEfs<MAX_EF){ //5 efs total
                    if((!result.endsWith(","))&&(!(result.endsWith(";")))&&(!(result.endsWith(":"))&&(result.length()>0)))
                              result+=",";

                    if(!result.endsWith(" "))
                        result += " ";

                    result += ef.toShortText();                                  }
                else{
                    ++otherFactors;
                }
                ++iEfs;
            }
            if(0!=otherFactors){
                result += " and "+otherFactors+" other conditions.";
                // Generic text informing the EB-eye viewwer that some
                // other factors that are not shown are present
                efToDisplayedText.put("otherconditions_displayed", "... ("+otherFactors+" more)");
            }
            return result;
        }

        private int totalExperiments;

        public void setTotalExperiments(int totalExperiments){
                this.totalExperiments = totalExperiments;
        }

        public int getTotalExperiments(){
            return totalExperiments;
        }
    }

    /**
     * constructor
     *
     * @param   gene    <STRONG>must</STRONG> be initialized
     */
    public AtlasGeneDescription(AtlasProperties atlasProp, AtlasGene gene){

        this.atlasProperties = atlasProp;

        efs = gene.getHeatMap(atlasProp.getGeneHeatmapIgnoredEfs()).getNameSortedList();

        Collections.sort(efs, new Comparator<EfvTree.EfEfv<UpdownCounter>>() {
            public int compare(EfvTree.EfEfv<UpdownCounter> o1, EfvTree.EfEfv<UpdownCounter> o2) {
                int result;

                String Ef1 = atlasProperties.getCuratedEf(o1.getEf());
                String Ef2 = atlasProperties.getCuratedEf(o2.getEf());

                result = GxaS4DasDataSource.SortOrd(Ef1) - GxaS4DasDataSource.SortOrd(Ef2);

                if(0==result)
                    result = Ef1.compareTo(Ef2);

                if(0==result)
                    result = (o2.getPayload().getNoStudies()) - (o1.getPayload().getNoStudies());

                return result;
            }
        });


        EfWriter writer = new EfWriter();

        writer.setTotalExperiments(gene.getNumberOfExperiments());

        for(EfvTree.EfEfv<UpdownCounter> r : efs){
           if(r.getEf().equals(writer.getCurrentEfName())){
               writer.addEfv(r);
           }
           else{
               writer.addEf(r.getEf());
               writer.addEfv(r);
           }
        }

        text = writer.toText();

        //sometimes ", ...;"  appears at the end of the description
        text = StringUtil.replaceLast(text,"...;","...");
        text = StringUtil.replaceLast(text," ...","...");

        experimentCountText = gene.getGeneName() + " is differentially expressed in " + writer.getTotalExperiments() + " experiments";
        totalExperiments = writer.getTotalExperiments();
        //&lt;a href="http://www.ebi.ac.uk/gxa">expressed&lt;/a> - was a test for ensemble portal
        text = experimentCountText + ": " + text;
        //text += "<a href=\"http://www.ebi.ac.uk/gxa\">.</a>";
    }


    /**
     *
     * @return human readable gene description, like "BRCA2 is differentially expressed in 91 experiments [93 up/91 dn]:  15 organism parts: kidney [0 up/2 dn], bone marrow [2 up/0 dn], ...; 21 disease states: normal [1 up/6 dn], glioblastoma [1 up/0 dn], ...; 21 cell types, 28 cell lines, 19 compound treatments and 17 other conditions."
     */
    @Override
    public String toString(){
        return text;
    }

    /**
     * @return efToText Map: Experimental factor name -> descriptive text, for the purpose of
     *         Lucene indexing in EB-eye
     */
    public Map<String, String> getEfToIndexedText() {
        return efToIndexedText;
    }

    /**
     * @return efToText Map: Experimental factor name -> descriptive text, for the purpose of
     *         displaying on the EB-eye search result page
     */
    public Map<String, String> getEfToDisplayedText() {
        return efToDisplayedText;
    }
    
    /**
     * @return totalExperiments the total number of experiments in which
     * this gene was differentially expressed in.
     */
    public Integer getTotalExperiments() {
        return totalExperiments;
    }

    public String toStringExperimentCount(){
        return experimentCountText;
    }
}