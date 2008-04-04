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
}
