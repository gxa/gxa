/**
 * 
 */
package uk.ac.ebi.ae3.indexbuilder.model;

import java.util.ArrayList;
import java.util.List;

/**
 * The class is a model for Experiment.
 * @author mdylag
 *
 */
public class Experiment
{
	private long id;
	private String accession;
	private boolean pub;
	private String name;
	private String releaseDate;
	private String mimegold;
	private String userId;
	private List<SampleAttribute> sampleAtrList = new ArrayList<SampleAttribute>();
	private List<FactorValue> factorValueList = new ArrayList<FactorValue>();
	private List<MimeScore> mimeScoreList = new ArrayList<MimeScore>();
	private List<ArrayDesign> arrayDesignList = new ArrayList<ArrayDesign>();
	private List<BioAssayDataGroup> bioAssayDataGrList = new ArrayList<BioAssayDataGroup>();
	private List<Bibliography> bibliographyList = new ArrayList<Bibliography>();
	private List<Provider> providerList = new ArrayList<Provider>();
	private List<ExperimentDesign> expDesignList = new ArrayList<ExperimentDesign>();
	private List<Description> descriptionList = new ArrayList<Description>();
	
	
	
	public Experiment()
	{
		
	}
	public Experiment(long id, String accession)
	{
		this(id,accession,true);
	}
	
	public Experiment(long id, String accession, boolean pub)
	{
		super();
		this.id = id;
		this.accession = accession;
		this.pub = pub;
	}
	
	public long getId()
	{
		return id;
	}
	public void setId(long id)
	{
		this.id = id;
	}
	public String getAccession()
	{
		return accession;
	}
	public void setAccession(String accession)
	{
		this.accession = accession;
	}
	public boolean isPub()
	{
		return pub;
	}
	public void setPub(boolean pub)
	{
		this.pub = pub;
	}
	public String getName()
	{
		return name;
	}
	public void setName(String name)
	{
		this.name = name;
	}
	public String getReleaseDate()
	{
		return releaseDate;
	}
	public void setReleaseDate(String releaseDate)
	{
		this.releaseDate = releaseDate;
	}
	public String getMimegold()
	{
		return mimegold;
	}
	public void setMimegold(String mimegold)
	{
		this.mimegold = mimegold;
	}
	public String getUserId()
	{
		return userId;
	}
	public void setUserId(String userId)
	{
		this.userId = userId;
	}
	public List<SampleAttribute> getSampleAtrList()
	{
		return sampleAtrList;
	}
	public void setSampleAtrList(List<SampleAttribute> sampleAtrList)
	{
		this.sampleAtrList = sampleAtrList;
	}
	public List<FactorValue> getFactorValueList()
	{
		return factorValueList;
	}
	public void setFactorValueList(List<FactorValue> factorValueList)
	{
		this.factorValueList = factorValueList;
	}
	public List<MimeScore> getMimeScoreList()
	{
		return mimeScoreList;
	}
	public void setMimeScoreList(List<MimeScore> mimeScoreList)
	{
		this.mimeScoreList = mimeScoreList;
	}
	public List<ArrayDesign> getArrayDesignList()
	{
		return arrayDesignList;
	}
	public void setArrayDesignList(List<ArrayDesign> arrayDesignList)
	{
		this.arrayDesignList = arrayDesignList;
	}
	public List<BioAssayDataGroup> getBioAssayDataGrList()
	{
		return bioAssayDataGrList;
	}
	public void setBioAssayDataGrList(List<BioAssayDataGroup> bioAssayDataGrList)
	{
		this.bioAssayDataGrList = bioAssayDataGrList;
	}
	public List<Bibliography> getBibliographyList()
	{
		return bibliographyList;
	}
	public void setBibliographyList(List<Bibliography> bibliographyList)
	{
		this.bibliographyList = bibliographyList;
	}
	public List<Provider> getProviderList()
	{
		return providerList;
	}
	public void setProviderList(List<Provider> providerList)
	{
		this.providerList = providerList;
	}
	public List<ExperimentDesign> getExpDesignList()
	{
		return expDesignList;
	}
	public void setExpDesignList(List<ExperimentDesign> expDesignList)
	{
		this.expDesignList = expDesignList;
	}
	public List<Description> getDescriptionList()
	{
		return descriptionList;
	}
	public void setDescriptionList(List<Description> descriptionList)
	{
		this.descriptionList = descriptionList;
	}
	
}
