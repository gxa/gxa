/**
 * EBI Microarray Informatics Team (c) 2007-2008
 */
package uk.ac.ebi.ae3.indexbuilder.utils;

import org.apache.commons.lang.StringUtils;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrInputDocument;
import org.dom4j.*;
import uk.ac.ebi.ae3.indexbuilder.Constants;
import uk.ac.ebi.ae3.indexbuilder.IndexBuilderException;

import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;
/**
 * 
 * @author Miroslaw Dylag
 *
 */
@Deprecated
public class XmlUtil
{
	private static final Logger log = Logger.getLogger(XmlUtil.class.getName());

  @Deprecated
	public static String createElement(SolrDocument doc)
	{
	    String value=(String)doc.getFieldValue("xml_doc");
	    return value;	    
	}	

  @Deprecated
	private static void addFieldFromAttr(Element element, String name, SolrInputDocument doc, String fieldName)
	{
		Attribute attr=element.attribute(name);
		if (attr!=null)
		{
			String value=attr.getStringValue();
			addField(doc, fieldName, value);
		}
	}

@Deprecated
	private static void addFieldFromEl(Element element, SolrInputDocument doc, String fieldName)
	{
		if (element!=null)
		{
			String value=element.getText();
			addField(doc, fieldName, value);
		}
	}


@Deprecated
	private static final void addField(SolrInputDocument doc, String fieldName, String value)
	{
	    if (StringUtils.isEmpty(value))
	    {
		doc.addField(fieldName, "");		
	    }
	    else
	    {
		doc.addField(fieldName, value);
	    }
		
	}
	

	/**
	 * Extracts all elements and attributes from xml and put as fields into the solr index core "expt"
	 * @param xmlDw
	 * @param doc
	 * @throws DocumentException
	 * @throws IndexBuilderException
	 */
  @Deprecated
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

        @SuppressWarnings("unchecked")
        List<Element> fields = xmlDoc.getRootElement().elements("field");
        for(Element field : fields) {
        	doc.addField(Constants.PREFIX_DWE+field.attribute("name").getValue(), field.getText());
        }
        
//        addFieldFromAttr(elExperiment, "EXPERIMENT_ID_KEY", doc, Constants.FIELD_DWEXP_ID);
//        addFieldFromAttr(elExperiment, "EXPERIMENT_IDENTIFIER", doc, Constants.FIELD_DWEXP_ACCESSION);
//        addFieldFromAttr(elExperiment, "EXPERIMENT_DESCRIPTION", doc, Constants.FIELD_DWEXP_EXPDESC);
//        addFieldFromEl(elExperiment, doc, Constants.FIELD_DWEXP_EXPTYPE);
        
        
        //Uncommented for now until DB XML is updated to include inidividual biosamples. Currently all BS attributes will be all in one field bs_attribute  
        //get bioassays
//        for (int i=0; i<Constants.ARRAY_ASSAY_ELEMENTS.length; i++)
//        {
//        	Element assElement = elExperiment.element(Constants.EL_assay_attributes);
//        	Iterator<Element> elementIt =assElement.elementIterator(Constants.ARRAY_ASSAY_ELEMENTS[i]);
//    		String assId=null;
//    		String value=null;    		
//        	while (elementIt.hasNext())
//        	{
//        		Element element=elementIt.next();
//        		value = element.getText();
//    			Attribute attr=element.attribute(Constants.AT_ASSAY_ID);
//    			if (attr!=null)
//    			{
//    			
//    				assId=attr.getStringValue();
//    			}
//        		if (!org.apache.commons.lang.StringUtils.isEmpty(value))
//        		{
//            		//Add data to index
////            		doc.addField(Constants.PREFIX_DWE + Constants.ARRAY_ASSAY_ELEMENTS[i], value);
//            		doc.addField(Constants.PREFIX_DWE  + "ids_" + Constants.ARRAY_ASSAY_ELEMENTS[i] + "_" + Constants.SUFFIX_ASSAY_ID, assId);
//        		}
//    			
//        		
//        	}
//       	}
        //process samples
//        for (int i=0; i<Constants.ARRAY_SAMPLE_ELEMENTS.length; i++)
//        {
//        	Element assElement = elExperiment.element(Constants.EL_assay_attributes);
//        	
//        	Iterator<Element> elementIt = assElement.elementIterator(Constants.ARRAY_ASSAY_ELEMENTS[i]);
//    		String assId=null;
//    		String sampleId=null;    		
//    		String value=null;    		
//        	
//    		while (elementIt.hasNext())
//        	{
//    			Element element = elementIt.next();
//        		value = element.getText();
//        		if (!org.apache.commons.lang.StringUtils.isEmpty(value))
//        		{
//        			Attribute attr1=element.attribute(Constants.AT_ASSAY_ID);
//        			Attribute attr2=element.attribute(Constants.AT_SAMPLE_ID);
//        			
//        			if (attr1!=null)
//        			{
//        			
//        				assId=attr1.getStringValue();
//        			}
//        			if (attr2!=null)
//        			{
//        			
//        				sampleId=attr2.getStringValue();
//        			}
//            		//Add data to index
//            		doc.addField(Constants.PREFIX_DWE + Constants.ARRAY_SAMPLE_ELEMENTS[i], value);
//            		doc.addField(Constants.PREFIX_DWE  + "ids_" + Constants.ARRAY_SAMPLE_ELEMENTS[i] + "_" + Constants.SUFFIX_ASSAY_ID, assId);
//            		doc.addField(Constants.PREFIX_DWE  + "ids_" + Constants.ARRAY_SAMPLE_ELEMENTS[i] + "_" + Constants.SUFFIX_SAMPLE_ID, sampleId);        			
//            		
//        		}
//        	}
//        }
        
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
		//doc.addField(Constants.FIELD_XML_DOC_AER, xmlAe);
		xmlAe=xmlAe.replace("\u0019", "");
		
