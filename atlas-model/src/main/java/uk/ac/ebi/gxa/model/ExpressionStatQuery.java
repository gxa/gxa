package  uk.ac.ebi.gxa.model;

/**
 * Created by IntelliJ IDEA.
 * User: Andrey
 * Date: Oct 22, 2009
 * Time: 11:47:28 AM
 * To change this template use File | Settings | File Templates.
 */
public class ExpressionStatQuery {
    public enum ExpressionQuery{
        up,down,upordown;
    }

    public ExpressionStatQuery hasGene(GeneQuery geneQuery){
        return this;
    }
    public ExpressionStatQuery hasProperty(PropertyQuery propertyQuery){
        return this;
    }

    public ExpressionStatQuery activeIn(ExpressionQuery expression, PropertyQuery property){
        return this;
    };

}
