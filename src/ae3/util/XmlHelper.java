package ae3.util;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.highlight.DefaultSolrHighlighter;
import org.apache.solr.highlight.SolrHighlighter;
import org.apache.solr.util.HighFrequencyDictionary;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;

import com.sun.corba.se.impl.orbutil.closure.Constant;

import uk.ac.ebi.ae3.indexbuilder.Constants;
import ae3.model.AtlasExperiment;
/**
 * TODO: DOCUMENT ME and unify name of elements in xml files
 * TODO: This class should be split to smaller piece of code.
 * The utility class has methods which help to create the XML file from a solr index.
 * 
 * @author mdylag
 *
 */
public class XmlHelper
{
	/** The XML elment name experiments*/
    	public static final String XML_EL_EXPERIMENTS= "experiments";
	/** The XML elment name experiment*/    	
	public static final String XML_EL_EXPERIMENT= "experiment";
	public static final String XML_EL_ID= "id";
	public static final String XML_EL_ACCESSION= "accession";
	public static final String XML_EL_NAME= "name";
	public static final String XML_EL_SPECIES= "species";
	public static final String XML_EL_SAMPLES= "samples";
	public static final String XML_EL_HYBS= "hybs";
	public static final String XML_EL_USER= "user";
	public static final String XML_EL_SECONDARYACCESSION= "secondaryaccession";
	public static final String XML_EL_SAMPLEATTRIBUTE= "sampleattribute";
	public static final String XML_EL_FACTOR= "factor";
	public static final String XML_EL_ARRAYDESIGN="arraydesign";
	public static final String XML_EL_RELEASEDATE="releasedate";
	
	public static final String XML_EL_COUNT="count";
	
	public static final String XML_EL_BIBLIOGRAPHY="bibliography";	
	public static final String XML_EL_AUTHORS="authors";	
	public static final String XML_EL_TITLE="title";	
	public static final String XML_EL_YEAR="year";	
	public static final String XML_EL_PAGES="pages";	
	public static final String XML_EL_ISUE="isue";	
	public static final String XML_EL_VOLUME="colume";	
	public static final String XML_EL_PUBLICATIONS="publications";	
	public static final String XML_EL_PROVIDER="provider";	
	public static final String XML_EL_CONTACT="contact";	
	public static final String XML_EL_ROLE="role";	
	public static final String XML_EL_EMAIL="email";	
	public static final String XML_EL_EXPERIMENTDESIGNS="experimentdesigns";
	public static final String XML_EL_EXPERIMENTDESIGN="experimentdesign";	
	
	public static final String XML_EL_DESCRIPTION="description";
	public static final String XML_EL_MIAMESCORE = "miamescores";
	
	public static final String XML_EL_CATEGORY= "category";
	public static final String XML_EL_VALUE= "value";
	public static final String XML_EL_FILES= "files";
	public static final String XML_EL_FGEM= "fgem";
	public static final String XML_EL_RAW= "raw";
	public static final String XML_EL_TWOCOLUMNS= "twocolumns";
	public static final String XML_EL_SDRF= "sdrf";
	public static final String XML_EL_BIOSAMPLES= "biosamples";
	public static final String XML_EL_PNG= "png";
	public static final String XML_EL_SVG= "svg";

	public static final String XML_AT_URL= "url";
	public static final String URL_EBI_DWONLOAD= "http://www.ebi.ac.uk/microarray-as/ae/download/";

	public static final String XML_AT_TOTAL= "total";
	public static final String XML_AT_START= "start";
	public static final String XML_AT_ROWS= "rows";
	public static final String XML_AT_COUNT = "count";
	public static final String XML_AT_CELCOUNT = "celcount";
	

