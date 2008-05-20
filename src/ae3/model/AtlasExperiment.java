package ae3.model;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.solr.common.SolrDocument;

import uk.ac.ebi.ae3.indexbuilder.Constants;

import java.util.Collection;
import java.util.ArrayList;
import java.util.Map;
import java.util.List;
import java.util.Set;
import java.util.Vector;

/**
 * Created by IntelliJ IDEA.
 * User: ostolop, mdylag
 * Date: Apr 17, 2008
 * Time: 9:31:07 AM
 * To change this template use File | Settings | File Templates.
 */
public class AtlasExperiment {
    private Long aerExpId;
    private String aerExpName;
    private String aerExpAccession;
    private String aerExpDescription;    
    private Collection aerUserId;    
    
    private Collection aerExperimentTypes;
    private Collection aerSampleAtsaCategory; 
    private Collection aerSampleAtsaValue;
    private Collection aerFactName; 
    private Collection aerFactOe; 
    private Collection aerMimeScoreName; 
    private Collection aerMimeScoreValue; 
    private Collection aerArrayDesId;
    private Collection aerArrayDesIdent; 
    private Collection aerArrayDesName; 
    private Collection aerArrayDesCount; 
    private Collection aerBdgName; 
    private Collection aerBdgId; 
    private Collection aerBdgNumBadCube; 
    private Collection aerBdgArrayDes; 
    private Collection aerBdgDataFormat; 
    private Collection aerBdgBioAssCount; 
    private Collection aerBdgIsDerivied; 
    private Collection aerBiPublication; 
    private Collection aerBiAuthors; 
    private Collection aerBiTitle; 
    private Collection aerBiYear; 
    private Collection aerBiVolume; 
    private Collection aerBiIssue; 
    private Collection aerBiPages; 
    private Collection aerProviderContac; 
    private Collection aerProviderRole;    
    private Collection aerDescId; 
    private Collection aerDescText; 

    private Long dwExpId;
    private String dwExpAccession;
    private String dwExpDescription;
    private Collection dwExpType;
    private List<AtlasDwAssay> atlasDwAsList;
    private List<AtlasDwSample> atlasDwSampleList;
    
    //assay
    //private 
    //samples
    
    private Collection<String> experimentFactorValues;
    private Collection<String> experimentFactors;
    private Map<String, List<String>> experimentHighlights;
    

