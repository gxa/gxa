package uk.ac.ebi.ae3.indexbuilder.model;

public class BioAssayDataGroup
{
	private String name;
	private long id;
	private String num_bad_cubes;
	private String arraydesign;
	private String dataformat;
	private String bioassayCount; 
	private String derived;
	public String getName()
	{
		return name;
	}
	public void setName(String name)
	{
		this.name = name;
	}
	public long getId()
	{
		return id;
	}
	public void setId(long id)
	{
		this.id = id;
	}
	public String getNum_bad_cubes()
	{
		return num_bad_cubes;
	}
	public void setNum_bad_cubes(String num_bad_cubes)
	{
		this.num_bad_cubes = num_bad_cubes;
	}
	public String getArraydesign()
	{
		return arraydesign;
	}
	public void setArraydesign(String arraydesign)
	{
		this.arraydesign = arraydesign;
	}
	public String getDataformat()
	{
		return dataformat;
	}
	public void setDataformat(String dataformat)
	{
		this.dataformat = dataformat;
	}
	public String getBioassayCount()
	{
		return bioassayCount;
	}
	public void setBioassayCount(String bioassayCount)
	{
		this.bioassayCount = bioassayCount;
	}
	public String getDerived()
	{
		return derived;
	}
	public void setDerived(String derived)
	{
		this.derived = derived;
	} 

}
