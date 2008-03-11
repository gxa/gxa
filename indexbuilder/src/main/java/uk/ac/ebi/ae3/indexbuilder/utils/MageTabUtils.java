package uk.ac.ebi.ae3.indexbuilder.utils;

import java.io.File;
import java.util.Collection;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.filefilter.FileFileFilter;
import org.apache.commons.io.filefilter.FileFilterUtils;

import uk.ac.ebi.ae3.indexbuilder.IndexBuilder;

public class MageTabUtils
{
	public static final String idf_extensions[] = {"idf.txt"};
	public static final String sdrf_extension[] = {"sdrf.txt"};
	public static final Collection getFiles(final String directory, final String[] extension)
	{
		Collection col=FileUtils.listFiles(new File(directory), extension, true);
		if (col.isEmpty()) {
			return null;
		}		
		return col;
		
	}
	
	public static final Collection getIdfFiles(String directory)
	{		
		return getFiles(directory, idf_extensions);	
	}

	public static final Collection getSdrfFiles(String directory)
	{		
		return getFiles(directory, sdrf_extension);	
	}
	
	
	
	public static void main(String[] args)
	{
		Collection c=getIdfFiles("D:\\temp\\ebi\\ArrayExpress-ftp\\experiment");
		System.out.println(c.size());	
		c=getSdrfFiles("D:\\temp\\ebi\\ArrayExpress-ftp\\experiment");
		System.out.println(c.size());
		
	}
}
