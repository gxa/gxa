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

import ae3.service.structuredquery.AtlasEfvService;
import uk.ac.ebi.gxa.utils.StringUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * A fake "storage" class which just enumerates possible EF headers curated properties. Can't store anything,
 * but can return some default value as well as available keys
 * @author pashky
 */
public class EfHeadersStorage implements Storage {

    private final static String PREFIX = "head.ef.";
    private static final int SUFFIXPOS = PREFIX.length();

    private AtlasEfvService efvService;

    public void setEfvService(AtlasEfvService efvService) {
        this.efvService = efvService;
    }

    public void setProperty(String name, String value) {
        // do nothing
    }

    public String getProperty(String name) {
        return name.startsWith(PREFIX) 
                ? StringUtil.upcaseFirst(name.substring(SUFFIXPOS)) : null;
    }

    public boolean isWritePersistent() {
        return false;
    }

    public Collection<String> getAvailablePropertyNames() {
        List<String> result = new ArrayList<String>();
        for(String ef : efvService.getAllFactors())
            result.add(PREFIX + ef);
        return result;
    }

    public void reload() {
        // do nothing
    }
}
