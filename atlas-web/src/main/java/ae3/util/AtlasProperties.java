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

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

/**
 * Helper wrapper class for properties file
 *
 * @author pashky
 */
public class AtlasProperties {
    private static Properties props = new Properties();

    static {
        try {
            props.load(AtlasProperties.class.getResourceAsStream("/atlas.properties"));
        }
        catch (IOException e) {
            throw new RuntimeException("Can't read properties file atlas.properties from resources!", e);
        }
    }

    public static void setProperty(String key, String newValue) {
        props.setProperty(key, newValue);
    }

    public static String getProperty(String key) {
        return props.getProperty(key) != null ? props.getProperty(key) : "";
    }

    public static List<String> getListProperty(String key) {
        return Arrays.asList(getProperty(key).split(","));
    }

    public static int getIntProperty(String key) {
        try {
            if (key.equals("atlas.last.experiment")) {
                return -1; // fixme: actually read this from DB somewhere
            }
            else {
                return Integer.valueOf(props.getProperty(key));
            }
        }
        catch (Exception e) {
            return 0;
        }
    }

    public static boolean getBoolProperty(String key) {
        try {
            return Boolean.valueOf(props.getProperty(key));
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean hasProperty(String key) {
        return props.containsKey(key)
                && !"".equals(props.getProperty(key))
                && !"no".equals(props.getProperty(key))
                && !"false".equals(props.getProperty(key))
                && !"0".equals(props.getProperty(key));
    }
}
