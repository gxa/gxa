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

package uk.ac.ebi.gxa.service.export;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Predicate;
import com.google.common.base.Strings;
import com.google.common.collect.Collections2;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import uk.ac.ebi.gxa.dao.PropertyValueDAO;
import uk.ac.ebi.gxa.properties.AtlasProperties;
import uk.ac.ebi.microarray.atlas.model.PropertyValue;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * User: nsklyar
 * Date: 23/05/2012
 */
public class CompoundExporter implements DataExporter {

    private final static String COMPOUND = "compound";

    //Regexp to match numbers corresponding to dose and unit, e.g. " 10 molar", " 0.666 nanograms per milliliter"
    private final static String DOSE_PATTERN = "\\s\\d+(\\.?\\d+)?\\s[a-zA-Z]+";

    @Autowired
    private PropertyValueDAO propertyValueDAO;

    @Autowired
    private AtlasProperties atlasProperties;

    public String generateDataAsString() {

        final List<PropertyValue> propertyValues = propertyValueDAO.findValuesForProperty(COMPOUND);

        Set<String> sortedValues = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
        sortedValues.addAll(
                Collections2.filter(
                        Collections2.transform(propertyValues, new Function<PropertyValue, String>() {
                            @Override
                            public String apply(PropertyValue propertyValue) {
                                return cutOfDoseAndUnit(propertyValue.getValue());
                            }
                        }),
                        new Predicate<String>() {
                            @Override
                            public boolean apply(String s) {
                                return filter(s);
                            }
                        }));

        return Joiner.on("\n").join(sortedValues);

    }

    protected boolean filter(String s) {
        if (Strings.isNullOrEmpty(s)) {
            return false;
        }
        for (String match : atlasProperties.getExportExcludeCompoundsMatch()) {
            if(match.equalsIgnoreCase(s)) {
                return false;
            }
        }
        for (String exclude : atlasProperties.getExportExcludeCompoundsContain()) {
            if (StringUtils.containsIgnoreCase(s, exclude)) {
                return false;
            }
        }
        return true;
    }

    protected String cutOfDoseAndUnit(String value) {
        final Pattern pattern = Pattern.compile(DOSE_PATTERN);
        final Matcher matcher = pattern.matcher(value);

        int start = value.length();

        //find start of last match
        while (matcher.find()) {
            start = matcher.start();
        }
        return value.substring(0, start).trim();
    }

    protected void setPropertyValueDAO(PropertyValueDAO propertyValueDAO) {
        this.propertyValueDAO = propertyValueDAO;
    }

    protected void setAtlasProperties(AtlasProperties atlasProperties) {
        this.atlasProperties = atlasProperties;
    }
}
