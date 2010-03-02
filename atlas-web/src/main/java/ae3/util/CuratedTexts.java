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

package ae3.util;

import java.io.IOException;
import java.util.Properties;

/**
 * @author pashky
 */
public class CuratedTexts {
    private static Properties props = new Properties();
    static {
        try {
            props.load(AtlasProperties.class.getResourceAsStream("/Curated.properties"));
        } catch (IOException e) {
            throw new RuntimeException("Can't read properties file Curated.properties from resources!", e);
        }
    }

    public static String get(String key) {
        return props.getProperty(key) != null ? props.getProperty(key) : key;
    }
    
    public static String getCurated(String key){
    	return props.getProperty("head.ef."+key) != null ? props.getProperty("head.ef."+key) : key;
    }

}