	/**
	 * 
	 * @param parent
	 * @param childName
	 * @param attrValue
	 */
	private static void addElementWithAttr(Element parent, String childName, String attrValue)
	{
		Element element = parent.addElement(childName);
		element.setText(attrValue);
		
	}
	/**
	 * 
	 * TODO:
	 * @param docList
	 * @param count
	 * @param start
	 * @param rows
	 * @return
	 */
	public static Document createXmlDoc(QueryResponse resp, long count, int start, int rows)
	{
	        SolrDocumentList docList=resp.getResults();
		Map<String,Map<String, List<String>>> hglMapAll=resp.getHighlighting();
	
		Document doc = createXmlDoc(count, start, rows);
		Element elRoot=doc.getRootElement();
		//
		Iterator<SolrDocument> docIt = docList.iterator();
		while (docIt.hasNext())
		{
		    	
			SolrDocument solrDocument =docIt.next();
			Element elExperiment = elRoot.addElement(XML_EL_EXPERIMENT);

			String attrValue = getLongFieldValue(solrDocument, Constants.FIELD_AER_EXPID);
			addElementWithAttr(elExperiment, XML_EL_ID, attrValue);

			String expAccession = (String)solrDocument.getFieldValue(Constants.FIELD_AER_EXPACCESSION);
			Map<String, List<String>> hglMapExp=hglMapAll.get(expAccession);			
			addElementWithAttr(elExperiment, XML_EL_ACCESSION, expAccession);
			
			attrValue = getStringFieldValue(expAccession, hglMapExp,solrDocument, Constants.FIELD_AER_EXPNAME);
			addElementWithAttr(elExperiment, XML_EL_NAME, attrValue);
			
			attrValue = getStringFieldValue(expAccession, hglMapExp,solrDocument, Constants.FIELD_AER_RELEASEDATE);
			addElementWithAttr(elExperiment, XML_EL_RELEASEDATE, attrValue);

			
			//Adding species
			Collection colSampleCat=solrDocument.getFieldValues(Constants.FIELD_AER_SAAT_CAT);
			Collection colSampleValue=solrDocument.getFieldValues(Constants.FIELD_AER_SAAT_VALUE);
			if (colSampleCat!=null)
			{
			    
			    Iterator<String> itCat=colSampleCat.iterator();
			    Iterator<String> itVal =null;
			    Map<String, String> map;
			    if (colSampleValue != null)
			    {
				itVal=colSampleValue.iterator();
			    }
			    while (itCat.hasNext())
			    {
				String cat=itCat.next();
				String value="";
				if (itVal != null)
				{
				    value=itVal.next();
				}
				if (cat.equalsIgnoreCase("Organism"))
				{
				    addElementWithAttr(elExperiment,XML_EL_SPECIES , value);
				}
			    }
			}
			//Adding samples
			attrValue = getIntFieldValue(solrDocument, Constants.FIELD_AER_TOTAL_SAMPL);
			addElementWithAttr(elExperiment,XML_EL_SAMPLES , attrValue);

			//Adding hybs
			attrValue = getIntFieldValue(solrDocument, Constants.FIELD_AER_TOTAL_HYBS);
			addElementWithAttr(elExperiment, XML_EL_HYBS, attrValue);
			
			//Add files
			Element element = elExperiment.addElement(XML_EL_FILES);
			//Add FGEM
			{
				String value=getStringFieldValue(expAccession, hglMapExp,solrDocument, Constants.FIELD_AER_FILE_FGEM);
				String valueCount=getIntFieldValue(solrDocument, Constants.FIELD_AER_FILE_FGEM);
			
				if (!StringUtils.isEmpty(value))
				{
					Element childEl=element.addElement(XML_EL_FGEM);
					childEl.addAttribute(XML_AT_URL, URL_EBI_DWONLOAD + value);
					childEl.addAttribute(XML_AT_COUNT, URL_EBI_DWONLOAD + valueCount);
					
				}

			}
			//Add RAW
			{
    			String value=getStringFieldValue(expAccession, hglMapExp,solrDocument, Constants.FIELD_AER_FILE_RAW);
    			String valueCount=getIntFieldValue(solrDocument, Constants.FIELD_AER_RAW_COUNT);
    			String valueCelCount=getIntFieldValue(solrDocument, Constants.FIELD_AER_RAW_CELCOUNT);
    			
    			if (!StringUtils.isEmpty(value))
    			{
    				Element childEl=element.addElement(XML_EL_RAW);
    				childEl.addAttribute(XML_AT_URL, URL_EBI_DWONLOAD + value);
    				childEl.addAttribute(XML_AT_CELCOUNT, URL_EBI_DWONLOAD + valueCelCount);
    				childEl.addAttribute(XML_AT_COUNT, URL_EBI_DWONLOAD + valueCount);
    				
    			}
			}
			//Add TWOCOLUMNS
			{
    			String value=getStringFieldValue(expAccession, hglMapExp,solrDocument, Constants.FIELD_AER_FILE_TWOCOLUMNS);
    			if (!StringUtils.isEmpty(value))
    			{
    				Element childEl=element.addElement(XML_EL_TWOCOLUMNS);
    				childEl.addAttribute(XML_AT_URL, URL_EBI_DWONLOAD + value);
    			}
			}
			//Add SDRF
			{
    			String value=getStringFieldValue(expAccession, hglMapExp,solrDocument, Constants.FIELD_AER_FILE_SDRF);
    			if (!StringUtils.isEmpty(value))
    			{
    				Element childEl=element.addElement(XML_EL_SDRF);
    				childEl.addAttribute(XML_AT_URL, URL_EBI_DWONLOAD + value);
    			}
			}
			//Adding biosamples
			{
    			String valuePng=getStringFieldValue(expAccession, hglMapExp,solrDocument, Constants.FIELD_AER_FILE_BIOSAMPLEPNG);
    			String valueSvg=getStringFieldValue(expAccession, hglMapExp,solrDocument, Constants.FIELD_AER_FILE_BIOSAMPLESVG);
    			
    			if (!StringUtils.isEmpty(valueSvg) || !StringUtils.isEmpty(valuePng) )
    				
    			{
    				Element childEl=element.addElement(XML_EL_BIOSAMPLES);
    				if (!StringUtils.isEmpty(valuePng))
    				{
    					Element childEl1=element.addElement(XML_EL_PNG);
    					childEl1.addAttribute(XML_AT_URL, URL_EBI_DWONLOAD + valuePng);
    				}
    				if (!StringUtils.isEmpty(valueSvg))
    				{
    					Element childEl1=element.addElement(XML_EL_SVG);
    					childEl1.addAttribute(XML_AT_URL, URL_EBI_DWONLOAD + valueSvg);
    				}
    
    			}
			}
			//Adding user
			{
				Collection col=solrDocument.getFieldValues(Constants.FIELD_AER_USER_ID);
				Iterator it=col.iterator();
				while (it.hasNext())
				{
					element = elExperiment.addElement(XML_EL_USER);
					Long valueLong = (Long)it.next();
					element.setText(valueLong.toString());
				}
			}
			//Adding seconary accession
			//Adding sample attributes
			{
				Map<String, Collection> map = new HashMap<String, Collection>();
				map.put(XML_EL_CATEGORY, colSampleCat);
				map.put(XML_EL_VALUE, colSampleValue);
				createElementFromMap(elExperiment, map, XML_EL_SAMPLEATTRIBUTE);
			}
			//Adding factor
			{
				Collection col1=solrDocument.getFieldValues(Constants.FIELD_AER_FV_FACTORNAME);
				Collection col2=solrDocument.getFieldValues(Constants.FIELD_AER_FV_OE);
				Map<String, Collection> map = new HashMap<String, Collection>();
				map.put(XML_EL_NAME, col1);
				map.put(XML_EL_VALUE, col2);
				createElementFromMap(elExperiment, map, XML_EL_FACTOR);				
			}
			//Adding miamescores
			{
				Element elMimescores = elExperiment.addElement(XML_EL_MIAMESCORE);
				Collection col1=solrDocument.getFieldValues(Constants.FIELD_AER_MIMESCORE_NAME);
				Collection col2=solrDocument.getFieldValues(Constants.FIELD_AER_MIMESCORE_VALUE);
				Iterator<String> it1 = null;
				Iterator<String> it2 = null;
				if (col1 != null)
				{
					it1 = col1.iterator();
				}
				if (col2 != null)
				{
					it2 = col2.iterator();
				}
				if (col1 != null)
				{
					while (it1.hasNext())
					{
						String elName=it1.next();
						Element el = elMimescores.addElement(elName.toLowerCase());
						if (col2!= null && it2.hasNext())
						{
							el.setText(it2.next());
						}
						
					}
				}
			}
			
			//Adding Array design
			{
				Collection col1=solrDocument.getFieldValues(Constants.FIELD_AER_ARRAYDES_ID);
				Collection col2=solrDocument.getFieldValues(Constants.FIELD_AER_ARRAYDES_IDENTIFIER);
				Collection col3=solrDocument.getFieldValues(Constants.FIELD_AER_ARRAYDES_NAME);
				Collection col4=solrDocument.getFieldValues(Constants.FIELD_AER_ARRAYDES_COUNT);
				Map<String, Collection> map = new HashMap<String, Collection>();
				map.put(XML_EL_ID, col1);
				map.put(XML_EL_ACCESSION, col2);
				map.put(XML_EL_NAME, col3);
				map.put(XML_EL_COUNT, col4);				
				createElementFromMap(elExperiment, map, XML_EL_ARRAYDESIGN);	
			}
			//Adding bibliography
			{
				Collection col1=solrDocument.getFieldValues(Constants.FIELD_AER_BI_AUTHORS);
				Collection col2=solrDocument.getFieldValues(Constants.FIELD_AER_BI_TITLE);
				Collection col3=solrDocument.getFieldValues(Constants.FIELD_AER_BI_YEAR);
				Collection col4=solrDocument.getFieldValues(Constants.FIELD_AER_BI_PAGES);
				Collection col5=solrDocument.getFieldValues(Constants.FIELD_AER_BI_PUBLICATION);
				Collection col6=solrDocument.getFieldValues(Constants.FIELD_AER_BI_ISSUE);
				
				Map<String, Collection> map = new HashMap<String, Collection>();
				map.put(XML_EL_AUTHORS, col1);
				map.put(XML_EL_TITLE, col2);
				map.put(XML_EL_YEAR, col3);
				map.put(XML_EL_PAGES, col4);
				map.put(XML_EL_PUBLICATIONS, col5);				
				map.put(XML_EL_TITLE, col6);
				createElementFromMap(elExperiment, map, XML_EL_BIBLIOGRAPHY);			
			}
			//Adding provider
			{
				Collection col1=solrDocument.getFieldValues(Constants.FIELD_AER_PROVIDER_CONTRACT);
				Collection col2=solrDocument.getFieldValues(Constants.FIELD_AER_PROVIDER_ROLE);
				Collection col3=solrDocument.getFieldValues(Constants.FIELD_AER_PROVIDER_EMAIL);
				
				Map<String, Collection> map = new HashMap<String, Collection>();
				map.put(XML_EL_CONTACT, col1);
				map.put(XML_EL_ROLE, col2);
				map.put(XML_EL_EMAIL, col3);
				createElementFromMap(elExperiment, map, XML_EL_PROVIDER);			
				
			}
			//Adding experiment design type
			{			
				Collection col1=solrDocument.getFieldValues(Constants.FIELD_AER_EXPDES_TYPE);
				if (col1 != null)
				{
					Iterator<String> it = col1.iterator();
					Element elExpDesigns=elExperiment.addElement(XML_EL_EXPERIMENTDESIGN);
					while (it.hasNext())
					{
						String value = it.next();
						value=getHighLight(expAccession, hglMapExp, value, Constants.FIELD_AER_EXPDES_TYPE);

						elExpDesigns.addElement(XML_EL_EXPERIMENTDESIGN).setText(value);						
					}
				}
				
			}
			//Adding descriptions
			{
				Collection col1=solrDocument.getFieldValues(Constants.FIELD_AER_DESC_ID);
				Collection col2=solrDocument.getFieldValues(Constants.FIELD_AER_DESC_TEXT);
				if (col1!= null)
				{
					Iterator<String> it1 = col1.iterator();
					Iterator<String> it2 = null;
					if (col2 != null)
					{
						it2 = col2.iterator();
					}

					while (it1.hasNext())
					{
						String idValue=it1.next();
						Element el=elExperiment.addElement(XML_EL_DESCRIPTION);
						el.addElement(XML_EL_ID).setText(idValue);
						if (col2 != null && it2.hasNext())
						{
						      String valueDesc = it2.next();
						      valueDesc=getHighLight(expAccession, hglMapExp, valueDesc, Constants.FIELD_AER_DESC_TEXT);
						      el.setText(valueDesc);
						}
						
					}
				}
					
			
			}
			
		}		
		return doc;
	}
	
