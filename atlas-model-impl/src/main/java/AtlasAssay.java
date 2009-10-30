import uk.ac.ebi.gxa.model.Assay;
import uk.ac.ebi.gxa.model.PropertyCollection;
import uk.ac.ebi.microarray.atlas.model.Property;

import java.util.Collection;

/**
 * Created by IntelliJ IDEA.
 * User: Andrey
 * Date: Oct 30, 2009
 * Time: 10:56:13 AM
 * To change this template use File | Settings | File Templates.
 */
public class AtlasAssay implements Assay {

    private String experimentAccession;
    private String accession;
    private int id;
    private Collection<String> sampleAccessions;
    private PropertyCollection properties;

    public String getExperimentAccession(){
        return experimentAccession;
    }
    public void setExperimentAccession(String experimentAccession){
        this.experimentAccession = experimentAccession;
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

    public Collection<String> getSampleAccessions(){
        return this.sampleAccessions;
    }
    public void setSampleAccessions(Collection<String> sampleAccessions){
        this.sampleAccessions = sampleAccessions;
    }

    public PropertyCollection getProperties(){
        return this.properties;
    }
    public void setProperties(PropertyCollection properties){
        this.properties = properties;
    }
    
    public int getPositionInMatrix(){
        return 0;
    }
}
