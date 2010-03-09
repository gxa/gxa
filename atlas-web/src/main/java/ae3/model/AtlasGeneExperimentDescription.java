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

import uk.ac.ebi.gxa.utils.StringUtil;
import uk.ac.ebi.gxa.properties.AtlasProperties;
import uk.ac.ebi.microarray.atlas.model.ExpressionAnalysis;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: Andrey
 * Date: Sep 1, 2009
 * Time: 4:50:02 PM
 * To change this template use File | Settings | File Templates.
 */
public class AtlasGeneExperimentDescription {
    final int MAX_EXP_FACTOR_VALUES = 3;

    private AtlasGene atlasGene;
    private AtlasExperiment atlasExperiment;
    private AtlasProperties atlasProperties;
    private Boolean overExpressed;

    public EfWriter writer = null;

    class EfWriter{
        List<Ef> efs = new ArrayList<Ef>();

        public String getCurrentEfName(){
            if(0==efs.size())
                return null;
            return getCurrentEf().Name;
        }
        public Ef getCurrentEf(){
            if(0==efs.size())
                return null;

            return efs.get(efs.size()-1);
        }
        public void AddEfv(ExpressionAnalysis r){
            getCurrentEf().AddEfv(r.getEfvName(), r.getTStatistic() > 0 );
        }
        public void AddEf(String name){
           Ef ef = new Ef();
           ef.Name = name;
           efs.add(ef);
        }
    }

    public class Ef {
            class Efv {
                public Efv(String name, Boolean up){
                       Name = name;
                       Up = up;
                }
                public String Name;
                public Boolean Up;
            }
            List<Efv> efv = new ArrayList<Efv>();
            String Name = null;
            public void AddEfv(String name, Boolean up){
                efv.add(new Efv(name,up));
            }
            public String LongText(){

                String result = "";

                if(efv.size()<2)
                 result+= StringUtil.decapitalise(atlasProperties.getCuratedEf(Name));
                else
                 result+= StringUtil.pluralize(StringUtil.decapitalise(atlasProperties.getCuratedEf(Name)));

                boolean first = true;

                for(Efv v:efv){
                    if(!first)
                        result += ", ";

                    first = false;

                    if(!result.endsWith(" "))
                        result += " ";

                    result+= StringUtil.quoteComma(v.Name);
                    result+=" ";
                    result+=v.Up ? "[up]" : "[dn]" ;
                    //result+=" ";
                }

                result += "; ";

                return result;
            }
        }

    public AtlasGeneExperimentDescription(AtlasProperties atlasProperties, AtlasGene atlasGene, AtlasExperiment atlasExperiment, Boolean overExpressed) throws Exception{
        this.atlasProperties = atlasProperties;
        this.atlasGene = atlasGene;
          this.atlasExperiment = atlasExperiment;
          this.overExpressed = overExpressed;

        writer = new EfWriter();

        if(null==atlasGene)
            throw new Exception("null atlasGene passed to AtlasGeneExperimentDescription");

        if(null==atlasExperiment)
            throw new Exception("null atlasExperiment passed to AtlasGeneExperimentDescription");

        if(null==atlasExperiment.getId())
            throw new Exception("can not get experimentid for geneid="+ atlasGene.getGeneId());

        if(null==atlasGene.getAtlasResultsForExperiment(atlasExperiment.getId()))
            throw new Exception("getAtlasResultsForExperiment returns null for geneid="+ atlasGene.getGeneId());

        for(ExpressionAnalysis r : atlasGene.getAtlasResultsForExperiment(atlasExperiment.getId())){
           if(r.getEfName().equals(writer.getCurrentEfName())){
               writer.AddEfv(r);
           }
           else{
               writer.AddEf(r.getEfName());
               writer.AddEfv(r);
           }
        }
    }

    /*
    *  @returns human-readable description like "E-GEOD-803 "Transcription profiling of human normal tissue" versus bone marrow [up], kidney [dn], thymus [up] organism parts;"
    */
    public String toShortString(){
        String result = "";

        result += overExpressed ? "over-expressed in" : "under-expressed in";

        result += " " + atlasExperiment.getAccession() + " \"" + atlasExperiment.getDescription() + "\"";

        /*
        HashMap<String, String> hhm = atlasExperiment.getHighestRankEFs();
        */

        result += " versus ";

        String HighestRankExperimentalFactor = atlasGene.getHighestRankEF(atlasExperiment.getId()).getFirst();



        /* this can be used to display all (including neutral) factor values
        for(String s : atlasGene.getAllFactorValues(HighestRankExperimentalFactor)){
                notes += s;
                notes += " ";
                //notes += hhm.get(s);
        }*/


        int iCount = 0;

        for(ExpressionAnalysis la : atlasGene.getAtlasResultsForExperiment(atlasExperiment.getId())){
            if(la.getEfName().equals(HighestRankExperimentalFactor)){
                if(iCount<=MAX_EXP_FACTOR_VALUES)
                    result += (StringUtil.quoteComma(la.getEfvName()) + (la.isUp() ? " [up]" : " [dn]") + ", ");

                iCount++;
            }
        }

        result = StringUtil.replaceLast(result,", ", "");

        if(iCount>MAX_EXP_FACTOR_VALUES)
            result += " and "+ (iCount-MAX_EXP_FACTOR_VALUES) + " other ";

        if(iCount==1)
            result += " " + StringUtil.decapitalise(atlasProperties.getCuratedEf(HighestRankExperimentalFactor));
        else
            result += " " + StringUtil.pluralize(StringUtil.decapitalise(atlasProperties.getCuratedEf(HighestRankExperimentalFactor)));

        return result;
    }

    /**
     *  @returns human-readable description like "Transcription profiling of human  Acute Lymphoblastic Leukemia CEM_C1 cells treated with rapamycin identifies rapamycin as a glucocorticoid resistance reversal agent. This experiment has 5 factors: clinical info, compound treatment, dose, phenotype, time. BRCA2 activity in clinical info pretreatment primary sample [dn]; compound treatments dimethyl sulfoxide [up], none [dn], rapamycin [up]; dose 10 nM [up]; phenotypes glucocorticoid resistant [dn], glucocorticoid sensitive [dn]; times 3 h [up], control [up]."
    */

    public String toLongString(){

        String result = atlasExperiment.getDescription();

        if(!result.endsWith("."))
            result += ".";    

        result += " This experiment has "+ this.writer.efs.size() + (this.writer.efs.size() == 1 ? " factor: " : " factors: ");

        for(Ef f: this.writer.efs){
            if(!result.endsWith(": "))
                result += ", ";

            result += StringUtil.decapitalise(atlasProperties.getCuratedEf(f.Name));
        }

        result += ". ";

        result += "" + atlasGene.getGeneName() + " activity in ";

        for(Ef f: this.writer.efs){
            result+= f.LongText();
        }

        result = StringUtil.replaceLast(result,", ",".");
        result = StringUtil.replaceLast(result,"; ",".");

        return result;
    }
}
