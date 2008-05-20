package ae3.service;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;

import uk.ac.ebi.ae3.indexbuilder.Constants;
import ae3.model.AtlasExperiment;

public class XmlHelper
{
	public static final String XML_EL_EXPERIMENTS= "experiments";
	public static final String XML_EL_EXPERIMENT= "experiment";
	public static final String XML_EL_KEYWORDS= "keywords";
	public static final String XML_EL_KEYWORD= "keyword";
	
	public static final String XML_AT_COUNT= "count";
	public static final String XML_AT_START= "start";
	public static final String XML_AT_ROWS= "rows";
	
	public static Document createXmlDoc(List<AtlasExperiment> expts, String[] keywords, String count, String start, String rows)
	{
		Document doc = createXmlDoc(keywords, count, start, rows);
		Element el=doc.getRootElement();
    	Iterator<AtlasExperiment> itExps=expts.iterator();
    	while (itExps.hasNext())
    	{
    		AtlasExperiment exp=itExps.next();
    		addExperimentToDoc(el, exp);
    	}
		return doc;
	}

	private static void addExperimentToDoc(Element elExps, AtlasExperiment expt)
	{
		Element elExp=elExps.addElement(XML_EL_EXPERIMENT);
		//Experiment identifier
		elExp.addAttribute(Constants.AT_accnum, expt.getAerExpAccession());
		//Experiment id
		elExp.addAttribute(Constants.AT_id, Long.toString(expt.getAerExpId()));
		//experiment name
		elExp.addAttribute(Constants.AT_name, expt.getAerExpName());		
		elExp.addElement(Constants.EL_users);
		
		//Add sample attribute
		Element el=elExp.addElement(Constants.EL_sample_attributes);
		Vector<String> elNames = new Vector<String>();
		elNames.add(Constants.AT_CATEGORY);
		elNames.add(Constants.AT_VALUE);
		if (expt.hasAerSampleAttributes())
		{
			createElementsFromCollection(el, Constants.EL_sampleattribute, 
					elNames, expt.getAerSampleAttributes());
			
		}

		el=elExp.addElement(Constants.EL_factorvalues);
		elNames.clear();
		elNames.add(Constants.AT_FACTORNAME);
		elNames.add(Constants.AT_FV_OE);
		if (expt.hasAerFactorAttributes())
		{
			createElementsFromCollection(el, Constants.EL_factorvalue, 
					elNames, expt.getAerFactorAttributes());
			
		}
		//TODO: Add mimescore 
		el=elExp.addElement(Constants.EL_arraydesigns);
		elNames.clear();
		elNames.add(Constants.AT_id);
		elNames.add(Constants.AT_identifier);
		elNames.add(Constants.AT_name);
		elNames.add(Constants.AT_count);		
		if (expt.hasAerArrayDesigns())
		{
			createElementsFromCollection(el, Constants.EL_arraydesign, 
					elNames, expt.getAerArrayDesigns());
			
		}
		//
		
		
		
		
	}
	
	private static void createElementsFromCollection(Element parent, String childElName, 
													 Vector<String> elName, Vector<Collection<String>> vec)
	{
		Vector<Element> vecElem = new Vector<Element>();
		for (int i=0; i<vec.size(); i++)
		{
		  Collection col1 = vec.get(i);
		  Iterator<String> it=col1.iterator();
		  int k=0;
		  boolean bAddtoVec = false;
		  while (it.hasNext())
		  {
			  String value=it.next();
			  Element elChild = null;
			  if (k<vecElem.size())
			  {
				  elChild=vecElem.get(k);
				  bAddtoVec = false;
			  }			  
			  if (elChild==null)
			  {
				  elChild = parent.addElement(childElName);
				  bAddtoVec = true;				  
			  }
			  Element elChild1= elChild.addElement(elName.get(i));
			  elChild1.setText(value);
			  if (bAddtoVec)
			  {
				  vecElem.add(elChild);
			  }
			  k++;
		  }
		}

//		while (it.hasNext())
	//	{
		//	String cat=it.next();
			//Element _el=parent.addElement(childElName);
			//_el.addElement(elName1).setText(cat);
			//_el.addElement(elName2).setText(it2.next());
		//}
		
	}
	
	public static Document createXmlDoc(String[] keywords, String count)
	{
		return createXmlDoc(keywords, count, null, null);
	}
	public static Document createXmlDoc(String[] keywords, String count, String start, String rows)
	{
		Document doc = DocumentHelper.createDocument();
		//Add count information
        Element rootEl = doc.addElement(XML_EL_EXPERIMENTS);
        Element keyWordEl = rootEl.addElement(XML_EL_KEYWORDS);        
        if (keywords != null & keywords.length > 0)
        {
        	for (String string : keywords)
			{
        		System.out.println(string);
                Element keyWordsEl = keyWordEl.addElement(XML_EL_KEYWORD);
        		keyWordsEl.addText(string);				
			}
        }
    	
    	rootEl.addAttribute("count", count);
    	if (!org.apache.commons.lang.StringUtils.isEmpty(start))
    	{
        	rootEl.addAttribute("start", start);    		
    	}

    	if (!org.apache.commons.lang.StringUtils.isEmpty(rows))
    	{
        	rootEl.addAttribute("rows", rows);    		
    	}

		return doc;
	}
	/**
	 * Function searches data in an index and create the XML document and print it
	 * @return
	 */
	public static void createXmlAndPrint()
	{
		//get
		Document doc = DocumentHelper.createDocument();
		
	}
	
}
