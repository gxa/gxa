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
package uk.ac.ebi.gxa.properties;

import uk.ac.ebi.gxa.utils.StringUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import ae3.service.structuredquery.AtlasGenePropertyService;

/**
 * @author pashky
 */
public class GenePropHeadersStorage implements Storage {
    private final static String PREFIX = "geneproperty.";
    private final static String PREFIX_CURATED = "curatedname.";
    private final static String PREFIX_API = "apiname.";
    private final static String PREFIX_LINK = "link.";

    private AtlasGenePropertyService genePropService;

    public void setGenePropService(AtlasGenePropertyService genePropService) {
        this.genePropService = genePropService;
    }

    public void setProperty(String name, String value) {
        // do nothing
    }

    public String getProperty(String name) {
        if(!name.startsWith(PREFIX))
            return null;

        String what = name.substring(PREFIX.length());
        if(what.startsWith(PREFIX_CURATED))
            return StringUtil.upcaseFirst(what.substring(PREFIX_CURATED.length()));
        if(what.startsWith(PREFIX_LINK))
            return "";
        if(what.startsWith(PREFIX_API)) {
            String property = what.substring(PREFIX_API.length()).toLowerCase();
            return property.endsWith("s") ? property : property + "s";
        }

        return null;
    }

    public boolean isWritePersistent() {
        return false;
    }

    public Collection<String> getAvailablePropertyNames() {
        List<String> result = new ArrayList<String>();
        for(String v : genePropService.getIdNameDescProperties()) {
            result.add(PREFIX + PREFIX_API + v);
            result.add(PREFIX + PREFIX_LINK + v);
            result.add(PREFIX + PREFIX_CURATED + v);
        }
        return result;
    }

    public void reload() {
        // do nothing
    }
}
