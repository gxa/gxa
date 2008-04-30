/**
 * EBI Microarray Informatics Team (c) 2007-2008
 */
package uk.ac.ebi.ae3.indexbuilder.utils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.text.DefaultStyledDocument.AttributeUndoableEdit;

import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrInputDocument;
import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.util.AttributeHelper;
import org.springframework.util.StringUtils;

import uk.ac.ebi.ae3.indexbuilder.IndexBuilderException;
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

	/**
	 * 
	 * @param element
	 * @param name
	 * @param doc
	 * @param fieldName
	 * @return
	 */
	private static void addFieldFromAttr(Element element, String name, SolrInputDocument doc, String fieldName)
	{
		Attribute attr=element.attribute(name);
		if (attr!=null)
		{
			String value=attr.getStringValue();
			addField(doc, fieldName, value);
		}
	}
	
	private static void addFieldFromEl(Element element, SolrInputDocument doc, String fieldName)
	{
		if (element!=null)
		{
			String value=element.getText();
			addField(doc, fieldName, value);
		}
	}

	
	private static final void addField(SolrInputDocument doc, String fieldName, String value)
	{
		doc.addField(fieldName, value);
		
	}
	

	/**
	 * Extracts all elements and attributes from xml and put as fields into the solr index core "expt"
	 * @param xmlDw
	 * @param doc
	 * @throws DocumentException
	 * @throws IndexBuilderException
	 */
	public static void addExperimentFromDW(String xmlDw, SolrInputDocument doc) throws DocumentException, IndexBuilderException
	{
		if (doc == null)
			throw new IndexBuilderException("Solr documentd can not be null");
		if (xmlDw == null)
			return;
		Document xmlDoc = null;
		//Parse xml String		
        xmlDoc = DocumentHelper.parseText(xmlDw);
        Element elExperiment=xmlDoc.getRootElement();
        addFieldFromAttr(elExperiment, "EXPERIMENT_ID_KEY", doc, "dwexp_id");
        addFieldFromAttr(elExperiment, "EXPERIMENT_IDENTIFIER", doc, "dwexp_accession");
        addFieldFromAttr(elExperiment, "EXPERIMENT_DESCRIPTION", doc, "dwexp_expdescription");
        addFieldFromEl(elExperiment, doc, "dwexp_exptype");
        //  
        //get bioassays
        for (int i=0; i<ConfigurationService.ARRAY_ASSAY_ELEMENTS.length; i++)
        {
        	Element assElement = elExperiment.element(ConfigurationService.EL_assay_attributes);
        	Element element =assElement.element(ConfigurationService.ARRAY_ASSAY_ELEMENTS[i]);
    		String assId=null;
    		String value=null;    		
        	
        	if (element != null)
        	{
        		value = element.getText();
        		if (!org.apache.commons.lang.StringUtils.isEmpty(value))
        		{
        			Attribute attr=element.attribute(ConfigurationService.AT_ASSAY_ID);
        			if (attr!=null)
        			{
        			
        				assId=attr.getStringValue();
        			}
            		//Add data to index
            		doc.addField(ConfigurationService.PREFIX_AEDW + ConfigurationService.ARRAY_ASSAY_ELEMENTS[i], value);
            		doc.addField(ConfigurationService.PREFIX_AEDW  + ConfigurationService.ARRAY_ASSAY_ELEMENTS[i]+ ConfigurationService.SUFFIX_ASSAY_ID, assId);        			
        		}
        	}
        }
        //process samples
        for (int i=0; i<ConfigurationService.ARRAY_SAMPLE_ELEMENTS.length; i++)
        {
        	Element assElement = elExperiment.element(ConfigurationService.EL_assay_attributes);
        	Element element =assElement.element(ConfigurationService.ARRAY_ASSAY_ELEMENTS[i]);
    		String assId=null;
    		String sampleId=null;    		
    		String value=null;    		
        	
        	if (element != null)
        	{
        		value = element.getText();
        		if (!org.apache.commons.lang.StringUtils.isEmpty(value))
        		{
        			Attribute attr1=element.attribute(ConfigurationService.AT_ASSAY_ID);
        			Attribute attr2=element.attribute(ConfigurationService.AT_SAMPLE_ID);
        			
        			if (attr1!=null)
        			{
        			
        				assId=attr1.getStringValue();
        			}
        			if (attr2!=null)
        			{
        			
        				sampleId=attr2.getStringValue();
        			}
            		//Add data to index
            		doc.addField(ConfigurationService.PREFIX_AEDW + ConfigurationService.ARRAY_ASSAY_ELEMENTS[i], value);
            		doc.addField(ConfigurationService.PREFIX_AEDW  + ConfigurationService.ARRAY_ASSAY_ELEMENTS[i]+ ConfigurationService.SUFFIX_ASSAY_ID, assId);
            		doc.addField(ConfigurationService.PREFIX_AEDW  + ConfigurationService.ARRAY_ASSAY_ELEMENTS[i]+ ConfigurationService.SUFFIX_SAMPLE_ID, sampleId);        			
            		
        		}
        	}
        }
        
	}
	
	/**
	 * 
	 * @param xmlAe
	 * @return
	 * @throws DocumentException
	 */
	public static SolrInputDocument createSolrInputDoc(String xmlAe) throws DocumentException
	{
		//Create an instance of SolrInputDocument 
		SolrInputDocument doc = new SolrInputDocument();
		doc.addField(ConfigurationService.FIELD_XML_DOC_AER, xmlAe);
		xmlAe=xmlAe.replace("\u0019", "");
		
		Document xmlDoc = null;
		//Parse xml String		
        xmlDoc = DocumentHelper.parseText(xmlAe);
		//Get Roor element
		Element elExperiment=xmlDoc.getRootElement();
		
		addFieldFromAttr(elExperiment,ConfigurationService.AT_accnum , doc, ConfigurationService.FIELD_AER_EXPACCESSION);		
		addFieldFromAttr(elExperiment, ConfigurationService.AT_id, doc, ConfigurationService.FIELD_AER_EXPID);		
		addFieldFromAttr(elExperiment, ConfigurationService.AT_name, doc, ConfigurationService.FIELD_AER_EXPNAME);		
		Element el;
		List<Element> list=elExperiment.elements(ConfigurationService.EL_users);
		for (int i=0;i<list.size();i++)
		{
			el=list.get(i);
			Iterator<Element> it=el.elementIterator(ConfigurationService.EL_user);
			while (it.hasNext())
			{
				Element e=it.next();
				addFieldFromAttr(e, "id", doc, ConfigurationService.FIELD_AER_USER_ID);
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
				addFieldFromAttr(e, "CATEGORY", doc, ConfigurationService.FIELD_AER_SAAT_CAT);
				addFieldFromAttr(e, "VALUE", doc, ConfigurationService.FIELD_AER_SAAT_VALUE);				
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
				addFieldFromAttr(e, "FACTORNAME", doc, ConfigurationService.FIELD_AER_FV_FACTORNAME );
				addFieldFromAttr(e, "FV_OE", doc, ConfigurationService.FIELD_AER_FV_OE);				
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
				addFieldFromAttr(e, "name", doc, ConfigurationService.FIELD_AER_MIMESCORE_NAME);
				addFieldFromAttr(e, "value", doc, ConfigurationService.FIELD_AER_MIMESCORE_VALUE);				
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
				addFieldFromAttr(e, "id", doc, ConfigurationService.FIELD_AER_ARRAYDES_ID);
				addFieldFromAttr(e, "identifier", doc, ConfigurationService.FIELD_AER_ARRAYDES_IDENTIFIER);
				addFieldFromAttr(e, "name", doc, ConfigurationService.FIELD_AER_ARRAYDES_NAME);
				addFieldFromAttr(e, "count", doc, ConfigurationService.FIELD_AER_ARRAYDES_COUNT);				
				
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
				addFieldFromAttr(e, "name", doc, ConfigurationService.FIELD_AER_BDG_NAME);
				addFieldFromAttr(e, "id", doc, ConfigurationService.FIELD_AER_BDG_ID);
				addFieldFromAttr(e, "num_bad_cubes", doc, ConfigurationService.FIELD_AER_BDG_NUM_BAD_CUBES);
				addFieldFromAttr(e, "arraydesign", doc, ConfigurationService.FIELD_AER_BDG_ARRAYDESIGN);				
				addFieldFromAttr(e, "dataformat", doc, ConfigurationService.FIELD_AER_BDG_DATAFORMAT);				
				addFieldFromAttr(e, "bioassay_count", doc, ConfigurationService.FIELD_AER_BDG_BIOASSAY_COUNT);
				addFieldFromAttr(e, "is_derived", doc, ConfigurationService.FIELD_AER_BDG_IS_DERIVED);				
			}
		}		
		list=elExperiment.elements(ConfigurationService.EL_bibliography);
		for (int i=0;i<list.size();i++)
		{
			el=list.get(i);
			addFieldFromAttr(el, "publication", doc, ConfigurationService.FIELD_AER_BI_PUBLICATION);
			addFieldFromAttr(el, "authors", doc, ConfigurationService.FIELD_AER_BI_AUTHORS);
			addFieldFromAttr(el, "title", doc, ConfigurationService.FIELD_AER_BI_TITLE);
			addFieldFromAttr(el, "year", doc, ConfigurationService.FIELD_AER_BI_YEAR);
			addFieldFromAttr(el, "volume", doc, ConfigurationService.FIELD_AER_BI_VOLUME);
			addFieldFromAttr(el, "issue", doc, ConfigurationService.FIELD_AER_BI_ISSUE);
			addFieldFromAttr(el, "pages", doc, ConfigurationService.FIELD_AER_BI_PAGES);
			
		}		
		list=elExperiment.elements(ConfigurationService.EL_providers);
		for (int i=0;i<list.size();i++)
		{
			el=list.get(i);
			Iterator<Element> it=el.elementIterator("provider");
			while (it.hasNext())
			{
				Element e=it.next();
				addFieldFromAttr(e, "contact", doc, ConfigurationService.FIELD_AER_PROVIDER_CONTRACT);
				addFieldFromAttr(e, "role", doc, ConfigurationService.FIELD_AER_PROVIDER_ROLE);
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
				addFieldFromAttr(e, "type", doc, ConfigurationService.FIELD_AER_EXPDES_TYPES);
			}
		}				
		list=elExperiment.elements(ConfigurationService.EL_description);
		for (int i=0;i<list.size();i++)
		{
			el=list.get(i);
			addFieldFromAttr(el, "id", doc, ConfigurationService.FIELD_AER_DESC_ID);
			doc.addField(ConfigurationService.FIELD_AER_DESC_TEXT, el.getText());			
		}		
		return doc;
	}
}
