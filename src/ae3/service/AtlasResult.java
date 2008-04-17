package ae3.service;

import ae3.model.AtlasExperiment;
import ae3.model.AtlasGene;
import ae3.model.AtlasTuple;

import java.util.HashMap;

/**
 * Created by IntelliJ IDEA.
* User: ostolop
* Date: 06-Apr-2008
* Time: 18:57:56
* To change this template use File | Settings | File Templates.
*/
public class AtlasResult {
    private AtlasExperiment experiment;
    private AtlasGene gene;
    private AtlasTuple atuple;

    public void setExperiment(AtlasExperiment experiment) {
        this.experiment = experiment;
    }

    public AtlasExperiment getExperiment() {
        return experiment;
    }

    public AtlasGene getGene() {
        return gene;
    }

    public void setGene(AtlasGene gene) {
        this.gene = gene;
    }

    public AtlasTuple getAtuple() {
        return atuple;
    }

    public void setAtuple(AtlasTuple atuple) {
        this.atuple = atuple;
    }
}
