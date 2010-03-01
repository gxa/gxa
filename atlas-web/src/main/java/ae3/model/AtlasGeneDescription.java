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

package ae3.model;

import ae3.service.GxaDasDataSource;
import ae3.util.CuratedTexts;
import uk.ac.ebi.gxa.utils.StringUtil;
import uk.ac.ebi.microarray.atlas.model.ExpressionAnalysis;

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

    private String text;

    class Ef {
        class Efv{
            public Efv(String name, int up, int dn){
                   this.name = name;
                   this.up = up;
                   this.dn = dn;
            }
            public String name;
            public int up;
            public int dn;
            public String toText(){
                return "" + StringUtil.quoteComma(name) + " [" + up + " up/" + dn +" dn]";
            }
        }
        List<Efv> efv = new ArrayList<Efv>();
        String Name = null;
        public void addEfv(String name, int up, int dn){
            efv.add(new Efv(name,up,dn));
        }
        public String toLongText(){
            String result = toShortText()+": ";

            int i = 0;
            for(Efv v:efv){
                 if (i==0){
                     result += v.toText();
                 }
                 else if(i<MAX_EFV){
                     result += ", " + v.toText();
                 }
                 else if(i==MAX_EFV){
                     result += ", ...;"; //semicolon replaces comma after ...
                 }
                 else
                     break;
                ++i;
            }
            return result;
        }
        public String toShortText(){
            return "" + efv.size()+" "+ StringUtil.pluralize(StringUtil.decapitalise(CuratedTexts.get("head.ef." + this.Name)));
        }
    }

    class EfWriter{
        List<Ef> Efs = new ArrayList<Ef>();
        int iEFs = 0;

        public int totalUp =0;
        public int totalDn =0;

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
        public void addEfv(ListResultRow r){
            getCurrentEf().addEfv(r.getFv(), r.getCount_up(), r.getCount_dn());
            totalUp += r.getCount_up();
            totalDn += r.getCount_dn();
        }
        public void addEf(String name){
           Ef ef = new Ef();
           ef.Name = name;
           Efs.add(ef);
        }
        public String toText(){
            String result = "";
            int iEfs = 0;
            int OtherFactors = 0; //not shown
            for(Ef ef:Efs){
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

                    result += ef.toShortText();
                }
                else{
                    ++OtherFactors;
                }
                ++iEfs;
            }
            if(0!=OtherFactors){
                result += " and "+OtherFactors+" other conditions.";
            }
            return result;
        }
        public int getTotalUp(){
            return totalUp;
        }
        public int getTotalDn(){
            return totalDn;
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
    public AtlasGeneDescription(AtlasGene gene){

        List<ListResultRow> efs = gene.getHeatMapRows();

        Collections.sort(efs, new Comparator<ListResultRow>() {
            public int compare(ListResultRow o1, ListResultRow o2) {
                int result;

                String Ef1 = CuratedTexts.get("head.ef." + o1.getEf());
                String Ef2 = CuratedTexts.get("head.ef." + o2.getEf());

                result = GxaDasDataSource.SortOrd(Ef1) - GxaDasDataSource.SortOrd(Ef2);

                if(0==result)
                    result = Ef1.compareTo(Ef2);

                if(0==result)
                    result = (o2.getCount_dn() + o2.getCount_up()) - (o1.getCount_dn() + o1.getCount_up());

                return result;
            }
        });


        EfWriter writer = new EfWriter();

        writer.setTotalExperiments(gene.getNumberOfExperiments());

        for(ListResultRow r : efs){
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

        //&lt;a href="http://www.ebi.ac.uk/gxa">expressed&lt;/a> - was a test for ensemble portal
        text = gene.getGeneName() + " is differentially expressed in " + writer.getTotalExperiments() + " experiments [" + writer.getTotalUp()+" up/" +writer.getTotalDn() + " dn]: " + text;
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
}