package ae3.model;

import org.apache.solr.common.SolrDocument;

import uk.ac.ebi.ae3.indexbuilder.Constants;

import java.util.Collection;
import java.util.ArrayList;
import java.util.Map;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: ostolop
 * Date: Apr 17, 2008
 * Time: 9:31:07 AM
 * To change this template use File | Settings | File Templates.
 */
public class AtlasExperiment {
    private Long experimentId;
    private String expName;
    private Collection<String> experimentTypes;
    private String experimentAccession;
    private String experimentDescription;
    private Collection<String> experimentFactorValues;
    private Collection<String> experimentFactors;
    private Map<String, List<String>> experimentHighlights;
    private AtlasExperiment experimentDw;

    public AtlasExperiment(SolrDocument exptDoc) {
    	
        this.setExperimentId((Long)exptDoc.getFieldValue(Constants.FIELD_AER_EXPID));
        this.setExperimentTypes(exptDoc.getFieldValues(Constants.FIELD_AER_EXP_TYPE));        
        this.setExperimentAccession((String) exptDoc.getFieldValue(Constants.FIELD_AER_EXPACCESSION));
        Collection col=exptDoc.getFieldValues(Constants.FIELD_AER_DESC_ID);
        ;
        int i = 0;
        //uncomment
        //this.setExperimentDescription((String) exptDoc.getFieldValue("exp_description"));
        //this.setExperimentFactorValues(exptDoc.getFieldValues("exp_factor_value"));
        //this.setExperimentFactors(exptDoc.getFieldValues("exp_factor"));
        
    }

    public void setExperimentId(Long experimentId) {
        this.experimentId = experimentId;
    }

    public void setExperimentTypes(Collection experimentTypes) {
        this.experimentTypes = experimentTypes;
    }

    public void setExperimentAccession(String experimentAccession) {
        this.experimentAccession = experimentAccession;
    }

    public void setExperimentDescription(String experimentDescription) {
        this.experimentDescription = experimentDescription;
    }

    public void setExperimentFactorValues(Collection experimentFactorValues) {
        this.experimentFactorValues = experimentFactorValues;
    }

    public void setExperimentFactors(Collection experimentFactors) {
        this.experimentFactors = experimentFactors;
    }

    public Long getExperimentId() {
        return experimentId;
    }

    public Collection<String> getExperimentTypes() {
        return experimentTypes;
    }

    public String getExperimentAccession() {
        return experimentAccession;
    }

    public String getExperimentDescription() {
        return experimentDescription;
    }

    public Collection<String> getExperimentFactorValues() {
        return experimentFactorValues;
    }

    public Collection<String> getExperimentFactors() {
        return experimentFactors;
    }

    public void setExperimentHighlights(Map<String, List<String>> experimentHighlights) {
        this.experimentHighlights = experimentHighlights;
    }

    public Map<String, List<String>> getExperimentHighlights() {
        return experimentHighlights;
    }
}
