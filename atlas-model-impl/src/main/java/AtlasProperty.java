import uk.ac.ebi.gxa.model.Property;

import java.util.Collection;

/**
 * Created by IntelliJ IDEA.
 * User: Andrey
 * Date: Oct 30, 2009
 * Time: 11:31:16 AM
 * To change this template use File | Settings | File Templates.
 */
public class AtlasProperty implements Property {

    private String name;
    private Collection<String> values;
    private String accession;
    private int id;

    public String getName(){
        return name;
    }
    public void setName(String name){
        this.name = name;
    }

    public Collection<String> getValues(){
        return values;
    }
    public void setValues(Collection<String> values){
        this.values = values;
    }


    public String getAccession(){
        return accession;
    }
    public void setAccession(String accession){
        this.accession = accession;
    }

    public int getId(){
        return id;
    }
    public void setid(int id){
        this.id = id;
    }


}