    public AtlasExperiment(SolrDocument exptDoc) {
    	
        this.setAerExpId ((Long)exptDoc.getFieldValue(Constants.FIELD_AER_EXPID));
        this.aerExpAccession =  (String) exptDoc.getFieldValue(Constants.FIELD_AER_EXPACCESSION);
        this.setAerExpName((String)exptDoc.getFieldValue(Constants.FIELD_AER_EXPNAME));        

        this.aerUserId = exptDoc.getFieldValues(Constants.FIELD_AER_USER_ID);
        this.aerSampleAtsaCategory = exptDoc.getFieldValues(Constants.FIELD_AER_SAAT_CAT);
        this.aerSampleAtsaValue  = exptDoc.getFieldValues(Constants.FIELD_AER_SAAT_VALUE);       
        this.aerFactName = exptDoc.getFieldValues(Constants.FIELD_AER_FV_FACTORNAME);
        this.aerFactOe  = exptDoc.getFieldValues(Constants.FIELD_AER_FV_OE);       
        this.aerMimeScoreName  = exptDoc.getFieldValues(Constants.FIELD_AER_MIMESCORE_NAME);       
        this.aerMimeScoreValue  = exptDoc.getFieldValues(Constants.FIELD_AER_MIMESCORE_VALUE);
        Collection col1= exptDoc.getFieldValues(Constants.FIELD_AER_ARRAYDES_ID); 
        this.aerArrayDesId  =        
        this.aerArrayDesIdent  = exptDoc.getFieldValues(Constants.FIELD_AER_ARRAYDES_IDENTIFIER);       
        this.aerArrayDesName  = exptDoc.getFieldValues(Constants.FIELD_AER_ARRAYDES_NAME);       
        this.aerArrayDesCount  = exptDoc.getFieldValues(Constants.FIELD_AER_ARRAYDES_COUNT);       
        this.aerBdgId  = exptDoc.getFieldValues(Constants.FIELD_AER_BDG_ID);       
        this.aerBdgName  = exptDoc.getFieldValues(Constants.FIELD_AER_BDG_NAME);       
        this.aerBdgNumBadCube  = exptDoc.getFieldValues(Constants.FIELD_AER_BDG_NUM_BAD_CUBES);       
        this.aerBdgArrayDes  = exptDoc.getFieldValues(Constants.FIELD_AER_BDG_ARRAYDESIGN);       
        this.aerBdgDataFormat  = exptDoc.getFieldValues(Constants.FIELD_AER_BDG_DATAFORMAT);       
        this.aerBdgBioAssCount  = exptDoc.getFieldValues(Constants.FIELD_AER_BDG_BIOASSAY_COUNT);       
        this.aerBdgIsDerivied  = exptDoc.getFieldValues(Constants.FIELD_AER_BDG_IS_DERIVED);       
        this.aerBiAuthors  = exptDoc.getFieldValues(Constants.FIELD_AER_BI_AUTHORS);       
        this.aerBiIssue  = exptDoc.getFieldValues(Constants.FIELD_AER_BI_ISSUE);       
        this.aerBiPages  = exptDoc.getFieldValues(Constants.FIELD_AER_BI_PAGES);       
        this.aerBiPublication  = exptDoc.getFieldValues(Constants.FIELD_AER_BI_PUBLICATION);       
        this.aerBiTitle  = exptDoc.getFieldValues(Constants.FIELD_AER_BI_TITLE);       
        this.aerBiVolume  = exptDoc.getFieldValues(Constants.FIELD_AER_BI_VOLUME);       
        this.aerProviderContac  = exptDoc.getFieldValues(Constants.FIELD_AER_PROVIDER_CONTRACT);       
        this.aerExperimentTypes  = exptDoc.getFieldValues(Constants.FIELD_AER_EXP_TYPE);       
        this.aerDescId  = exptDoc.getFieldValues(Constants.FIELD_AER_DESC_ID);
        this.aerDescText  = exptDoc.getFieldValues(Constants.FIELD_AER_DESC_TEXT);      

        this.setDwExpId((Long) exptDoc.getFieldValue(Constants.FIELD_DWEXP_ID));
        this.dwExpAccession = (String)exptDoc.getFieldValue(Constants.FIELD_DWEXP_ACCESSION);
        this.dwExpDescription = (String)exptDoc.getFieldValue(Constants.FIELD_DWEXP_EXPDESC);
        this.dwExpType = (Collection)exptDoc.getFieldValues(Constants.FIELD_DWEXP_EXPTYPE);

    	atlasDwAsList = new ArrayList<AtlasDwAssay>();

    	//Bioassay DW
        for (int i=0; i<Constants.ARRAY_ASSAY_ELEMENTS.length; i++)
        {
        	Collection colValue=exptDoc.getFieldValues(Constants.PREFIX_AEDW + Constants.ARRAY_ASSAY_ELEMENTS[i]);
        	Collection colId=exptDoc.getFieldValues(Constants.PREFIX_AEDW  + Constants.ARRAY_ASSAY_ELEMENTS[i]+ Constants.SUFFIX_ASSAY_ID);
        	this.atlasDwAsList.add(new AtlasDwAssay(Constants.ARRAY_ASSAY_ELEMENTS[i], colValue, colId));
        	
        }       
        //samples DW
        this.atlasDwSampleList = new ArrayList<AtlasDwSample>();
        for (int i=0; i<Constants.ARRAY_SAMPLE_ELEMENTS.length; i++)
        {
        	Collection colSampleIds=exptDoc.getFieldValues(Constants.PREFIX_AEDW  + Constants.ARRAY_ASSAY_ELEMENTS[i]+ Constants.SUFFIX_SAMPLE_ID);
        	Collection colAssayIds=exptDoc.getFieldValues(Constants.PREFIX_AEDW  + Constants.ARRAY_ASSAY_ELEMENTS[i]+ Constants.SUFFIX_ASSAY_ID);
        	Collection colValues=exptDoc.getFieldValues(Constants.PREFIX_AEDW + Constants.ARRAY_ASSAY_ELEMENTS[i]);        	
        	this.atlasDwSampleList.add(new AtlasDwSample(Constants.ARRAY_SAMPLE_ELEMENTS[i], colSampleIds, colAssayIds, colValues));
        	
    		

        }
        //uncomment
        //this.setExperimentDescription((String) exptDoc.getFieldValue("exp_description"));
        //this.setExperimentFactorValues(exptDoc.getFieldValues("exp_factor_value"));
        //this.setExperimentFactors(exptDoc.getFieldValues("exp_factor"));
        
    }

