package ae3.model;

import ae3.service.structuredquery.UpdownCounter;
import uk.ac.ebi.gxa.utils.EfvTree;

import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: Andrey
 * Date: Jun 25, 2010
 * Time: 10:43:15 AM
 * To change this template use File | Settings | File Templates.
 */
public class ExperimentalFactor {
    public int RESULT_ALL_VALUES_SIZE = 6;
    private AtlasGene gene;
    private Collection<String> omittedEfs;
    private String name;
    private List<String> experiments;
    private HashMap<Long,String> experimentAccessions;

    public ExperimentalFactor(AtlasGene gene, String name, Collection<String> omittedEfs){
        this.name = name;
        this.gene = gene;
        this.omittedEfs = omittedEfs;
    } 

    public String getName(){
        return this.name;
    }

    public List<EfvTree.EfEfv<UpdownCounter>> getValues(){
        //ArrayList<ExperimentalFactorValue> result = new ArrayList<ExperimentalFactorValue>();

        //result.add(new ExperimentalFactorValue());

        List<EfvTree.EfEfv<UpdownCounter>> result = new ArrayList<EfvTree.EfEfv<UpdownCounter>>();

        for(EfvTree.EfEfv<UpdownCounter> f : gene.getHeatMap(this.name, omittedEfs).getNameSortedList()){
            if(f.getEf().equals(this.name)){
                result.add(f);
            }
        }

        return result; //gene.getHeatMap(this.name, omittedEfs).getValueSortedList();
    }

    public List<EfvTree.EfEfv<UpdownCounter>> getTopValues(){
        List<EfvTree.EfEfv<UpdownCounter>> result = new ArrayList<EfvTree.EfEfv<UpdownCounter>>();

        for(EfvTree.EfEfv<UpdownCounter> f : gene.getHeatMap(this.name, omittedEfs).getNameSortedList()){
            if(f.getEf().equals(this.name)){
                if(result.size()<RESULT_ALL_VALUES_SIZE){
                    result.add(f);
                }
            }
        }
        return result; //gene.getHeatMap(this.name, omittedEfs).getValueSortedList();
    }

    public int getMoreValuesCount(){
        //store overspill count (if slow).
        return getValues().size()-getTopValues().size();
    }

    public Collection<String> getExperiments(){
        return experimentAccessions.values();
    }

    //public void setExperiments(List<String> experiments){
    //    this.experiments = experiments;
    //}

    public void addExperiment(Long id, String Accession){
        if(experimentAccessions==null)
            experimentAccessions=new HashMap<Long,String>();

        experimentAccessions.put(id,Accession);
    }

    public Map<Long,String> getExperimentAccessions(){
        return experimentAccessions;
    }
}
