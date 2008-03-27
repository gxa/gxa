/**
 * EBI Microarray Informatics Team (c) 2007-2008
 */
package uk.ac.ebi.ae3.indexbuilder.utils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.solr.common.SolrInputDocument;
import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;

import uk.ac.ebi.ae3.indexbuilder.model.Experiment;
import uk.ac.ebi.ae3.indexbuilder.model.SampleAttribute;
import uk.ac.ebi.ae3.indexbuilder.service.ConfigurationService;
/**
 * 
 * @author mdylag
 *
 */
public class XmlUtil
{
	/**
	 * 
	 * @param xml
	 * @param exp
	 * @throws DocumentException
	 */
	public static void createExperiment(final String xml, final Experiment exp) 
	throws DocumentException
	{		
		Document doc = DocumentHelper.parseText(xml);
		Element elExperiment=doc.getRootElement();
		exp.setName(elExperiment.attribute("name").getText());
		//Get user
		exp.setUserId(elExperiment.element("users").element("user").attribute("id").getStringValue());
		exp.setSampleAtrList(getSampleAttributes(elExperiment));
	}
	
	/**
	 * 
	 * @param elExperiment
	 * @return
	 */
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
	
	/**
	 * TODO: Method does not complete
	 * @param experiment
	 */
	private static void createXml(Experiment experiment)
	{
		
	}

	/**
	 * 
	 * @param element
	 * @param name
	 * @param doc
	 * @param fieldName
	 * @return
	 */
	private static String addFieldToDoc(Element element, String name, SolrInputDocument doc, String fieldName)
	{
		Attribute attr=element.attribute(name);
		String value = null;
		if (attr!=null)
		{
			value = attr.getStringValue();
			doc.addField(fieldName, value); 
		}
		return value;			
	}
	
	
	
