package uk.ac.ebi.gxa.utils;

import java.io.File;
import java.io.InputStream;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * @author pashky
 */
public class FileUtil {

    public static File createTempDirectory(String prefix) {
        File path;
        int counter = 0;
        do {
            path = new File(System.getProperty("java.io.tmpdir"), prefix + (counter++));
        } while(!path.mkdirs());
        return path;
    }

    public static void deleteDirectory(File dir){
		if(dir.isDirectory()) {
            for (File file : dir.listFiles())
                deleteDirectory(file);
		}
        dir.delete();
	}

    public static void createDirectory(File path, String local) {
        if(!new File(path, local).mkdirs())
            throw new RuntimeException("Can't create temporary directory: " + local + " in :" + path);
    }


    public static void writeFileFromResource(final Class clazz, String resource, File basePath, String target) {
        File file = new File(basePath, target);

        File path = file.getParentFile();
        if(!path.exists() && !path.mkdirs())
            throw new RuntimeException("Can't create target directories: " + path);

        InputStream istream = clazz.getClassLoader().getResourceAsStream("META-INF/" + resource);
        try {
            FileOutputStream ostream = new FileOutputStream(file);
            byte b[] = new byte[2048];
            int i;
            while((i = istream.read(b)) >= 0)
                ostream.write(b, 0, i);
            ostream.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }
}
