package  uk.ac.ebi.gxa.model;

/**
 * Query to retrieve object by accession or id - uniform way for quick retrieval of single object
 */
public class AccessionQuery<T> {
    public AccessionQuery(){
    }
    
    public AccessionQuery(AccessionQuery value){
      this.id = value.id;
      this.accession = value.accession;
    }
    
    private String id;
    public String getId(){
        return this.id;
    }
    public T hasId(String id){
        this.id = id;
        return (T)this;
    }

    private String accession;
    public String getAccession(){
        return this.accession;
    }
    public T hasAccession(String accession){
        this.accession = accession;
        return (T)this;
    }
}
