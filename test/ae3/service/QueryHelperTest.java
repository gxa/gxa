package ae3.service;

import uk.ac.ebi.ae3.indexbuilder.Constants;
import ae3.AtlasAbstractTest;
import ae3.service.search.XmlHelper;

public class QueryHelperTest extends AtlasAbstractTest
{
	String keywords = "cancer";
	String species = "mus";
	Long arrayDesId = new Long(12);
	

	public void test_parseQuery()
	{
		String query = QueryHelper.prepareQuery(keywords, species, arrayDesId);
		log.info("######### Query is " + query);
	}
	
	public void test_convParam()
	{
		assertEquals(QueryHelper.convParamSortToFieldName(XmlHelper.XML_EL_ACCESSION),Constants.FIELD_AER_EXPACCESSION);
		assertEquals(QueryHelper.convParamSortToFieldName(XmlHelper.XML_EL_NAME),Constants.FIELD_AER_EXPNAME);
		assertEquals(QueryHelper.convParamSortToFieldName(XmlHelper.XML_EL_RELEASEDATE),Constants.FIELD_AER_RELEASEDATE);
		assertEquals(QueryHelper.convParamSortToFieldName(XmlHelper.XML_EL_SPECIES),Constants.FIELD_AER_SAAT_VALUE);
		assertEquals(QueryHelper.convParamSortToFieldName(XmlHelper.XML_EL_FGEM),Constants.FIELD_AER_FGEM_COUNT);
		assertEquals(QueryHelper.convParamSortToFieldName(XmlHelper.XML_EL_RAW),Constants.FIELD_AER_RAW_COUNT);
		

	}
	public void test_convOrder()
	{
		assertEquals(QueryHelper.convParamSortToFieldName(XmlHelper.XML_EL_ACCESSION),Constants.FIELD_AER_EXPACCESSION);
	}

}
