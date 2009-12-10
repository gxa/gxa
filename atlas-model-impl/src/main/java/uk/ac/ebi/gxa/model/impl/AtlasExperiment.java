package uk.ac.ebi.gxa.model.impl;

import uk.ac.ebi.gxa.model.Experiment;
import uk.ac.ebi.gxa.model.PropertyCollection;

import java.util.Collection;
import java.util.Date;

/**
 * Created by IntelliJ IDEA.
 * User: Andrey
 * Date: Nov 26, 2009
 * Time: 11:32:13 AM
 * To change this template use File | Settings | File Templates.
 */
public class AtlasExperiment implements Experiment {
    private String accession;
    private int id;
    private Collection<String> assayAccessions = null;
    private Collection<String> sampleAccessions = null;
    private PropertyCollection properties = null;


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


        public Collection<String> getType(){
            return null;
        }

        public String getDescription(){
            return "";
        }

        public Date getLoadDate(){
            return null;
        }

        public DEGStatus getDEGStatus(){
             return DEGStatus.UNKNOWN;
        }

        public String getPerformer(){
            return "";
        }

        public String getLab(){
            return "";
        }

        public Collection<String> getAssayAccessions(){
            return this.assayAccessions;
        }

        public Collection<String> getSampleAccessions(){
            return this.sampleAccessions;
        }

        public PropertyCollection getProperties(){
            return properties;
        }

        public void setSampleAccessions(Collection<String> accessions){
            this.sampleAccessions = accessions;
        }

        public void setAssayAccessions(Collection<String> accessions){
            this.assayAccessions = accessions;
        }

        public void setProperties(PropertyCollection properties){
            this.properties = properties;
        }
}
