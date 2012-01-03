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

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

import static uk.ac.ebi.gxa.exceptions.LogUtil.createUnexpected;

/**
 * @author alf
 */
public final class ResourceUtil {

    private ResourceUtil() {
    }

    public static File getResourceRoot(Class clazz) {
        try {
            final String classResource = getClassResource(clazz).toString();
            final String root = classResource.substring(0, classResource.length() - clazz.getName().length() - ".class".length());
            return new File(new URL(root).getPath()); // Drop "file://" if present
        } catch (MalformedURLException e) {
            throw createUnexpected("Non-supported class loader", e);
        }
    }

    public static URL getClassResource(Class clazz) {
        return clazz.getClassLoader().getResource(clazz.getName().replace('.', '/') + ".class");
    }
}
