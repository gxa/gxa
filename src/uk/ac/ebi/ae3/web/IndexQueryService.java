/**
 * 
 */
package uk.ac.ebi.ae3.web;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Iterator;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.lucene.index.CorruptIndexException;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.xml.sax.SAXException;

import uk.ac.ebi.ae3.indexbuilder.Constants;
import uk.ac.ebi.ae3.indexbuilder.IndexException;
import uk.ac.ebi.ae3.indexbuilder.service.SolrEmbededIndex;
import uk.ac.ebi.ae3.indexbuilder.utils.XmlUtil;

/**
 * 
 * Class description goes here.
 * @deprecated
 * @version 	1.0 2008-04-03
 * @author 	Miroslaw Dylag
 */
public class IndexQueryService
{

	private SolrEmbededIndex solrEmbeddedIndex;
	


	/**
	 * @throws SAXException 
	 * @throws IOException 
	 * @throws ParserConfigurationException 
	 * 
	 */
	public IndexQueryService(String multicoredir) 
	{
		solrEmbeddedIndex = new SolrEmbededIndex(multicoredir);
		
	}
	
	public void init() throws ParserConfigurationException, IOException, SAXException, IndexException
	{
	    this.solrEmbeddedIndex.init();
	}
	
	public void dispose() 
	{
	    this.solrEmbeddedIndex.dispose();
	}
	
	
	public SolrDocumentList getExperiments(String[] keywords, int start, int rows) throws SolrServerException, CorruptIndexException, IOException
	{
	    String query = parseQuery(keywords);	    
	    SolrDocumentList l=this.solrEmbeddedIndex.search(query,start, rows);
	    return l;
	}

	/**
	 * Return number of documents in index
	 * @param keywords
	 * @return
	 * @throws SolrServerException
	 * @throws CorruptIndexException
	 * @throws IOException
	 */
	public long getCount(String[] keywords) throws SolrServerException, CorruptIndexException, IOException
	{
	    String query = parseQuery(keywords);
	    long count=this.solrEmbeddedIndex.getCount(query);
	    return count;
	}
	
	private static final String parseQuery(String[] keywords)
	{
	    StringBuffer buff = new StringBuffer();
	    for (int i=0; i<keywords.length; i++) {
    		String val = keywords[i];
    		buff.append(Constants.FIELD_AER_EXPACCESSION).append(":").append(val);
    		buff.append(" ");
    		buff.append(val).append(" ");
	    }
	    
	    String query = buff.toString().trim();
	    System.out.println(query);
	    return query;
	    
	}
	public void printExperiments(String[] keywords, PrintWriter out) throws CorruptIndexException, SolrServerException, IOException
	{
		long count=getCount(keywords);
		if (count==0)
		{
		    Document doc = DocumentHelper.createDocument(DocumentHelper.createElement("xml"));
		    out.println(doc.asXML());
		    return;
		}
		int start=0;
		int max=400;
		int rows = 0;
		int size = 0;
		
		if (count==1)
		{
		    SolrDocumentList l=getExperiments(keywords, 0, 1);
		    String xml=XmlUtil.createElement(l.get(0));
		    out.println(xml);
		}
		else
		{
		    out.print("<experiments>");
		    while (rows < count)
		    {
	
			start = rows;
			rows = rows + max; 
			SolrDocumentList l=getExperiments(keywords, start, rows);
			Iterator<SolrDocument> it=l.iterator();
			while (it.hasNext())
			{
			    SolrDocument doc=it.next();
			    String xml=XmlUtil.createElement(doc);
			    out.println(xml);
			    out.flush();
			    
			}

		    }
		    
		    out.print("</experiments>");

		}
	    
	}
	/**
	 * Main method fot test this class. It solution is not good.
	 * In future I want to add JUnit tests.
	 * @param args
	 * @throws ParserConfigurationException
	 * @throws IOException
	 * @throws SAXException
	 * @throws SolrServerException
	 * @throws IndexException 
	 */
	public static void main(String[] args) throws ParserConfigurationException, IOException, SAXException, SolrServerException, IndexException
	{
		String value = "D:\\tools\\workspaces\\ebi2\\ae3\\indexbuilder\\data\\multicore";
		//"C:\\Users\\mdylag\\workspaces\\ebi\\ae3\\indexbuilder\\data\\multicore"
		IndexQueryService idx = new IndexQueryService(value);
		idx.init();
		String[] keywords = {"cancer"};
		PrintWriter out = new PrintWriter(System.out);
		idx.printExperiments(keywords, out);
		idx.dispose();
	
	}
	
	
}
