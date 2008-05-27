/**
 * EBI Microarray Informatics Team (c) 2007-2008
 */
package uk.ac.ebi.ae3.indexbuilder.service;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.common.SolrInputDocument;
import org.xml.sax.SAXException;

import uk.ac.ebi.ae3.indexbuilder.Constants;
import uk.ac.ebi.ae3.indexbuilder.IndexBuilderException;
import uk.ac.ebi.ae3.indexbuilder.IndexException;
import uk.ac.ebi.microarray.tools4ae.helpers.FileHelper;
/**
 * 
 * @author mdylag
 *
 */
public abstract class IndexBuilderService
{
	/** an instance of {@link UpdateResponse}**/
	protected UpdateResponse response;
	/** an instance of SolrEmbeddedIndex */
	private SolrEmbeddedIndex solrEmbeddedIndex;
	/** a path to the ftp dir*/
	private String ftpDir;
	/** an instance of loging interface*/
	protected static final Log log = LogFactory.getLog(IndexBuilderService.class);
	/** The extension of the fgem files*/
	private static final String FILE_FILTER_FGEM = "processed.zip";
	/** The extension of the raw files*/	
	private static final String FILE_FILTER_RAW = "raw.zip";
	/** The extension of the 2columns files*/
	private static final String FILE_FILTER_2COLUMNS = "2columns.txt";
	/** The extension of the SDRF files*/
	private static final String FILE_FILTER_SDRF = "sdrf.txt";
	/** The extension of the biosamples png files*/
	private static final String FILE_FILTER_BIOSAMPLES_PNG = "biosamples.png";
	/** The extension of the biosamples svg files*/
	private static final String FILE_FILTER_BIOSAMPLES_SVG = "biosamples.svg";
	
	
	/**
	 * Default constructor 
	 * @param confService
	 * @throws ParserConfigurationException
	 * @throws IOException
	 * @throws SAXException
	 */
	public IndexBuilderService() throws ParserConfigurationException, IOException, SAXException
	{
	}
	

	/**
	 * Dispose system and solr respurce. Doing commit and dispose for the SolrEmbeddedIndex instance.
	 * @throws SolrServerException
	 * @throws IOException
	 */
	protected void dispose() throws SolrServerException, IOException
	{
	   solrEmbeddedIndex.commit();
	   solrEmbeddedIndex.dispose();
	}

	/**
	 *  
	 * @throws Exception
	 * @throws IndexException 
	 */
	public void buildIndex() throws Exception, IndexException
	{
		try
		{
		    solrEmbeddedIndex.init();
		    createIndexDocs();		
		}
		catch (Exception e)
		{
			throw new IndexBuilderException(e);
		}		
		finally
		{
		    dispose();			
		}
	}
	
	protected abstract void createIndexDocs() throws Exception;


	/**
	 * 
	 * @return
	 */
	public SolrEmbeddedIndex getSolrEmbeddedIndex() {
	    return solrEmbeddedIndex;
	}

	/**
	 * 
	 * @param solrEmbeddedIndex
	 */
	public void setSolrEmbeddedIndex(SolrEmbeddedIndex solrEmbeddedIndex) {
	    this.solrEmbeddedIndex = solrEmbeddedIndex;
	}



	/**
	 * Return a path to the experiments' ftp directory.
	 * @return - String which contains path to directory when are the experiments
	 */
	public String getFtpDir()
	{
		return ftpDir;
	}

	/**
	 * Sets 
	 * @param ftpDir
	 */
	public void setFtpDir(String ftpDir)
	{
		this.ftpDir = ftpDir;
	}

	protected void getInfoFromFtp(String identifier, SolrInputDocument doc)
	{
   	    String subDir = identifier.substring(identifier.indexOf("-")+1, identifier.lastIndexOf("-"));
		String dir = FilenameUtils.concat(ftpDir, subDir);
		dir = FilenameUtils.concat(dir, identifier);
		String[] fileFilter = {FILE_FILTER_FGEM, FILE_FILTER_RAW,FILE_FILTER_SDRF,"2columns.txt","biosamples.png","biosamples.svg"};
		log.info(dir);
		File fDir = new File(dir);
		if (fDir.isDirectory())
		{
			Collection<File> col= FileUtils.listFiles(fDir, fileFilter, true);
			Iterator<File> it=col.iterator();
			FileHelper.AeFileType mhyType;
			while (it.hasNext())
			{
				File f=it.next();
				String filename = f.getName();
				if (FileHelper.isSdrfExtension(filename))
				{
					log.info("##################################### Find file" + f);
					doc.addField(Constants.FIELD_AER_FILE_SDRF, filename);
				}
				else if (FileHelper.isFgemExtension(filename))
				{
					log.info("##################################### Find file" + f);
					doc.addField(Constants.FIELD_AER_FILE_FGEM, filename);					
				}
				else if (FileHelper.isBiosamplePng(filename))
				{
					log.info("##################################### Find file" + f);
					doc.addField(Constants.FIELD_AER_FILE_BIOSAMPLEPNG, filename);					
				}
				else if (FileHelper.isBiosampleSvg(filename))
				{
					log.info("##################################### Find file" + f);
					doc.addField(Constants.FIELD_AER_FILE_BIOSAMPLESVG, filename);					
					
				}
				else if (FileHelper.isRawExtension(filename))
				{
					log.info("##################################### Find file" + f);
					doc.addField(Constants.FIELD_AER_FILE_RAW, filename);					

				}
				else if (FileHelper.isTwoColumnsExtension(filename))
				{
					log.info("##################################### Find file" + f);
					doc.addField(Constants.FIELD_AER_FILE_TWOCOLUMNS, filename);					
					
				}

			
			}
		
		}
		else
		{
			log.warn("Directory " + fDir.getAbsolutePath() + " does not exist.");
		}
	}
}
