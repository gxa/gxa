package  uk.ac.ebi.gxa.model;

/**
 * Query object for ArrayDesign object; features hasName(), hasType() and hasProvider() methods. 
 * User: Andrey
 * Date: Oct 21, 2009
 * Time: 2:07:14 PM
 * To change this template use File | Settings | File Templates.
 */
public class ArrayDesignQuery extends AccessionQuery<ArrayDesignQuery> {

    private String name;
    public String getName(){
        return name;
    }
    public ArrayDesignQuery hasName(String name){
        this.name = name;
        return this;
    }

    private String type;
    public String getType(){
        return type;
    }
    public ArrayDesignQuery hasType(String type){
        this.type = type;
        return this;
    }

    private String provider;
    public String getProvider(){
        return provider;
    }
    public ArrayDesignQuery hasProvider(String provider){
        this.provider = provider;
        return this;
    }

}