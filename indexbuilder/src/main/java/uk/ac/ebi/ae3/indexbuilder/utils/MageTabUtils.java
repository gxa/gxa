package uk.ac.ebi.ae3.indexbuilder.utils;

import java.io.File;
import java.io.IOException;
import java.net.FileNameMap;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.filefilter.FileFileFilter;
import org.apache.commons.io.filefilter.FileFilterUtils;

import uk.ac.ebi.ae3.indexbuilder.IndexBuilder;
import uk.ac.ebi.ae3.indexbuilder.IndexBuilderException;

public class MageTabUtils
{
	/** **/
	public static final String idf_extensions[] = {"idf.txt"};
	/** **/
	public static final String sdrf_extension[] = {"sdrf.txt"};
	
	/**
	 * Returns a Collection of File. 
	 * @param directory - the name of directory
	 * @param extension - the table which contains extension
	 * @return Collection<File>
	 */
	public static final Collection<File> getFiles(final String directory, final String[] extension)
	{
		Collection<File> col=FileUtils.listFiles(new File(directory), extension, true);
		if (col.isEmpty()) {
			return null;
		}		
		return col;
		
	}
	
	/**
	 * DOCUMENT ME
	 * @param directory
	 * @return
	 */
	public static final Collection<File> getIdfFiles(String directory)
	{		
		return getFiles(directory, idf_extensions);	
	}

	/**
	 * DOCUMENT ME
	 * @param directory
	 * @return
	 */
	public static final Collection<File> getSdrfFiles(String directory)
	{		
		return getFiles(directory, sdrf_extension);	
	}
	/**
	 * DOCUMENT ME
	 * @param idfFile
	 * @return
	 */
	public static final Collection<File> getSdrfFilesForIdf(File idfFile)
	{
		String path=FilenameUtils.getFullPath(idfFile.getAbsolutePath());
		String fileName = idfFile.getName();
		Collection<File> c=getSdrfFiles(path);
		return c;
	}
	
	/**
	 * DOCUMENT ME
	 * @param idfFile
	 * @return
	 * @throws IndexBuilderException
	 */
	public static final File getSdrfFileForIdf(File idfFile) throws IndexBuilderException
	{
		Collection<File> c=getSdrfFilesForIdf(idfFile);
		if (c==null || c.size()==0)
		{
			return null;
		}
		else if (c.size() > 1)
		{
			throw new IndexBuilderException("To many SDRFs files. In directory");
		}
		else
		{
			return c.iterator().next();
		}
	}

	/**
	 * DOCUMENT ME
	 * @param file
	 * @return
	 */
	public static final String getAccesionNumberFromDir(File file)
	{
		String path=FilenameUtils.getFullPath(file.getAbsolutePath());
		path=FilenameUtils.normalizeNoEndSeparator(path);
		int sep=FilenameUtils.indexOfLastSeparator(path);
		String accesionNumber = path.substring(sep+1, path.length()); 
		System.out.println(accesionNumber);
		return accesionNumber;
	}
	/**
	 * DOCUMENT ME
	 * @param idfFile
	 * @return
	 */
	public static final String getAccesionNumberFromIdf(File idfFile)
	{
		String filename=idfFile.getName();
		filename=filename.replace(IndexBuilder.IDF_EXTENSION, "");
		System.out.println(filename);
		return filename;
	}
	
	
	public static void main(String[] args) throws IOException, Exception
	{
		Collection c=getIdfFiles("D:\\temp\\ebi\\ArrayExpress-ftp\\experiment\\");
		Iterator<File> it=c.iterator();
		while (it.hasNext())
		{
			File idf=it.next();
			System.out.println(idf.getPath());
			File sdrf=getSdrfFileForIdf(idf);
			String an=getAccesionNumberFromDir(idf);
			getAccesionNumberFromIdf(idf);
		}
		c=getSdrfFiles("D:\\temp\\ebi\\ArrayExpress-ftp\\experiment\\");
		
	}
}
