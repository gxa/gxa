package ae3.util;

import java.io.IOException;
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

    public static String getProperty(String key) {
        return props.getProperty(key) != null ? props.getProperty(key) : "";
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

    public static boolean hasProperty(String key) {
        return props.containsKey(key)
                && !"".equals(props.getProperty(key))
                && !"no".equals(props.getProperty(key))
                && !"false".equals(props.getProperty(key))
                && !"0".equals(props.getProperty(key));
    }
}
