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
 * http://ostolop.github.com/gxa/
 */

package ae3.util;

import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.solr.common.SolrDocument;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author pashky
 */
public class DecoratedSolrDocument {
    private SolrDocument solrDocument;
    private Map<String, List<String>> highlights;

    public DecoratedSolrDocument(SolrDocument solrDocument, Map<String, List<String>> highlights) {
        this.solrDocument = solrDocument;
        this.highlights = highlights;
    }

    // all this hacks to satisfy and trick JSP 2.0 EL resolver, we don't intent to implement Map in full
    public static abstract class MapEmulator<Target> implements Map<String, Target> {
        abstract Target mapValue(String key);

        public boolean isEmpty() { return false; }

        public boolean containsKey(Object key) {
            return mapValue(key != null ? key.toString() : null) != null;
        }


        public Target get(Object key) {
            return mapValue(key != null ? key.toString() : null);
        }

        public int size() { throw new NotImplementedException(); }
        public boolean containsValue(Object value) { throw new NotImplementedException(); }
        public Target put(String key, Target value) { throw new NotImplementedException(); }
        public Target remove(Object key) { throw new NotImplementedException(); }
        public void putAll(Map<? extends String, ? extends Target> t) { throw new NotImplementedException(); }
        public void clear() { throw new NotImplementedException(); }
        public Set<String> keySet() { throw new NotImplementedException(); }
        public Collection<Target> values() { throw new NotImplementedException(); }
        public Set<Map.Entry<String, Target>> entrySet() { throw new NotImplementedException(); }
    }

    public Map<String,String> getValue() {
        return new MapEmulator<String>() {
            public String mapValue(String key) {
                Collection fval = solrDocument.getFieldValues(key);
                if(fval != null)
                    return StringUtils.join(fval, ", ");
                return "";
            }
        };
    }

    public Map<String,String> getHtmlValue() {
        return new MapEmulator<String>() {
            public String mapValue(String key) {
                return StringEscapeUtils.escapeHtml(getValue().get(key));
            }
        };
    }

    public Map<String,String> getHilit() {
        return new MapEmulator<String>() {
            public String mapValue(String key) {
                List<String> val = highlights.get(key);
                if(val == null || val.size() == 0)
                    return StringEscapeUtils.escapeHtml(getValue().get(key));
                return org.apache.commons.lang.StringUtils.join(val, ", ");
            }
        };
    }

    public Map<String,Collection<String>> getValues() {
        return new MapEmulator<Collection<String>>() {
            public Collection<String> mapValue(String key) {
                @SuppressWarnings("unchecked")
                Collection<String> c = (Collection)solrDocument.getFieldValues(key);
                return c;
            }
        };
    }

    public Object getFieldValue(String key) {
        return solrDocument.getFieldValue(key);
    }

    public Collection getFieldValues(String key) {
        return solrDocument.getFieldValues(key);
    }

    public SolrDocument getOriginal() {
        return solrDocument;
    }
}