	/**
	 * 
	 * @param xml
	 * @return
	 * @throws DocumentException
	 */
	public static SolrInputDocument createSolrInputDoc(String xml) throws DocumentException
	{
		//Create an instance of SolrInputDocument 
		SolrInputDocument doc = new SolrInputDocument();
        
		//Parse xml
		Document xmlDoc = DocumentHelper.parseText(xml);
		//Get Roor element
		Element elExperiment=xmlDoc.getRootElement();
		
		addFieldToDoc(elExperiment,ConfigurationService.AT_accnum , doc, "ConfigurationService.ACCESION_NUMBER");		
		addFieldToDoc(elExperiment, ConfigurationService.AT_id, doc, "exp_id");		
		addFieldToDoc(elExperiment, ConfigurationService.AT_name, doc, "exp_name");		
		addFieldToDoc(elExperiment, ConfigurationService.AT_releasedate, doc, "exp_releasedate");	
		Element el;
		List<Element> list=elExperiment.elements(ConfigurationService.EL_users);
		for (int i=0;i<list.size();i++)
		{
			el=list.get(i);
			Iterator<Element> it=el.elementIterator(ConfigurationService.EL_user);
			while (it.hasNext())
			{
				Element e=it.next();
				addFieldToDoc(e, "CATEGORY", doc, "saat_cat");
				addFieldToDoc(e, "VALUE", doc, "saat_value");				
			}
			
		}
		
		list=elExperiment.elements(ConfigurationService.EL_secondaryaccessions);
		for (int i=0;i<list.size();i++)
		{
			
		}
		//process sample attributes
		list=elExperiment.elements(ConfigurationService.EL_sampleattributes);
		for (int i=0;i<list.size();i++)
		{
			el=list.get(i);
			Iterator<Element> it=el.elementIterator(ConfigurationService.EL_sampleattribute);
			while (it.hasNext())
			{
				Element e=it.next();
				addFieldToDoc(e, "CATEGORY", doc, "saat_cat");
				addFieldToDoc(e, "VALUE", doc, "saat_value");				
			}
		}
		
		list=elExperiment.elements(ConfigurationService.EL_factorvalues);
		for (int i=0;i<list.size();i++)
		{
			el=list.get(i);
			Iterator<Element> it=el.elementIterator(ConfigurationService.EL_factorvalue);
			while (it.hasNext())
			{
				Element e=it.next();
				addFieldToDoc(e, "FACTORNAME", doc, "fv_factorname");
				addFieldToDoc(e, "FV_OE", doc, "fv_oe");				
			}
		}
		
		list=elExperiment.elements(ConfigurationService.EL_miamescores);
		for (int i=0;i<list.size();i++)
		{
			el=list.get(i);
			Iterator<Element> it=el.elementIterator(ConfigurationService.EL_miamescore);
			addFieldToDoc(el, "mimescore", doc, "mimescore");
			while (it.hasNext())
			{
				Element e=it.next();
				addFieldToDoc(e, "name", doc, "mimescore_name");
				addFieldToDoc(e, "value", doc, "mimescore_value");				
			}
		}
		
		list=elExperiment.elements(ConfigurationService.EL_arraydesigns);
		for (int i=0;i<list.size();i++)
		{
			el=list.get(i);
			Iterator<Element> it=el.elementIterator(ConfigurationService.EL_arraydesign);
			while (it.hasNext())
			{
				Element e=it.next();
				addFieldToDoc(e, "id", doc, "arraydes_id");
				addFieldToDoc(e, "identifier", doc, "arraydes_identifier");
				addFieldToDoc(e, "name", doc, "arraydes_name");
				addFieldToDoc(e, "count", doc, "arraydes_count");				
				
			}
		}
		
		list=elExperiment.elements(ConfigurationService.EL_bioassaydatagroups);
		for (int i=0;i<list.size();i++)
		{
			el=list.get(i);
			Iterator<Element> it=el.elementIterator(ConfigurationService.EL_bioassaydatagroup);
			while (it.hasNext())
			{
				Element e=it.next();
				addFieldToDoc(e, "name", doc, "bdg_name");
				addFieldToDoc(e, "id", doc, "bdg_id");
				addFieldToDoc(e, "num_bad_cubes", doc, "bdg_num_bad_cubes");
				addFieldToDoc(e, "arraydesign", doc, "bdg_arraydesign");				
				addFieldToDoc(e, "dataformat", doc, "bdg_dataformat");				
				addFieldToDoc(e, "bioassay_count", doc, "bdg_bioassay_count");
				addFieldToDoc(e, "is_derived", doc, "bdg_is_derived");				
			}
		}		
		list=elExperiment.elements(ConfigurationService.EL_bibliography);
		for (int i=0;i<list.size();i++)
		{
			el=list.get(i);
			addFieldToDoc(el, "publication", doc, "bi_publication");
			addFieldToDoc(el, "authors", doc, "bi_authors");
			addFieldToDoc(el, "title", doc, "bi_title");
			addFieldToDoc(el, "year", doc, "bi_year");
			addFieldToDoc(el, "volume", doc, "bi_volume");
			addFieldToDoc(el, "issue", doc, "bi_issue");
			addFieldToDoc(el, "pages", doc, "bi_pages");
			
		}		
		list=elExperiment.elements(ConfigurationService.EL_providers);
		for (int i=0;i<list.size();i++)
		{
			el=list.get(i);
			Iterator<Element> it=el.elementIterator("provider");
			while (it.hasNext())
			{
				Element e=it.next();
				addFieldToDoc(e, "contact", doc, "provider_contact");
				addFieldToDoc(e, "role", doc, "provider_role");
			}
		}				
		list=elExperiment.elements(ConfigurationService.EL_experimentdesigns);
		for (int i=0;i<list.size();i++)
		{
			el=list.get(i);
			Iterator<Element> it=el.elementIterator(ConfigurationService.EL_experimentdesign);
			while (it.hasNext())
			{
				Element e=it.next();
				addFieldToDoc(e, "type", doc, "expdes_type");
			}
		}				
		list=elExperiment.elements(ConfigurationService.EL_description);
		for (int i=0;i<list.size();i++)
		{
			el=list.get(i);
			addFieldToDoc(el, "id", doc, "desc_id");
			doc.addField("desc_text", el.getText());			
		}		
		return doc;
	}
}
