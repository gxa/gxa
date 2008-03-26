package uk.ac.ebi.ae3.indexbuilder.model;

public class Bibliography
{
	private String publication;
	private String authors;
	private String year;
	private String volume;
	private String issue;
	private String pages;
	public String getPublication()
	{
		return publication;
	}
	public void setPublication(String publication)
	{
		this.publication = publication;
	}
	public String getAuthors()
	{
		return authors;
	}
	public void setAuthors(String authors)
	{
		this.authors = authors;
	}
	public String getYear()
	{
		return year;
	}
	public void setYear(String year)
	{
		this.year = year;
	}
	public String getVolume()
	{
		return volume;
	}
	public void setVolume(String volume)
	{
		this.volume = volume;
	}
	public String getIssue()
	{
		return issue;
	}
	public void setIssue(String issue)
	{
		this.issue = issue;
	}
	public String getPages()
	{
		return pages;
	}
	public void setPages(String pages)
	{
		this.pages = pages;
	}
}
