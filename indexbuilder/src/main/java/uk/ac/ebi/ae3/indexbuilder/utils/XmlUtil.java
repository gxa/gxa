package uk.ac.ebi.ae3.indexbuilder.utils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import uk.ac.ebi.ae3.indexbuilder.model.Experiment;
import uk.ac.ebi.ae3.indexbuilder.model.SampleAttribute;

public class XmlUtil
{
	private static final String EL_EXPERIMENT="";
	//private static final String EL_;
	
	public static void createExperiment(final String xml, final Experiment exp) 
	throws DocumentException
	{		
		Document doc = DocumentHelper.parseText(xml);
		Element elExperiment=doc.getRootElement();
		exp.setName(elExperiment.attribute("name").getText());
		//Get user
		exp.setUserId(elExperiment.element("users").element("user").attribute("id").getStringValue());
	}
	
	private static List<SampleAttribute> getSampleAttributes(Element elExperiment)
	{
		List<SampleAttribute> list = new ArrayList<SampleAttribute>();
		List<Element> sampleAttr=elExperiment.element("sampleattributes").elements();
		Iterator<Element> it=sampleAttr.iterator();
		Element el;
		while (it.hasNext())
		{
			SampleAttribute sampleAttribute = new SampleAttribute(); 
			el=it.next();
			sampleAttribute.setCategory(el.attribute("CATEGORY").getStringValue());
			sampleAttribute.setValue(el.attribute("VALUE").getStringValue());
			list.add(sampleAttribute);

		}
		
		
		return list;
	}
	
	public static void createXml(Experiment experiment)
	{
		
	}
}
