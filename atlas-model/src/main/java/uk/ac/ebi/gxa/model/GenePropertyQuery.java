package uk.ac.ebi.gxa.model;

/**
 * Created by IntelliJ IDEA.
 * User: Andrey
 * Date: Nov 10, 2009
 * Time: 1:57:32 PM
 * To change this template use File | Settings | File Templates.
 */
public class GenePropertyQuery extends AbstractPropertyQuery<GenePropertyQuery> {
    public GenePropertyQuery(){};

    public GenePropertyQuery(AccessionQuery accessionQuery){
        super(accessionQuery);
    }
}