	private static boolean isHighLight(Map<String, List<String>> map, String fieldName)
	{
	  
	    return map.containsKey(fieldName);
	}
	
	private static String getHighLight(String expAccession, Map<String, List<String>> map, String text, String fieldName)
	{
	    if (!isHighLight(map,  fieldName))
	    {
		return text;
	    }
	    if (map==null || map.size() == 0)
	    {
		return text;
	    }
	    String textHg = text; 
	    
	    if (map!=null && map.size() > 0)
	    {
		List<String> list=map.get(fieldName);
		Iterator<String> it=list.iterator();
		while (it.hasNext())
		{
		    String hgValue=it.next();
		    int fromIndex = 0;		
		    int endIndex = 0;
		
		    while ( (fromIndex=hgValue.indexOf("<em>", fromIndex)) != -1)
		    {
				endIndex = hgValue.indexOf("</em>", fromIndex);
				String match=hgValue.substring(fromIndex, endIndex+5);
				String strSearch = match.replace("<em>", "").replace("</em>", "");			
				if (textHg.indexOf(match)==-1)
				{
				    textHg=textHg.replace(strSearch, match);
				}
				fromIndex=endIndex;
		    }
		}
	    }
	    return textHg;
	}

	private static String getLongFieldValue(SolrDocument solrDocument,String name)
	{
		Long value = (Long)solrDocument.getFieldValue(name);
		if (value != null)
		{
			return value.toString();
		}
		return "";
	}
		
