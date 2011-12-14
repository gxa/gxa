/*
 * Copyright 2008-2011 Microarray Informatics Team, EMBL-European Bioinformatics Institute
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

import org.apache.commons.codec.binary.Hex;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.List;

import static com.google.common.io.Closeables.closeQuietly;
import static java.util.Arrays.asList;
import static java.util.Collections.sort;
import static uk.ac.ebi.gxa.exceptions.LogUtil.createUnexpected;

/**
 */
public class DigestUtil {
    private static final byte[] SEPARATOR = {(byte) 0xA3, (byte) 141};
    private static final String ALGORITHM = "SHA1";
    private static final Charset CHARSET = Charset.forName("UTF-8");

    public static byte[] digest(File... files) throws IOException {
        return digest(asList(files));
    }

    public static byte[] digest(List<File> files) throws IOException {
        MessageDigest digest = getDigestInstance();
        sort(files, new Comparator<File>() {
            @Override
            public int compare(File o1, File o2) {
                try {
                    return o1.getCanonicalPath().compareTo(o2.getCanonicalPath());
                } catch (IOException e) {
                    throw createUnexpected("Cannot get canonical path", e);
                }
            }
        });
        for (File f : files) {
            update(digest, f);
            putSeparator(digest);
        }
        return digest.digest();
    }

    public static MessageDigest getDigestInstance() {
        try {
            return MessageDigest.getInstance(ALGORITHM);
        } catch (NoSuchAlgorithmException e) {
            throw createUnexpected("Cannot get a digester", e);
        }
    }

    public static void update(MessageDigest complete, File f) throws IOException {
        InputStream fis = null;
        try {
            fis = new FileInputStream(f);

            byte[] buffer = new byte[1024];
            int numRead;
            do {
                numRead = fis.read(buffer);
                if (numRead > 0) {
                    complete.update(buffer, 0, numRead);
                }
            } while (numRead != -1);
        } finally {
            closeQuietly(fis);
        }
    }

    public static void update(MessageDigest digest, String s) {
        if (s != null)
            digest.update(s.getBytes(CHARSET));
        putSeparator(digest);
    }

    private static void putSeparator(MessageDigest digest) {
        digest.update(SEPARATOR);
    }

    /**
     * Converts byte array into a hex string
     *
     * @param bytes bytes to format
     * @return formatted hex string (lowercase)
     */
    public static String toHex(byte[] bytes) {
        return new String(Hex.encodeHex(bytes));
    }
}