		Document xmlDoc = null;
		//Parse xml String		
        xmlDoc = DocumentHelper.parseText(xmlAe);
		//Get Roor element
		Element elExperiment=xmlDoc.getRootElement();
		
		addFieldFromAttr(elExperiment,Constants.AT_accession , doc, Constants.FIELD_AER_EXPACCESSION);		
		addFieldFromAttr(elExperiment, Constants.AT_id, doc, Constants.FIELD_AER_EXPID);		
		addFieldFromAttr(elExperiment, Constants.AT_name, doc, Constants.FIELD_AER_EXPNAME);
		addFieldFromAttr(elExperiment, Constants.AT_releasedate, doc, Constants.FIELD_AER_RELEASEDATE);
		
		
		Element el;
		List<Element> list=elExperiment.elements(Constants.EL_user);
		for (int i=0;i<list.size();i++)
		{
			el=list.get(i);
			addFieldFromEl(el, doc, Constants.FIELD_AER_USER_ID);
		}
		
		list=elExperiment.elements(Constants.EL_secondaryaccession);
		for (int i=0;i<list.size();i++)
		{
			
		}
		//process sample attributes
		list=elExperiment.elements(Constants.EL_sampleattribute);
		for (int i=0;i<list.size();i++)
		{
			el=list.get(i);
			addFieldFromAttr(el, "category", doc, Constants.FIELD_AER_SAAT_CAT);
			addFieldFromAttr(el, "value", doc, Constants.FIELD_AER_SAAT_VALUE);				
		}
		//process experiment factor
		list=elExperiment.elements(Constants.EL_factorvalue);
		for (int i=0;i<list.size();i++)
		{
			el=list.get(i);
			addFieldFromAttr(el, Constants.AT_name, doc, Constants.FIELD_AER_FV_FACTORNAME );
			addFieldFromAttr(el, Constants.AT_value, doc, Constants.FIELD_AER_FV_OE);				
		}
		//process mimescore
		list=elExperiment.elements(Constants.EL_miamescore);
		for (int i=0;i<list.size();i++)
		{
			el=list.get(i);
			Iterator<Element> it=el.elementIterator(Constants.EL_score);
			while (it.hasNext())
			{
				Element e=it.next();
				addFieldFromAttr(e, Constants.AT_name, doc, Constants.FIELD_AER_MIMESCORE_NAME);
				addFieldFromAttr(e, Constants.AT_value, doc, Constants.FIELD_AER_MIMESCORE_VALUE);				
			}
		}
		//process arraydesign
		list=elExperiment.elements(Constants.EL_arraydesign);
		for (int i=0;i<list.size();i++)
		{
			el=list.get(i);
			addFieldFromAttr(el, Constants.AT_id, doc, Constants.FIELD_AER_ARRAYDES_ID);
			addFieldFromAttr(el, Constants.AT_identifier, doc, Constants.FIELD_AER_ARRAYDES_IDENTIFIER);
			addFieldFromAttr(el, Constants.AT_name, doc, Constants.FIELD_AER_ARRAYDES_NAME);
			addFieldFromAttr(el, Constants.AT_count, doc, Constants.FIELD_AER_ARRAYDES_COUNT);				
		}
		//processing bioassay data group
		{
			list=elExperiment.elements(Constants.EL_bioassaydatagroup);
			int fgemCount = 0;
			int rawCount = 0;
			int rawCelCount = 0;			
    		for (int i=0;i<list.size();i++)
    		{
    			el=list.get(i);
    			addFieldFromAttr(el, "id", doc, Constants.FIELD_AER_BDG_ID);
    			addFieldFromAttr(el, "name", doc, Constants.FIELD_AER_BDG_NAME);
    			addFieldFromAttr(el, "bioassaydatacubes", doc, Constants.FIELD_AER_BDG_NUM_BAD_CUBES);
    			addFieldFromAttr(el, "arraydesignprovider", doc, Constants.FIELD_AER_BDG_ARRAYDESIGN);

    			String _dataformat = el.attributeValue("dataformat");
    			addField(doc, Constants.FIELD_AER_BDG_DATAFORMAT, _dataformat);
    			
    			String _bioassays = el.attributeValue("bioassays");
    			int _bioassaysInt=Integer.valueOf(_bioassays);
    			addField(doc, Constants.FIELD_AER_BDG_BIOASSAY_COUNT, _bioassays);

    			String _isDerivied=el.attributeValue("isderived");
    			addField(doc, Constants.FIELD_AER_BDG_IS_DERIVED, _isDerivied);
    			if (_isDerivied.equalsIgnoreCase("1"))
    			{
    				fgemCount = fgemCount + _bioassaysInt;
    				if (_dataformat != null && _dataformat.indexOf("CEL")!=-1)
    				{
    					rawCelCount = rawCelCount + _bioassaysInt;
    				}
    				
    			}
    			else
    			{
    				rawCount = rawCount + _bioassaysInt;    				
    			}
    		}
			//adding fgem count 
			doc.addField(Constants.FIELD_AER_FGEM_COUNT, fgemCount);
			//adding raw celcount
			doc.addField(Constants.FIELD_AER_RAW_CELCOUNT, rawCelCount);
			//adding raw count
			doc.addField(Constants.FIELD_AER_RAW_COUNT, rawCount);
    		
		}
		list=elExperiment.elements(Constants.EL_bibliography);
		for (int i=0;i<list.size();i++)
		{
			el=list.get(i);
			addFieldFromAttr(el, "accession", doc, Constants.FIELD_AER_BI_ACCESSION);
			addFieldFromAttr(el, "publication", doc, Constants.FIELD_AER_BI_PUBLICATION);
			addFieldFromAttr(el, "authors", doc, Constants.FIELD_AER_BI_AUTHORS);
			addFieldFromAttr(el, "title", doc, Constants.FIELD_AER_BI_TITLE);
			addFieldFromAttr(el, "year", doc, Constants.FIELD_AER_BI_YEAR);
			addFieldFromAttr(el, "volume", doc, Constants.FIELD_AER_BI_VOLUME);
			addFieldFromAttr(el, "issue", doc, Constants.FIELD_AER_BI_ISSUE);
			addFieldFromAttr(el, "pages", doc, Constants.FIELD_AER_BI_PAGES);
			addFieldFromAttr(el, "uri", doc, Constants.FIELD_AER_BI_URI);
			
			
		}		
		//processing provider
		list=elExperiment.elements(Constants.EL_provider);
		for (int i=0;i<list.size();i++)
		{
			el=list.get(i);
			addFieldFromAttr(el, "contact", doc, Constants.FIELD_AER_PROVIDER_CONTRACT);
			addFieldFromAttr(el, "role", doc, Constants.FIELD_AER_PROVIDER_ROLE);
			addFieldFromAttr(el, "email", doc, Constants.FIELD_AER_PROVIDER_EMAIL);
		}				
		//processing experiment desing
		list=elExperiment.elements(Constants.EL_experimentdesign);
		for (int i=0;i<list.size();i++)
		{
			el=list.get(i);
			addFieldFromEl(el, doc, Constants.FIELD_AER_EXPDES_TYPE);
		}
		
