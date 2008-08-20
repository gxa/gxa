package ae3.model;

import java.util.Collection;
import java.io.Serializable;

public class AtlasDwAssay implements Serializable
{
	private String name;
	private Collection<String> values;
	private Collection<String>  assayIds;	
	
	public AtlasDwAssay(String name, Collection<String> values, Collection<String> assayIds)
	{
		super();
		this.name = name;
		this.values = values;
		this.assayIds = assayIds;
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
