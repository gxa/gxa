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

package uk.ac.ebi.gxa.web.wro4j.tag;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;

/**
 * @author Olga Melnichuk
 */
class ResourcePath {
    private static final String PATH_SEPARATOR = "/";

    /**
     * Normalizes the path string:
     * - strips the leading and trailing slashes
     * - removes double slashes.
     *
     * @param path a path string to normalize
     * @return a normalized path string
     */
    public static String normalizePath(String path) {
        return joiner().join(splitter().split(path));
    }

    /**
     * Joins two paths.
     *
     * @param path1 a first path to start from
     * @param path2 a second path to add to the first one
     * @return a joined path string
     */
    public static String join(String path1, String... path2) {
        String path = normalizePath(path1 + PATH_SEPARATOR + joiner().join(path2));
        return path1.startsWith(PATH_SEPARATOR) ? PATH_SEPARATOR + path : path;
    }

    private static Splitter splitter() {
        return Splitter.on(PATH_SEPARATOR).omitEmptyStrings();
    }

    private static Joiner joiner() {
        return Joiner.on(PATH_SEPARATOR);
    }
}