	private static String getStringFieldValue(String expAccession, Map<String,List<String>>map, SolrDocument solrDocument,String fieldName)
	{
		String value = (String)solrDocument.getFieldValue(fieldName);
		if (value != null)
		{
		    value=getHighLight(expAccession, map, value, fieldName);
		    return value;
		}
		return "";
	}

	private static String getIntFieldValue(SolrDocument solrDocument,String name)
	{
		Integer value = (Integer)solrDocument.getFieldValue(name);
		if (value != null)
		{
			return value.toString();
		}
		return "0";
	}
	
	
	private static Document createXmlDoc(long total, int start, int rows)
	{
		Document doc = DocumentHelper.createDocument();
		//Add header attributes
        Element rootEl = doc.addElement(XML_EL_EXPERIMENTS);
        String _total = Long.toString(total);
        String _start = Integer.toString(start);
        String _rows = Integer.toString(rows);

    	rootEl.addAttribute(XML_AT_TOTAL, _total);
    	if (!org.apache.commons.lang.StringUtils.isEmpty(_start))
    	{
        	rootEl.addAttribute(XML_AT_START, _start);    		
    	}

    	if (!org.apache.commons.lang.StringUtils.isEmpty(_rows))
    	{
        	rootEl.addAttribute(XML_AT_ROWS, _rows);    		
    	}

		return doc;
	}
	
