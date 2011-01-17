package uk.ac.ebi.gxa.loader.utils;

import com.google.common.io.ByteStreams;
import com.google.common.io.Closeables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URL;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class ZipUtil {
    public static final Logger log = LoggerFactory.getLogger(ZipUtil.class);
    /**
     * We MUST use forward slash in Zip regardless of system's {@link File#separator}
     */
    private static final String SEPARATOR = "/";

    private static void addDirectory(ZipOutputStream zout, File from, String path) throws IOException {
        File[] files = from.listFiles();
        if (files == null)
            throw new IOException(from + " is not a directory");

        for (File file : files) {
            log.debug("Adding file: {}", file);
            // TODO: it might make sense to maintain a queue as shown at
            // http://stackoverflow.com/questions/1399126/java-util-zip-recreating-directory-structure
            // Note though there are lots of mistakes in all the code examples there Ñ DO NOT just copy-n-paste
            if (file.isDirectory()) {
                addDirectory(zout, file, path + SEPARATOR + file.getName());
                continue;
            }

            FileInputStream fin = null;
            try {
                fin = new FileInputStream(file);
                zout.putNextEntry(new ZipEntry(path + SEPARATOR + file.getName()));
                ByteStreams.copy(fin, zout);
            } finally {
                try {
                    zout.closeEntry();
                } catch (IOException e) {
                    log.error("Cannot close Zip entry", e);
                }
                Closeables.closeQuietly(fin);
            }
        }
    }

    public static void compress(File folder, File archive) throws IOException {
        if (!folder.exists()) {
            throw new FileNotFoundException("Cannot find " + folder);
        }
        if (!folder.canRead()) {
            throw new IOException("Cannot read " + folder);
        }

        ZipOutputStream zout = null;
        try {
            zout = new ZipOutputStream(new FileOutputStream(archive));
            zout.setLevel(9);
            zout.setComment("EMBL-EBI Gene Expression Atlas");
            addDirectory(zout, folder, "");
            log.debug("Zip file has been created!");
        } finally {
            Closeables.closeQuietly(zout);
        }
    }

    public static void decompress(URL archive, File folder) throws IOException {
        if (!folder.exists() && !folder.mkdir())
            throw new IOException("Cannot create destination folder");

        ZipInputStream zis = null;
        try {
            zis = new ZipInputStream(new BufferedInputStream(archive.openConnection().getInputStream()));
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                FileOutputStream fos = null;
                try {
                    File outFile = new File(folder, entry.getName());
                    File outFileFolder = new File(outFile.getParent());
                    if (!outFileFolder.exists() && !outFileFolder.mkdirs())
                        throw new IOException("Cannot create directories");

                    if (!outFile.exists() && !outFile.createNewFile())
                        throw new IOException("Cannot create output file");

                    fos = new FileOutputStream(outFile);
                    ByteStreams.copy(zis, fos);
                } finally {
                    Closeables.closeQuietly(fos);
                }
            }
        } finally {
            Closeables.closeQuietly(zis);
        }
    }
}
