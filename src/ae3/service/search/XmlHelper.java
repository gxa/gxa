package ae3.service.search;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import org.apache.solr.common.SolrDocumentList;
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
	
	public static final String XML_AT_COUNT= "total";
	public static final String XML_AT_START= "start";
	public static final String XML_AT_ROWS= "rows";

	private static Document createXmlDoc(SolrDocumentList list, long count, int start, int rows)
	{
		Document doc = createXmlDoc(count, start, rows);
		Element el=doc.getRootElement();
		
		return doc;
	}
	
	public static Document createXmlDoc(long total, int start, int rows)
	{
		Document doc = DocumentHelper.createDocument();
		//Add header attributes
        Element rootEl = doc.addElement(XML_EL_EXPERIMENTS);
        String _total = Long.toString(total);
        String _start = Integer.toString(start);
        String _rows = Integer.toString(rows);

    	rootEl.addAttribute("total", _total);
    	if (!org.apache.commons.lang.StringUtils.isEmpty(_start))
    	{
        	rootEl.addAttribute("start", _start);    		
    	}

    	if (!org.apache.commons.lang.StringUtils.isEmpty(_rows))
    	{
        	rootEl.addAttribute("rows", _rows);    		
    	}

		return doc;
	}

	/*private static void addExperimentToDoc(Element elExps, AtlasExperiment expt)
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
		//el=elExp.addElement(Constants.EL_arraydesigns);
		//elNames.clear();
		//elNames.add(Constants.AT_id);
		//elNames.add(Constants.AT_identifier);
		//elNames.add(Constants.AT_name);
		//elNames.add(Constants.AT_count);		
		//if (expt.hasAerArrayDesigns())
		//{
			//createElementsFromCollection(el, Constants.EL_arraydesign, 
				//	elNames, expt.getAerArrayDesigns());
			
		//}
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
	
	*/

}
