/**
 * EBI Microarray Informatics Team (c) 2007-2008
 */
package uk.ac.ebi.ae3.indexbuilder.service;

import org.apache.commons.dbcp.BasicDataSource;
import org.apache.solr.common.SolrInputDocument;
import uk.ac.ebi.ae3.indexbuilder.Constants;
import uk.ac.ebi.ae3.indexbuilder.IndexBuilderException;
import uk.ac.ebi.ae3.indexbuilder.dao.ExperimentDwJdbcDao;
import uk.ac.ebi.ae3.indexbuilder.dao.ExperimentJdbcDao;
import uk.ac.ebi.ae3.indexbuilder.model.Experiment;
import uk.ac.ebi.ae3.indexbuilder.utils.XmlUtil;

import java.io.UnsupportedEncodingException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Collection;
import java.util.Iterator;
/**
 * 
 * Class description goes here.
 *
 * @version 	1.0 2008-04-01
 * @author 	Miroslaw Dylag
 */
@Deprecated
public class IndexBuilderFromDb extends IndexBuilderService
{
    	/** */
	private ExperimentJdbcDao experimentDao;
	private ExperimentDwJdbcDao experimentDwDao;
	private BasicDataSource dataSource;
    //private String mageDir;

	
	public IndexBuilderFromDb() throws Exception
	{
		
	}
	
    public BasicDataSource getDataSource() {
        return dataSource;
    }

    public void setDataSource(BasicDataSource dataSource) {
        this.dataSource = dataSource;
    }

	/**
	 * 
	 */
	@Override
	protected void createIndexDocs() throws IndexBuilderException
	{
    try {
		Connection sql = getDataSource().getConnection();
		PreparedStatement geneCounts = sql.prepareStatement("select unique ef, efv, updn, count(distinct gene_id_key) gc" +
										"      from atlas" +
										"      where experiment_id_key = ?" +
										"      group by ef, efv, updn");
		
		Collection<Experiment> exps;
		if(getPendingOnly()){
			exps=experimentDwDao.getPendingExperiments();
			
		}else
			exps=experimentDwDao.getExperiments();		
		Iterator<Experiment> it=exps.iterator();
			
			while (it.hasNext())
			{
				Experiment exp=it.next();
//				String xml=experimentDao.getExperimentAsXml(exp);
				
				SolrInputDocument doc = new SolrInputDocument();;
//				doc = XmlUtil.createSolrInputDoc(xml);
				String xmlDw=experimentDwDao.getExperimentAsXml(exp);
				
				//Add information about ftp files to SolrDocument
//				getInfoFromFtp(exp.getAccession(), doc);
//				if (experimentDwDao.experimentExists(exp))
				if(xmlDw!="")
				{
				  
				  doc.addField(Constants.FIELD_EXP_IN_DW, true);
				  XmlUtil.addExperimentFromDW(xmlDw, doc);
				  getLog().info("Adding "+doc.getField("dwe_exp_accession").getValue().toString());
				  String expId = doc.getField("dwe_exp_id").getValue().toString();
				  geneCounts.setString(1, expId);
		          ResultSet gcRS = geneCounts.executeQuery();
		          while(gcRS.next()){
		        	  String ef = gcRS.getString("EF");
		        	  String efv = gcRS.getString("EFV");
		        	  if(efv.equals("V1") || ef.equals("V1"))
		                    continue;
		        	  String updn = gcRS.getString("updn");
		        	  int count = gcRS.getInt("GC");
		        	  String efvid = encodeEfv(ef) + "_" + encodeEfv(efv);
		        	  String expr = updn.equals("-1") ? "_dn" : "_up";
		        	  doc.addField("cnt_efv_"+ efvid + expr, count);
		          }
		          getSolrEmbeddedIndex().addDoc(doc);
				}
				else
				{
					getLog().info(exp.getAccession()+ "skipped");
//					doc.addField(Constants.FIELD_EXP_IN_DW, false);					
				}
//				if (doc!=null)
//				{
//					
//				}

			}
    }
    catch (Exception e) {
      throw new IndexBuilderException(e);
    }
	}
	
  @Deprecated
	public ExperimentJdbcDao getExperimentDao()
	{
		return experimentDao;
	}

  @Deprecated
	public void setExperimentDao(ExperimentJdbcDao experimentDao)
	{
		this.experimentDao = experimentDao;
	}

  @Deprecated
	public ExperimentDwJdbcDao getExperimentDwDao()
	{
		return experimentDwDao;
	}

  @Deprecated
	public void setExperimentDwDao(ExperimentDwJdbcDao experimentDwDao)
	{
		this.experimentDwDao = experimentDwDao;
	}
	
    private static String encodeEfv(String v) {
        try {
            StringBuffer r = new StringBuffer();
            for(char x : v.toCharArray())
            {
                if(Character.isJavaIdentifierPart(x))
                    r.append(x);
                else
                    for(byte b : Character.toString(x).getBytes("UTF-8"))
                        r.append("_").append(String.format("%x", b));
            }
            return r.toString();
        } catch(UnsupportedEncodingException e){
            throw new IllegalArgumentException("Unable to encode EFV in UTF-8", e);
        }
    }
	
	
	


}
