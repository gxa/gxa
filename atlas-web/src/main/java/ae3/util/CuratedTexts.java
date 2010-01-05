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