	private static void createElementFromMap(Element parent,Map<String, Collection> map, String childElementName)
	{		
		Set<String> set = map.keySet();
		Iterator<String> it1=set.iterator();
		Vector<Element> vecEl = new Vector<Element>();
		while (it1.hasNext())
		{
			String name = it1.next();
			System.out.println(name);
			Collection col=map.get(name);
			if (col!=null)
			{
				Iterator it2=col.iterator();
				int index = 0;
				while (it2.hasNext())
				{
					Object o=it2.next();
					String value = convertObjectToString(o); 
					if (vecEl.size()<=index)
					{
						Element el = DocumentHelper.createElement(childElementName);
						el.addElement(name).setText(value);;
						vecEl.add(el);
						
					}
					else
					{
						Element el = vecEl.get(index);
						el.addElement(name).setText(value);
					}
					index++;
				}
			}
			
		}
		for (int i=0;i<vecEl.size();i++)
		{
			parent.add(vecEl.get(i));
		}
		
	}
	
	private static String convertObjectToString(Object o)
	{
	   if (o instanceof String) {
		  String value= (String) o;
		  return value;
	   }
	   else if (o instanceof Long) {
		  Long  value= (Long) o;
		  return value.toString();
	   }
	   else if (o instanceof Integer) 
	   {
		  Integer  value = (Integer) o;
		  return value.toString();
	   }
	   return "";
	}

}
