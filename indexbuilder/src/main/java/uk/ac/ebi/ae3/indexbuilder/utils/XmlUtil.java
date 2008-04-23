/**
 * EBI Microarray Informatics Team (c) 2007-2008
 */
package uk.ac.ebi.ae3.indexbuilder.utils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrInputDocument;
import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;

import uk.ac.ebi.ae3.indexbuilder.model.Experiment;
import uk.ac.ebi.ae3.indexbuilder.service.ConfigurationService;
/**
 * 
 * @author mdylag
 *
 */
public class XmlUtil
{
	
	/**
	 * TODO: Method does not complete
	 * @param experiment
	 */
	public static String createElement(SolrDocument doc)
	{
	    String value=(String)doc.getFieldValue("xml_doc");
	    return value;	    
	}
	
	private void run()
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
	private static void addFieldToDoc(Element element, String name, SolrInputDocument doc, String fieldName)
	{
		Attribute attr=element.attribute(name);
		if (attr!=null)
		{
			String value=attr.getStringValue();
			addField(doc, fieldName, value);
		}
	}
	
	private static final void addField(SolrInputDocument doc, String fieldName, String value)
	{
		doc.addField(fieldName, value);
		
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
		doc.addField("xml_doc", xml);
		System.out.println(xml);
		xml=xml.replace("\u0019", "");
		
		Document xmlDoc = null;
		//Parse xml String		
	        xmlDoc = DocumentHelper.parseText(xml);
		//Get Roor element
		Element elExperiment=xmlDoc.getRootElement();
		
		addFieldToDoc(elExperiment,ConfigurationService.AT_accnum , doc, ConfigurationService.FIELD_AE_EXP_ACCESSION);		
		addFieldToDoc(elExperiment, ConfigurationService.AT_id, doc, ConfigurationService.FIELD_EXP_ID);		
		addFieldToDoc(elExperiment, ConfigurationService.AT_name, doc, ConfigurationService.FIELD_EXP_NAME);		
		Element el;
		List<Element> list=elExperiment.elements(ConfigurationService.EL_users);
		for (int i=0;i<list.size();i++)
		{
			el=list.get(i);
			Iterator<Element> it=el.elementIterator(ConfigurationService.EL_user);
			while (it.hasNext())
			{
				Element e=it.next();
				addFieldToDoc(e, "id", doc, "exp_user_id");
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
				addFieldToDoc(e, "CATEGORY", doc, "exp_saat_cat");
				addFieldToDoc(e, "VALUE", doc, "exp_saat_value");				
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
				addFieldToDoc(e, "FACTORNAME", doc, "exp_fv_factorname");
				addFieldToDoc(e, "FV_OE", doc, "exp_fv_oe");				
			}
		}
		
		list=elExperiment.elements(ConfigurationService.EL_miamescores);
		for (int i=0;i<list.size();i++)
		{
			el=list.get(i);
			Iterator<Element> it=el.elementIterator(ConfigurationService.EL_miamescore);
			while (it.hasNext())
			{
				Element e=it.next();
				addFieldToDoc(e, "name", doc, "exp_mimescore_name");
				addFieldToDoc(e, "value", doc, "exp_mimescore_value");				
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
				addFieldToDoc(e, "id", doc, "exp_arraydes_id");
				addFieldToDoc(e, "identifier", doc, "exp_arraydes_identifier");
				addFieldToDoc(e, "name", doc, "exp_arraydes_name");
				addFieldToDoc(e, "count", doc, "exp_arraydes_count");				
				
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
				addFieldToDoc(e, "name", doc, "exp_bdg_name");
				addFieldToDoc(e, "id", doc, "exp_bdg_id");
				addFieldToDoc(e, "num_bad_cubes", doc, "exp_bdg_num_bad_cubes");
				addFieldToDoc(e, "arraydesign", doc, "exp_bdg_arraydesign");				
				addFieldToDoc(e, "dataformat", doc, "exp_bdg_dataformat");				
				addFieldToDoc(e, "bioassay_count", doc, "exp_bdg_bioassay_count");
				addFieldToDoc(e, "is_derived", doc, "exp_bdg_is_derived");				
			}
		}		
		list=elExperiment.elements(ConfigurationService.EL_bibliography);
		for (int i=0;i<list.size();i++)
		{
			el=list.get(i);
			addFieldToDoc(el, "publication", doc, "exp_bi_publication");
			addFieldToDoc(el, "authors", doc, "exp_bi_authors");
			addFieldToDoc(el, "title", doc, "exp_bi_title");
			addFieldToDoc(el, "year", doc, "exp_bi_year");
			addFieldToDoc(el, "volume", doc, "exp_bi_volume");
			addFieldToDoc(el, "issue", doc, "exp_bi_issue");
			addFieldToDoc(el, "pages", doc, "exp_bi_pages");
			
		}		
		list=elExperiment.elements(ConfigurationService.EL_providers);
		for (int i=0;i<list.size();i++)
		{
			el=list.get(i);
			Iterator<Element> it=el.elementIterator("provider");
			while (it.hasNext())
			{
				Element e=it.next();
				addFieldToDoc(e, "contact", doc, "exp_provider_contact");
				addFieldToDoc(e, "role", doc, "exp_provider_role");
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
				addFieldToDoc(e, "type", doc, "exp_expdes_type");
			}
		}				
		list=elExperiment.elements(ConfigurationService.EL_description);
		for (int i=0;i<list.size();i++)
		{
			el=list.get(i);
			addFieldToDoc(el, "id", doc, "exp_desc_id");
			doc.addField(ConfigurationService.FIELD_EXP_DESC_TEXT, el.getText());			
		}		
		return doc;
	}
}