		{
			boolean addedHybs = false;
			boolean addedSampl = false;
    		list=elExperiment.elements(Constants.EL_description);
    		for (int i=0;i<list.size();i++)
    		{
    			el=list.get(i);
    			addFieldFromAttr(el, "id", doc, Constants.FIELD_AER_DESC_ID);
    			String descText = el.getText();
    			doc.addField(Constants.FIELD_AER_DESC_TEXT, descText);
    
    			//Parse string to find num of hybs and samples
    			if (descText.indexOf("(Generated description)") != -1)
    			{
    				
    				Integer totalSample = getSamplesFromDesc(descText);
    				Integer totalHybs = getHybsFromDesc(descText);
    				if (totalSample != null & !addedSampl)
    				{
    					addedSampl = true;
    					doc.addField(Constants.FIELD_AER_TOTAL_SAMPL, totalSample);
    				}
    				if (totalHybs!=null & !addedHybs)
    				{
    					addedHybs = true;
    					doc.addField(Constants.FIELD_AER_TOTAL_HYBS, totalHybs);
    				}
    			}
    			
    		}
		}
		
		return doc;
	}

	private static final Integer getSamplesFromDesc(String descText)
	{
		Integer value = null;
		try
		{
			if (!StringUtils.isEmpty(descText))
			{
				int idxHybBegin = descText.indexOf("Experiment with");
				int idxHybEnd = descText.indexOf("hybridizations");

				String str=descText.substring(idxHybBegin + 16, idxHybEnd);
				value = new Integer(Integer.parseInt(str.trim()));
			}
		}
		catch (Exception e) 
		{
			log.info("Error" + e.getMessage());
		}
		finally
		{
			return value;
		}
	}
	
	private static final Integer getHybsFromDesc(String descText)
	{
		Integer value =null;
		try
		{
			if (!StringUtils.isEmpty(descText))
			{

				int idxSampBegin = descText.indexOf("hybridizations, using");
				int idxSampEnd = descText.indexOf("samples");
				String str=descText.substring(idxSampBegin + 21, idxSampEnd);
				value = new Integer(Integer.parseInt(str.trim()));
			}
		}
		catch (Exception e) 
		{
			log.info("Error" + e.getMessage());
		}
		finally
		{
			return value;
		}
		
		
	}

}
