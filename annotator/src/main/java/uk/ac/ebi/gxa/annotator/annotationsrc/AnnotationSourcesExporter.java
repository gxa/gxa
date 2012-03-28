/*
 * Copyright 2008-2012 Microarray Informatics Team, EMBL-European Bioinformatics Institute
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

package uk.ac.ebi.gxa.annotator.annotationsrc;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Predicate;
import com.google.common.base.Splitter;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;

/**
 * User: nsklyar
 * Date: 19/03/2012
 */
public class AnnotationSourcesExporter{

    private static String TYPE_NAME = "Type: ";


    public static String joinAsText(Collection<String> sources, final String typeName, String separator) {

        Collection<String> sourceStrings = Collections2.transform(sources, new Function<String, String>() {
            @Override
            public String apply(String source) {
                StringBuilder sb = new StringBuilder();
                sb.append(createTypeName(typeName));
                sb.append(source);
                return sb.toString();
            }
        });

        return Joiner.on(separator).join(sourceStrings);
    }

    public static String joinAll(String separator, String... sourceStrings) {
        return Joiner.on(separator).join(sourceStrings);
    }

    public static Collection<String> getStringSourcesOfType(String text, final String type, String separator) {
        final Iterable<String> split = Splitter.on(separator).split(text.trim());

        final ArrayList<String> sourcesStrings = Lists.newArrayList(split);
        return Collections2.transform(Collections2.filter(sourcesStrings, new Predicate<String>() {
            @Override
            public boolean apply(@Nullable String s) {
                assert s != null;
                return s.contains(createTypeName(type));
            }
        }), new Function<String, String>() {
            @Override
            public String apply(@Nullable String s) {
                assert s != null;
                return StringUtils.substringAfter(s, createTypeName(type));
            }
        });
    }

    private static String createTypeName(String typeName) {
        StringBuilder sb = new StringBuilder();
        sb.append(TYPE_NAME);
        sb.append(typeName);
        sb.append("\n");
        return sb.toString();
    }
}
