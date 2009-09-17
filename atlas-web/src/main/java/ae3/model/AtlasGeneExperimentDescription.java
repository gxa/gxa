package ae3.model;

import uk.ac.ebi.ae3.indexbuilder.Experiment;
import ae3.util.CuratedTexts;
import ae3.util.StringUtils;

import java.util.List;
import java.util.ArrayList;

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
        public void AddEfv(Experiment r){
            getCurrentEf().AddEfv(r.getEfv(), r.getExpression().isUp() );
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
                 result+= StringUtils.decapitalise(CuratedTexts.get("head.ef." + Name));
                else
                 result+= StringUtils.pluralize(StringUtils.decapitalise(CuratedTexts.get("head.ef." + Name)));

                boolean first = true;

                for(Efv v:efv){
                    if(!first)
                        result += ", ";

                    first = false;

                    if(!result.endsWith(" "))
                        result += " ";

                    result+=StringUtils.quoteComma(v.Name);
                    result+=" ";
                    result+=v.Up ? "[up]" : "[dn]" ;
                    //result+=" ";
                }

                result += "; ";

                return result;
            }
        }

    public AtlasGeneExperimentDescription(AtlasGene atlasGene, AtlasExperiment atlasExperiment, Boolean overExpressed) throws Exception{
          this.atlasGene = atlasGene;
          this.atlasExperiment = atlasExperiment;
          this.overExpressed = overExpressed;

        writer = new EfWriter();

        if(null==atlasGene)
            throw new Exception("null atlasGene passed to AtlasGeneExperimentDescription");

        if(null==atlasExperiment)
            throw new Exception("null atlasExperiment passed to AtlasGeneExperimentDescription");

        if(null==atlasExperiment.getDwExpId())
            throw new Exception("can not get experimentid for geneid="+ atlasGene.getGeneId());

        if(null==atlasGene.getAtlasResultsForExperiment(atlasExperiment.getDwExpId()))
            throw new Exception("getAtlasResultsForExperiment returns null for geneid="+ atlasGene.getGeneId());

        for(Experiment r : atlasGene.getAtlasResultsForExperiment(atlasExperiment.getDwExpId())){
           if(r.getEf().equals(writer.getCurrentEfName())){
               writer.AddEfv(r);
           }
           else{
               writer.AddEf(r.getEf());
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

        result += " " + atlasExperiment.getDwExpAccession() + " \"" + atlasExperiment.getDwExpDescription() + "\"";

        /*
        HashMap<String, String> hhm = atlasExperiment.getHighestRankEFs();
        */

        result += " versus ";

        String HighestRankExperimentalFactor = atlasGene.getHighestRankEF(atlasExperiment.getDwExpId()).getFirst();



        /* this can be used to display all (including neutral) factor values
        for(String s : atlasGene.getAllFactorValues(HighestRankExperimentalFactor)){
                notes += s;
                notes += " ";
                //notes += hhm.get(s);
        }*/


        int iCount = 0;

        for(Experiment la : atlasGene.getAtlasResultsForExperiment(atlasExperiment.getDwExpId())){
            if(la.getEf().equals(HighestRankExperimentalFactor)){
                if(iCount<=MAX_EXP_FACTOR_VALUES)
                    result += (StringUtils.quoteComma(la.getEfv()) + (la.getExpression().isUp() ? " [up]" : " [dn]") + ", ");

                iCount++;
            }
        }

        result = StringUtils.ReplaceLast(result,", ", "");

        if(iCount>MAX_EXP_FACTOR_VALUES)
            result += " and "+ (iCount-MAX_EXP_FACTOR_VALUES) + " other ";

        if(iCount==1)
            result += " " + StringUtils.decapitalise(CuratedTexts.get("head.ef." + HighestRankExperimentalFactor));
        else
            result += " " + StringUtils.pluralize(StringUtils.decapitalise(CuratedTexts.get("head.ef." + HighestRankExperimentalFactor)));

        return result;
    }

    /**
     *  @returns human-readable description like "Transcription profiling of human  Acute Lymphoblastic Leukemia CEM_C1 cells treated with rapamycin identifies rapamycin as a glucocorticoid resistance reversal agent. This experiment has 5 factors: clinical info, compound treatment, dose, phenotype, time. BRCA2 activity in clinical info pretreatment primary sample [dn]; compound treatments dimethyl sulfoxide [up], none [dn], rapamycin [up]; dose 10 nM [up]; phenotypes glucocorticoid resistant [dn], glucocorticoid sensitive [dn]; times 3 h [up], control [up]."
    */

    public String toLongString(){

        String result = atlasExperiment.getDwExpDescription();

        if(!result.endsWith("."))
            result += ".";    

        result += " This experiment has "+ this.writer.efs.size() + (this.writer.efs.size() == 1 ? " factor: " : " factors: ");

        for(Ef f: this.writer.efs){
            if(!result.endsWith(": "))
                result += ", ";

            result += StringUtils.decapitalise(CuratedTexts.get("head.ef." + f.Name));
        }

        result += ". ";

        result += "" + atlasGene.getGeneName() + " activity in ";

        for(Ef f: this.writer.efs){
            result+= f.LongText();
        }

        result = StringUtils.ReplaceLast(result,", ",".");
        result = StringUtils.ReplaceLast(result,"; ",".");

        return result;
    }
}
