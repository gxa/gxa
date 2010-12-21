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

import java.io.File;

/**
 * File utility functions
 * @author pashky
 */
public class FileUtil {
    /**
     * Creates guaranteed unqiue temporary directory in java.io.tmpdir space
     * The directory will have name containing specified prefix and some unique number
     * @param prefix prefix to use
     * @return created directory reference
     */
    public static File createTempDirectory(String prefix) {
        File path;
        int counter = 0;
        do {
            path = new File(System.getProperty("java.io.tmpdir"), prefix + (counter++));
        } while(!path.mkdirs());
        return path;
    }

    /**
     * Recursively deletes directory tree
     * @param dir root directory to kill
     * @return if delet was successful
     */
    public static boolean deleteDirectory(File dir){
        if(dir.isDirectory()) {
            for (File file : dir.listFiles())
                deleteDirectory(file);
        }
        return dir.delete();
    }
}
