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

import com.google.common.base.Joiner;
import com.google.common.collect.Multimap;
import com.google.common.collect.TreeMultimap;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.lang.StringUtils;
import uk.ac.ebi.gxa.exceptions.LogUtil;

import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import static com.google.common.io.Closeables.closeQuietly;

/**
 * User: nsklyar
 * Date: 19/03/2012
 */
class AnnotationSourceProperties {
    public static final String SOFTWARE_NAME_PROPNAME = "software.name";
    public static final String SOFTWARE_VERSION_PROPNAME = "software.version";
    public static final String TYPES_PROPNAME = "types";
    public static final String URL_PROPNAME = "url";

    public static final List<String> PROPNAMES = Arrays.asList(
            SOFTWARE_NAME_PROPNAME,
            SOFTWARE_VERSION_PROPNAME,
            TYPES_PROPNAME,
            URL_PROPNAME);

    //BioMart properties
    public static final String ORGANISM_PROPNAME = "organism";
    public static final String MYSQLDBNAME_PROPNAME = "mySqlDbName";
    public static final String MYSQLDBURL_PROPNAME = "mySqlDbUrl";
    public static final String DATASET_NAME_PROPNAME = "datasetName";
    public static final String DATABASE_NAME_PROPNAME = "databaseName";

    public final static List<String> BM_PROPNAMES = Arrays.asList(ORGANISM_PROPNAME,
            MYSQLDBNAME_PROPNAME,
            MYSQLDBURL_PROPNAME,
            DATASET_NAME_PROPNAME,
            DATABASE_NAME_PROPNAME);

    public static final String EXTPROPERTY_PROPNAME = "property";
    public static final String ARRAYDESIGN_PROPNAME = "arrayDesign";

    private PropertiesConfiguration properties;


    public AnnotationSourceProperties() {
        properties = new PropertiesConfiguration();
        properties.setListDelimiter(',');
    }

    public static AnnotationSourceProperties createPropertiesFromText(String text) {
        AnnotationSourceProperties annotationSourceProperties = new AnnotationSourceProperties();
        Reader input = new StringReader(text);
        try {
            annotationSourceProperties.properties.load(input);
            return annotationSourceProperties;
        } catch (ConfigurationException e) {
            throw LogUtil.createUnexpected("Cannot read annotation properties", e);
        } finally {
            closeQuietly(input);
        }
    }

    public String serializeToString() {
        Writer writer = new StringWriter();
        try {
            properties.save(writer);
            return writer.toString().trim();
        } catch (ConfigurationException e) {
            throw LogUtil.createUnexpected("Cannot create serialize  AnnotationSource as String ", e);
        } finally {
            closeQuietly(writer);
        }
    }

    public String getProperty(String key) {
        return properties.getString(key);
    }

    public void addProperty(String key, Object value) {
        properties.addProperty(key, value);
    }

    public void addListPropertiesWithPrefix(String propNamePrefix, Multimap<String, String> atlasNameToBioMartNames) {

        for (String beProp : atlasNameToBioMartNames.keySet()) {
            Collection<String> bmNames = atlasNameToBioMartNames.get(beProp);
            properties.addProperty(propNamePrefix + "." + beProp, Joiner.on(',').join(bmNames));
        }
    }

    public void  addListProperties(String propertyName, Collection<String> propertyValues) {
        properties.addProperty(propertyName, Joiner.on(',').join(propertyValues));
    }

    public Collection<String> getListPropertiesOfType(String type) {
        return Arrays.asList(properties.getStringArray(type));
    }

    public Multimap<String, String> getListPropertiesWithPrefix(String prefix) {
        Multimap<String, String> result = TreeMultimap.create();
        final Iterator<String> keys = properties.getKeys(prefix);
        while (keys.hasNext()) {
            String key = keys.next();
            final Collection<String> propertiesOfType = getListPropertiesOfType(key);
            for (String value : propertiesOfType) {
                result.put(StringUtils.removeStartIgnoreCase(key, prefix + "."), value);
            }
        }
        return result;
    }
}
