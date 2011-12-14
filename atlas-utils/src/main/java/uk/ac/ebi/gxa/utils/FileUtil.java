/*
 * Copyright 2008-2010 Microarray Informatics Team, EMBL-European Bioinformatics Institute
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *
 * For further details of the Gene Expression Atlas project, including source code,
 * downloads and documentation, please see:
 *
 * http://gxa.github.com/gxa
 */

package uk.ac.ebi.gxa.utils;

import java.io.*;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import static uk.ac.ebi.gxa.exceptions.LogUtil.createUnexpected;

/**
 * File utility functions
 *
 * @author pashky
 */
public class FileUtil {
    public static FilenameFilter extension(final String ext, final boolean allowTxt) {
        return new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.endsWith("." + ext) || allowTxt && name.endsWith("." + ext + ".txt");
            }
        };
    }

    /**
     * Creates guaranteed unqiue temporary directory in java.io.tmpdir space
     * The directory will have name containing specified prefix and some unique number
     *
     * @param prefix prefix to use
     * @return created directory reference
     */
    public static File createTempDirectory(String prefix) {
        File path;
        int counter = 0;
        do {
            path = new File(getTempDirectory(), prefix + (counter++));
        } while (!path.mkdirs());
        return path;
    }

    /**
     * Recursively deletes directory tree
     *
     * @param dir root directory to kill
     * @return if delet was successful
     */
    public static boolean deleteDirectory(File dir) {
        if (dir.isDirectory()) {
            for (File file : dir.listFiles())
                deleteDirectory(file);
        }
        return dir.delete();
    }

    public static File tempFile(String name) {
        return new File(getTempDirectory(), name);
    }

    public static File getTempDirectory() {
        File result = new File(System.getProperty("java.io.tmpdir"));
        if (!result.canWrite())
            throw createUnexpected(result + " is not writable");
        if (!result.isDirectory())
            throw createUnexpected(result + " is not a directory");
        return result;
    }

    public static byte[] getMD5(File file) throws IOException, NoSuchAlgorithmException {
        final MessageDigest md = MessageDigest.getInstance("MD5");
        InputStream is = new FileInputStream(file);
        try {
            is = new DigestInputStream(is, md);
            byte[] buffer = new byte[4096];
            while (is.read(buffer) > 0) {
                // just read all bytes
            }
            return md.digest();
        } finally {
            is.close();
        }
    }
}
