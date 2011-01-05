package uk.ac.ebi.gxa.loader.utils;

import java.io.*;
import java.net.URL;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Created by IntelliJ IDEA.
 * User: Andrey
 * Date: Dec 6, 2010
 * Time: 9:32:14 AM
 * To change this template use File | Settings | File Templates.
 */
public class ZipUtil {
    //put directory content to folder, recursively
    private static void addDirectory(ZipOutputStream zout, File fileSource, String relativePath) throws Exception{
        //get sub-folder/files list
        File[] files = fileSource.listFiles();
        for(int i=0; i < files.length; i++)
        {
            //if the file is directory, call the function recursively
            if(files[i].isDirectory())
            {
                addDirectory(zout, files[i], relativePath+File.separatorChar+files[i].getName());
                continue;
            }
            try
            {
                //create byte buffer
                byte[] buffer = new byte[1024];
                //create object of FileInputStream
                FileInputStream fin = new FileInputStream(files[i]);
                zout.putNextEntry(new ZipEntry(relativePath+File.separatorChar+files[i].getName()));
                int length;
                while((length = fin.read(buffer)) > 0)
                {
                   zout.write(buffer, 0, length);
                }
                 zout.closeEntry();
                 fin.close();
            }
            catch(IOException exception)
            {
                throw exception;
            }
        }
    }

    //compress given folder
    public static void compress(String folder, String archive) throws Exception {
        try {
            //create object of FileOutputStream
            FileOutputStream fout = new FileOutputStream(archive);
            //create object of ZipOutputStream from FileOutputStream
            ZipOutputStream zout = new ZipOutputStream(fout);
            //create File object from source directory
            File fileSource = new File(folder);
            addDirectory(zout, fileSource, "");
             //close the ZipOutputStream
            zout.close();
            System.out.println("Zip file has been created!");
        }
        catch (IOException ioe) {
            throw ioe;
        }
    }

    //decompress
    public static void decompress(URL archive, String folder) throws Exception {
        try {
            final int BUFFER = 2048;
            BufferedOutputStream dest = null;

            File destFolder = new File(folder);
            if(!destFolder.exists())
               destFolder.mkdir(); 

            ZipInputStream zis = new
                    ZipInputStream(new
                    BufferedInputStream(archive.openConnection().getInputStream()));
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                int count;
                byte data[] = new byte[BUFFER];
                // write the files to the disk

                File outFile = new File(folder + File.separatorChar + entry.getName());
                File outFileFolder = new File(outFile.getParent());
                if(!outFileFolder.exists())
                    outFileFolder.mkdirs();

                if(!outFile.exists())
                    outFile.createNewFile();

                FileOutputStream fos = new
                        FileOutputStream(outFile);
                dest = new BufferedOutputStream(fos,
                        BUFFER);
                while ((count = zis.read(data, 0,
                        BUFFER)) != -1) {
                    dest.write(data, 0, count);
                }
                dest.flush();
                dest.close();
            }
            zis.close();
        } catch (Exception e) {
            throw e;
        }
    }
}
