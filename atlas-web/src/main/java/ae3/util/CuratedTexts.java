package ae3.util;

import java.util.Properties;
import java.io.IOException;

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

}
