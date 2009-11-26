package uk.ac.ebi.gxa.model.impl;

import uk.ac.ebi.gxa.model.Gene;
import uk.ac.ebi.gxa.model.PropertyCollection;

/**
 * Created by IntelliJ IDEA.
 * User: Andrey
 * Date: Nov 26, 2009
 * Time: 11:31:59 AM
 * To change this template use File | Settings | File Templates.
 */
public class AtlasGene implements Gene {

    private String accession;
    private int id;
    private PropertyCollection properties;

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

    public PropertyCollection getProperties(){
        return this.properties;
    }
    public void setProperties(PropertyCollection properties){
        this.properties = properties;
    }

    public String getSpecies(){
        return null;
    }
}
