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

import com.google.common.io.PatternFilenameFilter;

import javax.annotation.Nullable;
import javax.servlet.jsp.PageContext;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

/**
 * @author Olga Melnichuk
 */
class AggregatedResourcePath {
    private final AggregatedResourceNamePattern namePattern;
    private final String aggregationPath;

    public AggregatedResourcePath(@Nullable String aggregationPath, AggregatedResourceNamePattern namePattern) {
        this.aggregationPath = aggregationPath == null ? "" : aggregationPath;
        this.namePattern = namePattern;
    }

    public ResourceFinder relativeTo(PageContext pageContext) {
        String contextRealPath = pageContext.getServletContext().getRealPath("/");
        return new ResourceFinder(new File(contextRealPath, aggregationPath));
    }

    class ResourceFinder {
        private final File folder;

        private ResourceFinder(File folder) {
            this.folder = folder;
        }

        public Collection<String> findAll(String resourceName) {
            String[] names = folder.list(new PatternFilenameFilter(namePattern.pattern(resourceName)));

            Collection<String> list = new ArrayList<String>();
            for (String name : names) {
                list.add(ResourcePath.join(aggregationPath, name));
            }
            return list;
        }

        public String findOne(String resourceName) throws Wro4jTagException {
            Collection<String> paths = findAll(resourceName);
            switch (paths.size()) {
                case 1:
                    return paths.iterator().next();
                case 0:
                    throw new Wro4jTagException("More than one file matches the pattern '" + namePattern + "': " + Arrays.toString(paths.toArray()));
                default:
                    throw new Wro4jTagException("No file matching the pattern: '" + namePattern + "' found in the path: " + folder.getAbsolutePath());
            }
        }
    }
}
