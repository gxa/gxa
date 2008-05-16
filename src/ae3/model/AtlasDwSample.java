package ae3.model;

import java.util.Collection;

public class AtlasDwSample
{
	private String name;
	private Collection<String> values;
	private Collection<String>  assayIds;	
	private Collection<String>  sampleIds;	
	
	public AtlasDwSample(String name, Collection<String> sampleIds, Collection<String> assayIds, Collection<String> values)
	{
		super();
		this.name = name;
		this.values = values;
		this.assayIds = assayIds;
		this.sampleIds = sampleIds;
		
	}
	public String getName()
	{
		return name;
	}
	public void setName(String name)
	{
		this.name = name;
	}
	
}