    public void setAerExpId (Long aerExpId ) {
        this.aerExpId = aerExpId ;
    }

    public void setDwExpType(Collection<String> experimentTypes) {
        this.dwExpType = experimentTypes;
    }

    public void setDwExpAccession(String experimentAccession) {
        this.dwExpAccession = experimentAccession;
    }

    public void setDwExpDescription(String experimentDescription) {
        this.dwExpDescription = experimentDescription;
    }

    public void setExperimentFactorValues(Collection experimentFactorValues) {
        this.experimentFactorValues = experimentFactorValues;
    }

    public void setExperimentFactors(Collection experimentFactors) {
        this.experimentFactors = experimentFactors;
    }

    public Long getAerExpId () {
        return aerExpId ;
    }

    public Collection<String> getDwExpType() {
        return dwExpType;
    }

    public String getDwExpAccession() {
        return dwExpAccession;
    }

    public String getDwExpDescription() {
        return dwExpDescription;
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

	public Long getDwExpId()
	{
		return dwExpId;
	}

	public void setDwExpId(Long dwExpId)
	{
		this.dwExpId = dwExpId;
	}

	public String getAerExpName()
	{
		return aerExpName;
	}

	public void setAerExpName(String aerExpName)
	{
		this.aerExpName = aerExpName;
	}

	public String getAerExpAccession()
	{
		return aerExpAccession;
	}

	public void setAerExpAccession(String aerExpAccession)
	{
		this.aerExpAccession = aerExpAccession;
	}

	public String getAerExpDescription()
	{
		return aerExpDescription;
	}

	public void setAerExpDescription(String aerExpDescription)
	{
		this.aerExpDescription = aerExpDescription;
	}

	public Collection getAerExperimentTypes()
	{
		return aerExperimentTypes;
	}

	public void setAerExperimentTypes(Collection aerExperimentTypes)
	{
		this.aerExperimentTypes = aerExperimentTypes;
	}

	public Collection getAerSampleAtsaCategory()
	{
		return aerSampleAtsaCategory;
	}

	public Collection getAerSampleAtsaValue()
	{
		return aerSampleAtsaValue;
	}

	public Collection getAerFactName()
	{
		return aerFactName;
	}

	public Collection getAerFactOe()
	{
		return aerFactOe;
	}
	
	public Vector<Collection<String>> getAerSampleAttributes()
	{		
		if (!hasAerSampleAttributes())
			return null;
		Vector<Collection<String>> vec = new Vector<Collection<String>>();
		vec.add(this.aerSampleAtsaCategory);
		vec.add(this.aerSampleAtsaValue);
		return vec;		
	}
	
	public boolean hasAerSampleAttributes()
	{
		if (this.aerSampleAtsaCategory == null | this.aerSampleAtsaValue == null)
			return false;
		return true;
		
	}
	
	public Vector<Collection<String>> getAerFactorAttributes()
	{		
		if (!hasAerFactorAttributes())
			return null;
		Vector<Collection<String>> vec = new Vector<Collection<String>>();
		vec.add(this.aerFactName);
		vec.add(this.aerFactOe);
		return vec;		
	}
	
	public boolean hasAerFactorAttributes()
	{
		if (this.aerFactName == null | this.aerFactOe == null)
			return false;
		return true;
		
	}
	public Vector<Collection<String>> getAerArrayDesigns()
	{		
		if (!hasAerArrayDesigns())
			return null;
		Vector<Collection<String>> vec = new Vector<Collection<String>>();
		vec.add(this.aerArrayDesId);
		vec.add(this.aerArrayDesIdent);
		vec.add(this.aerArrayDesName);
		vec.add(this.aerArrayDesCount);		
		return vec;		
	}
	
	public boolean hasAerArrayDesigns()
	{
		if (this.aerArrayDesId == null)
			return false;
		return true;
		
	}
	
}
