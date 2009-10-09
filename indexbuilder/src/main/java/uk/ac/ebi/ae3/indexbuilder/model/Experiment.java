/**
 * 
 */
package uk.ac.ebi.ae3.indexbuilder.model;

/**
 * The class is a model for Experiment.
 * @author Miroslaw Dylag
 *
 */
@Deprecated
public class Experiment
{
	private Long id;
	private String accession;
	private boolean pub;
	private String name;
	private String releaseDate;
	private String mimegold;
	private String userId;
	private String expSource;	
	
	public Experiment()
	{
		id=new Long(-1);
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
	
	public Long getId()
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
}
